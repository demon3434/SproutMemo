import os
import tarfile
import tempfile
import shutil
import subprocess
import logging
from urllib.parse import urlparse
from fastapi import APIRouter, Depends, UploadFile, File
from fastapi.responses import FileResponse
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.core.config import settings
from app.core.deps import require_admin, get_current_user
from app.core.exceptions import AppException
from app.core.response import ok
from app.models.system import SystemUnit, PaperCategory

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/system", tags=["System Backup & Restore"])


def parse_db_url(url_str: str):
    """Parse connection components from DATABASE_URL"""
    parsed = urlparse(url_str)
    host = parsed.hostname or "localhost"
    port = str(parsed.port or 5432)
    username = parsed.username or "postgres"
    password = parsed.password or ""
    database = parsed.path.lstrip("/")
    return host, port, username, password, database


@router.get("/backup", response_class=FileResponse)
def create_backup(
    current_user=Depends(require_admin),
    db: Session = Depends(get_db)
):
    """Generate and download uncompressed tarball backup containing PostgreSQL dump and uploads"""
    # 1. Create temporary directory
    temp_dir = tempfile.mkdtemp()
    sql_path = os.path.join(temp_dir, "backup.sql")
    tar_path = os.path.join(temp_dir, "backup.tar")

    try:
        # 2. Database Dump
        is_postgresql = db.bind is not None and db.bind.dialect.name == "postgresql"
        if is_postgresql:
            host, port, user, password, dbname = parse_db_url(settings.DATABASE_URL)
            cmd = ["pg_dump", "-h", host, "-p", port, "-U", user, "-d", dbname, "-F", "p", "-f", sql_path]
            
            env = os.environ.copy()
            env["PGPASSWORD"] = password
            
            logger.info("Executing pg_dump for backup...")
            result = subprocess.run(cmd, env=env, capture_output=True, text=True)
            if result.returncode != 0:
                logger.error(f"pg_dump failed: {result.stderr}")
                raise AppException(code=500, message=f"数据库备份失败: {result.stderr}")
        else:
            # SQLite Mock Dump for local unit tests
            with open(sql_path, "w", encoding="utf-8") as f:
                f.write("-- SQLite Mock Dump for Testing\n")

        # 3. Create uncompressed tarball
        with tarfile.open(tar_path, "w") as tar:
            # Add SQL file
            if os.path.exists(sql_path):
                tar.add(sql_path, arcname="backup.sql")
                
            # Add uploads folder recursively
            if os.path.exists(settings.UPLOAD_DIR):
                tar.add(settings.UPLOAD_DIR, arcname="uploads")

        # Return tarfile response
        return FileResponse(
            path=tar_path,
            filename="backup.tar",
            media_type="application/x-tar",
            background=None  # We delete temp dir manually in background or let OS clean it
        )
    except Exception as e:
        shutil.rmtree(temp_dir, ignore_errors=True)
        if isinstance(e, AppException):
            raise e
        logger.error(f"Backup generation failed: {e}", exc_info=True)
        raise AppException(code=500, message=f"备份生成失败: {str(e)}")


@router.post("/restore", response_model=dict)
def restore_backup(
    file: UploadFile = File(...),
    current_user=Depends(require_admin),
    db: Session = Depends(get_db)
):
    """Upload and restore system data from an uncompressed backup tarball"""
    temp_dir = tempfile.mkdtemp()
    uploaded_tar_path = os.path.join(temp_dir, "restore.tar")

    try:
        # 1. Save uploaded file to temp path
        with open(uploaded_tar_path, "wb") as f:
            f.write(file.file.read())

        # 2. Extract tarball
        with tarfile.open(uploaded_tar_path, "r") as tar:
            tar.extractall(path=temp_dir)

        sql_path = os.path.join(temp_dir, "backup.sql")
        extracted_uploads_dir = os.path.join(temp_dir, "uploads")

        # 3. Database Restore
        if os.path.exists(sql_path):
            is_postgresql = db.bind is not None and db.bind.dialect.name == "postgresql"
            if is_postgresql:
                host, port, user, password, dbname = parse_db_url(settings.DATABASE_URL)
                
                # Execute SQL script using psql
                # In PostgreSQL, we might want to drop tables first, or we can assume backup.sql includes drop if generated with clean.
                # Since simple pg_dump runs, we execute it directly.
                cmd = ["psql", "-h", host, "-p", port, "-U", user, "-d", dbname, "-f", sql_path]
                
                env = os.environ.copy()
                env["PGPASSWORD"] = password
                
                logger.info("Executing psql to restore backup...")
                result = subprocess.run(cmd, env=env, capture_output=True, text=True)
                if result.returncode != 0:
                    logger.error(f"psql restore failed: {result.stderr}")
                    raise AppException(code=500, message=f"数据库恢复失败: {result.stderr}")
            else:
                logger.info("SQLite testing environment - skipping psql restore")
        else:
            raise AppException(code=400, message="备份文件中缺少 backup.sql，无法还原数据库")

        # 4. Uploaded Files Restore (merge/overwrite)
        if os.path.exists(extracted_uploads_dir):
            os.makedirs(settings.UPLOAD_DIR, exist_ok=True)
            # Copy files from extracted dir to actual uploads directory
            for root, dirs, files in os.walk(extracted_uploads_dir):
                rel_root = os.path.relpath(root, extracted_uploads_dir)
                dest_dir = settings.UPLOAD_DIR if rel_root == "." else os.path.join(settings.UPLOAD_DIR, rel_root)
                os.makedirs(dest_dir, exist_ok=True)
                
                for f in files:
                    src_file = os.path.join(root, f)
                    dest_file = os.path.join(dest_dir, f)
                    shutil.copy2(src_file, dest_file)

        return ok(msg="数据还原成功")
    except Exception as e:
        if isinstance(e, AppException):
            raise e
        logger.error(f"Restore failed: {e}", exc_info=True)
        raise AppException(code=500, message=f"数据还原失败: {str(e)}")
    finally:
        shutil.rmtree(temp_dir, ignore_errors=True)





@router.get("/units", response_model=dict)
def get_system_units(
    db: Session = Depends(get_db),
    current_user=Depends(get_current_user)
):
    """Retrieve all standard system units ordered by sort_order"""
    units = db.query(SystemUnit).order_by(SystemUnit.sort_order.asc()).all()
    items = [
        {
            "id": u.id,
            "name": u.name,
            "unit_type": u.unit_type,
            "sort_order": u.sort_order
        } for u in units
    ]
    return ok(data={"items": items})


@router.get("/paper-categories", response_model=dict)
def get_paper_categories(
    db: Session = Depends(get_db),
    current_user=Depends(get_current_user)
):
    """Retrieve all paper categories ordered by sort_order"""
    categories = db.query(PaperCategory).order_by(PaperCategory.sort_order.asc()).all()
    items = [
        {
            "code": c.code,
            "name": c.name,
            "sort_order": c.sort_order
        } for c in categories
    ]
    return ok(data={"items": items})
