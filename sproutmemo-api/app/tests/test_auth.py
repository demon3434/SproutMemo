import pytest
from app.models.user import User, Student, ParentStudentRelation
from app.models.system import RefreshToken


def test_student_registration(client, db):
    # 1. Register student
    reg_data = {
        "username": "student_test",
        "password": "password123",
        "role": "STUDENT",
        "nickname": "Test Kid",
        "enrollment_year": 2024,
        "semester_offset": 0
    }
    response = client.post("/api/v1/auth/register", json=reg_data)
    assert response.status_code == 200
    json_data = response.json()
    assert json_data["code"] == 200
    assert json_data["msg"] == "注册成功"
    
    data = json_data["data"]
    assert data["username"] == "student_test"
    assert data["role"] == "STUDENT"
    assert data["student_profile"]["enrollment_year"] == 2024
    
    # Verify in DB
    db_user = db.query(User).filter(User.username == "student_test").first()
    assert db_user is not None
    assert db_user.nickname == "Test Kid"
    assert db_user.student_profile is not None
    assert db_user.student_profile.enrollment_year == 2024


def test_parent_registration_and_binding(client, db):
    # Register a student first
    student = User(username="kid_one", password_hash="hash", role="STUDENT")
    db.add(student)
    db.flush()
    student_profile = Student(user_id=student.id, enrollment_year=2023)
    db.add(student_profile)
    db.commit()

    # Register parent binding to kid_one
    parent_reg_data = {
        "username": "parent_test",
        "password": "password123",
        "role": "PARENT",
        "nickname": "Parent Nick",
        "student_ids": [str(student.id)]
    }
    response = client.post("/api/v1/auth/register", json=parent_reg_data)
    assert response.status_code == 200
    json_data = response.json()
    assert json_data["code"] == 200
    
    # Check DB binding
    db_parent = db.query(User).filter(User.username == "parent_test").first()
    assert db_parent is not None
    relations = db.query(ParentStudentRelation).filter(ParentStudentRelation.parent_id == db_parent.id).all()
    assert len(relations) == 1
    assert relations[0].student_id == student.id


def test_login_and_refresh_flow(client, db):
    # Register student
    reg_data = {
        "username": "flow_user",
        "password": "mypassword",
        "role": "STUDENT",
        "enrollment_year": 2024
    }
    client.post("/api/v1/auth/register", json=reg_data)
    
    # 1. Login success
    login_data = {
        "username": "flow_user",
        "password": "mypassword"
    }
    response = client.post("/api/v1/auth/login", json=login_data)
    assert response.status_code == 200
    json_data = response.json()
    assert json_data["code"] == 200
    assert json_data["msg"] == "登录成功"
    
    tokens = json_data["data"]
    access_token = tokens["access_token"]
    refresh_token = tokens["refresh_token"]
    assert access_token is not None
    assert refresh_token is not None
    
    # Verify RefreshToken in DB
    assert db.query(RefreshToken).count() == 1
    
    # 2. Login fail
    login_data_fail = {
        "username": "flow_user",
        "password": "wrongpassword"
    }
    response_fail = client.post("/api/v1/auth/login", json=login_data_fail)
    assert response_fail.status_code == 400
    assert response_fail.json()["code"] == 400

    # 3. Refresh token success
    refresh_payload = {
        "refresh_token": refresh_token
    }
    response_refresh = client.post("/api/v1/auth/refresh", json=refresh_payload)
    assert response_refresh.status_code == 200
    json_refresh = response_refresh.json()
    assert json_refresh["code"] == 200
    
    new_tokens = json_refresh["data"]
    assert new_tokens["access_token"] is not None
    assert new_tokens["refresh_token"] != refresh_token
    
    # Old token should be deleted and rotated in DB
    assert db.query(RefreshToken).count() == 1
