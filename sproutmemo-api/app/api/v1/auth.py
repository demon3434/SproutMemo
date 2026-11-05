import uuid
from datetime import datetime, timezone, timedelta
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.models.user import User, Student, ParentStudentRelation
from app.models.system import RefreshToken, Subject
from app.schemas.user import UserRegister, UserLogin, TokenResponse, TokenRefresh, UserOut, UserDetailOut, StudentProfileOut
from app.core.security import get_password_hash, verify_password
from app.core.auth_utils import create_access_token, create_refresh_token, decode_token
from app.core.exceptions import AppException, AuthException
from app.core.response import ok
from app.core.config import settings
from app.core.deps import get_current_user

router = APIRouter(prefix="/auth", tags=["Authentication"])


def get_user_detail_response(user: User, db: Session) -> UserDetailOut:
    """Helper to convert User model to UserDetailOut schema including profiles and relation mappings"""
    student_profile = None
    if user.role == "STUDENT" and user.student_profile:
        student_profile = StudentProfileOut(
            enrollment_year=user.student_profile.enrollment_year,
            semester_offset=user.student_profile.semester_offset
        )
        
    bound_students = None
    if user.role == "PARENT":
        bound_students = []
        relations = db.query(ParentStudentRelation).filter(ParentStudentRelation.parent_id == user.id).all()
        for rel in relations:
            student_user = db.query(User).filter(User.id == rel.student_id).first()
            if student_user:
                child_profile = None
                if student_user.student_profile:
                    child_profile = StudentProfileOut(
                        enrollment_year=student_user.student_profile.enrollment_year,
                        semester_offset=student_user.student_profile.semester_offset
                    )
                bound_students.append(UserDetailOut(
                    id=student_user.id,
                    username=student_user.username,
                    role=student_user.role,
                    nickname=student_user.nickname,
                    created_at=student_user.created_at,
                    student_profile=child_profile,
                    bound_students=None
                ))

    return UserDetailOut(
        id=user.id,
        username=user.username,
        role=user.role,
        nickname=user.nickname,
        created_at=user.created_at,
        student_profile=student_profile,
        bound_students=bound_students
    )


@router.post("/register", response_model=dict)
def register(data: UserRegister, db: Session = Depends(get_db)):
    """Register a new user (ADMIN, STUDENT, or PARENT)"""
    # Check if username exists
    existing_user = db.query(User).filter(User.username == data.username).first()
    if existing_user:
        raise AppException(code=400, message="用户名已存在")

    # Create new User
    user = User(
        username=data.username,
        password_hash=get_password_hash(data.password),
        role=data.role,
        nickname=data.nickname,
    )
    db.add(user)
    db.flush()  # Populate user.id

    # Role specific creation
    if data.role == "STUDENT":
        if data.enrollment_year is None:
            raise AppException(code=400, message="学生注册必须填写入学年份 (enrollment_year)")
        student = Student(
            user_id=user.id,
            enrollment_year=data.enrollment_year,
            semester_offset=data.semester_offset
        )
        db.add(student)
    elif data.role == "PARENT":
        if data.student_ids:
            for s_id in data.student_ids:
                # Check student exists and indeed is a student
                student_user = db.query(User).filter(User.id == s_id, User.role == "STUDENT").first()
                if not student_user:
                    raise AppException(code=404, message=f"绑定的学生ID {s_id} 不存在或不是学生角色")
                relation = ParentStudentRelation(parent_id=user.id, student_id=s_id)
                db.add(relation)

    db.commit()
    db.refresh(user)

    # Return structured success envelope
    user_detail = get_user_detail_response(user, db)
    return ok(data=user_detail, msg="注册成功")


@router.post("/login", response_model=dict)
def login(data: UserLogin, db: Session = Depends(get_db)):
    """User login endpoint returning access token and long-lived refresh token"""
    user = db.query(User).filter(User.username == data.username).first()
    if not user or not verify_password(data.password, user.password_hash):
        raise AppException(code=400, message="用户名或密码错误")

    # Clean old expired refresh tokens for this user
    db.query(RefreshToken).filter(
        RefreshToken.user_id == user.id,
        RefreshToken.expired_at < datetime.now(timezone.utc)
    ).delete()

    # Create token DB record
    token_id = uuid.uuid4()
    expire_time = datetime.now(timezone.utc) + timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS)
    
    # Generate token strings
    access_token = create_access_token(user.id, user.username, user.role)
    refresh_token_str = create_refresh_token(user.id, token_id)

    db_refresh_token = RefreshToken(
        id=token_id,
        user_id=user.id,
        token=refresh_token_str,
        expired_at=expire_time
    )
    db.add(db_refresh_token)
    db.commit()

    user_detail = get_user_detail_response(user, db)
    token_res = TokenResponse(
        access_token=access_token,
        refresh_token=refresh_token_str,
        user=user_detail
    )

    return ok(data=token_res.model_dump(), msg="登录成功")


@router.post("/refresh", response_model=dict)
def refresh(data: TokenRefresh, db: Session = Depends(get_db)):
    """Exchange a valid Refresh Token for a new Access Token and refreshed Refresh Token"""
    # 1. Decode token
    payload = decode_token(data.refresh_token, "refresh")
    token_id_str = payload.get("jti")
    user_id_str = payload.get("sub")
    
    if not token_id_str or not user_id_str:
        raise AuthException("Refresh Token无效")

    token_id = uuid.UUID(token_id_str)
    
    # 2. Check if token exists in DB
    db_token = db.query(RefreshToken).filter(RefreshToken.id == token_id).first()
    if not db_token:
        raise AuthException("会话已失效，请重新登录")

    # 3. Check expiration
    expired_at = db_token.expired_at
    now = datetime.now(timezone.utc)
    if expired_at.tzinfo is None:
        now = now.replace(tzinfo=None)
    if expired_at < now:
        db.delete(db_token)
        db.commit()
        raise AuthException("会话已过期，请重新登录")

    # 4. Get User
    user = db.query(User).filter(User.id == db_token.user_id).first()
    if not user:
        raise AuthException("用户不存在")

    # 5. Generate new access token
    new_access_token = create_access_token(user.id, user.username, user.role)

    # 6. Rotate Refresh Token for security (optional, but recommended)
    # Let's delete the old token and issue a new one
    db.delete(db_token)
    
    new_token_id = uuid.uuid4()
    new_expire_time = datetime.now(timezone.utc) + timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS)
    new_refresh_token_str = create_refresh_token(user.id, new_token_id)

    new_db_refresh_token = RefreshToken(
        id=new_token_id,
        user_id=user.id,
        token=new_refresh_token_str,
        expired_at=new_expire_time
    )
    db.add(new_db_refresh_token)
    db.commit()

    user_detail = get_user_detail_response(user, db)
    token_res = TokenResponse(
        access_token=new_access_token,
        refresh_token=new_refresh_token_str,
        user=user_detail
    )

    return ok(data=token_res.model_dump(), msg="刷新成功")


@router.get("/me", response_model=dict)
def get_me(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    """Get current logged-in user profile"""
    user_detail = get_user_detail_response(current_user, db)
    return ok(data=user_detail, msg="获取成功")


from pydantic import BaseModel, Field
from typing import Optional

class ProfileUpdate(BaseModel):
    nickname: Optional[str] = Field(None, max_length=100)
    password: Optional[str] = Field(None, min_length=6)
    semester_offset: Optional[int] = None


@router.put("/profile", response_model=dict)
def update_profile(
    data: ProfileUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Update current user's profile info (nickname, password, or student semester_offset)"""
    if data.nickname is not None:
        current_user.nickname = data.nickname.strip() or None

    if data.password is not None:
        pwd = data.password.strip()
        if pwd:
            current_user.password_hash = get_password_hash(pwd)

    if data.semester_offset is not None:
        if current_user.role != "STUDENT":
            raise AppException(code=400, message="只有学生账号可以调整学期偏移量")
        
        student_profile = db.query(Student).filter(Student.user_id == current_user.id).first()
        if not student_profile:
            raise NotFoundException("未找到该学生的账户档案记录")
        student_profile.semester_offset = data.semester_offset

    db.commit()
    db.refresh(current_user)

    user_detail = get_user_detail_response(current_user, db)
    return ok(data=user_detail, msg="修改个人信息成功")

