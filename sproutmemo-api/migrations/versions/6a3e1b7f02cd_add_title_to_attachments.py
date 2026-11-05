"""add_title_to_attachments

Revision ID: 6a3e1b7f02cd
Revises: 558b907f12e5
Create Date: 2026-07-30 16:30:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '6a3e1b7f02cd'
down_revision: Union[str, Sequence[str], None] = '558b907f12e5'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # 1. Add title column to attachments table
    op.add_column('attachments', sa.Column('title', sa.String(length=255), nullable=True))
    
    # 2. Populate title with original_name for historical records
    op.execute("UPDATE attachments SET title = original_name")


def downgrade() -> None:
    # Drop title column from attachments table
    op.drop_column('attachments', 'title')
