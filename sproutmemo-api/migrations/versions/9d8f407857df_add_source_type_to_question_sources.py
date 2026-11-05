"""add_source_type_to_question_sources

Revision ID: 9d8f407857df
Revises: 6a3e1b7f02cd
Create Date: 2026-07-31 01:27:58.856863

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '9d8f407857df'
down_revision: Union[str, Sequence[str], None] = '6a3e1b7f02cd'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    # 1. Add source_type column to question_sources table
    op.add_column(
        'question_sources',
        sa.Column('source_type', sa.String(length=20), nullable=False, server_default='BOOK')
    )

    # 2. Update existing paper sources to 'PAPER' type based on notes
    op.execute(
        """
        UPDATE question_sources
        SET source_type = 'PAPER'
        WHERE id IN (
            SELECT DISTINCT question_source_id
            FROM notes
            WHERE source_paper_name IS NOT NULL AND source_paper_name <> ''
        )
        """
    )


def downgrade() -> None:
    """Downgrade schema."""
    op.drop_column('question_sources', 'source_type')
