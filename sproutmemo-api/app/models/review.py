import uuid
from datetime import datetime, timezone
from sqlalchemy import String, Integer, ForeignKey, DateTime, Boolean, Numeric, Text, Uuid, Float
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.session import Base


class ReviewStrategy(Base):
    """复习筛题策略维度表"""
    __tablename__ = "review_strategies"

    code: Mapped[str] = mapped_column(String(30), primary_key=True)  # RANDOM, EBBINGHAUS, CHALLENGE, PERSISTENT_FAILURE
    name: Mapped[str] = mapped_column(String(50), nullable=False)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
    sort_order: Mapped[int] = mapped_column(Integer, default=0, nullable=False)


class ReviewCountPreset(Base):
    """题量档位预设维度表"""
    __tablename__ = "review_count_presets"

    code: Mapped[str] = mapped_column(String(30), primary_key=True)  # PRESET_3, PRESET_5, PRESET_8, PRESET_10
    count_value: Mapped[int] = mapped_column(Integer, nullable=False)
    name: Mapped[str] = mapped_column(String(50), nullable=False)
    sort_order: Mapped[int] = mapped_column(Integer, default=0, nullable=False)


class ReviewOutcome(Base):
    """练习回单结果维度表"""
    __tablename__ = "review_outcomes"

    code: Mapped[str] = mapped_column(String(30), primary_key=True)  # TOO_EASY, WRONG_AGAIN, STILL_CLUELESS
    name: Mapped[str] = mapped_column(String(50), nullable=False)
    color_hex: Mapped[str] = mapped_column(String(20), nullable=False, default="#3b82f6")
    suggested_mastery: Mapped[str | None] = mapped_column(String(20), nullable=True)
    sort_order: Mapped[int] = mapped_column(Integer, default=0, nullable=False)


class ReviewSession(Base):
    """复习会话主表"""
    __tablename__ = "review_sessions"

    id: Mapped[uuid.UUID] = mapped_column(
        Uuid, primary_key=True, default=uuid.uuid4
    )
    student_id: Mapped[uuid.UUID] = mapped_column(
        Uuid, ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True
    )
    semester_id: Mapped[uuid.UUID] = mapped_column(
        Uuid, ForeignKey("semesters.id", ondelete="CASCADE"), nullable=False, index=True
    )
    subject_id: Mapped[uuid.UUID | None] = mapped_column(
        Uuid, ForeignKey("subjects.id", ondelete="SET NULL"), nullable=True, index=True
    )
    unit_code: Mapped[int | None] = mapped_column(
        Integer, ForeignKey("system_units.id", ondelete="SET NULL"), nullable=True, index=True
    )
    strategy_code: Mapped[str] = mapped_column(
        String(30), ForeignKey("review_strategies.code", ondelete="RESTRICT"), nullable=False
    )
    target_count: Mapped[int] = mapped_column(Integer, nullable=False, default=5)
    completed_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    status: Mapped[str] = mapped_column(String(20), nullable=False, default="IN_PROGRESS", index=True)  # IN_PROGRESS, COMPLETED, ABANDONED

    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc), nullable=False, index=True
    )
    completed_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )

    # Relationships
    student: Mapped["User"] = relationship("User", foreign_keys=[student_id])
    semester: Mapped["Semester"] = relationship("Semester", foreign_keys=[semester_id])
    subject: Mapped["Subject | None"] = relationship("Subject", foreign_keys=[subject_id])
    strategy: Mapped["ReviewStrategy"] = relationship("ReviewStrategy", foreign_keys=[strategy_code])
    logs: Mapped[list["ReviewLog"]] = relationship("ReviewLog", back_populates="session", cascade="all, delete-orphan", order_by="ReviewLog.sequence_index")
    session_notes: Mapped[list["ReviewSessionNote"]] = relationship(
        "ReviewSessionNote", back_populates="session", cascade="all, delete-orphan", order_by="ReviewSessionNote.sort_order"
    )


class ReviewLog(Base):
    """单题复习日志明细表"""
    __tablename__ = "review_logs"

    id: Mapped[uuid.UUID] = mapped_column(
        Uuid, primary_key=True, default=uuid.uuid4
    )
    session_id: Mapped[uuid.UUID] = mapped_column(
        Uuid, ForeignKey("review_sessions.id", ondelete="CASCADE"), nullable=False, index=True
    )
    student_id: Mapped[uuid.UUID] = mapped_column(
        Uuid, ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True
    )
    note_id: Mapped[uuid.UUID] = mapped_column(
        Uuid, ForeignKey("notes.id", ondelete="CASCADE"), nullable=False, index=True
    )
    sequence_index: Mapped[int] = mapped_column(Integer, nullable=False, default=1)
    duration_seconds: Mapped[float] = mapped_column(Float, nullable=False, default=0.0)
    outcome_code: Mapped[str] = mapped_column(
        String(30), ForeignKey("review_outcomes.code", ondelete="RESTRICT"), nullable=False
    )
    previous_mastery: Mapped[str] = mapped_column(String(20), nullable=False)
    new_mastery: Mapped[str] = mapped_column(String(20), nullable=False)
    previous_stars: Mapped[float] = mapped_column(Numeric(2, 1), nullable=False)
    new_stars: Mapped[float] = mapped_column(Numeric(2, 1), nullable=False)
    exclude_from_review_snapshot: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)

    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc), nullable=False, index=True
    )

    # Relationships
    session: Mapped["ReviewSession"] = relationship("ReviewSession", back_populates="logs")
    note: Mapped["Note"] = relationship("Note")
    outcome: Mapped["ReviewOutcome"] = relationship("ReviewOutcome")


class ReviewSessionNote(Base):
    """复习会话与题目关联明细表"""
    __tablename__ = "review_session_notes"

    id: Mapped[uuid.UUID] = mapped_column(
        Uuid, primary_key=True, default=uuid.uuid4
    )
    session_id: Mapped[uuid.UUID] = mapped_column(
        Uuid, ForeignKey("review_sessions.id", ondelete="CASCADE"), nullable=False, index=True
    )
    note_id: Mapped[uuid.UUID] = mapped_column(
        Uuid, ForeignKey("notes.id", ondelete="CASCADE"), nullable=False, index=True
    )
    sort_order: Mapped[int] = mapped_column(Integer, nullable=False, default=0)

    # Relationships
    session: Mapped["ReviewSession"] = relationship("ReviewSession", back_populates="session_notes")
    note: Mapped["Note"] = relationship("Note")

