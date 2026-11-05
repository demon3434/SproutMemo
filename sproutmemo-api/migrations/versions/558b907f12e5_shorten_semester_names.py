"""shorten_semester_names

Revision ID: 558b907f12e5
Revises: 11860f6a6891
Create Date: 2026-07-30 14:30:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '558b907f12e5'
down_revision: Union[str, Sequence[str], None] = '11860f6a6891'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    name_mappings = {
        "小学一年级上学期": "小一上",
        "小学一年级下学期": "小一下",
        "小学二年级上学期": "小二上",
        "小学二年级下学期": "小二下",
        "小学三年级上学期": "小三上",
        "小学三年级下学期": "小三下",
        "小学四年级上学期": "小四上",
        "小学四年级下学期": "小四下",
        "小学五年级上学期": "小五上",
        "小学五年级下学期": "小五下",
        "小学六年级上学期": "小六上",
        "小学六年级下学期": "小六下",
        "初中一年级上学期": "初一上",
        "初中一年级下学期": "初一下",
        "初中二年级上学期": "初二上",
        "初中二年级下学期": "初二下",
        "初中三年级上学期": "初三上",
        "初中三年级下学期": "初三下",
        "高中一年级上学期": "高一上",
        "高中一年级下学期": "高一下",
        "高中二年级上学期": "高二上",
        "高中二年级下学期": "高二下",
        "高中三年级上学期": "高三上",
        "高中三年级下学期": "高三下",
        # Historical / fallback names
        "一年级上学期": "小一上",
        "一年级下学期": "小一下",
        "二年级上学期": "小二上",
        "二年级下学期": "小二下",
        "三年级上学期": "小三上",
        "三年级下学期": "小三下",
        "四年级上学期": "小四上",
        "四年级下学期": "小四下",
        "五年级上学期": "小五上",
        "五年级下学期": "小五下",
        "六年级上学期": "小六上",
        "六年级下学期": "小六下",
        "七年级上学期": "初一上",
        "七年级下学期": "初一下",
        "八年级上学期": "初二上",
        "八年级下学期": "初二下",
        "九年级上学期": "初三上",
        "九年级下学期": "初三下",
    }
    for old_name, new_name in name_mappings.items():
        op.execute(
            sa.text(
                "UPDATE semesters SET name = :new_name WHERE name = :old_name"
            ).bindparams(old_name=old_name, new_name=new_name)
        )


def downgrade() -> None:
    pass
