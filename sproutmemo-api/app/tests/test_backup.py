import io
import os
import tarfile
import pytest
from app.models.user import User, Student
from app.models.note import Note, Attachment
from app.models.system import Semester, Subject, SemesterSubject
from app.core.config import settings


@pytest.fixture
def admin_headers(client, db):
    from app.core.security import get_password_hash
    pwd_hash = get_password_hash("adminpassword")
    
    admin = User(username="admin_backup", password_hash=pwd_hash, role="ADMIN")
    db.add(admin)
    db.commit()

    # Login admin
    login_res = client.post("/api/v1/auth/login", json={"username": "admin_backup", "password": "adminpassword"})
    admin_token = login_res.json()["data"]["access_token"]
    return {"Authorization": f"Bearer {admin_token}"}


def test_backup_and_restore_workflow(client, db, admin_headers):
    # 1. Setup mock data: student and a note with an attachment
    from app.core.security import get_password_hash
    student_pwd = get_password_hash("pass123")
    student = User(username="student_backup", password_hash=student_pwd, role="STUDENT")
    db.add(student)
    db.flush()
    db.add(Student(user_id=student.id, enrollment_year=2024, semester_offset=0))
    
    semester = Semester(name="小二上", is_active=True)
    subject = Subject(subject_name="语文")
    db.add(semester)
    db.add(subject)
    db.flush()

    assoc = SemesterSubject(semester_id=semester.id, subject_id=subject.id, is_active=True)
    db.add(assoc)
    db.commit()

    # Log in student and create a note
    login_res = client.post("/api/v1/auth/login", json={"username": "student_backup", "password": "pass123"})
    student_token = login_res.json()["data"]["access_token"]
    student_headers = {"Authorization": f"Bearer {student_token}"}

    # Create dummy upload
    dummy_img = io.BytesIO(b"super secret image data")
    dummy_img.name = "hidden.jpg"

    form_data = {
        "semester_id": str(semester.id),
        "subject_id": str(subject.id),
        "mastery_level": "CLUELESS",
        "student_id": str(student.id)
    }
    
    files = [("images", (dummy_img.name, dummy_img, "image/jpeg"))]

    create_res = client.post("/api/v1/notes", data=form_data, files=files, headers=student_headers)
    assert create_res.status_code == 200
    note_id = create_res.json()["data"]["id"]
    att_path = create_res.json()["data"]["attachments"][0]["storage_path"]
    
    import re
    # Verify new date-partitioned storage path format YYYY/MM/DD/uuid.ext
    assert re.match(r"^\d{4}/\d{2}/\d{2}/[0-9a-f]{32}\.jpg$", att_path)
    
    # Verify file exists on disk
    full_att_path = os.path.join(settings.UPLOAD_DIR, att_path)
    assert os.path.exists(full_att_path)

    # 2. Trigger Admin Backup
    backup_response = client.get("/api/v1/system/backup", headers=admin_headers)
    assert backup_response.status_code == 200
    assert backup_response.headers["content-type"] == "application/x-tar"
    backup_tar_bytes = backup_response.content

    # Verify tar contents
    tar_io = io.BytesIO(backup_tar_bytes)
    with tarfile.open(fileobj=tar_io, mode="r") as tar:
        names = tar.getnames()
        assert "backup.sql" in names
        # Path should contain our uploaded file
        has_file = any(att_path in name for name in names)
        assert has_file or "uploads" in names

    # 3. Simulate disaster: delete note and file
    db.query(Attachment).delete()
    db.query(Note).delete()
    db.commit()
    
    # Delete upload folder
    if os.path.exists(full_att_path):
        os.remove(full_att_path)
    assert not os.path.exists(full_att_path)
    assert db.query(Note).count() == 0

    # 4. Trigger Admin Restore
    restore_file = io.BytesIO(backup_tar_bytes)
    restore_file.name = "backup.tar"
    
    restore_files = {"file": (restore_file.name, restore_file, "application/x-tar")}
    restore_response = client.post("/api/v1/system/restore", files=restore_files, headers=admin_headers)
    assert restore_response.status_code == 200
    assert restore_response.json()["code"] == 200

    # 5. Verify restored state
    assert os.path.exists(full_att_path)
    with open(full_att_path, "rb") as f:
        content = f.read()
        assert content == b"super secret image data"

    # Clean up physical file after test
    if os.path.exists(full_att_path):
        os.remove(full_att_path)
