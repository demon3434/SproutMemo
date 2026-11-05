import os
import shutil
import subprocess
import uuid
import logging
from datetime import datetime
from typing import List, Optional
from fastapi import UploadFile
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.exceptions import AppException, NotFoundException
from app.models.user import User
from app.models.note import Note, Attachment
from app.models.system import QuestionSource, PaperCategory
from app.schemas.note import NoteOut

logger = logging.getLogger(__name__)


def save_uploaded_file(
    student_id: uuid.UUID,
    semester_slug: str,
    note_id: uuid.UUID,
    file_type: str,  # IMAGE, AUDIO, VIDEO
    upload_file: UploadFile
) -> str:
    """Save upload file to local disk and return storage path"""
    orig_name = upload_file.filename or "file"
    _, ext = os.path.splitext(orig_name)
    secure_name = f"{uuid.uuid4().hex}{ext.lower()}"

    now = datetime.now()
    date_path = now.strftime("%Y/%m/%d")
    relative_path = os.path.join(date_path, secure_name).replace("\\", "/")

    absolute_dir = os.path.join(settings.UPLOAD_DIR, date_path)
    absolute_path = os.path.join(absolute_dir, secure_name)

    os.makedirs(absolute_dir, exist_ok=True)

    with open(absolute_path, "wb") as buffer:
        buffer.write(upload_file.file.read())

    if file_type == "VIDEO" and (
        orig_name.lower().endswith(".mp4")
        or (upload_file.content_type and upload_file.content_type == "video/mp4")
    ):
        optimized_path = absolute_path + ".faststart.mp4"
        ffmpeg_cmd = [
            "ffmpeg", "-y", "-i", absolute_path,
            "-c", "copy",
            "-movflags", "+faststart",
            optimized_path
        ]
        try:
            res = subprocess.run(ffmpeg_cmd, capture_output=True)
            if res.returncode == 0 and os.path.exists(optimized_path):
                os.replace(optimized_path, absolute_path)
        except Exception:
            pass

    return relative_path


def rotate_attachment_file(att: Attachment, angle: int) -> None:
    """Rotate an existing IMAGE attachment on disk by a multiple of 90 degrees.
    Generates a new storage_path UUID filename so client HTTP/Coil caches bust automatically.
    """
    if (att.file_type or "").upper() != "IMAGE":
        raise AppException(code=400, message="仅支持旋转图片附件")

    norm_angle = int(angle) % 360
    if norm_angle == 0:
        return
    if norm_angle not in (90, 180, 270):
        raise AppException(code=400, message="旋转角度必须是 90 度的整数倍")

    old_abs_path = os.path.join(settings.UPLOAD_DIR, att.storage_path)
    if not os.path.exists(old_abs_path):
        raise NotFoundException("图片文件不存在")

    _, ext = os.path.splitext(att.storage_path)
    if not ext:
        ext = ".jpg"
    secure_name = f"{uuid.uuid4().hex}{ext.lower()}"
    date_path = datetime.now().strftime("%Y/%m/%d")
    new_relative_path = os.path.join(date_path, secure_name).replace("\\", "/")
    new_abs_dir = os.path.join(settings.UPLOAD_DIR, date_path)
    new_abs_path = os.path.join(new_abs_dir, secure_name)
    os.makedirs(new_abs_dir, exist_ok=True)

    vf_map = {
        90: "transpose=1",
        180: "transpose=1,transpose=1",
        270: "transpose=2",
    }
    vf = vf_map[norm_angle]
    ffmpeg_cmd = [
        "ffmpeg", "-y", "-i", old_abs_path,
        "-vf", vf,
        "-q:v", "2",
        "-map_metadata", "-1",
        new_abs_path
    ]
    try:
        res = subprocess.run(ffmpeg_cmd, capture_output=True, check=False)
        if res.returncode != 0 or not os.path.exists(new_abs_path):
            shutil.copyfile(old_abs_path, new_abs_path)
    except Exception:
        shutil.copyfile(old_abs_path, new_abs_path)

    try:
        if os.path.exists(old_abs_path) and os.path.abspath(old_abs_path) != os.path.abspath(new_abs_path):
            os.remove(old_abs_path)
    except Exception as e:
        logger.warning(f"Failed to remove old rotated file {old_abs_path}: {e}")

    att.storage_path = new_relative_path
    try:
        att.file_size = os.path.getsize(new_abs_path)
    except Exception:
        pass


def append_attachments_to_note(
    db: Session,
    note_id: uuid.UUID,
    student_id: uuid.UUID,
    semester_folder: str,
    file_type: str,
    files: List[UploadFile],
    titles: List[str],
    start_sort_order: int = 0
) -> None:
    """Save and attach a list of uploaded files to a note."""
    for idx, f in enumerate(files):
        if not f.filename:
            continue
        path = save_uploaded_file(student_id, semester_folder, note_id, file_type, f)
        custom_title = titles[idx].strip() if idx < len(titles) and titles[idx] else None
        if not custom_title:
            custom_title = None

        att = Attachment(
            note_id=note_id,
            file_type=file_type,
            storage_path=path,
            original_name=f.filename,
            title=custom_title,
            file_size=f.size or 0,
            sort_order=start_sort_order + idx
        )
        db.add(att)


def resolve_or_create_question_source(
    db: Session,
    student_id: uuid.UUID,
    semester_id: uuid.UUID,
    subject_id: uuid.UUID,
    question_source_id: Optional[str],
    source_paper_name: Optional[str],
    paper_category: Optional[str]
) -> Optional[uuid.UUID]:
    """Validate an existing question source or dynamically create a PAPER QuestionSource."""
    if question_source_id:
        resolved_source_id = uuid.UUID(question_source_id)
        source = db.query(QuestionSource).filter(QuestionSource.id == resolved_source_id).first()
        if not source:
            raise NotFoundException("指定的题目来源不存在")
        if source.student_id != student_id or source.semester_id != semester_id or source.subject_id != subject_id:
            raise AppException(code=400, message="指定的题目来源与笔记的学期或科目不一致")
        return resolved_source_id

    if source_paper_name and source_paper_name.strip():
        stripped_name = source_paper_name.strip()
        source = db.query(QuestionSource).filter(
            QuestionSource.name == stripped_name,
            QuestionSource.student_id == student_id,
            QuestionSource.semester_id == semester_id,
            QuestionSource.subject_id == subject_id,
            QuestionSource.is_deleted == False
        ).first()

        if not source:
            cat_code = paper_category or "DAILY_HW"
            category_exists = db.query(PaperCategory).filter(PaperCategory.code == cat_code).first()
            if not category_exists:
                cat_code = "DAILY_HW"

            source = QuestionSource(
                id=uuid.uuid4(),
                student_id=student_id,
                semester_id=semester_id,
                subject_id=subject_id,
                name=stripped_name,
                category_code=cat_code,
                source_type="PAPER",
                is_active=True,
                is_deleted=False,
                sort_order=0
            )
            db.add(source)
        return source.id

    return None


def build_note_out(n: Note, db: Session) -> NoteOut:
    """Populate NoteOut with student, semester, subject and question source names"""
    student_user = db.query(User).filter(User.id == n.student_id).first()
    student_name = student_user.nickname or student_user.username if student_user else None

    semester_name = n.semester.name if n.semester else None
    subject_name = n.subject.subject_name if n.subject else None

    source_name = None
    source_type = "BOOK"
    paper_category = None
    paper_category_name = None
    if n.question_source_id:
        source = db.query(QuestionSource).filter(QuestionSource.id == n.question_source_id).first()
        if source:
            source_name = source.name
            source_type = source.source_type
            paper_category = source.category_code
            if source.category:
                paper_category_name = source.category.name

    note_out = NoteOut.model_validate(n)
    note_out.student_name = student_name
    note_out.semester_name = semester_name
    note_out.stage_id = n.semester.stage_id if n.semester else None
    note_out.stage_name = n.semester.stage.name if (n.semester and n.semester.stage) else None
    note_out.subject_name = subject_name
    note_out.question_source_name = source_name
    note_out.source_type = source_type
    note_out.paper_category = paper_category
    note_out.paper_category_name = paper_category_name
    note_out.unit_name = n.unit.name if n.unit else None
    return note_out

