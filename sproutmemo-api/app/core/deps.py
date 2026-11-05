import uuid
from fastapi import Depends
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.models.user import User
from app.core.auth_utils import decode_token
from app.core.exceptions import AuthException, ForbiddenException

# Token bearer scheme
security_scheme = HTTPBearer(auto_error=False)


def get_current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(security_scheme),
    db: Session = Depends(get_db),
) -> User:
    """Validate access token and yield the current active user"""
    if not credentials:
        raise AuthException("认证失败，未提供Token")
        
    token = credentials.credentials
    payload = decode_token(token, "access")
    
    user_id_str = payload.get("sub")
    if not user_id_str:
        raise AuthException("Token中缺少用户标识")
        
    try:
        user_id = uuid.UUID(user_id_str)
    except ValueError:
        raise AuthException("用户ID格式无效")
        
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise AuthException("用户不存在")
        
    # Optional: check if user is active if we support accounts freezing
    return user


def require_role(allowed_roles: list[str]):
    """Generic role dependency builder"""
    def dependency(current_user: User = Depends(get_current_user)) -> User:
        if current_user.role not in allowed_roles:
            raise ForbiddenException(f"无权执行此操作，仅限角色: {', '.join(allowed_roles)}")
        return current_user
    return dependency


# Specialized role checkers
require_admin = require_role(["ADMIN"])
require_student = require_role(["STUDENT"])
require_parent = require_role(["PARENT"])
require_student_or_parent = require_role(["STUDENT", "PARENT"])
require_student_or_admin = require_role(["STUDENT", "ADMIN"])
