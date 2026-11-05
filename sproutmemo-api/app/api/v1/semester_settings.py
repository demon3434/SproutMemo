import uuid
from datetime import datetime
from typing import Literal
from pydantic import BaseModel
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
import sqlalchemy as sa

from app.db.session import get_db
from app.models.user import User
from app.models.system import Subject, Semester, SemesterSubject
from app.core.deps import get_current_user
from app.core.exceptions import AppException, ForbiddenException, NotFoundException
from app.core.response import ok

router = APIRouter(prefix="/semester_settings", tags=["Semester-Subject Curriculum Templates"])


# Schemas
class SemesterSubjectCreate(BaseModel):
    semester_id: uuid.UUID
    subject_id: uuid.UUID
    is_active: bool = True


class SubjectOut(BaseModel):
    id: uuid.UUID
    subject_name: str
    is_active: bool
    sort_order: int

    class Config:
        from_attributes = True


class SemesterSubjectOut(BaseModel):
    id: uuid.UUID
    semester_id: uuid.UUID
    subject_id: uuid.UUID
    is_active: bool
    sort_order: int = 0
    subject: SubjectOut | None = None
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True


class SemesterSubjectUpdate(BaseModel):
    is_active: bool


@router.post("/subjects", response_model=dict)
def add_semester_subject(
    data: SemesterSubjectCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Associate a global subject with a semester template (Admin only)"""
    if current_user.role != "ADMIN":
        raise ForbiddenException("只有管理员可以配置学期与学科的模板关系")

    # Validate that semester exists (not deleted)
    sem = db.query(Semester).filter(Semester.id == data.semester_id, Semester.is_deleted == False).first()
    if not sem:
        raise NotFoundException("未找到对应的学期记录")

    # Validate that subject exists (not deleted)
    subject = db.query(Subject).filter(Subject.id == data.subject_id, Subject.is_deleted == False).first()
    if not subject:
        raise NotFoundException("未找到对应的科目记录")

    # Check if duplicate association exists (including soft-deleted ones)
    existing = db.query(SemesterSubject).filter(
        SemesterSubject.semester_id == data.semester_id,
        SemesterSubject.subject_id == data.subject_id
    ).first()

    if existing:
        if existing.is_deleted:
            # Resurrect: place at the end of the current ordering
            max_order = db.query(sa.func.max(SemesterSubject.sort_order)).filter(
                SemesterSubject.semester_id == data.semester_id,
                SemesterSubject.is_deleted == False
            ).scalar() or 0
            existing.is_deleted = False
            existing.is_active = data.is_active
            existing.sort_order = max_order + 1
            db.commit()
            db.refresh(existing)
            sub_data = SubjectOut.model_validate(subject)
            res_data = SemesterSubjectOut.model_validate(existing)
            res_data.subject = sub_data
            return ok(data=res_data.model_dump(), msg="关联模板创建成功")
        else:
            raise AppException(code=400, message="该学期已关联过该科目")

    # Assign sort_order = max existing + 1 for this semester
    max_order = db.query(sa.func.max(SemesterSubject.sort_order)).filter(
        SemesterSubject.semester_id == data.semester_id,
        SemesterSubject.is_deleted == False
    ).scalar() or 0

    assoc = SemesterSubject(
        semester_id=data.semester_id,
        subject_id=data.subject_id,
        is_active=data.is_active,
        sort_order=max_order + 1
    )
    db.add(assoc)
    db.commit()
    db.refresh(assoc)

    sub_data = SubjectOut.model_validate(subject)
    res_data = SemesterSubjectOut.model_validate(assoc)
    res_data.subject = sub_data

    return ok(data=res_data.model_dump(), msg="关联模板创建成功")


@router.get("/subjects", response_model=dict)
def list_semester_subjects(
    semester_id: uuid.UUID | None = None,
    only_active: bool = True,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """List template subject mappings for a semester (Available to all roles)"""
    # Join with Semester and Subject to filter out deleted ones
    query = db.query(SemesterSubject)\
        .join(Semester, SemesterSubject.semester_id == Semester.id)\
        .join(Subject, SemesterSubject.subject_id == Subject.id)\
        .filter(
            SemesterSubject.is_deleted == False,
            Semester.is_deleted == False,
            Subject.is_deleted == False
        )

    if semester_id:
        query = query.filter(SemesterSubject.semester_id == semester_id)
    if only_active:
        query = query.filter(SemesterSubject.is_active == True)

    # Sort by the mapping's own sort_order
    assocs = query.order_by(SemesterSubject.sort_order).all()

    items = []
    for assoc in assocs:
        subject = db.query(Subject).filter(Subject.id == assoc.subject_id).first()
        if only_active and subject and not subject.is_active:
            continue

        sub_data = SubjectOut.model_validate(subject) if subject else None
        res_item = SemesterSubjectOut.model_validate(assoc)
        res_item.subject = sub_data
        items.append(res_item.model_dump())

    return ok(data={"items": items}, msg="获取成功")


@router.post("/subjects/{id}/move", response_model=dict)
def move_semester_subject(
    id: uuid.UUID,
    direction: Literal["up", "down"],
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Move a semester-subject mapping up or down within its semester (Admin only)"""
    if current_user.role != "ADMIN":
        raise ForbiddenException("只有管理员可以调整学科顺序")

    assoc = db.query(SemesterSubject).filter(
        SemesterSubject.id == id,
        SemesterSubject.is_deleted == False
    ).first()
    if not assoc:
        raise NotFoundException("未找到该模板关联配置")

    # Get all siblings in the same semester, sorted by sort_order
    siblings = db.query(SemesterSubject).filter(
        SemesterSubject.semester_id == assoc.semester_id,
        SemesterSubject.is_deleted == False
    ).order_by(SemesterSubject.sort_order).all()

    idx = next((i for i, s in enumerate(siblings) if s.id == assoc.id), None)
    if idx is None:
        raise NotFoundException("未找到该条目的位置信息")

    if direction == "up" and idx > 0:
        swap = siblings[idx - 1]
    elif direction == "down" and idx < len(siblings) - 1:
        swap = siblings[idx + 1]
    else:
        return ok(msg="已在边界，无法移动")

    # Swap sort_order values
    assoc.sort_order, swap.sort_order = swap.sort_order, assoc.sort_order
    db.commit()
    return ok(msg="排序已更新")


@router.put("/subjects/{id}", response_model=dict)
def update_semester_subject(
    id: uuid.UUID,
    data: SemesterSubjectUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Toggle template mapping active status (Admin only)"""
    if current_user.role != "ADMIN":
        raise ForbiddenException("只有管理员可以修改模板关系")

    assoc = db.query(SemesterSubject).filter(SemesterSubject.id == id, SemesterSubject.is_deleted == False).first()
    if not assoc:
        raise NotFoundException("未找到该模板关联配置")

    assoc.is_active = data.is_active
    db.commit()
    db.refresh(assoc)

    subject = db.query(Subject).filter(Subject.id == assoc.subject_id).first()
    sub_data = SubjectOut.model_validate(subject) if subject else None
    res_data = SemesterSubjectOut.model_validate(assoc)
    res_data.subject = sub_data

    return ok(data=res_data.model_dump(), msg="配置修改成功")


@router.delete("/subjects/{id}", response_model=dict)
def delete_semester_subject(
    id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Delete a template mapping (Admin only)"""
    if current_user.role != "ADMIN":
        raise ForbiddenException("只有管理员可以删除模板关系")

    assoc = db.query(SemesterSubject).filter(SemesterSubject.id == id, SemesterSubject.is_deleted == False).first()
    if not assoc:
        raise NotFoundException("未找到该模板关联配置")

    assoc.is_deleted = True
    db.commit()
    return ok(msg="取消关联模板成功")
