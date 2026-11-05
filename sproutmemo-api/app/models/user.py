import uuid
from datetime import datetime, timezone
from sqlalchemy import String, Integer, ForeignKey, DateTime, Uuid
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.session import Base


class User(Base):
    __tablename__ = "users"

    id: Mapped[uuid.UUID] = mapped_column(
        Uuid, primary_key=True, default=uuid.uuid4
    )
    username: Mapped[str] = mapped_column(String(50), unique=True, nullable=False, index=True)
    password_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    role: Mapped[str] = mapped_column(String(20), nullable=False, index=True)  # ADMIN, STUDENT, PARENT
    nickname: Mapped[str | None] = mapped_column(String(100), nullable=True)
    is_active: Mapped[bool] = mapped_column(default=True, nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc), nullable=False
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        onupdate=lambda: datetime.now(timezone.utc),
        nullable=False,
    )

    # Relationships
    student_profile: Mapped["Student | None"] = relationship(
        "Student", back_populates="user", uselist=False, cascade="all, delete-orphan"
    )
    
    # Parent links
    parent_relations: Mapped[list["ParentStudentRelation"]] = relationship(
        "ParentStudentRelation",
        foreign_keys="[ParentStudentRelation.parent_id]",
        back_populates="parent",
        cascade="all, delete-orphan",
    )
    
    # Student links
    student_relations: Mapped[list["ParentStudentRelation"]] = relationship(
        "ParentStudentRelation",
        foreign_keys="[ParentStudentRelation.student_id]",
        back_populates="student",
        cascade="all, delete-orphan",
    )


class Student(Base):
    __tablename__ = "students"

    user_id: Mapped[uuid.UUID] = mapped_column(
        Uuid, ForeignKey("users.id", ondelete="CASCADE"), primary_key=True
    )
    enrollment_year: Mapped[int] = mapped_column(Integer, nullable=False)
    semester_offset: Mapped[int] = mapped_column(Integer, default=0, nullable=False)

    # Relationships
    user: Mapped["User"] = relationship("User", back_populates="student_profile")


class ParentStudentRelation(Base):
    __tablename__ = "parent_student_relations"

    parent_id: Mapped[uuid.UUID] = mapped_column(
        Uuid, ForeignKey("users.id", ondelete="CASCADE"), primary_key=True
    )
    student_id: Mapped[uuid.UUID] = mapped_column(
        Uuid, ForeignKey("users.id", ondelete="CASCADE"), primary_key=True
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc), nullable=False
    )

    # Relationships
    parent: Mapped["User"] = relationship(
        "User", foreign_keys=[parent_id], back_populates="parent_relations"
    )
    student: Mapped["User"] = relationship(
        "User", foreign_keys=[student_id], back_populates="student_relations"
    )
