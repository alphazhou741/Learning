from collections.abc import Generator

from sqlalchemy import create_engine
from sqlalchemy.orm import DeclarativeBase, Session, sessionmaker

from .config import get_database_url


class Base(DeclarativeBase):
    pass


def build_engine(url: str | None = None):
    target_url = url or get_database_url()
    connect_args = {"check_same_thread": False} if target_url.startswith("sqlite") else {}
    return create_engine(
        target_url,
        pool_pre_ping=True,
        connect_args=connect_args,
    )


engine = build_engine()
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


def get_db() -> Generator[Session, None, None]:
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
