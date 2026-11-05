import uuid
from datetime import datetime
from pydantic import BaseModel, Field, field_validator
from typing import List, Optional


class UserBase(BaseModel):
    username: str = Field(..., min_length=3, max_length=50)
    nickname: Optional[str] = Field(None, max_length=100)


class UserRegister(UserBase):
    password: str = Field(..., min_length=6, max_length=50)
    role: str = Field(..., description="ADMIN, STUDENT, or PARENT")
    
    # Student specific
    enrollment_year: Optional[int] = None
    semester_offset: int = 0
    
    # Parent specific
    student_ids: Optional[List[uuid.UUID]] = None

    @field_validator("role")
    @classmethod
    def validate_role(cls, v: str) -> str:
        role_upper = v.upper()
        if role_upper not in ["ADMIN", "STUDENT", "PARENT"]:
            raise ValueError("角色只能是 ADMIN, STUDENT, 或 PARENT")
        return role_upper


class UserLogin(BaseModel):
    username: str
    password: str


class UserOut(UserBase):
    id: uuid.UUID
    role: str
    created_at: datetime

    class Config:
        from_attributes = True


class StudentProfileOut(BaseModel):
    enrollment_year: int
    semester_offset: int

    class Config:
        from_attributes = True


class UserDetailOut(UserOut):
    student_profile: Optional[StudentProfileOut] = None
    bound_students: Optional[List['UserDetailOut']] = None


UserDetailOut.model_rebuild()


class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    user: UserDetailOut


class TokenRefresh(BaseModel):
    refresh_token: str
