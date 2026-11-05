"""add_sort_order_is_deleted_to_question_sources

Revision ID: c3d4e5f6a7b8
Revises: a1b2c3d4e5f6
Create Date: 2026-07-29 17:10:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'c3d4e5f6a7b8'
down_revision: Union[str, None] = 'a1b2c3d4e5f6'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Add is_deleted column
    op.add_column('question_sources',
        sa.Column('is_deleted', sa.Boolean(), nullable=False, server_default='false')
    )
    # Add sort_order column
    op.add_column('question_sources',
        sa.Column('sort_order', sa.Integer(), nullable=False, server_default='0')
    )
    # Create index for is_deleted
    op.create_index('ix_question_sources_is_deleted', 'question_sources', ['is_deleted'])

    # Initialize sort_order for existing records, per (student_id, semester_id, subject_id) group
    op.execute("""
        UPDATE question_sources qs
        SET sort_order = sub.rn
        FROM (
            SELECT id,
                   (ROW_NUMBER() OVER (
                       PARTITION BY student_id, semester_id, subject_id
                       ORDER BY created_at
                   ) - 1) AS rn
            FROM question_sources
        ) sub
        WHERE qs.id = sub.id
    """)


def downgrade() -> None:
    op.drop_index('ix_question_sources_is_deleted', table_name='question_sources')
    op.drop_column('question_sources', 'sort_order')
    op.drop_column('question_sources', 'is_deleted')
