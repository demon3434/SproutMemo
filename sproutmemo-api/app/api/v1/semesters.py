import uuid
from datetime import datetime
from pydantic import BaseModel
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.models.user import User
from app.models.system import Semester
from app.core.deps import get_current_user
from app.core.exceptions import AppException, ForbiddenException, NotFoundException
from app.core.response import ok

router = APIRouter(prefix="/semesters", tags=["Semester Management"])


class StageOutNested(BaseModel):
    id: uuid.UUID
    name: str
    sort_order: int
    is_active: bool

    class Config:
        from_attributes = True


class SemesterCreate(BaseModel):
    name: str
    stage_id: uuid.UUID | None = None
    start_date: datetime | None = None
    end_date: datetime | None = None
    is_active: bool = True
    sort_order: int = 0


class SemesterOut(BaseModel):
    id: uuid.UUID
    name: str
    stage_id: uuid.UUID | None = None
    start_date: datetime | None = None
    end_date: datetime | None = None
    is_active: bool
    sort_order: int
    created_at: datetime
    stage: StageOutNested | None = None

    class Config:
        from_attributes = True


class SemesterUpdate(BaseModel):
    name: str | None = None
    stage_id: uuid.UUID | None = None
    start_date: datetime | None = None
    end_date: datetime | None = None
    is_active: bool | None = None
    sort_order: int | None = None


@router.post("", response_model=dict)
def create_semester(
    data: SemesterCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Create a new semester (Admin only)"""
    if current_user.role != "ADMIN":
        raise ForbiddenException("只有管理员可以创建学期")

    # Check for duplicate name
    existing = db.query(Semester).filter(Semester.name == data.name.strip()).first()
    if existing:
        if existing.is_deleted:
            existing.is_deleted = False
            existing.stage_id = data.stage_id
            existing.is_active = data.is_active
            existing.sort_order = data.sort_order
            db.commit()
            db.refresh(existing)
            return ok(data=SemesterOut.model_validate(existing), msg="学期创建成功")
        else:
            raise AppException(code=400, message=f"学期 '{data.name}' 已存在，请勿重复创建")

    semester = Semester(
        name=data.name.strip(),
        stage_id=data.stage_id,
        start_date=data.start_date,
        end_date=data.end_date,
        is_active=data.is_active,
        sort_order=data.sort_order
    )
    db.add(semester)
    db.commit()
    db.refresh(semester)
    return ok(data=SemesterOut.model_validate(semester), msg="学期创建成功")


@router.put("/{id}", response_model=dict)
def update_semester(
    id: uuid.UUID,
    data: SemesterUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Update semester (Admin only)"""
    if current_user.role != "ADMIN":
        raise ForbiddenException("只有管理员可以修改学期")

    semester = db.query(Semester).filter(Semester.id == id, Semester.is_deleted == False).first()
    if not semester:
        raise NotFoundException("未找到该学期")

    if data.name is not None:
        name_str = data.name.strip()
        if name_str != semester.name:
            existing = db.query(Semester).filter(Semester.name == name_str).first()
            if existing:
                if existing.is_deleted:
                    raise AppException(code=400, message=f"学期 '{name_str}' 已被软删除")
                raise AppException(code=400, message=f"学期 '{name_str}' 已存在")
            semester.name = name_str

    if data.stage_id is not None:
        semester.stage_id = data.stage_id
    if data.start_date is not None:
        semester.start_date = data.start_date
    if data.end_date is not None:
        semester.end_date = data.end_date
    if data.is_active is not None:
        semester.is_active = data.is_active
    if data.sort_order is not None:
        semester.sort_order = data.sort_order

    db.commit()
    db.refresh(semester)
    return ok(data=SemesterOut.model_validate(semester), msg="修改成功")


@router.get("", response_model=dict)
def list_semesters(
    only_active: bool = True,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """List semesters"""
    from app.models.system import Stage
    # Outer join with Stage to sort by Stage's sort_order first, then Semester's sort_order
    # Filter out soft-deleted semesters AND soft-deleted stages
    query = db.query(Semester).outerjoin(Stage, Semester.stage_id == Stage.id)\
        .filter(Semester.is_deleted == False)
    
    if only_active:
        query = query.filter(Semester.is_active == True)
    
    semesters = query.order_by(
        Stage.sort_order.asc(),
        Semester.sort_order.asc(),
        Semester.created_at.asc()
    ).all()
    items = [SemesterOut.model_validate(s) for s in semesters]
    return ok(data={"items": items}, msg="获取成功")


@router.delete("/{id}", response_model=dict)
def delete_semester(
    id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Delete a semester (Admin only)"""
    if current_user.role != "ADMIN":
        raise ForbiddenException("只有管理员可以删除学期")

    semester = db.query(Semester).filter(Semester.id == id, Semester.is_deleted == False).first()
    if not semester:
        raise NotFoundException("未找到对应的学期记录")

    semester.is_deleted = True
    db.commit()
    return ok(msg="学期删除成功")


@router.post("/{id}/move", response_model=dict)
def move_semester(
    id: uuid.UUID,
    direction: str,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Move semester ordering up/down within the same stage (Admin only)"""
    if current_user.role != "ADMIN":
        raise ForbiddenException("只有管理员可以调整排序")

    semester = db.query(Semester).filter(Semester.id == id, Semester.is_deleted == False).first()
    if not semester:
        raise NotFoundException("未找到该学期")

    if direction == "up":
        # Find predecessor under same stage
        target = db.query(Semester).filter(
            Semester.is_deleted == False,
            Semester.stage_id == semester.stage_id,
            Semester.sort_order < semester.sort_order
        ).order_by(Semester.sort_order.desc()).first()
    elif direction == "down":
        # Find successor under same stage
        target = db.query(Semester).filter(
            Semester.is_deleted == False,
            Semester.stage_id == semester.stage_id,
            Semester.sort_order > semester.sort_order
        ).order_by(Semester.sort_order.asc()).first()
    else:
        raise AppException(code=400, message="无效的移动方向")

    if target:
        # Swap sort_orders
        semester.sort_order, target.sort_order = target.sort_order, semester.sort_order
        db.commit()
        return ok(msg="排序调整成功")
    
    return ok(msg="已处于边界，无需调整")
