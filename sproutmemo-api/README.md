# SproutMemo API (FastAPI Backend)

This is the production-grade PostgreSQL backend API for the SproutMemo (知拾) system, built with FastAPI, SQLAlchemy 2.0, Alembic, and JWT authentication.

## Default Admin Credentials
When the database is freshly initialized, a default administrator is seeded automatically:
- **Username**: `admin`
- **Password**: `admin123`
- **Role**: `ADMIN`

Configurable via `.env` using `DEFAULT_ADMIN_USERNAME` and `DEFAULT_ADMIN_PASSWORD`.

## Guidelines
- Follow the workspace rules in `.agents/AGENTS.md`.
- Keep single code files under 500 lines.
- Ensure all line endings are LF and files are UTF-8 encoded.

