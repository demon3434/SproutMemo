import os
from pydantic_settings import BaseSettings
from functools import lru_cache


class Settings(BaseSettings):
    # App
    APP_NAME: str = "SproutMemo"
    APP_VERSION: str = "1.0.0"
    DEBUG: bool = False

    # Database (PostgreSQL default)
    DATABASE_URL: str = "postgresql://sprout:sprout@localhost:5432/sproutmemo"

    # JWT Authentication
    SECRET_KEY: str = "sproutmemo-secret-key-change-in-production-2026"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 120        # 2 hours
    REFRESH_TOKEN_EXPIRE_DAYS: int = 365          # 365 days for child long session

    # File Storage
    DATA_DIR: str = "./data"
    UPLOAD_DIR: str = "./data/uploads"
    BACKUP_DIR: str = "./data/backups"
    SNAPSHOT_DIR: str = "./data/snapshots"
    LOG_DIR: str = "./data/logs"

    # Upload limit (bytes)
    MAX_UPLOAD_SIZE: int = 100 * 1024 * 1024      # 100MB

    # LAN Discovery (mDNS) defaults
    SERVICE_DISCOVERY_NAME: str = "SproutMemo"
    SERVICE_DISCOVERY_TYPE: str = "_sproutmemo._tcp.local."
    SERVICE_ADVERTISE_HOST: str = ""
    SERVICE_ADVERTISE_PORT: int = 8080
    HOST_IP: str = ""
    HOST_PORT: int = 8080

    # Default admin
    DEFAULT_ADMIN_USERNAME: str = "admin"
    DEFAULT_ADMIN_PASSWORD: str = "admin123"

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        extra = "ignore"


@lru_cache()
def get_settings() -> Settings:
    return Settings()


settings = get_settings()
