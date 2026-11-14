from sqlalchemy import BigInteger, Boolean, Float, ForeignKey, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from .db import Base


class User(Base):
    __tablename__ = "users"

    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    username: Mapped[str] = mapped_column(String(64), unique=True, index=True, nullable=False)
    password_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    created_at_ms: Mapped[int] = mapped_column(BigInteger, nullable=False)

    children: Mapped[list["ChildProfile"]] = relationship(
        back_populates="owner",
        cascade="all, delete-orphan",
    )


class ChildProfile(Base):
    __tablename__ = "child_profiles"

    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    user_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("users.id", ondelete="CASCADE"), index=True, nullable=False
    )
    name: Mapped[str] = mapped_column(String(64), nullable=False, default="小宇")
    grade: Mapped[str] = mapped_column(String(32), nullable=False, default="二年级")
    avatar_tone: Mapped[str] = mapped_column(String(24), nullable=False, default="amber")
    default_term: Mapped[str] = mapped_column(String(8), nullable=False, default="上")
    mastery_streak: Mapped[int] = mapped_column(Integer, nullable=False, default=2)
    en_audio_mode: Mapped[str] = mapped_column(String(24), nullable=False, default="bundled_tts")
    speech_rate: Mapped[float] = mapped_column(Float, nullable=False, default=0.92)
    reset_version: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    updated_at_ms: Mapped[int] = mapped_column(BigInteger, nullable=False)
    created_at_ms: Mapped[int] = mapped_column(BigInteger, nullable=False)

    owner: Mapped["User"] = relationship(back_populates="children")


class TopicRecordRow(Base):
    __tablename__ = "topic_records"

    child_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("child_profiles.id", ondelete="CASCADE"), primary_key=True
    )
    topic_id: Mapped[str] = mapped_column(String(64), primary_key=True)
    status: Mapped[str] = mapped_column(String(16), nullable=False, default="UNKNOWN")
    consecutive_correct: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    attempts: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    wrong_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    last_question_id: Mapped[str | None] = mapped_column(String(64), nullable=True)
    updated_at_ms: Mapped[int] = mapped_column(BigInteger, nullable=False, default=0)


class StudyLogRow(Base):
    __tablename__ = "study_logs"

    child_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("child_profiles.id", ondelete="CASCADE"), primary_key=True
    )
    study_date: Mapped[str] = mapped_column(String(10), primary_key=True)  # YYYY-MM-DD
    math_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    chinese_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    english_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    minutes: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    note: Mapped[str] = mapped_column(Text, nullable=False, default="")
    updated_at_ms: Mapped[int] = mapped_column(BigInteger, nullable=False, default=0)


class QuizEventRow(Base):
    __tablename__ = "quiz_events"

    event_id: Mapped[str] = mapped_column(String(64), primary_key=True)
    child_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("child_profiles.id", ondelete="CASCADE"), index=True, nullable=False
    )
    subject: Mapped[str] = mapped_column(String(16), nullable=False)
    topic_id: Mapped[str] = mapped_column(String(64), nullable=False)
    question_id: Mapped[str] = mapped_column(String(64), nullable=False)
    is_correct: Mapped[bool] = mapped_column(Boolean, nullable=False)
    is_skipped: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    study_date: Mapped[str] = mapped_column(String(10), nullable=False)
    note: Mapped[str] = mapped_column(String(128), nullable=False, default="")
    created_at_ms: Mapped[int] = mapped_column(BigInteger, nullable=False)
