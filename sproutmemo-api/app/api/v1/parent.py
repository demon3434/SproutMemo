import uuid
from pydantic import BaseModel
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.models.user import User, Student, ParentStudentRelation
from app.core.deps import get_current_user
from app.core.exceptions import ForbiddenException, NotFoundException
from app.core.response import ok

router = APIRouter(prefix="/parent", tags=["Parent - Child Management"])


class StudentOut(BaseModel):
    id: uuid.UUID
    username: str
    nickname: str | None
    enrollment_year: int
    semester_offset: int

    class Config:
        from_attributes = True


class SemesterOffsetUpdate(BaseModel):
    semester_offset: int


def _require_parent(current_user: User) -> None:
    if current_user.role != "PARENT":
        raise ForbiddenException("仅家长账号可访问此接口")


def _get_child_or_404(parent: User, child_id: uuid.UUID, db: Session) -> User:
    """Verify that child_id belongs to this parent and return the child User."""
    relation = db.query(ParentStudentRelation).filter(
        ParentStudentRelation.parent_id == parent.id,
        ParentStudentRelation.student_id == child_id
    ).first()
    if not relation:
        raise ForbiddenException("该孩子不在您的管理范围内")

    child = db.query(User).filter(User.id == child_id, User.role == "STUDENT").first()
    if not child:
        raise NotFoundException("未找到对应的孩子账号")
    return child


@router.get("/children", response_model=dict)
def list_children(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """List all children linked to the current parent account."""
    _require_parent(current_user)

    relations = db.query(ParentStudentRelation).filter(
        ParentStudentRelation.parent_id == current_user.id
    ).all()

    items = []
    for rel in relations:
        child = db.query(User).filter(User.id == rel.student_id, User.role == "STUDENT").first()
        if not child or not child.student_profile:
            continue
        items.append({
            "id": str(child.id),
            "username": child.username,
            "nickname": child.nickname,
            "enrollment_year": child.student_profile.enrollment_year,
            "semester_offset": child.student_profile.semester_offset,
            "is_active": child.is_active,
        })

    return ok(data={"items": items}, msg="获取成功")


@router.put("/children/{child_id}/semester_offset", response_model=dict)
def update_child_semester_offset(
    child_id: uuid.UUID,
    data: SemesterOffsetUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Update a child's semester offset (parent only)."""
    _require_parent(current_user)
    child = _get_child_or_404(current_user, child_id, db)

    if not child.student_profile:
        raise NotFoundException("该孩子没有学生档案")

    child.student_profile.semester_offset = data.semester_offset
    db.commit()
    return ok(msg="学期偏移量更新成功")
