import io
import pytest
import uuid
from datetime import datetime, timezone
from app.models.user import User, Student
from app.models.note import Note, Attachment
from app.models.system import Semester, Subject, SemesterSubject, QuestionSource
from app.core.security import get_password_hash


@pytest.fixture
def auth_headers(client, db):
    # Create student
    pwd_hash = get_password_hash("password123")
    
    student = User(username="student_notes", password_hash=pwd_hash, role="STUDENT")
    db.add(student)
    db.flush()
    db.add(Student(user_id=student.id, enrollment_year=2024, semester_offset=0))
    
    # Create semester & subject
    semester = Semester(name="小二上", is_active=True)
    subject = Subject(subject_name="数学")
    db.add(semester)
    db.add(subject)
    db.flush()

    # Link student to subject for this semester
    assoc = SemesterSubject(semester_id=semester.id, subject_id=subject.id, is_active=True)
    db.add(assoc)
    
    db.commit()

    # Login student
    login_res = client.post("/api/v1/auth/login", json={"username": "student_notes", "password": "password123"})
    student_token = login_res.json()["data"]["access_token"]

    return {
        "student": {"Authorization": f"Bearer {student_token}"},
        "student_id": student.id,
        "semester_id": semester.id,
        "subject_id": subject.id,
    }


def test_notes_crud_and_concurrency(client, db, auth_headers):
    # 1. Create question source first
    source = QuestionSource(
        student_id=auth_headers["student_id"],
        semester_id=auth_headers["semester_id"],
        subject_id=auth_headers["subject_id"],
        name="数学口算题"
    )
    db.add(source)
    db.commit()

    # Create dummy file bytes for upload
    dummy_img = io.BytesIO(b"dummy image content")
    dummy_img.name = "test_image.png"

    dummy_aud = io.BytesIO(b"dummy audio content")
    dummy_aud.name = "test_audio.mp3"

    # 2. Create Note
    form_data = {
        "semester_id": str(auth_headers["semester_id"]),
        "subject_id": str(auth_headers["subject_id"]),
        "question_source_id": str(source.id),
        "source_paper_name": "单元试卷一",
        "unit_name": "第一单元",
        "page_number": "第12页",
        "auxiliary_text": "<p>求 x 的值</p>",
        "difficulty_stars": 3.5,
        "mastery_level": "PARTIAL",
        "exclude_from_review": "false",
        "student_id": str(auth_headers["student_id"])
    }

    files = [
        ("images", (dummy_img.name, dummy_img, "image/png")),
        ("audios", (dummy_aud.name, dummy_aud, "audio/mp3"))
    ]

    response = client.post(
        "/api/v1/notes",
        data=form_data,
        files=files,
        headers=auth_headers["student"]
    )
    assert response.status_code == 200
    res_data = response.json()["data"]
    note_id = res_data["id"]
    
    # Confirm DB values and HTML sanitation
    assert res_data["auxiliary_text"] == "<p>求 x 的值</p>"  # nh3 should allow basic tags
    assert len(res_data["attachments"]) == 2
    
    import re
    # Verify new date-partitioned storage path format YYYY/MM/DD/uuid.ext
    assert res_data["attachments"][0]["file_type"] == "IMAGE"
    assert re.match(r"^\d{4}/\d{2}/\d{2}/[0-9a-f]{32}\.png$", res_data["attachments"][0]["storage_path"])
    
    assert res_data["attachments"][1]["file_type"] == "AUDIO"
    assert re.match(r"^\d{4}/\d{2}/\d{2}/[0-9a-f]{32}\.mp3$", res_data["attachments"][1]["storage_path"])
    
    assert res_data["semester_id"] == str(auth_headers["semester_id"])
    assert res_data["subject_id"] == str(auth_headers["subject_id"])
    assert res_data["question_source_id"] == str(source.id)

    # Keep track of updated_at and sync_base_time for concurrency checks
    updated_at_str = res_data["updated_at"]

    # 3. Retrieve note details
    response_details = client.get(f"/api/v1/notes/{note_id}", headers=auth_headers["student"])
    assert response_details.status_code == 200
    assert response_details.json()["data"]["difficulty_stars"] == 3.5

    # 4. Perform an update (success case, sync_base_time matches updated_at)
    update_form = {
        "difficulty_stars": 4.5,
        "sync_base_time": updated_at_str,
        "force": "false"
      }
    response_update = client.put(
        f"/api/v1/notes/{note_id}",
        data=update_form,
        headers=auth_headers["student"]
    )
    assert response_update.status_code == 200
    new_data = response_update.json()["data"]
    assert new_data["difficulty_stars"] == 4.5
    
    # Capture new update time
    new_updated_at_str = new_data["updated_at"]

    # 5. Perform a concurrent update (fail case, using outdated sync_base_time)
    outdated_update_form = {
        "difficulty_stars": 5.0,
        "sync_base_time": updated_at_str,  # Old sync time before the first update!
        "force": "false"
    }
    response_conflict = client.put(
        f"/api/v1/notes/{note_id}",
        data=outdated_update_form,
        headers=auth_headers["student"]
    )
    assert response_conflict.status_code == 409
    assert response_conflict.json()["code"] == 409
    assert "force=true" in response_conflict.json()["msg"]

    # 6. Force override concurrent update (success case, using force=true)
    force_update_form = {
        "difficulty_stars": 5.0,
        "sync_base_time": updated_at_str,  # Old sync time
        "force": "true"
    }
    response_force = client.put(
        f"/api/v1/notes/{note_id}",
        data=force_update_form,
        headers=auth_headers["student"]
    )
    assert response_force.status_code == 200
    assert response_force.json()["data"]["difficulty_stars"] == 5.0

    # 6.5. Persistent rotation of an existing IMAGE attachment in View Mode
    img_att_id = res_data["attachments"][0]["id"]
    old_storage_path = res_data["attachments"][0]["storage_path"]
    response_rotate = client.post(
        f"/api/v1/notes/{note_id}/attachments/{img_att_id}/rotate",
        json={"angle": 90},
        headers=auth_headers["student"]
    )
    assert response_rotate.status_code == 200
    rotated_note_data = response_rotate.json()["data"]
    rotated_img_att = next(a for a in rotated_note_data["attachments"] if a["id"] == img_att_id)
    assert rotated_img_att["storage_path"] != old_storage_path
    assert re.match(r"^\d{4}/\d{2}/\d{2}/[0-9a-f]{32}\.png$", rotated_img_att["storage_path"])

    # 6.6. Update note changing semester_id + source_paper_name + attachment_metadata rotation
    import json
    sem2 = Semester(name="小二下", is_active=True)
    db.add(sem2)
    db.flush()
    db.add(SemesterSubject(semester_id=sem2.id, subject_id=auth_headers["subject_id"], is_active=True))
    db.commit()

    meta_json = json.dumps([{"id": img_att_id, "title": "旋转后图片", "sort_order": 0, "rotation": 90}])
    response_sem_change = client.put(
        f"/api/v1/notes/{note_id}",
        data={
            "semester_id": str(sem2.id),
            "subject_id": str(auth_headers["subject_id"]),
            "source_paper_name": "期末综合卷",
            "paper_category": "DAILY_HW",
            "attachment_metadata": meta_json,
            "sync_base_time": rotated_note_data["updated_at"],
            "force": "true"
        },
        headers=auth_headers["student"]
    )
    assert response_sem_change.status_code == 200
    sem_changed_data = response_sem_change.json()["data"]
    assert sem_changed_data["semester_id"] == str(sem2.id)
    assert sem_changed_data["question_source_name"] == "期末综合卷"

    # Restore original semester & source so step 7 filter check succeeds
    client.put(
        f"/api/v1/notes/{note_id}",
        data={
            "semester_id": str(auth_headers["semester_id"]),
            "subject_id": str(auth_headers["subject_id"]),
            "question_source_id": str(source.id),
            "sync_base_time": sem_changed_data["updated_at"],
            "force": "true"
        },
        headers=auth_headers["student"]
    )

    # 7. List notes with filters
    response_list = client.get(
        f"/api/v1/notes?subject_id={auth_headers['subject_id']}&question_source_id={source.id}",
        headers=auth_headers["student"]
    )
    assert response_list.status_code == 200
    list_items = response_list.json()["data"]["items"]
    assert len(list_items) == 1

    # 8. Soft Delete Note
    response_delete = client.delete(f"/api/v1/notes/{note_id}", headers=auth_headers["student"])
    assert response_delete.status_code == 200
    
    # Verify indeed soft deleted (not visible in list)
    response_list_empty = client.get("/api/v1/notes", headers=auth_headers["student"])
    assert len(response_list_empty.json()["data"]["items"]) == 0
