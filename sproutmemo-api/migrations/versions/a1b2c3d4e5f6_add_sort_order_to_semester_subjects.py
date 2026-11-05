"""add_sort_order_to_semester_subjects

Revision ID: a1b2c3d4e5f6
Revises: ec12701c983c
Create Date: 2026-07-29 01:36:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'a1b2c3d4e5f6'
down_revision: Union[str, Sequence[str], None] = 'ec12701c983c'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Add sort_order to semester_subjects and initialize from subject sort_order."""
    # Add column with server default of 0
    op.add_column(
        'semester_subjects',
        sa.Column('sort_order', sa.Integer(), nullable=False, server_default=sa.text('0'))
    )

    # Initialize sort_order for existing records within each semester,
    # ordered by the linked subject's sort_order.
    # We do this with a window function via a CTE.
    op.execute(sa.text("""
        UPDATE semester_subjects ss
        SET sort_order = ranked.rn
        FROM (
            SELECT
                ss2.id,
                ROW_NUMBER() OVER (
                    PARTITION BY ss2.semester_id
                    ORDER BY sub.sort_order, sub.subject_name
                ) AS rn
            FROM semester_subjects ss2
            JOIN subjects sub ON ss2.subject_id = sub.id
            WHERE ss2.is_deleted = false
        ) ranked
        WHERE ss.id = ranked.id
    """))


def downgrade() -> None:
    """Remove sort_order from semester_subjects."""
    op.drop_column('semester_subjects', 'sort_order')
