import uuid
from datetime import datetime
from pydantic import BaseModel
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.models.user import User
from app.models.system import Stage
from app.core.deps import get_current_user
from app.core.exceptions import AppException, ForbiddenException, NotFoundException
from app.core.response import ok

router = APIRouter(prefix="/stages", tags=["Learning Stage Management"])


class StageCreate(BaseModel):
    name: str
    sort_order: int = 0
    is_active: bool = True


class StageOut(BaseModel):
    id: uuid.UUID
    name: str
    sort_order: int
    is_active: bool
    created_at: datetime

    class Config:
        from_attributes = True


class StageUpdate(BaseModel):
    name: str | None = None
    sort_order: int | None = None
    is_active: bool | None = None


@router.post("", response_model=dict)
def create_stage(
    data: StageCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Create a new stage (Admin only)"""
    if current_user.role != "ADMIN":
        raise ForbiddenException("只有管理员可以创建学习阶段")

    existing = db.query(Stage).filter(Stage.name == data.name.strip()).first()
    if existing:
        if existing.is_deleted:
            existing.is_deleted = False
            existing.is_active = data.is_active
            existing.sort_order = data.sort_order
            db.commit()
            db.refresh(existing)
            return ok(data=StageOut.model_validate(existing), msg="学习阶段创建成功")
        else:
            raise AppException(code=400, message=f"阶段 '{data.name}' 已存在")

    stage = Stage(
        name=data.name.strip(),
        sort_order=data.sort_order,
        is_active=data.is_active
    )
    db.add(stage)
    db.commit()
    db.refresh(stage)
    return ok(data=StageOut.model_validate(stage), msg="学习阶段创建成功")


@router.put("/{id}", response_model=dict)
def update_stage(
    id: uuid.UUID,
    data: StageUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Update learning stage (Admin only)"""
    if current_user.role != "ADMIN":
        raise ForbiddenException("只有管理员可以修改学习阶段")

    stage = db.query(Stage).filter(Stage.id == id, Stage.is_deleted == False).first()
    if not stage:
        raise NotFoundException("未找到该学习阶段")

    if data.name is not None:
        name_str = data.name.strip()
        if name_str != stage.name:
            existing = db.query(Stage).filter(Stage.name == name_str).first()
            if existing:
                if existing.is_deleted:
                    raise AppException(code=400, message=f"阶段 '{name_str}' 已被软删除")
                raise AppException(code=400, message=f"阶段 '{name_str}' 已存在")
            stage.name = name_str

    if data.sort_order is not None:
        stage.sort_order = data.sort_order
    if data.is_active is not None:
        stage.is_active = data.is_active

    db.commit()
    db.refresh(stage)
    return ok(data=StageOut.model_validate(stage), msg="修改成功")


@router.get("", response_model=dict)
def list_stages(
    only_active: bool = True,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """List learning stages"""
    query = db.query(Stage).filter(Stage.is_deleted == False)
    if only_active:
        query = query.filter(Stage.is_active == True)
    
    stages = query.order_by(Stage.sort_order.asc(), Stage.created_at.asc()).all()
    items = [StageOut.model_validate(s) for s in stages]
    return ok(data={"items": items}, msg="获取成功")


@router.delete("/{id}", response_model=dict)
def delete_stage(
    id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Delete a stage (Admin only)"""
    if current_user.role != "ADMIN":
        raise ForbiddenException("只有管理员可以删除学习阶段")

    stage = db.query(Stage).filter(Stage.id == id, Stage.is_deleted == False).first()
    if not stage:
        raise NotFoundException("未找到对应的学习阶段")

    # Check if there are any non-deleted semesters associated with this stage
    from app.models.system import Semester
    active_semesters = db.query(Semester).filter(
        Semester.stage_id == id,
        Semester.is_deleted == False
    ).count()

    if active_semesters > 0:
        raise AppException(code=400, message="无法删除：该学习阶段下还有正在使用的学期")

    stage.is_deleted = True
    db.commit()
    return ok(msg="学习阶段删除成功")


@router.post("/{id}/move", response_model=dict)
def move_stage(
    id: uuid.UUID,
    direction: str,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Move stage ordering up/down (Admin only)"""
    if current_user.role != "ADMIN":
        raise ForbiddenException("只有管理员可以调整排序")

    stage = db.query(Stage).filter(Stage.id == id, Stage.is_deleted == False).first()
    if not stage:
        raise NotFoundException("未找到该阶段")

    if direction == "up":
        # Find predecessor (largest sort_order < current)
        target = db.query(Stage).filter(
            Stage.is_deleted == False,
            Stage.sort_order < stage.sort_order
        ).order_by(Stage.sort_order.desc()).first()
    elif direction == "down":
        # Find successor (smallest sort_order > current)
        target = db.query(Stage).filter(
            Stage.is_deleted == False,
            Stage.sort_order > stage.sort_order
        ).order_by(Stage.sort_order.asc()).first()
    else:
        raise AppException(code=400, message="无效的移动方向")

    if target:
        # Swap sort_orders
        stage.sort_order, target.sort_order = target.sort_order, stage.sort_order
        db.commit()
        return ok(msg="排序调整成功")
    
    return ok(msg="已处于边界，无需调整")
