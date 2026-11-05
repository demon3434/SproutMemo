"""rename_semesters_and_set_sort_order

Revision ID: b800ac818280
Revises: fae68e3413f7
Create Date: 2026-07-29 00:37:22.742412

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'b800ac818280'
down_revision: Union[str, Sequence[str], None] = 'fae68e3413f7'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    # 1. Map old names to new names and sort orders
    name_mappings = [
        ("一年级上学期", "小学一年级上学期", 1),
        ("一年级下学期", "小学一年级下学期", 2),
        ("二年级上学期", "小学二年级上学期", 3),
        ("二年级下学期", "小学二年级下学期", 4),
        ("三年级上学期", "小学三年级上学期", 5),
        ("三年级下学期", "小学三年级下学期", 6),
        ("四年级上学期", "小学四年级上学期", 7),
        ("四年级下学期", "小学四年级下学期", 8),
        ("五年级上学期", "小学五年级上学期", 9),
        ("五年级下学期", "小学五年级下学期", 10),
        ("六年级上学期", "小学六年级上学期", 11),
        ("六年级下学期", "小学六年级下学期", 12),
        ("七年级上学期", "初中一年级上学期", 13),
        ("七年级下学期", "初中一年级下学期", 14),
        ("八年级上学期", "初中二年级上学期", 15),
        ("八年级下学期", "初中二年级下学期", 16),
        ("九年级上学期", "初中三年级上学期", 17),
        ("九年级下学期", "初中三年级下学期", 18),
        ("高一上学期", "高中一年级上学期", 19),
        ("高一下学期", "高中一年级下学期", 20),
        ("高二上学期", "高中二年级上学期", 21),
        ("高二下学期", "高中二年级下学期", 22),
        ("高三上学期", "高中三年级上学期", 23),
        ("高三下学期", "高中三年级下学期", 24)
    ]
    
    for old_name, new_name, sort_order in name_mappings:
        op.execute(
            sa.text(
                "UPDATE semesters SET name = :new_name, sort_order = :sort_order "
                "WHERE name = :old_name"
            ).bindparams(old_name=old_name, new_name=new_name, sort_order=sort_order)
        )
        op.execute(
            sa.text(
                "UPDATE semesters SET sort_order = :sort_order "
                "WHERE name = :new_name"
            ).bindparams(new_name=new_name, sort_order=sort_order)
        )

    # 2. Seed default template subject mapping
    connection = op.get_bind()
    semesters = connection.execute(sa.text("SELECT id, name FROM semesters")).fetchall()
    sem_map = {name: str(sid) for sid, name in semesters}
    
    subjects = connection.execute(sa.text("SELECT id, subject_name FROM subjects")).fetchall()
    sub_map = {name: str(sid) for sid, name in subjects}

    import uuid
    templates_to_insert = []
    
    primary_sems = [
        "小学一年级上学期", "小学一年级下学期", "小学二年级上学期", "小学二年级下学期",
        "小学三年级上学期", "小学三年级下学期", "小学四年级上学期", "小学四年级下学期",
        "小学五年级上学期", "小学五年级下学期", "小学六年级上学期", "小学六年级下学期"
    ]
    for sem in primary_sems:
        if sem in sem_map:
            for sub in ["语文", "数学", "英语"]:
                if sub in sub_map:
                    templates_to_insert.append((sem_map[sem], sub_map[sub]))

    jh7_sems = ["初中一年级上学期", "初中一年级下学期"]
    for sem in jh7_sems:
        if sem in sem_map:
            for sub in ["语文", "数学", "英语", "生物", "历史", "地理", "政治"]:
                if sub in sub_map:
                    templates_to_insert.append((sem_map[sem], sub_map[sub]))

    jh8_sems = ["初中二年级上学期", "初中二年级下学期"]
    for sem in jh8_sems:
        if sem in sem_map:
            for sub in ["语文", "数学", "英语", "物理", "生物", "历史", "地理", "政治"]:
                if sub in sub_map:
                    templates_to_insert.append((sem_map[sem], sub_map[sub]))

    jh9_sems = ["初中三年级上学期", "初中三年级下学期"]
    for sem in jh9_sems:
        if sem in sem_map:
            for sub in ["语文", "数学", "英语", "物理", "化学", "历史", "政治"]:
                if sub in sub_map:
                    templates_to_insert.append((sem_map[sem], sub_map[sub]))

    high_sems = [
        "高中一年级上学期", "高中一年级下学期", "高中二年级上学期", "高中二年级下学期",
        "高中三年级上学期", "高中三年级下学期"
    ]
    for sem in high_sems:
        if sem in sem_map:
            for sub in ["语文", "数学", "英语", "物理", "化学", "生物", "历史", "地理", "政治"]:
                if sub in sub_map:
                    templates_to_insert.append((sem_map[sem], sub_map[sub]))

    for sem_id, sub_id in templates_to_insert:
        exist = connection.execute(
            sa.text("SELECT id FROM semester_subjects WHERE semester_id = :sem_id AND subject_id = :sub_id")
            .bindparams(sem_id=sem_id, sub_id=sub_id)
        ).fetchone()
        
        if not exist:
            id_val = str(uuid.uuid4())
            op.execute(
                sa.text(
                    "INSERT INTO semester_subjects (id, semester_id, subject_id, is_active, created_at, updated_at) "
                    "VALUES (:id, :semester_id, :subject_id, true, NOW(), NOW())"
                ).bindparams(id=id_val, semester_id=sem_id, subject_id=sub_id)
            )


def downgrade() -> None:
    pass
