import uuid
from datetime import datetime, timezone
from typing import List, Optional
from sqlalchemy import func, select, and_, or_, desc
from sqlalchemy.orm import Session

from app.models.note import Note
from app.models.review import ReviewLog, ReviewSession


def get_review_notes(
    db: Session,
    student_id: uuid.UUID,
    semester_id: uuid.UUID,
    subject_id: Optional[uuid.UUID],
    unit_code: Optional[int],
    strategy_code: str,
    target_count: int
) -> List[Note]:
    """根据筛题策略抽取对应的复习题目列表"""
    # 基础查询与过滤
    base_query = db.query(Note).filter(
        Note.student_id == student_id,
        Note.semester_id == semester_id,
        Note.is_deleted == False,
        Note.exclude_from_review == False
    )

    if subject_id:
        base_query = base_query.filter(Note.subject_id == subject_id)
    if unit_code and unit_code != 999:
        base_query = base_query.filter(Note.unit_code == unit_code)

    if strategy_code == "RANDOM":
        # 听天由命：纯随机抽题
        return base_query.order_by(func.random()).limit(target_count).all()

    elif strategy_code == "CHALLENGE":
        # 自我挑战：难度 >= 4 星且排除熟练掌握
        challenge_query = base_query.filter(
            Note.difficulty_stars >= 4.0,
            Note.mastery_level != "MASTERED"
        ).order_by(desc(Note.difficulty_stars), func.random())
        
        notes = challenge_query.limit(target_count).all()
        if len(notes) < target_count:
            # 数量不足时用随机托底补足
            existing_ids = {n.id for n in notes}
            fallback = base_query.filter(Note.id.notin_(existing_ids)).order_by(func.random()).limit(target_count - len(notes)).all()
            notes.extend(fallback)
        return notes

    elif strategy_code == "PERSISTENT_FAILURE":
        # 屡败屡战：优先抽历史回单中频做错(WRONG_AGAIN / STILL_CLUELESS)且从未做对过(TOO_EASY)的题目
        failed_notes_stmt = (
            select(ReviewLog.note_id)
            .where(
                ReviewLog.student_id == student_id,
                ReviewLog.outcome_code.in_(["WRONG_AGAIN", "STILL_CLUELESS"])
            )
            .group_by(ReviewLog.note_id)
            .order_by(func.count(ReviewLog.id).desc())
        )
        failed_ids = db.scalars(failed_notes_stmt).all()

        if failed_ids:
            # 排除曾经做对过的题目
            easy_ids_stmt = select(ReviewLog.note_id).where(
                ReviewLog.student_id == student_id,
                ReviewLog.outcome_code == "TOO_EASY"
            )
            easy_ids = set(db.scalars(easy_ids_stmt).all())
            target_failed_ids = [nid for nid in failed_ids if nid not in easy_ids]

            if target_failed_ids:
                failed_notes = base_query.filter(Note.id.in_(target_failed_ids)).limit(target_count).all()
                if len(failed_notes) == target_count:
                    return failed_notes
                # 补足
                existing_ids = {n.id for n in failed_notes}
                fallback = base_query.filter(Note.id.notin_(existing_ids)).order_by(func.random()).limit(target_count - len(failed_notes)).all()
                return failed_notes + fallback

        # 如果没有失败历史，降级回随机抽题
        return base_query.order_by(func.random()).limit(target_count).all()

    elif strategy_code == "EBBINGHAUS":
        # 艾宾浩斯：结合掌握度与上次复习时间间隔做加权排序
        all_candidate_notes = base_query.all()
        if not all_candidate_notes:
            return []

        now = datetime.now(timezone.utc)

        def calculate_ebbinghaus_score(note: Note) -> float:
            # 1. 基础掌握度因子：CLUELESS 最高，MASTERED 最低
            mastery_weight = 3.0 if note.mastery_level == "CLUELESS" else (2.0 if note.mastery_level == "PARTIAL" else 1.0)
            
            # 2. 时间差因子：未复习过赋予最大天数
            if note.last_reviewed_at is None:
                days_since_review = 30.0
            else:
                days_since_review = max((now - note.last_reviewed_at).total_seconds() / 86400.0, 0.1)

            # 综合得分：距离上次复习越久、掌握度越低，得分越高
            return mastery_weight * (1.0 + days_since_review)

        sorted_notes = sorted(all_candidate_notes, key=calculate_ebbinghaus_score, reverse=True)
        return sorted_notes[:target_count]

    else:
        # 默认降级为随机
        return base_query.order_by(func.random()).limit(target_count).all()
