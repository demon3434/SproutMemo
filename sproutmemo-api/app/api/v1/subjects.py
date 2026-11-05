import uuid
from datetime import datetime
from pydantic import BaseModel
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
import sqlalchemy as sa

from app.db.session import get_db
from app.models.user import User
from app.models.system import Subject
from app.core.deps import get_current_user
from app.core.exceptions import AppException, ForbiddenException, NotFoundException
from app.core.response import ok

# Router definition
router = APIRouter(prefix="/subjects", tags=["Subject Management"])


# Schemas
class SubjectCreate(BaseModel):
    subject_name: str
    sort_order: int = 0
    is_active: bool = True


class SubjectOut(BaseModel):
    id: uuid.UUID
    subject_name: str
    sort_order: int
    is_active: bool
    created_at: datetime

    class Config:
        from_attributes = True


class SubjectUpdate(BaseModel):
    subject_name: str | None = None
    sort_order: int | None = None
    is_active: bool | None = None


@router.post("", response_model=dict)
def create_subject(
    data: SubjectCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Create a new global subject (Admin only)"""
    if current_user.role != "ADMIN":
        raise ForbiddenException("只有管理员可以添加全局学科")

    # Check if duplicate subject exists (including deleted ones)
    duplicate = db.query(Subject).filter(
        Subject.subject_name == data.subject_name.strip()
    ).first()
    if duplicate:
        if duplicate.is_deleted:
            max_order = db.query(sa.func.max(Subject.sort_order)).filter(
                Subject.is_deleted == False
            ).scalar() or 0
            duplicate.is_deleted = False
            duplicate.is_active = data.is_active
            duplicate.sort_order = max_order + 1
            db.commit()
            db.refresh(duplicate)
            return ok(data=SubjectOut.model_validate(duplicate), msg="科目添加成功")
        else:
            raise AppException(code=400, message=f"科目 '{data.subject_name}' 已配置，请勿重复添加")

    # Auto-assign sort_order to the end of the list
    max_order = db.query(sa.func.max(Subject.sort_order)).filter(
        Subject.is_deleted == False
    ).scalar() or 0

    subject = Subject(
        subject_name=data.subject_name.strip(),
        sort_order=max_order + 1,
        is_active=data.is_active
    )
    db.add(subject)
    db.commit()
    db.refresh(subject)

    return ok(data=SubjectOut.model_validate(subject), msg="科目添加成功")



@router.put("/{id}", response_model=dict)
def update_subject(
    id: uuid.UUID,
    data: SubjectUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Update global subject (Admin only)"""
    if current_user.role != "ADMIN":
        raise ForbiddenException("只有管理员可以修改学科")

    subject = db.query(Subject).filter(Subject.id == id, Subject.is_deleted == False).first()
    if not subject:
        raise NotFoundException("未找到该学科")

    if data.subject_name is not None:
        name_str = data.subject_name.strip()
        if name_str != subject.subject_name:
            duplicate = db.query(Subject).filter(Subject.subject_name == name_str).first()
            if duplicate:
                if duplicate.is_deleted:
                    # If duplicate is deleted, we remove/bypass it by renaming the deleted one or raising error.
                    # Best practice: forbid conflict with existing name.
                    raise AppException(code=400, message=f"科目 '{name_str}' 已被软删除，请联系管理员恢复或使用其他名称")
                raise AppException(code=400, message=f"科目 '{name_str}' 已存在")
            subject.subject_name = name_str

    if data.sort_order is not None:
        subject.sort_order = data.sort_order
    if data.is_active is not None:
        subject.is_active = data.is_active

    db.commit()
    db.refresh(subject)
    return ok(data=SubjectOut.model_validate(subject), msg="学科修改成功")


@router.get("", response_model=dict)
def list_subjects(
    only_active: bool = True,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """List all global subjects"""
    query = db.query(Subject).filter(Subject.is_deleted == False)
    if only_active:
        query = query.filter(Subject.is_active == True)
        
    subjects = query.order_by(sa.asc(Subject.sort_order), sa.asc(Subject.subject_name)).all()
    items = [SubjectOut.model_validate(s) for s in subjects]
    return ok(data={"items": items}, msg="获取成功")


@router.delete("/{id}", response_model=dict)
def delete_subject(
    id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Delete a global subject (Admin only)"""
    if current_user.role != "ADMIN":
        raise ForbiddenException("只有管理员可以删除全局学科")

    subject = db.query(Subject).filter(Subject.id == id, Subject.is_deleted == False).first()
    if not subject:
        raise NotFoundException("未找到对应的科目记录")

    subject.is_deleted = True
    db.commit()
    return ok(msg="科目删除成功")


@router.post("/{id}/move", response_model=dict)
def move_subject(
    id: uuid.UUID,
    direction: str,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Move a subject up or down in sort order (Admin only)"""
    if current_user.role != "ADMIN":
        raise ForbiddenException("只有管理员可以调整学科顺序")

    subjects = db.query(Subject).filter(
        Subject.is_deleted == False
    ).order_by(sa.asc(Subject.sort_order), sa.asc(Subject.subject_name)).all()

    idx = next((i for i, s in enumerate(subjects) if s.id == id), None)
    if idx is None:
        raise NotFoundException("未找到该学科")

    if direction == "up" and idx > 0:
        swap = subjects[idx - 1]
    elif direction == "down" and idx < len(subjects) - 1:
        swap = subjects[idx + 1]
    else:
        return ok(msg="已在边界，无法移动")

    # Swap sort_order values
    subjects[idx].sort_order, swap.sort_order = swap.sort_order, subjects[idx].sort_order
    db.commit()
    return ok(msg="排序已更新")

