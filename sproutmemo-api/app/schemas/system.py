import uuid
from datetime import datetime
from pydantic import BaseModel, Field
from typing import Optional, List


class SourceBookBase(BaseModel):
    book_name: str = Field(..., min_length=1, max_length=150)
    publisher: Optional[str] = Field(None, max_length=100)


class SourceBookCreate(SourceBookBase):
    student_id: Optional[uuid.UUID] = Field(None, description="书本所属的学生ID，学生角色创建时无需填写，家长角色创建时必填")
    grade_semester: Optional[str] = Field(None, description="书本适用的年级学期，若为空则由系统自动根据当前学期推算")


class SourceBookUpdate(BaseModel):
    book_name: Optional[str] = Field(None, min_length=1, max_length=150)
    publisher: Optional[str] = Field(None, max_length=100)
    is_archived: Optional[bool] = None


class SourceBookOut(SourceBookBase):
    id: uuid.UUID
    student_id: uuid.UUID
    grade_semester: str
    is_archived: bool
    created_at: datetime

    class Config:
        from_attributes = True


class SourceBookListResponse(BaseModel):
    items: List[SourceBookOut]
