import uuid
from datetime import datetime
from pydantic import BaseModel
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
import sqlalchemy as sa

from app.db.session import get_db
from app.models.user import User, ParentStudentRelation
from app.models.system import QuestionSource, SemesterSubject
from app.core.deps import get_current_user, require_student_or_parent
from app.core.exceptions import AppException, ForbiddenException, NotFoundException
from app.core.response import ok

router = APIRouter(prefix="/source_books", tags=["Question Sources Shelf"])


class QuestionSourceCreate(BaseModel):
    semester_id: uuid.UUID
    subject_id: uuid.UUID
    name: str
    is_active: bool = True
    student_id: uuid.UUID | None = None


class QuestionSourceUpdate(BaseModel):
    name: str | None = None
    is_active: bool | None = None


class QuestionSourceOut(BaseModel):
    id: uuid.UUID
    student_id: uuid.UUID
    semester_id: uuid.UUID
    subject_id: uuid.UUID
    subject_name: str | None = None
    name: str
    source_type: str = "BOOK"
    category_code: str = "DAILY_HW"
    is_active: bool
    is_deleted: bool = False
    sort_order: int = 0
    created_at: datetime

    class Config:
        from_attributes = True


def validate_parent_child_binding(db: Session, parent_id: uuid.UUID, student_id: uuid.UUID):
    """Ensure parent is bound to the student"""
    binding = db.query(ParentStudentRelation).filter(
        ParentStudentRelation.parent_id == parent_id,
        ParentStudentRelation.student_id == student_id
    ).first()
    if not binding:
        raise ForbiddenException("您未绑定该学生，无权访问其数据")


def _get_source_and_check(db: Session, source_id: uuid.UUID, current_user: User) -> QuestionSource:
    """Fetch source and check access rights."""
    source = db.query(QuestionSource).filter(QuestionSource.id == source_id).first()
    if not source:
        raise NotFoundException("未找到对应的题目来源记录")
    if current_user.role == "STUDENT":
        if source.student_id != current_user.id:
            raise ForbiddenException("无权操作其他学生的记录")
    elif current_user.role == "PARENT":
        validate_parent_child_binding(db, current_user.id, source.student_id)
    return source


@router.post("", response_model=dict)
def create_question_source(
    data: QuestionSourceCreate,
    current_user: User = Depends(require_student_or_parent),
    db: Session = Depends(get_db)
):
    """Create a new question source for a student's semester subject"""
    target_student_id = None

    if current_user.role == "STUDENT":
        target_student_id = current_user.id
    elif current_user.role == "PARENT":
        if not data.student_id:
            raise AppException(code=400, message="家长角色创建题目来源时，必填 student_id")
        target_student_id = data.student_id
        validate_parent_child_binding(db, current_user.id, target_student_id)

    # Check that the subject is active for this semester
    assoc = db.query(SemesterSubject).filter(
        SemesterSubject.semester_id == data.semester_id,
        SemesterSubject.subject_id == data.subject_id,
        SemesterSubject.is_active == True
    ).first()
    if not assoc:
        raise AppException(code=400, message="该学科在此学期未开设或已被管理员停用，无法添加习题册")

    # Auto-assign sort_order = max + 1 within student+semester+subject group
    max_order_val = db.query(sa.func.max(QuestionSource.sort_order)).filter(
        QuestionSource.student_id == target_student_id,
        QuestionSource.semester_id == data.semester_id,
        QuestionSource.subject_id == data.subject_id,
        QuestionSource.is_deleted == False
    ).scalar()
    max_order = max_order_val if max_order_val is not None else -1

    source = QuestionSource(
        student_id=target_student_id,
        semester_id=data.semester_id,
        subject_id=data.subject_id,
        name=data.name.strip(),
        source_type="BOOK",
        is_active=data.is_active,
        sort_order=max_order + 1
    )
    db.add(source)
    db.commit()
    db.refresh(source)

    out = QuestionSourceOut.model_validate(source)
    out.subject_name = source.subject.subject_name if source.subject else None
    return ok(data=out, msg="创建成功")


@router.get("", response_model=dict)
def list_question_sources(
    student_id: uuid.UUID | None = None,
    semester_id: uuid.UUID | None = None,
    subject_id: uuid.UUID | None = None,
    source_type: str | None = "BOOK",
    category_code: str | None = None,
    unit_code: int | None = None,
    name_query: str | None = None,
    only_active: bool = True,
    include_deleted: bool = False,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """List question sources with filters, ordered by sort_order"""
    query = db.query(QuestionSource)

    # Access control & student filter
    if current_user.role == "STUDENT":
        query = query.filter(QuestionSource.student_id == current_user.id)
    elif current_user.role == "PARENT":
        if student_id:
            validate_parent_child_binding(db, current_user.id, student_id)
            query = query.filter(QuestionSource.student_id == student_id)
        else:
            child_ids = [r.student_id for r in db.query(ParentStudentRelation).filter(
                ParentStudentRelation.parent_id == current_user.id
            ).all()]
            query = query.filter(QuestionSource.student_id.in_(child_ids))
    elif current_user.role == "ADMIN":
        if student_id:
            query = query.filter(QuestionSource.student_id == student_id)
    else:
        raise ForbiddenException("无权执行此操作")

    # Additional filters
    if semester_id:
        query = query.filter(QuestionSource.semester_id == semester_id)
    if subject_id:
        query = query.filter(QuestionSource.subject_id == subject_id)
    if source_type:
        query = query.filter(QuestionSource.source_type == source_type)
    if category_code:
        query = query.filter(QuestionSource.category_code == category_code)
    if name_query:
        query = query.filter(QuestionSource.name.ilike(f"%{name_query.strip()}%"))
    if unit_code is not None:
        from app.models.note import Note
        query = query.join(Note, Note.question_source_id == QuestionSource.id).filter(
            Note.unit_code == unit_code,
            Note.is_deleted == False
        ).distinct()
    if only_active:
        query = query.filter(QuestionSource.is_active == True)
    if not include_deleted:
        query = query.filter(QuestionSource.is_deleted == False)

    sources = query.order_by(QuestionSource.sort_order.asc(), QuestionSource.created_at.asc()).all()

    # Only check and repair sort_order if we aren't querying a filtered subset
    if not name_query and unit_code is None:
        # Check and repair sort_order duplicates within groups
        groups = {}
        for s in sources:
            key = (s.student_id, s.semester_id, s.subject_id)
            groups.setdefault(key, []).append(s)

        need_commit = False
        for key, items_in_group in groups.items():
            seen_orders = set()
            has_dup = False
            for it in items_in_group:
                if it.sort_order in seen_orders:
                    has_dup = True
                    break
                seen_orders.add(it.sort_order)

            if has_dup or (len(items_in_group) > 1 and len(seen_orders) < len(items_in_group)):
                items_in_group.sort(key=lambda x: (x.sort_order, x.created_at))
                for index, it in enumerate(items_in_group):
                    if it.sort_order != index:
                        it.sort_order = index
                        need_commit = True

        if need_commit:
            db.commit()
            # Re-fetch sorted list after repair
            sources = query.order_by(QuestionSource.sort_order.asc(), QuestionSource.created_at.asc()).all()

    items = []
    for s in sources:
        out = QuestionSourceOut.model_validate(s)
        out.subject_name = s.subject.subject_name if s.subject else None
        items.append(out.model_dump())
    return ok(data={"items": items}, msg="获取成功")


@router.put("/{id}", response_model=dict)
def update_question_source(
    id: uuid.UUID,
    data: QuestionSourceUpdate,
    current_user: User = Depends(require_student_or_parent),
    db: Session = Depends(get_db)
):
    """Update question source (rename, change publisher or toggle status)"""
    source = _get_source_and_check(db, id, current_user)

    if data.name is not None:
        source.name = data.name.strip()
    if data.is_active is not None:
        source.is_active = data.is_active

    db.commit()
    db.refresh(source)
    out = QuestionSourceOut.model_validate(source)
    out.subject_name = source.subject.subject_name if source.subject else None
    return ok(data=out, msg="更新成功")


@router.post("/{id}/move", response_model=dict)
def move_question_source(
    id: uuid.UUID,
    direction: str,
    current_user: User = Depends(require_student_or_parent),
    db: Session = Depends(get_db)
):
    """Move a question source up or down within the same student+semester+subject group"""
    if direction not in ("up", "down"):
        raise AppException(code=400, message="direction 参数必须是 up 或 down")

    source = _get_source_and_check(db, id, current_user)

    # Find the adjacent sibling in the same group
    base_q = db.query(QuestionSource).filter(
        QuestionSource.student_id == source.student_id,
        QuestionSource.semester_id == source.semester_id,
        QuestionSource.subject_id == source.subject_id,
        QuestionSource.is_deleted == False,
        QuestionSource.id != source.id
    )

    if direction == "up":
        sibling = base_q.filter(
            QuestionSource.sort_order < source.sort_order
        ).order_by(QuestionSource.sort_order.desc()).first()
    else:
        sibling = base_q.filter(
            QuestionSource.sort_order > source.sort_order
        ).order_by(QuestionSource.sort_order.asc()).first()

    if not sibling:
        return ok(msg="已到边界，无法移动")

    source.sort_order, sibling.sort_order = sibling.sort_order, source.sort_order
    db.commit()
    return ok(msg="移动成功")


@router.delete("/{id}", response_model=dict)
def delete_question_source(
    id: uuid.UUID,
    current_user: User = Depends(require_student_or_parent),
    db: Session = Depends(get_db)
):
    """Soft-delete a question source (sets is_deleted=True)"""
    source = _get_source_and_check(db, id, current_user)
    source.is_deleted = True
    db.commit()
    return ok(msg="删除成功")
