import pytest
import uuid
from app.models.user import User, Student, ParentStudentRelation
from app.models.system import Semester, Subject, SemesterSubject, QuestionSource
from app.core.security import get_password_hash


@pytest.fixture
def auth_headers(client, db):
    pwd_hash = get_password_hash("password123")
    
    # Create student
    student = User(username="student_books", password_hash=pwd_hash, role="STUDENT")
    db.add(student)
    db.flush()
    db.add(Student(user_id=student.id, enrollment_year=2023, semester_offset=0))
    
    # Create parent
    parent = User(username="parent_books", password_hash=pwd_hash, role="PARENT")
    db.add(parent)
    db.flush()
    db.add(ParentStudentRelation(parent_id=parent.id, student_id=student.id))
    
    # Create stranger student
    stranger = User(username="stranger_books", password_hash=pwd_hash, role="STUDENT")
    db.add(stranger)
    db.flush()
    db.add(Student(user_id=stranger.id, enrollment_year=2023, semester_offset=0))

    # Create semester & subject
    semester = Semester(name="小三下", is_active=True)
    subject = Subject(subject_name="数学")
    db.add(semester)
    db.add(subject)
    db.flush()

    # Link student to subject for this semester globally
    assoc = SemesterSubject(semester_id=semester.id, subject_id=subject.id, is_active=True)
    db.add(assoc)

    db.commit()

    # Generate login tokens
    # Login student
    login_res = client.post("/api/v1/auth/login", json={"username": "student_books", "password": "password123"})
    student_token = login_res.json()["data"]["access_token"]

    # Login parent
    login_res_p = client.post("/api/v1/auth/login", json={"username": "parent_books", "password": "password123"})
    parent_token = login_res_p.json()["data"]["access_token"]
    
    # Login stranger
    login_res_s = client.post("/api/v1/auth/login", json={"username": "stranger_books", "password": "password123"})
    stranger_token = login_res_s.json()["data"]["access_token"]

    return {
        "student": {"Authorization": f"Bearer {student_token}"},
        "parent": {"Authorization": f"Bearer {parent_token}"},
        "stranger": {"Authorization": f"Bearer {stranger_token}"},
        "student_id": student.id,
        "stranger_id": stranger.id,
        "semester_id": semester.id,
        "subject_id": subject.id,
    }


def test_create_question_source_student(client, db, auth_headers):
    # Student creates a question source
    payload = {
        "semester_id": str(auth_headers["semester_id"]),
        "subject_id": str(auth_headers["subject_id"]),
        "name": "数学五年高考三年模拟"
    }
    response = client.post("/api/v1/source_books", json=payload, headers=auth_headers["student"])
    assert response.status_code == 200
    data = response.json()["data"]
    assert data["name"] == "数学五年高考三年模拟"
    assert data["student_id"] == str(auth_headers["student_id"])
    assert data["semester_id"] == str(auth_headers["semester_id"])
    assert data["subject_id"] == str(auth_headers["subject_id"])


def test_create_question_source_parent(client, db, auth_headers):
    # Parent creates source for bound student
    payload = {
        "student_id": str(auth_headers["student_id"]),
        "semester_id": str(auth_headers["semester_id"]),
        "subject_id": str(auth_headers["subject_id"]),
        "name": "语文课外阅读"
    }
    response = client.post("/api/v1/source_books", json=payload, headers=auth_headers["parent"])
    assert response.status_code == 200
    assert response.json()["code"] == 200

    # Parent creates source for stranger -> 403 Forbidden
    payload_stranger = {
        "student_id": str(auth_headers["stranger_id"]),
        "semester_id": str(auth_headers["semester_id"]),
        "subject_id": str(auth_headers["subject_id"]),
        "name": "偷看别人书",
    }
    response_fail = client.post("/api/v1/source_books", json=payload_stranger, headers=auth_headers["parent"])
    assert response_fail.status_code == 403


def test_list_and_update_question_source(client, db, auth_headers):
    # Setup: create source
    source = QuestionSource(
        student_id=auth_headers["student_id"],
        semester_id=auth_headers["semester_id"],
        subject_id=auth_headers["subject_id"],
        name="英语口语周报",
        is_active=True
    )
    db.add(source)
    db.commit()

    # 1. Student lists sources
    response = client.get("/api/v1/source_books", headers=auth_headers["student"])
    assert response.status_code == 200
    items = response.json()["data"]["items"]
    assert len(items) == 1
    assert items[0]["name"] == "英语口语周报"

    # 2. Stranger lists sources (should see 0, since they can only see their own)
    response_stranger = client.get("/api/v1/source_books", headers=auth_headers["stranger"])
    assert len(response_stranger.json()["data"]["items"]) == 0

    # 3. Parent lists sources by student_id
    response_parent = client.get(
        f"/api/v1/source_books?student_id={auth_headers['student_id']}",
        headers=auth_headers["parent"]
    )
    assert len(response_parent.json()["data"]["items"]) == 1

    # 4. Update and archive source (setting is_active=False)
    update_payload = {
        "name": "新英语周报",
        "is_active": False
    }
    res_update = client.put(f"/api/v1/source_books/{source.id}", json=update_payload, headers=auth_headers["student"])
    assert res_update.status_code == 200
    assert res_update.json()["data"]["is_active"] is False
    assert res_update.json()["data"]["name"] == "新英语周报"

    # 5. List only active should return 0 now
    response_after = client.get("/api/v1/source_books?only_active=true", headers=auth_headers["student"])
    assert len(response_after.json()["data"]["items"]) == 0


def test_student_cannot_create_for_another_student(client, db, auth_headers):
    # Student attempts to pass stranger's student_id in payload
    payload = {
        "student_id": str(auth_headers["stranger_id"]),
        "semester_id": str(auth_headers["semester_id"]),
        "subject_id": str(auth_headers["subject_id"]),
        "name": "数学易错题集"
    }
    response = client.post("/api/v1/source_books", json=payload, headers=auth_headers["student"])
    assert response.status_code == 200
    data = response.json()["data"]
    # Verified: strictly bound to the authenticated student's own ID
    assert data["student_id"] == str(auth_headers["student_id"])
    assert data["student_id"] != str(auth_headers["stranger_id"])


def test_mutual_parent_student_collaboration(client, db, auth_headers):
    # 1. Student creates a book
    payload = {
        "semester_id": str(auth_headers["semester_id"]),
        "subject_id": str(auth_headers["subject_id"]),
        "name": "孩子自建练习册"
    }
    create_res = client.post("/api/v1/source_books", json=payload, headers=auth_headers["student"])
    assert create_res.status_code == 200
    book_id = create_res.json()["data"]["id"]

    # 2. Parent renames the book created by student
    update_res = client.put(
        f"/api/v1/source_books/{book_id}",
        json={"name": "家长协同修改名称", "is_active": True},
        headers=auth_headers["parent"]
    )
    assert update_res.status_code == 200
    assert update_res.json()["data"]["name"] == "家长协同修改名称"

    # 3. Stranger attempts to modify the book -> 403 Forbidden
    stranger_res = client.put(
        f"/api/v1/source_books/{book_id}",
        json={"name": "陌生人盗改"},
        headers=auth_headers["stranger"]
    )
    assert stranger_res.status_code == 403

    # 4. Student toggles active state of the book
    toggle_res = client.put(
        f"/api/v1/source_books/{book_id}",
        json={"is_active": False},
        headers=auth_headers["student"]
    )
    assert toggle_res.status_code == 200
    assert toggle_res.json()["data"]["is_active"] is False

    # 5. Parent deletes the book
    del_res = client.delete(f"/api/v1/source_books/{book_id}", headers=auth_headers["parent"])
    assert del_res.status_code == 200

