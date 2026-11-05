"""add_review_tables_and_last_reviewed

Revision ID: 202607310001
Revises: 11860f6a6891
Create Date: 2026-07-31 02:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '202607310001'
down_revision: Union[str, Sequence[str], None] = '9d8f407857df'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # 1. Add last_reviewed_at column to notes
    op.add_column('notes', sa.Column('last_reviewed_at', sa.DateTime(timezone=True), nullable=True))
    op.create_index(op.f('ix_notes_last_reviewed_at'), 'notes', ['last_reviewed_at'], unique=False)

    # 2. Create review_strategies table
    op.create_table(
        'review_strategies',
        sa.Column('code', sa.String(length=30), nullable=False),
        sa.Column('name', sa.String(length=50), nullable=False),
        sa.Column('description', sa.Text(), nullable=True),
        sa.Column('sort_order', sa.Integer(), nullable=False, server_default='0'),
        sa.PrimaryKeyConstraint('code')
    )

    # 3. Create review_count_presets table
    op.create_table(
        'review_count_presets',
        sa.Column('code', sa.String(length=30), nullable=False),
        sa.Column('count_value', sa.Integer(), nullable=False),
        sa.Column('name', sa.String(length=50), nullable=False),
        sa.Column('sort_order', sa.Integer(), nullable=False, server_default='0'),
        sa.PrimaryKeyConstraint('code')
    )

    # 4. Create review_outcomes table
    op.create_table(
        'review_outcomes',
        sa.Column('code', sa.String(length=30), nullable=False),
        sa.Column('name', sa.String(length=50), nullable=False),
        sa.Column('color_hex', sa.String(length=20), nullable=False, server_default='#3b82f6'),
        sa.Column('suggested_mastery', sa.String(length=20), nullable=True),
        sa.Column('sort_order', sa.Integer(), nullable=False, server_default='0'),
        sa.PrimaryKeyConstraint('code')
    )

    # Seed metadata tables
    op.execute(
        "INSERT INTO review_strategies (code, name, description, sort_order) VALUES "
        "('RANDOM', '听天由命', '纯随机抽题', 10), "
        "('EBBINGHAUS', '艾宾浩斯', '根据复习时间与掌握度动态计算遗忘权重', 20), "
        "('CHALLENGE', '自我挑战', '仅难度4星及以上，且排除熟练掌握', 30), "
        "('PERSISTENT_FAILURE', '屡败屡战', '优先抽屡错且未掌握的题目', 40)"
    )
    op.execute(
        "INSERT INTO review_count_presets (code, count_value, name, sort_order) VALUES "
        "('PRESET_3', 3, '蜻蜓点水 (3题)', 10), "
        "('PRESET_5', 5, '稳扎稳打 (5题)', 20), "
        "('PRESET_8', 8, '精益求精 (8题)', 30), "
        "('PRESET_10', 10, '扶摇直上 (10题)', 40)"
    )
    op.execute(
        "INSERT INTO review_outcomes (code, name, color_hex, suggested_mastery, sort_order) VALUES "
        "('TOO_EASY', '太简单了！', '#10b981', 'MASTERED', 10), "
        "('WRONG_AGAIN', '又做错了', '#f59e0b', 'PARTIAL', 20), "
        "('STILL_CLUELESS', '照样不会', '#ef4444', 'CLUELESS', 30)"
    )

    # 5. Create review_sessions table
    op.create_table(
        'review_sessions',
        sa.Column('id', sa.Uuid(), nullable=False),
        sa.Column('student_id', sa.Uuid(), nullable=False),
        sa.Column('semester_id', sa.Uuid(), nullable=False),
        sa.Column('subject_id', sa.Uuid(), nullable=True),
        sa.Column('unit_code', sa.Integer(), nullable=True),
        sa.Column('strategy_code', sa.String(length=30), nullable=False),
        sa.Column('target_count', sa.Integer(), nullable=False, server_default='5'),
        sa.Column('completed_count', sa.Integer(), nullable=False, server_default='0'),
        sa.Column('status', sa.String(length=20), nullable=False, server_default='IN_PROGRESS'),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('completed_at', sa.DateTime(timezone=True), nullable=True),
        sa.ForeignKeyConstraint(['student_id'], ['users.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['semester_id'], ['semesters.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['subject_id'], ['subjects.id'], ondelete='SET NULL'),
        sa.ForeignKeyConstraint(['unit_code'], ['system_units.id'], ondelete='SET NULL'),
        sa.ForeignKeyConstraint(['strategy_code'], ['review_strategies.code'], ondelete='RESTRICT'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_review_sessions_student_id'), 'review_sessions', ['student_id'], unique=False)
    op.create_index(op.f('ix_review_sessions_semester_id'), 'review_sessions', ['semester_id'], unique=False)
    op.create_index(op.f('ix_review_sessions_subject_id'), 'review_sessions', ['subject_id'], unique=False)
    op.create_index(op.f('ix_review_sessions_unit_code'), 'review_sessions', ['unit_code'], unique=False)
    op.create_index(op.f('ix_review_sessions_status'), 'review_sessions', ['status'], unique=False)
    op.create_index(op.f('ix_review_sessions_created_at'), 'review_sessions', ['created_at'], unique=False)

    # 6. Create review_logs table
    op.create_table(
        'review_logs',
        sa.Column('id', sa.Uuid(), nullable=False),
        sa.Column('session_id', sa.Uuid(), nullable=False),
        sa.Column('student_id', sa.Uuid(), nullable=False),
        sa.Column('note_id', sa.Uuid(), nullable=False),
        sa.Column('sequence_index', sa.Integer(), nullable=False, server_default='1'),
        sa.Column('duration_seconds', sa.Float(), nullable=False, server_default='0.0'),
        sa.Column('outcome_code', sa.String(length=30), nullable=False),
        sa.Column('previous_mastery', sa.String(length=20), nullable=False),
        sa.Column('new_mastery', sa.String(length=20), nullable=False),
        sa.Column('previous_stars', sa.Numeric(precision=2, scale=1), nullable=False),
        sa.Column('new_stars', sa.Numeric(precision=2, scale=1), nullable=False),
        sa.Column('exclude_from_review_snapshot', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(['session_id'], ['review_sessions.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['student_id'], ['users.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['note_id'], ['notes.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['outcome_code'], ['review_outcomes.code'], ondelete='RESTRICT'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_review_logs_session_id'), 'review_logs', ['session_id'], unique=False)
    op.create_index(op.f('ix_review_logs_student_id'), 'review_logs', ['student_id'], unique=False)
    op.create_index(op.f('ix_review_logs_note_id'), 'review_logs', ['note_id'], unique=False)
    op.create_index(op.f('ix_review_logs_created_at'), 'review_logs', ['created_at'], unique=False)


def downgrade() -> None:
    op.drop_table('review_logs')
    op.drop_table('review_sessions')
    op.drop_table('review_outcomes')
    op.drop_table('review_count_presets')
    op.drop_table('review_strategies')
    op.drop_index(op.f('ix_notes_last_reviewed_at'), table_name='notes')
    op.drop_column('notes', 'last_reviewed_at')
