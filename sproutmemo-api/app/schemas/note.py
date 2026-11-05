import uuid
from datetime import datetime
from pydantic import BaseModel, Field, field_validator
from typing import List, Optional


class AttachmentOut(BaseModel):
    id: uuid.UUID
    file_type: str
    storage_path: str
    original_name: str
    title: Optional[str] = None
    file_size: int
    sort_order: int
    duration: Optional[int] = None
    created_at: datetime

    class Config:
        from_attributes = True


class NoteBase(BaseModel):
    subject_id: uuid.UUID
    question_source_id: Optional[uuid.UUID] = None
    source_paper_name: Optional[str] = Field(None, max_length=150)
    unit_code: int = Field(999)
    page_number: Optional[str] = Field(None, max_length=50)
    auxiliary_text: Optional[str] = None
    difficulty_stars: float = Field(1.0, ge=1.0, le=5.0)
    mastery_level: str = Field(..., description="CLUELESS, PARTIAL, MASTERED")

    @field_validator("mastery_level")
    @classmethod
    def validate_mastery(cls, v: str) -> str:
        mast_upper = v.upper()
        if mast_upper not in ["CLUELESS", "PARTIAL", "MASTERED"]:
            raise ValueError("掌握程度必须是 CLUELESS, PARTIAL, 或者是 MASTERED")
        return mast_upper


class NoteCreate(NoteBase):
    semester_id: uuid.UUID
    exclude_from_review: bool = False


class NoteUpdate(BaseModel):
    semester_id: Optional[uuid.UUID] = None
    subject_id: Optional[uuid.UUID] = None
    question_source_id: Optional[uuid.UUID] = None
    source_paper_name: Optional[str] = None
    unit_code: Optional[int] = None
    page_number: Optional[str] = None
    auxiliary_text: Optional[str] = None
    difficulty_stars: Optional[float] = Field(None, ge=1.0, le=5.0)
    mastery_level: Optional[str] = None
    exclude_from_review: Optional[bool] = None
    
    # Concurrency control fields
    sync_base_time: datetime = Field(..., description="客户端记录的最后更新时间戳，用于乐观锁校验")
    force: bool = Field(False, description="是否强制覆盖")

    @field_validator("mastery_level")
    @classmethod
    def validate_mastery(cls, v: Optional[str]) -> Optional[str]:
        if v is None:
            return v
        mast_upper = v.upper()
        if mast_upper not in ["CLUELESS", "PARTIAL", "MASTERED"]:
            raise ValueError("掌握程度必须是 CLUELESS, PARTIAL, 或者是 MASTERED")
        return mast_upper


class NoteOut(BaseModel):
    id: uuid.UUID
    student_id: uuid.UUID
    student_name: Optional[str] = None
    semester_id: uuid.UUID
    semester_name: Optional[str] = None
    stage_id: Optional[uuid.UUID] = None
    stage_name: Optional[str] = None
    subject_id: uuid.UUID
    subject_name: Optional[str] = None
    question_source_id: Optional[uuid.UUID] = None
    question_source_name: Optional[str] = None
    source_paper_name: Optional[str] = None
    unit_code: int
    unit_name: Optional[str] = None
    page_number: Optional[str] = None
    auxiliary_text: Optional[str] = None
    difficulty_stars: float
    mastery_level: str
    exclude_from_review: bool
    created_at: datetime
    updated_at: datetime
    source_type: Optional[str] = "BOOK"
    paper_category: Optional[str] = None
    paper_category_name: Optional[str] = None
    attachments: List[AttachmentOut] = []

    class Config:
        from_attributes = True


class NoteListResponse(BaseModel):
    total: int
    page: int
    size: int
    items: List[NoteOut]
