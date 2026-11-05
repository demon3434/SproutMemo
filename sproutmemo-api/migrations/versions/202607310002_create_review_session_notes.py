"""create_review_session_notes

Revision ID: 202607310002
Revises: 202607310001
Create Date: 2026-07-31 10:42:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '202607310002'
down_revision: Union[str, Sequence[str], None] = '202607310001'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Create review_session_notes table
    op.create_table(
        'review_session_notes',
        sa.Column('id', sa.Uuid(), nullable=False),
        sa.Column('session_id', sa.Uuid(), nullable=False),
        sa.Column('note_id', sa.Uuid(), nullable=False),
        sa.Column('sort_order', sa.Integer(), nullable=False, server_default='0'),
        sa.ForeignKeyConstraint(['session_id'], ['review_sessions.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['note_id'], ['notes.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_review_session_notes_session_id'), 'review_session_notes', ['session_id'], unique=False)
    op.create_index(op.f('ix_review_session_notes_note_id'), 'review_session_notes', ['note_id'], unique=False)


def downgrade() -> None:
    op.drop_table('review_session_notes')
