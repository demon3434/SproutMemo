import uuid
from datetime import datetime
from pydantic import BaseModel
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.models.user import User, Student, ParentStudentRelation
from app.models.system import Subject
from app.core.deps import require_admin
from app.core.security import get_password_hash
from app.core.exceptions import AppException, NotFoundException
from app.core.response import ok


# Router definition
router = APIRouter(prefix="/users", tags=["User Management"])


# Schemas
class UserCreateAdmin(BaseModel):
    username: str
    password: str | None = None
    nickname: str | None = None
    role: str  # STUDENT or PARENT
    enrollment_year: int | None = None
    semester_offset: int = 0
    student_ids: list[uuid.UUID] | None = None


class UserResetPassword(BaseModel):
    new_password: str | None = None


class UserEditAdmin(BaseModel):
    username: str | None = None
    nickname: str | None = None
    role: str | None = None
    enrollment_year: int | None = None
    semester_offset: int | None = None
    student_ids: list[uuid.UUID] | None = None


from app.schemas.user import StudentProfileOut


class UserToggleActive(BaseModel):
    is_active: bool


class UserAdminOut(BaseModel):
    id: uuid.UUID
    username: str
    role: str
    nickname: str | None
    is_active: bool
    student_profile: StudentProfileOut | None = None
    student_ids: list[uuid.UUID] | None = None
    created_at: datetime

    class Config:
        from_attributes = True


def serialize_user_admin(u: User) -> UserAdminOut:
    out = UserAdminOut.model_validate(u)
    if u.role == "PARENT":
        out.student_ids = [r.student_id for r in (u.parent_relations or [])]
    return out


@router.post("", response_model=dict)
def admin_create_user(
    data: UserCreateAdmin,
    current_user: User = Depends(require_admin),
    db: Session = Depends(get_db)
):
    """Admin-only endpoint to create a parent or student account"""
    # Check if duplicate exists
    existing = db.query(User).filter(User.username == data.username).first()
    if existing:
        raise AppException(code=400, message="用户名已存在")

    pwd = data.password.strip() if (data.password and data.password.strip()) else "123456"
    user = User(
        username=data.username,
        password_hash=get_password_hash(pwd),
        role=data.role,
        nickname=data.nickname,
        is_active=True
    )
    db.add(user)
    db.flush()

    if data.role == "STUDENT":
        if data.enrollment_year is None:
            raise AppException(code=400, message="创建学生用户必须填写入学年份 (enrollment_year)")
        student = Student(
            user_id=user.id,
            enrollment_year=data.enrollment_year,
            semester_offset=data.semester_offset
        )
        db.add(student)
    elif data.role == "PARENT":
        if data.student_ids:
            for s_id in data.student_ids:
                student_user = db.query(User).filter(User.id == s_id, User.role == "STUDENT").first()
                if not student_user:
                    raise AppException(code=404, message=f"绑定学生ID {s_id} 不存在或不是学生")
                relation = ParentStudentRelation(parent_id=user.id, student_id=s_id)
                db.add(relation)

    db.commit()
    db.refresh(user)
    return ok(data=serialize_user_admin(user), msg="用户创建成功")


@router.get("", response_model=dict)
def admin_list_users(
    role: str | None = None,
    page: int | None = None,
    limit: int = 10,
    current_user: User = Depends(require_admin),
    db: Session = Depends(get_db)
):
    """Admin-only endpoint to list all users with optional pagination"""
    query = db.query(User)
    if role:
        query = query.filter(User.role == role)
    
    total = query.count()
    
    if page is not None:
        offset = (page - 1) * limit
        users = query.order_by(User.created_at.desc()).offset(offset).limit(limit).all()
    else:
        users = query.order_by(User.created_at.desc()).all()
        
    items = [serialize_user_admin(u) for u in users]
    return ok(data={
        "items": items,
        "total": total,
        "page": page or 1,
        "limit": limit
    }, msg="获取成功")


@router.put("/{id}/reset_password", response_model=dict)
def admin_reset_password(
    id: uuid.UUID,
    data: UserResetPassword,
    current_user: User = Depends(require_admin),
    db: Session = Depends(get_db)
):
    """Admin-only endpoint to reset a user's password"""
    user = db.query(User).filter(User.id == id).first()
    if not user:
        raise NotFoundException("未找到对应的用户")

    pwd = data.new_password.strip() if (data.new_password and data.new_password.strip()) else "123456"
    user.password_hash = get_password_hash(pwd)
    db.commit()
    return ok(msg="密码重置成功")


@router.put("/{id}", response_model=dict)
def admin_edit_user(
    id: uuid.UUID,
    data: UserEditAdmin,
    current_user: User = Depends(require_admin),
    db: Session = Depends(get_db)
):
    """Admin-only endpoint to edit a user's details, role, and associations"""
    user = db.query(User).filter(User.id == id).first()
    if not user:
        raise NotFoundException("未找到对应的用户")

    # Protection: built-in admin account and ADMIN role cannot be modified
    if user.username == "admin" or user.role == "ADMIN":
        raise AppException(code=400, message="系统内置管理员账号禁止修改")

    target_role = data.role.upper().strip() if (data.role and data.role.strip()) else user.role
    if target_role not in ["STUDENT", "PARENT"]:
        raise AppException(code=400, message="普通用户角色仅支持 STUDENT 或 PARENT")

    if data.username is not None and data.username.strip():
        new_username = data.username.strip()
        if new_username != user.username:
            # Check duplicate
            existing = db.query(User).filter(User.username == new_username, User.id != id).first()
            if existing:
                raise AppException(code=400, message="用户名已存在")
            user.username = new_username

    if data.nickname is not None:
        user.nickname = data.nickname.strip()

    # Role switching and role-specific updates
    if user.role == "STUDENT" and target_role == "PARENT":
        from app.models.note import Note
        has_notes = db.query(Note).filter(Note.student_id == user.id).first()
        if has_notes:
            raise AppException(code=400, message="该学生账号下已存在错题数据，无法切换为家长账号")
        
        if user.student_profile:
            db.delete(user.student_profile)
        user.role = "PARENT"
        if data.student_ids is not None:
            for s_id in data.student_ids:
                s_user = db.query(User).filter(User.id == s_id, User.role == "STUDENT").first()
                if not s_user:
                    raise AppException(code=404, message=f"绑定学生ID {s_id} 不存在或不是学生")
                db.add(ParentStudentRelation(parent_id=user.id, student_id=s_id))

    elif user.role == "PARENT" and target_role == "STUDENT":
        db.query(ParentStudentRelation).filter(ParentStudentRelation.parent_id == user.id).delete()
        user.role = "STUDENT"
        enroll_yr = data.enrollment_year if data.enrollment_year is not None else datetime.now().year
        sem_off = data.semester_offset if data.semester_offset is not None else 0
        student = Student(
            user_id=user.id,
            enrollment_year=enroll_yr,
            semester_offset=sem_off
        )
        db.add(student)

    elif user.role == "STUDENT" and target_role == "STUDENT":
        if user.student_profile:
            if data.enrollment_year is not None:
                user.student_profile.enrollment_year = data.enrollment_year
            if data.semester_offset is not None:
                user.student_profile.semester_offset = data.semester_offset
        else:
            enroll_yr = data.enrollment_year if data.enrollment_year is not None else datetime.now().year
            sem_off = data.semester_offset if data.semester_offset is not None else 0
            student = Student(
                user_id=user.id,
                enrollment_year=enroll_yr,
                semester_offset=sem_off
            )
            db.add(student)

    elif user.role == "PARENT" and target_role == "PARENT":
        if data.student_ids is not None:
            db.query(ParentStudentRelation).filter(ParentStudentRelation.parent_id == user.id).delete()
            for s_id in data.student_ids:
                s_user = db.query(User).filter(User.id == s_id, User.role == "STUDENT").first()
                if not s_user:
                    raise AppException(code=404, message=f"绑定学生ID {s_id} 不存在或不是学生")
                db.add(ParentStudentRelation(parent_id=user.id, student_id=s_id))

    db.commit()
    db.refresh(user)
    return ok(data=serialize_user_admin(user), msg="用户信息修改成功")


@router.put("/{id}/toggle_active", response_model=dict)
def admin_toggle_active(
    id: uuid.UUID,
    data: UserToggleActive,
    current_user: User = Depends(require_admin),
    db: Session = Depends(get_db)
):
    """Admin-only endpoint to enable or disable a user account"""
    user = db.query(User).filter(User.id == id).first()
    if not user:
        raise NotFoundException("未找到对应的用户")

    # Prevent admin disabling themselves
    if user.id == current_user.id:
        raise AppException(code=400, message="无法停用您自己的管理员账号")

    user.is_active = data.is_active
    db.commit()
    db.refresh(user)
    return ok(data=serialize_user_admin(user), msg="状态更新成功")
