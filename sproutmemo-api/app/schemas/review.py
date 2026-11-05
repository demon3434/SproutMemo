import uuid
from datetime import datetime
from typing import List, Optional
from pydantic import BaseModel, Field

from app.schemas.note import NoteOut


class ReviewStrategySchema(BaseModel):
    code: str
    name: str
    description: Optional[str] = None
    sort_order: int

    class Config:
        from_attributes = True


class ReviewCountPresetSchema(BaseModel):
    code: str
    count_value: int
    name: str
    sort_order: int

    class Config:
        from_attributes = True


class ReviewOutcomeSchema(BaseModel):
    code: str
    name: str
    color_hex: str
    suggested_mastery: Optional[str] = None
    sort_order: int

    class Config:
        from_attributes = True


class ReviewPresetsResponse(BaseModel):
    strategies: List[ReviewStrategySchema]
    count_presets: List[ReviewCountPresetSchema]
    outcomes: List[ReviewOutcomeSchema]


class CreateReviewSessionRequest(BaseModel):
    semester_id: uuid.UUID
    subject_id: Optional[uuid.UUID] = None
    unit_code: Optional[int] = None
    strategy_code: str = Field(default="EBBINGHAUS")
    count_preset_code: str = Field(default="PRESET_5")
    student_id: Optional[uuid.UUID] = None
    note_ids: Optional[List[uuid.UUID]] = None


class ReviewHistoryItem(BaseModel):
    id: uuid.UUID
    student_id: uuid.UUID
    semester_id: uuid.UUID
    semester_name: str
    subject_id: Optional[uuid.UUID] = None
    subject_name: Optional[str] = None
    strategy_code: str
    strategy_name: str
    target_count: int
    completed_count: int
    status: str
    created_at: datetime
    completed_at: Optional[datetime] = None

    class Config:
        from_attributes = True


class ReviewSessionResponse(BaseModel):
    id: uuid.UUID
    student_id: uuid.UUID
    semester_id: uuid.UUID
    subject_id: Optional[uuid.UUID] = None
    unit_code: Optional[int] = None
    strategy_code: str
    target_count: int
    completed_count: int
    status: str
    created_at: datetime
    notes: List[NoteOut]

    class Config:
        from_attributes = True


class SubmitReviewItemRequest(BaseModel):
    note_id: uuid.UUID
    duration_seconds: float = Field(default=0.0, ge=0)
    outcome_code: str  # TOO_EASY, WRONG_AGAIN, STILL_CLUELESS
    new_mastery: str  # CLUELESS, PARTIAL, MASTERED
    new_stars: float = Field(default=1.0, ge=1.0, le=5.0)
    exclude_from_review: bool = Field(default=False)
    sequence_index: int = Field(default=1)


class SubmitReviewItemResponse(BaseModel):
    session_id: uuid.UUID
    note_id: uuid.UUID
    completed_count: int
    target_count: int
    is_session_completed: bool
