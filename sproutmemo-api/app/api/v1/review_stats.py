import uuid
from datetime import datetime, timedelta, timezone
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from app.core.deps import get_current_user
from app.core.response import make_response
from app.db.session import get_db
from app.models.user import User
from app.models.system import Semester
from app.models.review import ReviewSession, ReviewLog

router = APIRouter()


@router.get("/history", response_model=dict)
def get_review_history(
    student_id: uuid.UUID = Query(None),
    subject_id: uuid.UUID = Query(None),
    year_month: str = Query(None),
    limit: int = Query(20, ge=1, le=100),
    offset: int = Query(0, ge=0),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """获取历史已结转复习任务列表"""
    target_student_id = current_user.id
    if current_user.role == "PARENT":
        if not student_id:
            raise HTTPException(status_code=400, detail="家长身份查询历史记录必须指定 student_id")
        target_student_id = student_id

    # 查已完成（COMPLETED）及被中止（ABANDONED）的会话，并按时间倒序
    query = db.query(ReviewSession).filter(
        ReviewSession.student_id == target_student_id,
        ReviewSession.status.in_(["COMPLETED", "ABANDONED"])
    )
    if subject_id:
        query = query.filter(ReviewSession.subject_id == subject_id)

    if year_month:
        try:
            y, m = map(int, year_month.split("-"))
            import calendar
            _, last_day = calendar.monthrange(y, m)
            start_date = datetime(y, m, 1, 0, 0, 0, tzinfo=timezone.utc)
            end_date = datetime(y, m, last_day, 23, 59, 59, tzinfo=timezone.utc)
            query = query.filter(ReviewSession.created_at.between(start_date, end_date))
        except Exception:
            pass

    total = query.count()
    sessions = query.order_by(ReviewSession.created_at.desc()).offset(offset).limit(limit).all()

    data_list = []
    for s in sessions:
        # 获取关联表名称
        semester_name = s.semester.name if s.semester else "未知学期"
        subject_name = s.subject.subject_name if s.subject else "全部科目"
        strategy_name = s.strategy.name if s.strategy else s.strategy_code

        data_list.append({
            "id": str(s.id),
            "student_id": str(s.student_id),
            "semester_id": str(s.semester_id),
            "semester_name": semester_name,
            "subject_id": str(s.subject_id) if s.subject_id else None,
            "subject_name": subject_name,
            "strategy_code": s.strategy_code,
            "strategy_name": strategy_name,
            "target_count": s.target_count,
            "completed_count": s.completed_count,
            "status": s.status,
            "created_at": s.created_at.isoformat(),
            "completed_at": s.completed_at.isoformat() if s.completed_at else None,
        })

    return make_response(code=200, msg="success", data={"total": total, "items": data_list})


@router.get("/recent-stats", response_model=dict)
def get_recent_review_stats(
    student_id: uuid.UUID = Query(None),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """获取目标学生最近的复习统计数据（包括本周、本月、上月、本学期）"""
    target_student_id = current_user.id
    from app.models.user import Student, ParentStudentRelation
    if current_user.role == "PARENT":
        if not student_id:
            raise HTTPException(status_code=400, detail="家长身份查询统计数据必须指定 student_id")
        # 校验孩子绑定关系
        relation = db.query(ParentStudentRelation).filter(
            ParentStudentRelation.parent_id == current_user.id,
            ParentStudentRelation.student_id == student_id
        ).first()
        if not relation:
            raise HTTPException(status_code=403, detail="无权访问该学生的数据")
        target_student_id = student_id
    else:
        target_student_id = current_user.id

    now = datetime.now(timezone.utc)
    today_start = now.replace(hour=0, minute=0, second=0, microsecond=0)

    # 1. 本周起点 (周一)
    this_week_start = today_start - timedelta(days=today_start.weekday())

    # 2. 本月起点 (1号)
    this_month_start = today_start.replace(day=1)

    # 3. 上月区间
    if today_start.month == 1:
        last_month_start = today_start.replace(year=today_start.year - 1, month=12, day=1)
        last_month_end = today_start.replace(day=1) - timedelta(microseconds=1)
    else:
        last_month_start = today_start.replace(month=today_start.month - 1, day=1)
        last_month_end = today_start.replace(day=1) - timedelta(microseconds=1)

    # 4. 本学期起点
    this_semester_start = None
    student_profile = db.query(Student).filter(Student.user_id == target_student_id).first()
    if student_profile:
        from app.services.semester_service import calculate_grade_semester
        sem_name = calculate_grade_semester(student_profile.enrollment_year, student_profile.semester_offset)
        semester = db.query(Semester).filter(Semester.name == sem_name, Semester.is_deleted == False).first()
        if semester and semester.start_date:
            this_semester_start = semester.start_date

    if not this_semester_start:
        # 兜底推算：如果没有关联学期或其没有配置开始日期，默认推算为最近的 2/1 或 9/1
        if today_start.month >= 9:
            this_semester_start = today_start.replace(month=9, day=1)
        elif today_start.month >= 2:
            this_semester_start = today_start.replace(month=2, day=1)
        else:
            this_semester_start = today_start.replace(year=today_start.year - 1, month=9, day=1)

    def get_stats_in_range(start_date, end_date=None):
        # 统计复习任务
        session_query = db.query(ReviewSession).filter(
            ReviewSession.student_id == target_student_id,
            ReviewSession.created_at >= start_date
        )
        if end_date:
            session_query = session_query.filter(ReviewSession.created_at <= end_date)
        sessions = session_query.all()
        review_count = len(sessions)
        completed_count = sum(1 for s in sessions if s.status == "COMPLETED")
        session_ratio = f"{round((completed_count / review_count) * 100)}%" if review_count > 0 else "0%"

        # 统计刷题数量与正确率
        log_query = db.query(ReviewLog).filter(
            ReviewLog.student_id == target_student_id,
            ReviewLog.created_at >= start_date
        )
        if end_date:
            log_query = log_query.filter(ReviewLog.created_at <= end_date)
        logs = log_query.all()
        total_questions = len(logs)
        correct_questions = sum(1 for l in logs if l.outcome_code == "TOO_EASY")
        correct_ratio = f"{round((correct_questions / total_questions) * 100)}%" if total_questions > 0 else "0%"

        return {
            "review_count": review_count,
            "completed_count": completed_count,
            "session_ratio": session_ratio,
            "total_questions": total_questions,
            "correct_questions": correct_questions,
            "correct_ratio": correct_ratio
        }

    data = [
        {"range": "本周", **get_stats_in_range(this_week_start)},
        {"range": "本月", **get_stats_in_range(this_month_start)},
        {"range": "上月", **get_stats_in_range(last_month_start, last_month_end)},
        {"range": "本学期", **get_stats_in_range(this_semester_start)}
    ]

    return make_response(code=200, msg="success", data=data)

