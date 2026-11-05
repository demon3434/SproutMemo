import pytest
import uuid
from app.models.note import Note
from app.models.review import ReviewSession, ReviewLog
from app.models.user import User, Student
from app.models.system import Semester, Subject, SemesterSubject
from app.core.security import get_password_hash


@pytest.fixture
def auth_headers(client, db):
    # Create student
    pwd_hash = get_password_hash("password123")
    student = User(username="student_review", password_hash=pwd_hash, role="STUDENT")
    db.add(student)
    db.flush()
    db.add(Student(user_id=student.id, enrollment_year=2024, semester_offset=0))

    # Create semester & subject
    semester = Semester(name="三年级上学期", is_active=True)
    subject = Subject(subject_name="数学")
    db.add(semester)
    db.add(subject)
    db.flush()

    assoc = SemesterSubject(semester_id=semester.id, subject_id=subject.id, is_active=True)
    db.add(assoc)

    # Seed review dimensions
    from app.main import seed_review_metadata
    seed_review_metadata(db)

    db.commit()

    # Login student
    login_res = client.post("/api/v1/auth/login", json={"username": "student_review", "password": "password123"})
    student_token = login_res.json()["data"]["access_token"]

    return {
        "headers": {"Authorization": f"Bearer {student_token}"},
        "student": student,
        "semester": semester,
        "subject": subject,
    }


def test_get_review_presets(client, auth_headers):
    response = client.get("/api/v1/review/presets", headers=auth_headers["headers"])
    assert response.status_code == 200
    res = response.json()
    assert res["code"] == 200
    data = res["data"]
    assert "strategies" in data
    assert "count_presets" in data
    assert "outcomes" in data

    strategy_codes = [s["code"] for s in data["strategies"]]
    assert "RANDOM" in strategy_codes
    assert "EBBINGHAUS" in strategy_codes
    assert "CHALLENGE" in strategy_codes
    assert "PERSISTENT_FAILURE" in strategy_codes


def test_create_and_submit_review_session(client, db, auth_headers):
    student = auth_headers["student"]
    semester = auth_headers["semester"]
    subject = auth_headers["subject"]

    # 1. 创建 2 道可复习笔记，1 道已排除复习笔记
    n1 = Note(
        student_id=student.id,
        semester_id=semester.id,
        subject_id=subject.id,
        unit_code=1,
        auxiliary_text="复习测试题 1",
        difficulty_stars=3.0,
        mastery_level="CLUELESS",
        exclude_from_review=False
    )
    n2 = Note(
        student_id=student.id,
        semester_id=semester.id,
        subject_id=subject.id,
        unit_code=1,
        auxiliary_text="复习测试题 2",
        difficulty_stars=4.5,
        mastery_level="PARTIAL",
        exclude_from_review=False
    )
    n_excluded = Note(
        student_id=student.id,
        semester_id=semester.id,
        subject_id=subject.id,
        unit_code=1,
        auxiliary_text="已移出的测试题",
        difficulty_stars=2.0,
        mastery_level="MASTERED",
        exclude_from_review=True
    )
    db.add_all([n1, n2, n_excluded])
    db.commit()

    headers = auth_headers["headers"]

    # 2. 生成复习会话
    payload = {
        "semester_id": str(semester.id),
        "subject_id": str(subject.id),
        "strategy_code": "EBBINGHAUS",
        "count_preset_code": "PRESET_5"
    }
    response = client.post("/api/v1/review/session", json=payload, headers=headers)
    assert response.status_code == 200
    res = response.json()
    assert res["code"] == 200
    session_data = res["data"]
    session_id = session_data["id"]

    # 校验 excluded 笔记不包含在内，target_count 为 2
    assert session_data["target_count"] == 2
    note_ids = [n["id"] for n in session_data["notes"]]
    assert str(n_excluded.id) not in note_ids
    assert str(n1.id) in note_ids

    # 3. 提交单题打卡回单
    submit_payload = {
        "note_id": str(n1.id),
        "duration_seconds": 15.5,
        "outcome_code": "TOO_EASY",
        "new_mastery": "MASTERED",
        "new_stars": 4.0,
        "exclude_from_review": False,
        "sequence_index": 1
    }
    sub_res = client.post(f"/api/v1/review/session/{session_id}/submit", json=submit_payload, headers=headers)
    assert sub_res.status_code == 200
    sub_data = sub_res.json()["data"]
    assert sub_data["completed_count"] == 1
    assert sub_data["is_session_completed"] is False

    # 4. 验证 Note 属性更新及 ReviewLog 持久化
    db.refresh(n1)
    assert n1.mastery_level == "MASTERED"
    assert float(n1.difficulty_stars) == 4.0
    assert n1.last_reviewed_at is not None

    log = db.query(ReviewLog).filter(ReviewLog.session_id == uuid.UUID(session_id), ReviewLog.note_id == n1.id).first()
    assert log is not None
    assert log.duration_seconds == 15.5
    assert log.outcome_code == "TOO_EASY"


def test_preview_review_notes(client, db, auth_headers):
    student = auth_headers["student"]
    semester = auth_headers["semester"]
    subject = auth_headers["subject"]

    # 新建题
    n = Note(
        student_id=student.id,
        semester_id=semester.id,
        subject_id=subject.id,
        unit_code=1,
        auxiliary_text="新测试题",
        difficulty_stars=3.0,
        mastery_level="CLUELESS",
        exclude_from_review=False
    )
    db.add(n)
    db.commit()

    headers = auth_headers["headers"]
    response = client.get(
        f"/api/v1/review/preview?semester_id={semester.id}&subject_id={subject.id}&strategy_code=RANDOM",
        headers=headers
    )
    assert response.status_code == 200
    res = response.json()
    assert res["code"] == 200
    assert len(res["data"]) >= 1


def test_active_sessions_and_abandon(client, db, auth_headers):
    student = auth_headers["student"]
    semester = auth_headers["semester"]
    subject = auth_headers["subject"]

    # 1. 创建题
    n = Note(
        student_id=student.id,
        semester_id=semester.id,
        subject_id=subject.id,
        unit_code=1,
        auxiliary_text="测试题",
        difficulty_stars=3.0,
        mastery_level="CLUELESS",
        exclude_from_review=False
    )
    db.add(n)
    db.commit()

    headers = auth_headers["headers"]

    # 2. 生成进行中的会话
    payload = {
        "semester_id": str(semester.id),
        "subject_id": str(subject.id),
        "strategy_code": "RANDOM",
        "count_preset_code": "PRESET_5"
    }
    client.post("/api/v1/review/session", json=payload, headers=headers)

    # 3. 探测进行中会话
    active_res = client.get("/api/v1/review/active-sessions", headers=headers)
    assert active_res.status_code == 200
    active_data = active_res.json()["data"]
    assert active_data is not None
    assert active_data["status"] == "IN_PROGRESS"
    session_id = active_data["id"]

    # 4. 中止会话
    abandon_res = client.post(f"/api/v1/review/session/{session_id}/abandon", headers=headers)
    assert abandon_res.status_code == 200
    assert abandon_res.json()["data"]["status"] == "ABANDONED"

    # 5. 再次探测，应该没有进行中的会话
    active_res_2 = client.get("/api/v1/review/active-sessions", headers=headers)
    assert active_res_2.json()["data"] is None


def test_review_history_and_details(client, db, auth_headers):
    student = auth_headers["student"]
    semester = auth_headers["semester"]
    subject = auth_headers["subject"]

    headers = auth_headers["headers"]

    # 查询历史记录，初始应该为空
    history_res = client.get("/api/v1/review/history", headers=headers)
    assert history_res.status_code == 200
    assert len(history_res.json()["data"]["items"]) == 0

