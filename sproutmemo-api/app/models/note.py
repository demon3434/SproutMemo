import uuid
from datetime import datetime, timezone
from sqlalchemy import String, Integer, ForeignKey, DateTime, Boolean, Numeric, Text, Uuid, ForeignKeyConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.session import Base


class Note(Base):
    __tablename__ = "notes"

    id: Mapped[uuid.UUID] = mapped_column(
        Uuid, primary_key=True, default=uuid.uuid4
    )
    student_id: Mapped[uuid.UUID] = mapped_column(
        Uuid, ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True
    )
    semester_id: Mapped[uuid.UUID] = mapped_column(
        Uuid, ForeignKey("semesters.id", ondelete="CASCADE"), nullable=False, index=True
    )
    subject_id: Mapped[uuid.UUID] = mapped_column(
        Uuid, ForeignKey("subjects.id", ondelete="CASCADE"), nullable=False, index=True
    )
    question_source_id: Mapped[uuid.UUID | None] = mapped_column(
        Uuid, nullable=True, index=True
    )
    source_paper_name: Mapped[str | None] = mapped_column(String(150), nullable=True)
    unit_code: Mapped[int] = mapped_column(
        Integer, ForeignKey("system_units.id", ondelete="RESTRICT"), nullable=False, default=999, index=True
    )
    page_number: Mapped[str | None] = mapped_column(String(50), nullable=True)
    auxiliary_text: Mapped[str | None] = mapped_column(Text, nullable=True)
    difficulty_stars: Mapped[float] = mapped_column(Numeric(2, 1), default=1.0, nullable=False)
    mastery_level: Mapped[str] = mapped_column(String(20), nullable=False)  # CLUELESS, PARTIAL, MASTERED
    exclude_from_review: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    last_reviewed_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True, index=True
    )
    sync_base_time: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc), nullable=False
    )
    is_deleted: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False, index=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc), nullable=False, index=True
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        onupdate=lambda: datetime.now(timezone.utc),
        nullable=False,
    )

    __table_args__ = (
        ForeignKeyConstraint(
            ["question_source_id", "student_id", "semester_id", "subject_id"],
            ["question_sources.id", "question_sources.student_id", "question_sources.semester_id", "question_sources.subject_id"],
            ondelete="SET NULL",
            name="fk_note_question_source_composite"
        ),
    )

    # Relationships
    student: Mapped["User"] = relationship("User", foreign_keys=[student_id])
    semester: Mapped["Semester"] = relationship("Semester", foreign_keys=[semester_id])
    subject: Mapped["Subject"] = relationship("Subject", foreign_keys=[subject_id])
    question_source: Mapped["QuestionSource | None"] = relationship("QuestionSource", foreign_keys=[question_source_id])
    unit: Mapped["SystemUnit"] = relationship("SystemUnit")
    attachments: Mapped[list["Attachment"]] = relationship(
        "Attachment", back_populates="note", cascade="all, delete-orphan", order_by="Attachment.sort_order"
    )


class Attachment(Base):
    __tablename__ = "attachments"

    id: Mapped[uuid.UUID] = mapped_column(
        Uuid, primary_key=True, default=uuid.uuid4
    )
    note_id: Mapped[uuid.UUID] = mapped_column(
        Uuid, ForeignKey("notes.id", ondelete="CASCADE"), nullable=False, index=True
    )
    file_type: Mapped[str] = mapped_column(String(20), nullable=False)  # IMAGE, AUDIO, VIDEO
    storage_path: Mapped[str] = mapped_column(String(255), nullable=False)
    original_name: Mapped[str] = mapped_column(String(255), nullable=False)
    title: Mapped[str | None] = mapped_column(String(255), nullable=True)
    file_size: Mapped[int] = mapped_column(nullable=False)
    sort_order: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    duration: Mapped[int | None] = mapped_column(Integer, nullable=True)  # Duration in seconds for audio/video
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc), nullable=False
    )

    # Relationships
    note: Mapped["Note"] = relationship("Note", back_populates="attachments")
