from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, DeclarativeBase
from app.core.config import settings

# Normalize PostgreSQL driver for psycopg2
db_url = settings.DATABASE_URL
if db_url.startswith("postgresql://"):
    db_url = db_url.replace("postgresql://", "postgresql+psycopg2://", 1)

# Create PostgreSQL database engine with connection pooling
engine = create_engine(
    db_url,
    pool_size=10,
    max_overflow=20,
    pool_pre_ping=True,
    echo=settings.DEBUG,
)

# Local database session class
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


# Base class for database models
class Base(DeclarativeBase):
    pass


# Database session dependency injection
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
