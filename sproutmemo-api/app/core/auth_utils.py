import uuid
import jwt
from datetime import datetime, timedelta, timezone
from typing import Any, Dict

from app.core.config import settings
from app.core.exceptions import AuthException


def create_access_token(user_id: uuid.UUID, username: str, role: str) -> str:
    """Generate a short-lived access token (2 hours)"""
    expire = datetime.now(timezone.utc) + timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    to_encode: Dict[str, Any] = {
        "sub": str(user_id),
        "username": username,
        "role": role,
        "exp": int(expire.timestamp()),
        "type": "access"
    }
    encoded_jwt = jwt.encode(to_encode, settings.SECRET_KEY, algorithm=settings.ALGORITHM)
    return encoded_jwt


def create_refresh_token(user_id: uuid.UUID, token_id: uuid.UUID) -> str:
    """Generate a long-lived refresh token (365 days) with a jti ID matching the DB record"""
    expire = datetime.now(timezone.utc) + timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS)
    to_encode: Dict[str, Any] = {
        "sub": str(user_id),
        "jti": str(token_id),
        "exp": int(expire.timestamp()),
        "type": "refresh"
    }
    encoded_jwt = jwt.encode(to_encode, settings.SECRET_KEY, algorithm=settings.ALGORITHM)
    return encoded_jwt


def decode_token(token: str, expected_type: str = "access") -> Dict[str, Any]:
    """Decode a JWT and validate expiration and token type"""
    try:
        payload = jwt.decode(token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM])
        
        # Verify expiration
        exp = payload.get("exp")
        if not exp or datetime.now(timezone.utc).timestamp() > exp:
            raise AuthException("Token已过期")
            
        # Verify token type
        token_type = payload.get("type")
        if token_type != expected_type:
            raise AuthException("Token类型错误")
            
        return payload
    except jwt.PyJWTError:
        raise AuthException("Token无效或格式错误")
