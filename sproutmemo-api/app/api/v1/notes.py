import os
import json
import uuid
import nh3
import logging
from datetime import datetime, timezone
from typing import List, Optional
from fastapi import APIRouter, Depends, UploadFile, File, Form, Request, Body
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.models.user import User, Student, ParentStudentRelation
from app.models.note import Note, Attachment
from app.models.system import Semester, Subject, QuestionSource, SemesterSubject, SystemUnit
from app.schemas.note import NoteListResponse
from app.core.deps import get_current_user, require_student_or_parent
from app.core.exceptions import AppException, ForbiddenException, NotFoundException, ConflictException
from app.core.response import ok
from app.core.config import settings
from app.services.semester_service import get_semester_folder_slug
from app.services.note_media_service import (
    save_uploaded_file,
    rotate_attachment_file,
    append_attachments_to_note,
    resolve_or_create_question_source,
    build_note_out,
)
from app.api.v1.source_books import validate_parent_child_binding

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/notes", tags=["Notes Management"])


def _validate_web_upload_constraints(request: Request, audios: List[UploadFile], videos: List[UploadFile]) -> None:
    user_agent = request.headers.get("user-agent", "").lower()
    origin = request.headers.get("origin")
    is_web = origin or "mozilla" in user_agent or "chrome" in user_agent or "safari" in user_agent
    if is_web and (len(audios) > 0 or len(videos) > 0):
        raise AppException(code=400, message="Web端只允许上传图片，不支持上传音频或视频")


@router.post("", response_model=dict)
def create_note(
    request: Request,
    semester_id: str = Form(...),
    subject_id: str = Form(...),
    question_source_id: Optional[str] = Form(None),
    source_paper_name: Optional[str] = Form(None),
    paper_category: Optional[str] = Form(None),
    unit_code: int = Form(999),
    page_number: Optional[str] = Form(None),
    auxiliary_text: Optional[str] = Form(None),
    difficulty_stars: float = Form(1.0),
    mastery_level: str = Form(...),
    exclude_from_review: bool = Form(False),
    student_id: Optional[str] = Form(None),
    images: List[UploadFile] = File([]),
    image_titles: List[str] = Form([]),
    audios: List[UploadFile] = File([]),
    audio_titles: List[str] = Form([]),
    videos: List[UploadFile] = File([]),
    video_titles: List[str] = Form([]),
    current_user: User = Depends(require_student_or_parent),
    db: Session = Depends(get_db)
):
    """Create a new note with optional attachments (form upload)"""
    target_student_id = None
    if current_user.role == "STUDENT":
        target_student_id = current_user.id
    elif current_user.role == "PARENT":
        if not student_id:
            raise AppException(code=400, message="家长角色创建笔记时，必填 student_id")
        target_student_id = uuid.UUID(student_id)
        validate_parent_child_binding(db, current_user.id, target_student_id)

    student = db.query(Student).filter(Student.user_id == target_student_id).first()
    if not student:
        raise NotFoundException("未找到该学生的注册信息，无法创建笔记")

    _validate_web_upload_constraints(request, audios, videos)

    if len(images) > 9:
        raise AppException(code=400, message="图片数量不能超过9张")
    if len(audios) > 6:
        raise AppException(code=400, message="音频文件不能超过6个")
    if len(videos) > 3:
        raise AppException(code=400, message="视频文件不能超过3个")

    mastery_level = mastery_level.upper()
    if mastery_level not in ["CLUELESS", "PARTIAL", "MASTERED"]:
        raise AppException(code=400, message="掌握程度参数不合法")

    resolved_semester_id = uuid.UUID(semester_id)
    semester = db.query(Semester).filter(Semester.id == resolved_semester_id).first()
    if not semester:
        raise NotFoundException("指定的学期不存在")

    resolved_subject_id = uuid.UUID(subject_id)
    subject = db.query(Subject).filter(Subject.id == resolved_subject_id).first()
    if not subject:
        raise NotFoundException("指定的科目不存在")

    assoc = db.query(SemesterSubject).filter(
        SemesterSubject.semester_id == resolved_semester_id,
        SemesterSubject.subject_id == resolved_subject_id,
        SemesterSubject.is_active == True
    ).first()
    if not assoc:
        raise AppException(code=400, message="该学科在此学期未开设或已被管理员停用，无法录入笔记")

    resolved_source_id = resolve_or_create_question_source(
        db, target_student_id, resolved_semester_id, resolved_subject_id,
        question_source_id, source_paper_name, paper_category
    )

    unit_exists = db.query(SystemUnit).filter(SystemUnit.id == unit_code).first()
    if not unit_exists:
        unit_code = 999

    clean_text = nh3.clean(auxiliary_text) if auxiliary_text else None

    note_id = uuid.uuid4()
    note = Note(
        id=note_id,
        student_id=target_student_id,
        semester_id=resolved_semester_id,
        subject_id=resolved_subject_id,
        question_source_id=resolved_source_id,
        source_paper_name=source_paper_name,
        unit_code=unit_code,
        page_number=page_number,
        auxiliary_text=clean_text,
        difficulty_stars=difficulty_stars,
        mastery_level=mastery_level,
        exclude_from_review=exclude_from_review,
    )
    db.add(note)
    db.flush()

    semester_folder = get_semester_folder_slug(student.enrollment_year, student.semester_offset)
    append_attachments_to_note(db, note_id, target_student_id, semester_folder, "IMAGE", images, image_titles, 0)
    append_attachments_to_note(db, note_id, target_student_id, semester_folder, "AUDIO", audios, audio_titles, 0)
    append_attachments_to_note(db, note_id, target_student_id, semester_folder, "VIDEO", videos, video_titles, 0)

    db.commit()
    db.refresh(note)

    return ok(data=build_note_out(note, db), msg="笔记创建成功")


@router.get("", response_model=dict)
def list_notes(
    student_id: Optional[uuid.UUID] = None,
    semester_id: Optional[uuid.UUID] = None,
    subject_id: Optional[uuid.UUID] = None,
    question_source_id: Optional[uuid.UUID] = None,
    unit_code: Optional[int] = None,
    mastery_level: Optional[str] = None,
    exclude_from_review: Optional[bool] = None,
    source_type: Optional[str] = None,
    paper_category: Optional[str] = None,
    difficulty_stars: Optional[int] = None,
    page: int = 1,
    size: int = 20,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Query and filter notes list with pagination"""
    query = db.query(Note).filter(Note.is_deleted == False)

    if current_user.role == "STUDENT":
        query = query.filter(Note.student_id == current_user.id)
    elif current_user.role == "PARENT":
        if student_id:
            validate_parent_child_binding(db, current_user.id, student_id)
            query = query.filter(Note.student_id == student_id)
        else:
            child_ids = [r.student_id for r in db.query(ParentStudentRelation).filter(
                ParentStudentRelation.parent_id == current_user.id
            ).all()]
            query = query.filter(Note.student_id.in_(child_ids))
    elif current_user.role == "ADMIN" and student_id:
        query = query.filter(Note.student_id == student_id)

    if semester_id:
        query = query.filter(Note.semester_id == semester_id)
    if subject_id:
        query = query.filter(Note.subject_id == subject_id)
    if question_source_id:
        query = query.filter(Note.question_source_id == question_source_id)
    if unit_code is not None:
        query = query.filter(Note.unit_code == unit_code)
    if mastery_level:
        query = query.filter(Note.mastery_level == mastery_level.upper())
    if exclude_from_review is not None:
        query = query.filter(Note.exclude_from_review == exclude_from_review)
    if source_type:
        if source_type.upper() == "BOOK":
            query = query.filter(Note.source_paper_name == None)
        elif source_type.upper() == "PAPER":
            query = query.filter(Note.source_paper_name != None)
    if paper_category:
        query = query.join(QuestionSource, Note.question_source_id == QuestionSource.id).filter(
            QuestionSource.category_code == paper_category
        )
    if difficulty_stars is not None:
        query = query.filter(func.round(Note.difficulty_stars) == difficulty_stars)

    total = query.count()
    offset = (page - 1) * size
    notes = query.order_by(Note.created_at.desc()).offset(offset).limit(size).all()
    items = [build_note_out(n, db) for n in notes]

    res = NoteListResponse(total=total, page=page, size=size, items=items)
    return ok(data=res.model_dump())


@router.get("/{id}", response_model=dict)
def get_note_details(
    id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Retrieve details of a single note including attachments"""
    note = db.query(Note).filter(Note.id == id, Note.is_deleted == False).first()
    if not note:
        raise NotFoundException("笔记不存在")

    if current_user.role == "STUDENT":
        if note.student_id != current_user.id:
            raise ForbiddenException("无权查看其他学生的笔记")
    elif current_user.role == "PARENT":
        validate_parent_child_binding(db, current_user.id, note.student_id)

    return ok(data=build_note_out(note, db).model_dump())


@router.post("/{id}/attachments/{attachment_id}/rotate", response_model=dict)
def rotate_note_attachment(
    id: uuid.UUID,
    attachment_id: uuid.UUID,
    angle: Optional[int] = Form(None),
    payload: Optional[dict] = Body(None),
    current_user: User = Depends(require_student_or_parent),
    db: Session = Depends(get_db)
):
    """Persistently rotate an existing image attachment on a note and return the updated NoteOut."""
    note = db.query(Note).filter(Note.id == id, Note.is_deleted == False).first()
    if not note:
        raise NotFoundException("笔记不存在")

    if current_user.role == "STUDENT":
        if note.student_id != current_user.id:
            raise ForbiddenException("无权修改其他学生的笔记")
    elif current_user.role == "PARENT":
        validate_parent_child_binding(db, current_user.id, note.student_id)

    att = db.query(Attachment).filter(
        Attachment.id == attachment_id,
        Attachment.note_id == note.id
    ).first()
    if not att:
        raise NotFoundException("附件不存在")

    resolved_angle = angle
    if resolved_angle is None and isinstance(payload, dict):
        resolved_angle = payload.get("angle")
    if resolved_angle is None:
        resolved_angle = 90

    rotate_attachment_file(att, int(resolved_angle))

    now_utc = datetime.now(timezone.utc)
    note.sync_base_time = now_utc
    note.updated_at = now_utc

    db.commit()
    db.refresh(note)

    return ok(data=build_note_out(note, db).model_dump(), msg="图片方向已保存")


@router.put("/{id}", response_model=dict)
def update_note(
    id: uuid.UUID,
    request: Request,
    semester_id: Optional[str] = Form(None),
    subject_id: Optional[str] = Form(None),
    question_source_id: Optional[str] = Form(None),
    source_paper_name: Optional[str] = Form(None),
    paper_category: Optional[str] = Form(None),
    unit_code: Optional[int] = Form(None),
    page_number: Optional[str] = Form(None),
    auxiliary_text: Optional[str] = Form(None),
    difficulty_stars: Optional[float] = Form(None),
    mastery_level: Optional[str] = Form(None),
    exclude_from_review: Optional[bool] = Form(None),
    sync_base_time: str = Form(...),
    force: bool = Form(False),
    delete_attachment_ids: List[str] = Form([]),
    attachment_metadata: Optional[str] = Form(None),
    images: List[UploadFile] = File([]),
    image_titles: List[str] = Form([]),
    audios: List[UploadFile] = File([]),
    audio_titles: List[str] = Form([]),
    videos: List[UploadFile] = File([]),
    video_titles: List[str] = Form([]),
    current_user: User = Depends(require_student_or_parent),
    db: Session = Depends(get_db)
):
    """Update a note with optimistic locking and media change support"""
    note = db.query(Note).filter(Note.id == id, Note.is_deleted == False).first()
    if not note:
        raise NotFoundException("笔记不存在")

    if current_user.role == "STUDENT":
        if note.student_id != current_user.id:
            raise ForbiddenException("无权修改其他学生的笔记")
    elif current_user.role == "PARENT":
        validate_parent_child_binding(db, current_user.id, note.student_id)

    try:
        sync_time = datetime.fromisoformat(sync_base_time.replace("Z", "+00:00"))
    except ValueError:
        raise AppException(code=400, message="sync_base_time 时间格式不正确 (应为ISO格式)")

    db_updated = note.updated_at
    if db_updated.tzinfo is None:
        db_updated = db_updated.replace(tzinfo=timezone.utc)
    if sync_time.tzinfo is None:
        sync_time = sync_time.replace(tzinfo=timezone.utc)

    if not force and db_updated > sync_time:
        raise ConflictException(
            f"笔记在 {db_updated.isoformat()} 已被修改，您的修改基准时间是 {sync_time.isoformat()}。请刷新或使用 force=true 强制覆盖"
        )

    with db.no_autoflush:
        old_semester_id = note.semester_id
        old_subject_id = note.subject_id
        curr_semester_id = note.semester_id
        if semester_id is not None:
            resolved_semester_id = uuid.UUID(semester_id)
            semester = db.query(Semester).filter(Semester.id == resolved_semester_id).first()
            if not semester:
                raise NotFoundException("指定的学期不存在")
            curr_semester_id = resolved_semester_id
            note.semester_id = resolved_semester_id

        curr_subject_id = note.subject_id
        if subject_id is not None:
            resolved_subject_id = uuid.UUID(subject_id)
            subject = db.query(Subject).filter(Subject.id == resolved_subject_id).first()
            if not subject:
                raise NotFoundException("指定的科目不存在")
            curr_subject_id = resolved_subject_id
            note.subject_id = resolved_subject_id

        if curr_semester_id != old_semester_id or curr_subject_id != old_subject_id:
            note.question_source_id = None

        assoc = db.query(SemesterSubject).filter(
            SemesterSubject.semester_id == curr_semester_id,
            SemesterSubject.subject_id == curr_subject_id,
            SemesterSubject.is_active == True
        ).first()
        if not assoc:
            raise AppException(code=400, message="该学科在此学期未开设或已被管理员停用，无法录入笔记")

        if question_source_id is not None:
            if question_source_id == "":
                note.question_source_id = None
            else:
                note.question_source_id = resolve_or_create_question_source(
                    db, note.student_id, curr_semester_id, curr_subject_id,
                    question_source_id, None, None
                )

        if source_paper_name is not None:
            note.source_paper_name = source_paper_name
            if source_paper_name.strip():
                note.question_source_id = resolve_or_create_question_source(
                    db, note.student_id, curr_semester_id, curr_subject_id,
                    None, source_paper_name, paper_category
                )
            elif question_source_id is None:
                note.question_source_id = None

        if unit_code is not None:
            unit_exists = db.query(SystemUnit).filter(SystemUnit.id == unit_code).first()
            if not unit_exists:
                raise AppException(code=400, message="指定的单元大纲编码无效")
            note.unit_code = unit_code
        if page_number is not None:
            note.page_number = page_number
        if auxiliary_text is not None:
            note.auxiliary_text = nh3.clean(auxiliary_text) if auxiliary_text else None
        if difficulty_stars is not None:
            note.difficulty_stars = difficulty_stars
        if mastery_level:
            note.mastery_level = mastery_level.upper()
        if exclude_from_review is not None:
            note.exclude_from_review = exclude_from_review

    if delete_attachment_ids:
        for att_id_str in delete_attachment_ids:
            att_id = uuid.UUID(att_id_str)
            att = db.query(Attachment).filter(Attachment.id == att_id, Attachment.note_id == note.id).first()
            if att:
                abs_path = os.path.join(settings.UPLOAD_DIR, att.storage_path)
                if os.path.exists(abs_path):
                    try:
                        os.remove(abs_path)
                    except Exception as e:
                        logger.warning(f"Failed to delete physical file {abs_path}: {e}")
                db.delete(att)

    if attachment_metadata:
        try:
            meta_list = json.loads(attachment_metadata)
            for item in meta_list:
                att_id_str = item.get("id")
                new_title = item.get("title")
                new_order = item.get("sort_order")
                rotation_deg = item.get("rotation")
                if att_id_str:
                    att_id = uuid.UUID(att_id_str)
                    att = db.query(Attachment).filter(Attachment.id == att_id, Attachment.note_id == note.id).first()
                    if att:
                        if new_title is not None:
                            att.title = new_title
                        if new_order is not None:
                            att.sort_order = new_order
                        if rotation_deg is not None and int(rotation_deg) % 360 != 0:
                            rotate_attachment_file(att, int(rotation_deg))
        except Exception as e:
            logger.warning(f"Failed to parse attachment_metadata: {e}")

    student = db.query(Student).filter(Student.user_id == note.student_id).first()
    semester_folder = get_semester_folder_slug(student.enrollment_year, student.semester_offset)

    current_attachments = db.query(Attachment).filter(Attachment.note_id == note.id).all()
    img_count = sum(1 for a in current_attachments if a.file_type == "IMAGE")
    aud_count = sum(1 for a in current_attachments if a.file_type == "AUDIO")
    vid_count = sum(1 for a in current_attachments if a.file_type == "VIDEO")

    _validate_web_upload_constraints(request, audios, videos)

    if img_count + len(images) > 9:
        raise AppException(code=400, message=f"图片数量超过限制，当前已有 {img_count} 张")
    if aud_count + len(audios) > 6:
        raise AppException(code=400, message=f"音频文件数量超过限制，当前已有 {aud_count} 个")
    if vid_count + len(videos) > 3:
        raise AppException(code=400, message=f"视频文件数量超过限制，当前已有 {vid_count} 个")

    append_attachments_to_note(db, note.id, note.student_id, semester_folder, "IMAGE", images, image_titles, img_count)
    append_attachments_to_note(db, note.id, note.student_id, semester_folder, "AUDIO", audios, audio_titles, aud_count)
    append_attachments_to_note(db, note.id, note.student_id, semester_folder, "VIDEO", videos, video_titles, vid_count)

    note.sync_base_time = datetime.now(timezone.utc)
    note.updated_at = datetime.now(timezone.utc)

    db.commit()
    db.refresh(note)

    return ok(data=build_note_out(note, db).model_dump(), msg="笔记更新成功")


@router.delete("/{id}", response_model=dict)
def delete_note(
    id: uuid.UUID,
    current_user: User = Depends(require_student_or_parent),
    db: Session = Depends(get_db)
):
    """Soft delete a note"""
    note = db.query(Note).filter(Note.id == id, Note.is_deleted == False).first()
    if not note:
        raise NotFoundException("笔记不存在")

    if current_user.role == "STUDENT":
        if note.student_id != current_user.id:
            raise ForbiddenException("无权删除其他学生的笔记")
    elif current_user.role == "PARENT":
        validate_parent_child_binding(db, current_user.id, note.student_id)

    note.is_deleted = True
    note.updated_at = datetime.now(timezone.utc)
    db.commit()

    return ok(msg="笔记删除成功")
