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
from app.api.v1.review_stats import router as stats_router

router = APIRouter(prefix="/review", tags=["Review"])
router.include_router(stats_router)


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

