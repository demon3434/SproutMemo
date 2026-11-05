import uuid
from datetime import datetime, timezone
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from app.core.deps import get_current_user
from app.core.response import make_response
from app.db.session import get_db
from app.models.user import User
from app.models.note import Note
from app.models.system import Stage, Semester
from app.models.review import (
    ReviewStrategy,
    ReviewCountPreset,
    ReviewOutcome,
    ReviewSession,
    ReviewLog,
    ReviewSessionNote,
)
from app.schemas.review import (
    CreateReviewSessionRequest,
    SubmitReviewItemRequest,
)
from app.schemas.note import NoteOut
from app.api.v1.notes import build_note_out
from app.services.review_service import get_review_notes

router = APIRouter(prefix="/review", tags=["Review"])


@router.get("/presets", response_model=dict)
def get_review_presets(
    student_id: uuid.UUID = Query(None),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """获取复习配置的数据字典以及目标学生的错题库概览统计数据"""
    strategies = db.query(ReviewStrategy).order_by(ReviewStrategy.sort_order).all()
    count_presets = db.query(ReviewCountPreset).order_by(ReviewCountPreset.sort_order).all()
    outcomes = db.query(ReviewOutcome).order_by(ReviewOutcome.sort_order).all()
    stages = db.query(Stage).filter(Stage.is_deleted == False, Stage.is_active == True).order_by(Stage.sort_order).all()
    semesters = db.query(Semester).filter(Semester.is_deleted == False).order_by(Semester.sort_order).all()

    # 确定目标学生
    target_student_id = current_user.id
    if current_user.role == "PARENT":
        if student_id:
            target_student_id = student_id
        else:
            from app.models.user import ParentStudentRelation
            relation = db.query(ParentStudentRelation).filter(ParentStudentRelation.parent_id == current_user.id).first()
            if relation:
                target_student_id = relation.student_id

    # 统计错题库数据
    notes = db.query(Note).filter(Note.student_id == target_student_id, Note.is_deleted == False).all()
    total_count = len(notes)
    review_pool_count = sum(1 for n in notes if not n.exclude_from_review)
    
    clueless_count = sum(1 for n in notes if not n.exclude_from_review and n.mastery_level == 'CLUELESS')
    partial_count = sum(1 for n in notes if not n.exclude_from_review and n.mastery_level == 'PARTIAL')
    mastered_count = sum(1 for n in notes if not n.exclude_from_review and n.mastery_level == 'MASTERED')

    subject_dist = {}
    for n in notes:
        if not n.exclude_from_review:
            sub_name = n.subject.subject_name if n.subject else "其它"
            subject_dist[sub_name] = subject_dist.get(sub_name, 0) + 1

    # 获取今日已完成复习题目数
    today_start = datetime.now(timezone.utc).replace(hour=0, minute=0, second=0, microsecond=0)
    today_completed_notes = db.query(ReviewLog).filter(
        ReviewLog.created_at >= today_start
    ).join(ReviewSession, ReviewSession.id == ReviewLog.session_id).filter(
        ReviewSession.student_id == target_student_id
    ).count()

    stats = {
        "total_notes": total_count,
        "review_pool_notes": review_pool_count,
        "mastery_clueless": clueless_count,
        "mastery_partial": partial_count,
        "mastery_mastered": mastered_count,
        "today_completed_notes": today_completed_notes,
        "subject_distribution": [
            {"subject_name": k, "count": v} for k, v in subject_dist.items()
        ]
    }

    data = {
        "strategies": [
            {
                "code": s.code,
                "name": s.name,
                "description": s.description,
                "sort_order": s.sort_order,
            }
            for s in strategies
        ],
        "count_presets": [
            {
                "code": p.code,
                "count_value": p.count_value,
                "name": p.name,
                "sort_order": p.sort_order,
            }
            for p in count_presets
        ],
        "outcomes": [
            {
                "code": o.code,
                "name": o.name,
                "color_hex": o.color_hex,
                "suggested_mastery": o.suggested_mastery,
                "sort_order": o.sort_order,
            }
            for o in outcomes
        ],
        "stages": [
            {
                "id": str(st.id),
                "name": st.name,
                "sort_order": st.sort_order,
            }
            for st in stages
        ],
        "semesters": [
            {
                "id": str(sem.id),
                "name": sem.name,
                "stage_id": str(sem.stage_id) if sem.stage_id else None,
            }
            for sem in semesters
        ],
        "stats": stats
    }
    return make_response(code=200, msg="success", data=data)


@router.get("/preview", response_model=dict)
def preview_review_notes(
    semester_id: uuid.UUID = Query(...),
    subject_id: uuid.UUID = Query(None),
    unit_code: int = Query(None),
    strategy_code: str = Query("EBBINGHAUS"),
    count_preset_code: str = Query("PRESET_5"),
    student_id: uuid.UUID = Query(None),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """仅生成复习的题目预览列表，不在数据库记录会话"""
    target_student_id = current_user.id
    if current_user.role == "PARENT":
        if not student_id:
            raise HTTPException(status_code=400, detail="家长身份预览复习会话必须指定 student_id")
        target_student_id = student_id

    preset = db.query(ReviewCountPreset).filter(ReviewCountPreset.code == count_preset_code).first()
    target_count = preset.count_value if preset else 5

    notes = get_review_notes(
        db=db,
        student_id=target_student_id,
        semester_id=semester_id,
        subject_id=subject_id,
        unit_code=unit_code,
        strategy_code=strategy_code,
        target_count=target_count
    )

    if not notes:
        return make_response(
            code=404,
            msg="暂无可供复习的题目，请检查是否已有符合条件的习题或已被移出复习库"
        )

    note_responses = [build_note_out(n, db) for n in notes]
    return make_response(
        code=200,
        msg="success",
        data=[n.model_dump(mode="json") for n in note_responses]
    )


@router.post("/session", response_model=dict)
def create_review_session(
    payload: CreateReviewSessionRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """点击开始答题时，正式在数据库创建并生成复习会话与绑定的题目明细"""
    target_student_id = current_user.id
    if current_user.role == "PARENT":
        if not payload.student_id:
            raise HTTPException(status_code=400, detail="家长身份创建复习会话必须指定 student_id")
        target_student_id = payload.student_id

    # 查出题量档位
    preset = db.query(ReviewCountPreset).filter(ReviewCountPreset.code == payload.count_preset_code).first()
    target_count = preset.count_value if preset else 5

    # 题目处理
    notes = []
    if payload.note_ids:
        # 如果前端传来了生成好的 note_ids 列表，按照该顺序载入题目
        for nid in payload.note_ids:
            n = db.query(Note).filter(Note.id == nid, Note.is_deleted == False).first()
            if n:
                notes.append(n)
    else:
        # 否则兜底重新获取
        notes = get_review_notes(
            db=db,
            student_id=target_student_id,
            semester_id=payload.semester_id,
            subject_id=payload.subject_id,
            unit_code=payload.unit_code,
            strategy_code=payload.strategy_code,
            target_count=target_count
        )

    if not notes:
        raise HTTPException(status_code=404, detail="无可用的复习习题，无法建立会话")

    # 创建 session，状态默认为 IN_PROGRESS
    session = ReviewSession(
        student_id=target_student_id,
        semester_id=payload.semester_id,
        subject_id=payload.subject_id,
        unit_code=payload.unit_code,
        strategy_code=payload.strategy_code,
        target_count=len(notes),
        completed_count=0,
        status="IN_PROGRESS"
    )
    db.add(session)
    db.flush()  # 获得 session.id

    # 保存题目关联列表到 review_session_notes
    for idx, n in enumerate(notes):
        sn = ReviewSessionNote(
            session_id=session.id,
            note_id=n.id,
            sort_order=idx
        )
        db.add(sn)

    db.commit()
    db.refresh(session)

    note_responses = [build_note_out(n, db) for n in notes]
    session_dict = {
        "id": str(session.id),
        "student_id": str(session.student_id),
        "semester_id": str(session.semester_id),
        "subject_id": str(session.subject_id) if session.subject_id else None,
        "unit_code": session.unit_code,
        "strategy_code": session.strategy_code,
        "target_count": session.target_count,
        "completed_count": session.completed_count,
        "status": session.status,
        "created_at": session.created_at.isoformat(),
        "notes": [n.model_dump(mode="json") for n in note_responses]
    }
    return make_response(code=200, msg="success", data=session_dict)


@router.get("/active-sessions", response_model=dict)
def get_active_sessions(
    student_id: uuid.UUID = Query(None),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """检测当前学生是否有处于进行中的复习任务"""
    target_student_id = current_user.id
    if current_user.role == "PARENT":
        if not student_id:
            raise HTTPException(status_code=400, detail="家长身份查询进行中会话必须指定 student_id")
        target_student_id = student_id

    # 查询最新的一个 IN_PROGRESS 状态的复习会话
    session = db.query(ReviewSession).filter(
        ReviewSession.student_id == target_student_id,
        ReviewSession.status == "IN_PROGRESS"
    ).order_by(ReviewSession.created_at.desc()).first()

    if not session:
        return make_response(code=200, msg="success", data=None)

    # 查出这个会话名下的所有题目及打卡日志状态
    session_notes = db.query(ReviewSessionNote).filter(ReviewSessionNote.session_id == session.id).order_by(ReviewSessionNote.sort_order).all()
    notes_list = []
    for sn in session_notes:
        note_dict = build_note_out(sn.note, db).model_dump(mode="json")
        # 探测这道题是否已经在本次会话中打过卡
        log = db.query(ReviewLog).filter(ReviewLog.session_id == session.id, ReviewLog.note_id == sn.note_id).first()
        note_dict["is_completed"] = log is not None
        note_dict["review_log"] = {
            "duration_seconds": log.duration_seconds,
            "outcome_code": log.outcome_code,
            "new_mastery": log.new_mastery,
            "new_stars": float(log.new_stars),
        } if log else None
        notes_list.append(note_dict)

    data = {
        "id": str(session.id),
        "student_id": str(session.student_id),
        "semester_id": str(session.semester_id),
        "subject_id": str(session.subject_id) if session.subject_id else None,
        "unit_code": session.unit_code,
        "strategy_code": session.strategy_code,
        "target_count": session.target_count,
        "completed_count": session.completed_count,
        "status": session.status,
        "created_at": session.created_at.isoformat(),
        "notes": notes_list
    }
    return make_response(code=200, msg="success", data=data)


@router.get("/session/{session_id}", response_model=dict)
def get_review_session_details(
    session_id: uuid.UUID,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """获取单个复习会话明细（可用于会话恢复以及历史查看详情）"""
    session = db.query(ReviewSession).filter(ReviewSession.id == session_id).first()
    if not session:
        raise HTTPException(status_code=404, detail="复习会话不存在")

    # 获取关联题目明细
    session_notes = db.query(ReviewSessionNote).filter(ReviewSessionNote.session_id == session.id).order_by(ReviewSessionNote.sort_order).all()
    notes_list = []
    for sn in session_notes:
        note_dict = build_note_out(sn.note, db).model_dump(mode="json")
        # 附加打卡历史快照
        log = db.query(ReviewLog).filter(ReviewLog.session_id == session.id, ReviewLog.note_id == sn.note_id).first()
        note_dict["is_completed"] = log is not None
        note_dict["review_log"] = {
            "duration_seconds": log.duration_seconds,
            "outcome_code": log.outcome_code,
            "new_mastery": log.new_mastery,
            "new_stars": float(log.new_stars),
            "created_at": log.created_at.isoformat()
        } if log else None
        notes_list.append(note_dict)

    data = {
        "id": str(session.id),
        "student_id": str(session.student_id),
        "semester_id": str(session.semester_id),
        "subject_id": str(session.subject_id) if session.subject_id else None,
        "unit_code": session.unit_code,
        "strategy_code": session.strategy_code,
        "target_count": session.target_count,
        "completed_count": session.completed_count,
        "status": session.status,
        "created_at": session.created_at.isoformat(),
        "completed_at": session.completed_at.isoformat() if session.completed_at else None,
        "notes": notes_list
    }
    return make_response(code=200, msg="success", data=data)


@router.post("/session/{session_id}/abandon", response_model=dict)
def abandon_review_session(
    session_id: uuid.UUID,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """废弃/中止进行中的复习任务"""
    session = db.query(ReviewSession).filter(ReviewSession.id == session_id).first()
    if not session:
        raise HTTPException(status_code=404, detail="复习会话不存在")

    session.status = "ABANDONED"
    session.completed_at = datetime.now(timezone.utc)
    db.commit()

    return make_response(code=200, msg="success", data={"id": str(session.id), "status": session.status})


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


@router.post("/session/{session_id}/submit", response_model=dict)
def submit_review_item(
    session_id: uuid.UUID,
    payload: SubmitReviewItemRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """单题复习打卡回单提交"""
    session = db.query(ReviewSession).filter(ReviewSession.id == session_id).first()
    if not session:
        raise HTTPException(status_code=404, detail="复习会话不存在")

    note = db.query(Note).filter(Note.id == payload.note_id).first()
    if not note:
        raise HTTPException(status_code=404, detail="习题笔记不存在")

    # 记录前快照
    prev_mastery = note.mastery_level
    prev_stars = float(note.difficulty_stars)

    # 判定是否本题在本次会话中已打过卡
    existing_log = db.query(ReviewLog).filter(ReviewLog.session_id == session.id, ReviewLog.note_id == note.id).first()
    if existing_log:
        # 重复提交，我们直接更新它而不会重叠
        existing_log.duration_seconds = payload.duration_seconds
        existing_log.outcome_code = payload.outcome_code
        existing_log.new_mastery = payload.new_mastery
        existing_log.new_stars = payload.new_stars
        existing_log.exclude_from_review_snapshot = payload.exclude_from_review
    else:
        # 创建新日志
        log = ReviewLog(
            session_id=session.id,
            student_id=session.student_id,
            note_id=note.id,
            sequence_index=payload.sequence_index,
            duration_seconds=payload.duration_seconds,
            outcome_code=payload.outcome_code,
            previous_mastery=prev_mastery,
            new_mastery=payload.new_mastery,
            previous_stars=prev_stars,
            new_stars=payload.new_stars,
            exclude_from_review_snapshot=payload.exclude_from_review
        )
        db.add(log)
        # 更新会话进度
        session.completed_count += 1

    # 更新 Note 属性
    note.mastery_level = payload.new_mastery
    note.difficulty_stars = payload.new_stars
    note.exclude_from_review = payload.exclude_from_review
    note.last_reviewed_at = datetime.now(timezone.utc)

    is_completed = session.completed_count >= session.target_count
    if is_completed:
        session.status = "COMPLETED"
        session.completed_at = datetime.now(timezone.utc)

    db.commit()
    db.refresh(session)

    # 获取关联题目明细，以返回给客户端更新后的完整 Session 结构
    session_notes = db.query(ReviewSessionNote).filter(ReviewSessionNote.session_id == session.id).order_by(ReviewSessionNote.sort_order).all()
    notes_list = []
    for sn in session_notes:
        note_dict = build_note_out(sn.note, db).model_dump(mode="json")
        log = db.query(ReviewLog).filter(ReviewLog.session_id == session.id, ReviewLog.note_id == sn.note_id).first()
        note_dict["is_completed"] = log is not None
        note_dict["review_log"] = {
            "duration_seconds": log.duration_seconds,
            "outcome_code": log.outcome_code,
            "new_mastery": log.new_mastery,
            "new_stars": float(log.new_stars),
            "created_at": log.created_at.isoformat()
        } if log else None
        notes_list.append(note_dict)

    data = {
        "id": str(session.id),
        "student_id": str(session.student_id),
        "semester_id": str(session.semester_id),
        "subject_id": str(session.subject_id) if session.subject_id else None,
        "unit_code": session.unit_code,
        "strategy_code": session.strategy_code,
        "target_count": session.target_count,
        "completed_count": session.completed_count,
        "status": session.status,
        "created_at": session.created_at.isoformat(),
        "completed_at": session.completed_at.isoformat() if session.completed_at else None,
        "notes": notes_list
    }
    return make_response(code=200, msg="success", data=data)


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

    from datetime import timedelta, timezone
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

