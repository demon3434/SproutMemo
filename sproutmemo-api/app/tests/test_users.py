import uuid
from app.models.user import User, Student, ParentStudentRelation
from app.models.note import Note
from app.core.security import get_password_hash
from app.core.auth_utils import create_access_token


def get_admin_auth_header(db):
    admin = db.query(User).filter(User.username == "admin_test").first()
    if not admin:
        admin = User(
            username="admin_test",
            password_hash=get_password_hash("admin123"),
            role="ADMIN",
            nickname="Test Admin",
            is_active=True
        )
        db.add(admin)
        db.commit()
        db.refresh(admin)
    token = create_access_token(admin.id, admin.username, admin.role)
    return {"Authorization": f"Bearer {token}"}


def test_admin_cannot_be_modified(client, db):
    headers = get_admin_auth_header(db)
    
    # Create built-in admin
    admin = User(
        username="admin",
        password_hash=get_password_hash("admin123"),
        role="ADMIN",
        nickname="Admin",
        is_active=True
    )
    db.add(admin)
    db.commit()
    db.refresh(admin)

    res = client.put(f"/api/v1/users/{admin.id}", json={"nickname": "New Admin"}, headers=headers)
    assert res.status_code == 400
    assert "禁止修改" in res.json()["msg"]


def test_parent_child_relations_and_query(client, db):
    headers = get_admin_auth_header(db)

    # 1. Create a student
    student = User(username="child1", password_hash="123", role="STUDENT", nickname="Child 1", is_active=True)
    db.add(student)
    db.flush()
    db.add(Student(user_id=student.id, enrollment_year=2024, semester_offset=0))
    db.commit()

    # 2. Create a parent with student_ids
    res = client.post("/api/v1/users", json={
        "username": "parent1",
        "password": "pwd",
        "nickname": "Parent 1",
        "role": "PARENT",
        "student_ids": [str(student.id)]
    }, headers=headers)
    assert res.status_code == 200
    parent_data = res.json()["data"]
    assert parent_data["student_ids"] == [str(student.id)]

    # 3. Query users list, verify parent has student_ids
    res_list = client.get("/api/v1/users?role=PARENT", headers=headers)
    assert res_list.status_code == 200
    items = res_list.json()["data"]["items"]
    matched = [u for u in items if u["username"] == "parent1"]
    assert len(matched) == 1
    assert matched[0]["student_ids"] == [str(student.id)]


def test_role_switching_and_safety(client, db):
    headers = get_admin_auth_header(db)

    # 1. Create clean student
    stu = User(username="clean_student", password_hash="123", role="STUDENT", nickname="Clean Student", is_active=True)
    db.add(stu)
    db.flush()
    db.add(Student(user_id=stu.id, enrollment_year=2023, semester_offset=1))
    db.commit()

    # Switch to PARENT
    res = client.put(f"/api/v1/users/{stu.id}", json={
        "role": "PARENT",
        "nickname": "Now Parent"
    }, headers=headers)
    assert res.status_code == 200
    assert res.json()["data"]["role"] == "PARENT"
    assert res.json()["data"]["student_profile"] is None

    # Switch back to STUDENT
    res2 = client.put(f"/api/v1/users/{stu.id}", json={
        "role": "STUDENT",
        "enrollment_year": 2025,
        "semester_offset": 0
    }, headers=headers)
    assert res2.status_code == 200
    assert res2.json()["data"]["role"] == "STUDENT"
    assert res2.json()["data"]["student_profile"]["enrollment_year"] == 2025

    # Now add a dummy note for student to test safety check
    # Create required system dimensions for note
    from app.models.system import Semester, Subject
    semester = db.query(Semester).first()
    if not semester:
        semester = Semester(name="2025年秋季")
        db.add(semester)
        db.flush()
    subject = db.query(Subject).first()
    if not subject:
        subject = Subject(subject_name="数学")
        db.add(subject)
        db.flush()

    from app.models.system import SystemUnit
    unit = db.query(SystemUnit).filter(SystemUnit.id == 999).first()
    if not unit:
        unit = SystemUnit(id=999, name="综合/未分类", unit_type="COMPREHENSIVE")
        db.add(unit)
        db.flush()

    note = Note(
        student_id=stu.id,
        semester_id=semester.id,
        subject_id=subject.id,
        unit_code=999,
        mastery_level="MASTERED",
        difficulty_stars=1.0
    )
    db.add(note)
    db.commit()

    # Now attempt to switch role to PARENT while having notes -> must fail with 400
    res_fail = client.put(f"/api/v1/users/{stu.id}", json={
        "role": "PARENT"
    }, headers=headers)
    assert res_fail.status_code == 400
    assert "存在错题数据" in res_fail.json()["msg"]
