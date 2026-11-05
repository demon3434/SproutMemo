import os
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI, Request
from fastapi.staticfiles import StaticFiles
from fastapi.middleware.cors import CORSMiddleware
from fastapi.exceptions import RequestValidationError
from starlette.exceptions import HTTPException as StarletteHTTPException

from app.core.config import settings
from app.core.exceptions import AppException
from app.core.response import make_response
from app.api.v1.auth import router as auth_router
from app.api.v1.source_books import router as source_books_router
from app.api.v1.notes import router as notes_router
from app.api.v1.system import router as system_router
from app.api.v1.subjects import router as subjects_router
from app.api.v1.users import router as users_router
from app.api.v1.semester_settings import router as semester_settings_router
from app.api.v1.semesters import router as semesters_router
from app.api.v1.stages import router as stages_router
from app.api.v1.parent import router as parent_router
from app.api.v1.review import router as review_router
from app.db.session import SessionLocal
from app.models.user import User
from app.models.system import SystemUnit, PaperCategory
from app.core.security import get_password_hash

# Setup logging
logging.basicConfig(
    level=logging.INFO if not settings.DEBUG else logging.DEBUG,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Step 1: Auto-run database migrations (idempotent, safe on every restart)
    try:
        from alembic.config import Config
        from alembic import command
        logger.info("Running database migrations...")
        alembic_cfg = Config("/app/alembic.ini")
        command.upgrade(alembic_cfg, "head")
        logger.info("Database migrations completed.")
    except Exception as e:
        logger.error(f"Database migration failed: {e}", exc_info=True)

    # Step 2: Seed initial data
    db = SessionLocal()
    try:
        exists = db.query(User).first()
        if not exists:
            logger.info("Database is empty, seeding default admin user...")
            pwd_hash = get_password_hash(settings.DEFAULT_ADMIN_PASSWORD)
            admin = User(
                username=settings.DEFAULT_ADMIN_USERNAME,
                password_hash=pwd_hash,
                role="ADMIN",
                nickname="管理员"
            )
            db.add(admin)
            db.commit()
            logger.info("Default admin user successfully seeded!")
        else:
            admin_user = db.query(User).filter(User.username == settings.DEFAULT_ADMIN_USERNAME).first()
            if admin_user and admin_user.nickname == "Administrator":
                admin_user.nickname = "管理员"
                db.commit()
                logger.info("Auto-updated existing admin user nickname to '管理员'")

        seed_system_metadata(db)
        seed_review_metadata(db)
    except Exception as e:
        logger.error(f"Error seeding initial data: {e}", exc_info=True)
    finally:
        db.close()

    # Step 3: Start mDNS Broadcaster
    try:
        from app.services.mdns_discovery import get_mdns_settings_and_start
        await get_mdns_settings_and_start()
    except Exception as e:
        logger.error(f"Failed to start mDNS service discovery: {e}", exc_info=True)

    yield

    # Shutdown: Stop mDNS Broadcaster
    try:
        from app.services.mdns_discovery import stop_mdns_discovery
        await stop_mdns_discovery()
    except Exception as e:
        logger.error(f"Failed to stop mDNS service discovery: {e}", exc_info=True)


app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    debug=settings.DEBUG,
    lifespan=lifespan,
)

# CORS Middleware setup
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # In production, specify origins
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
    expose_headers=["Content-Disposition"],
)


# 1. Handle Custom Application Exceptions
@app.exception_handler(AppException)
async def app_exception_handler(request: Request, exc: AppException):
    logger.warning(f"Application error: code={exc.code}, msg={exc.message}")
    # Map exception code to HTTP status code (semantic match)
    status_code = exc.code if exc.code in [400, 401, 403, 404, 409] else 200
    return make_response(code=exc.code, msg=exc.message, status_code=status_code)


# 2. Handle FastAPI Validation Exceptions (Pydantic parsing errors)
@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    errors = exc.errors()
    # Format a friendly error message from validation errors
    msg_parts = []
    for err in errors:
        loc = " -> ".join(str(x) for x in err.get("loc", []))
        msg_parts.append(f"{loc}: {err.get('msg')}")
    friendly_msg = "输入参数格式错误: " + "; ".join(msg_parts)
    logger.warning(f"Validation error: {friendly_msg}")
    return make_response(code=400, msg=friendly_msg, status_code=400)


# 3. Handle Starlette HTTP Exceptions (e.g. 404 Route Not Found, 405 Method Not Allowed)
@app.exception_handler(StarletteHTTPException)
async def http_exception_handler(request: Request, exc: StarletteHTTPException):
    logger.warning(f"HTTP error: status={exc.status_code}, detail={exc.detail}")
    return make_response(code=exc.status_code, msg=str(exc.detail), status_code=exc.status_code)


# 4. Handle Any Other Uncaught Exceptions
@app.exception_handler(Exception)
async def general_exception_handler(request: Request, exc: Exception):
    logger.error(f"Uncaught exception: {str(exc)}", exc_info=True)
    return make_response(code=500, msg="服务器内部错误，请联系管理员", status_code=500)


# Register routers
app.include_router(auth_router, prefix="/api/v1")
app.include_router(source_books_router, prefix="/api/v1")
app.include_router(notes_router, prefix="/api/v1")
app.include_router(system_router, prefix="/api/v1")
app.include_router(subjects_router, prefix="/api/v1")
app.include_router(users_router, prefix="/api/v1")
app.include_router(semester_settings_router, prefix="/api/v1")
app.include_router(semesters_router, prefix="/api/v1")
app.include_router(stages_router, prefix="/api/v1")
app.include_router(parent_router, prefix="/api/v1")
app.include_router(review_router, prefix="/api/v1")

# Create upload directory if not exists
os.makedirs(settings.UPLOAD_DIR, exist_ok=True)
# Mount uploads static directory
app.mount("/uploads", StaticFiles(directory=settings.UPLOAD_DIR), name="uploads")


# Standard health check endpoint
@app.get("/api/v1/ping", tags=["Health"])
async def ping():
    return {"code": 200, "msg": "success", "data": "pong"}


def seed_system_metadata(db):
    """Seed standard system units and paper categories metadata if empty"""
    try:
        # Seed system_units
        units_to_seed = [
            {"id": 1, "name": "第 1 单元", "unit_type": "NUMBER", "sort_order": 10},
            {"id": 2, "name": "第 2 单元", "unit_type": "NUMBER", "sort_order": 20},
            {"id": 3, "name": "第 3 单元", "unit_type": "NUMBER", "sort_order": 30},
            {"id": 4, "name": "第 4 单元", "unit_type": "NUMBER", "sort_order": 40},
            {"id": 5, "name": "第 5 单元", "unit_type": "NUMBER", "sort_order": 50},
            {"id": 6, "name": "第 6 单元", "unit_type": "NUMBER", "sort_order": 60},
            {"id": 7, "name": "第 7 单元", "unit_type": "NUMBER", "sort_order": 70},
            {"id": 8, "name": "第 8 单元", "unit_type": "NUMBER", "sort_order": 80},
            {"id": 9, "name": "第 9 单元", "unit_type": "NUMBER", "sort_order": 90},
            {"id": 10, "name": "第 10 单元", "unit_type": "NUMBER", "sort_order": 100},
            {"id": 11, "name": "第 11 单元", "unit_type": "NUMBER", "sort_order": 110},
            {"id": 12, "name": "第 12 单元", "unit_type": "NUMBER", "sort_order": 120},
            {"id": 101, "name": "期中综合/复习", "unit_type": "COMPREHENSIVE", "sort_order": 130},
            {"id": 102, "name": "期末综合/复习", "unit_type": "COMPREHENSIVE", "sort_order": 140},
            {"id": 999, "name": "综合/未分类", "unit_type": "FALLBACK", "sort_order": 999},
        ]
        for u in units_to_seed:
            exists = db.query(SystemUnit).filter(SystemUnit.id == u["id"]).first()
            if not exists:
                db.add(SystemUnit(**u))
                logger.info(f"Seeded system unit: {u['name']}")

        # Seed paper_categories
        categories_to_seed = [
            {"code": "DAILY_HW", "name": "日常作业/练习", "sort_order": 10},
            {"code": "UNIT_TEST", "name": "单元测试/周考/月考", "sort_order": 20},
            {"code": "MIDTERM", "name": "期中考试/期中复习", "sort_order": 30},
            {"code": "FINAL", "name": "期末考试/期末复习", "sort_order": 40},
            {"code": "OTHER", "name": "其它综合", "sort_order": 50},
        ]
        for c in categories_to_seed:
            exists = db.query(PaperCategory).filter(PaperCategory.code == c["code"]).first()
            if not exists:
                db.add(PaperCategory(**c))
                logger.info(f"Seeded paper category: {c['name']}")
            else:
                if exists.name != c["name"]:
                    exists.name = c["name"]
                    logger.info(f"Updated paper category name to: {c['name']}")

        db.commit()
    except Exception as e:
        logger.error(f"Error seeding system metadata: {e}", exc_info=True)
        db.rollback()


def seed_review_metadata(db):
    """Seed review physical dimension tables if missing"""
    try:
        from app.models.review import ReviewStrategy, ReviewCountPreset, ReviewOutcome

        strategies = [
            {"code": "RANDOM", "name": "听天由命", "description": "纯随机抽题", "sort_order": 10},
            {"code": "EBBINGHAUS", "name": "艾宾浩斯", "description": "根据复习时间与掌握度动态计算遗忘权重", "sort_order": 20},
            {"code": "CHALLENGE", "name": "自我挑战", "description": "仅难度4星及以上，且排除熟练掌握", "sort_order": 30},
            {"code": "PERSISTENT_FAILURE", "name": "屡败屡战", "description": "优先抽屡错且未掌握的题目", "sort_order": 40},
        ]
        for s in strategies:
            exists = db.query(ReviewStrategy).filter(ReviewStrategy.code == s["code"]).first()
            if not exists:
                db.add(ReviewStrategy(**s))

        presets = [
            {"code": "PRESET_3", "count_value": 3, "name": "蜻蜓点水 (3题)", "sort_order": 10},
            {"code": "PRESET_5", "count_value": 5, "name": "稳扎稳打 (5题)", "sort_order": 20},
            {"code": "PRESET_8", "count_value": 8, "name": "精益求精 (8题)", "sort_order": 30},
            {"code": "PRESET_10", "count_value": 10, "name": "扶摇直上 (10题)", "sort_order": 40},
        ]
        for p in presets:
            exists = db.query(ReviewCountPreset).filter(ReviewCountPreset.code == p["code"]).first()
            if not exists:
                db.add(ReviewCountPreset(**p))

        outcomes = [
            {"code": "TOO_EASY", "name": "太简单了！", "color_hex": "#10b981", "suggested_mastery": "MASTERED", "sort_order": 10},
            {"code": "WRONG_AGAIN", "name": "又做错了", "color_hex": "#f59e0b", "suggested_mastery": "PARTIAL", "sort_order": 20},
            {"code": "STILL_CLUELESS", "name": "照样不会", "color_hex": "#ef4444", "suggested_mastery": "CLUELESS", "sort_order": 30},
        ]
        for o in outcomes:
            exists = db.query(ReviewOutcome).filter(ReviewOutcome.code == o["code"]).first()
            if not exists:
                db.add(ReviewOutcome(**o))

        db.commit()
    except Exception as e:
        logger.error(f"Error seeding review metadata: {e}", exc_info=True)
        db.rollback()


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=settings.DEBUG)
