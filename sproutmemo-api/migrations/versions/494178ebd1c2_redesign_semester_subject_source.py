"""redesign_semester_subject_source

Revision ID: 494178ebd1c2
Revises: bbe8c523759d
Create Date: 2026-07-27 12:49:10.116261

"""
from typing import Sequence, Union
import uuid
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '494178ebd1c2'
down_revision: Union[str, Sequence[str], None] = 'bbe8c523759d'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # 1. Drop foreign key constraint from notes to source_books
    op.drop_constraint('notes_source_book_id_fkey', 'notes', type_='foreignkey')
    
    # 2. Drop old tables
    op.drop_table('student_semester_subject_books')
    op.drop_table('student_semester_subjects')
    op.drop_table('source_books')
    op.drop_table('subjects')
    
    # 3. Create semesters table
    op.create_table(
        'semesters',
        sa.Column('id', sa.Uuid(), nullable=False),
        sa.Column('name', sa.String(length=100), nullable=False),
        sa.Column('start_date', sa.DateTime(timezone=True), nullable=True),
        sa.Column('end_date', sa.DateTime(timezone=True), nullable=True),
        sa.Column('is_active', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_semesters_name'), 'semesters', ['name'], unique=True)
    op.create_index(op.f('ix_semesters_is_active'), 'semesters', ['is_active'], unique=False)

    # 4. Create subjects table (now global-only)
    op.create_table(
        'subjects',
        sa.Column('id', sa.Uuid(), nullable=False),
        sa.Column('subject_name', sa.String(length=50), nullable=False),
        sa.Column('sort_order', sa.Integer(), nullable=False, server_default='0'),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_subjects_subject_name'), 'subjects', ['subject_name'], unique=True)

    # 5. Create student_semester_subjects table
    op.create_table(
        'student_semester_subjects',
        sa.Column('id', sa.Uuid(), nullable=False),
        sa.Column('student_id', sa.Uuid(), nullable=False),
        sa.Column('semester_id', sa.Uuid(), nullable=False),
        sa.Column('subject_id', sa.Uuid(), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.ForeignKeyConstraint(['student_id'], ['users.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['semester_id'], ['semesters.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['subject_id'], ['subjects.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('student_id', 'semester_id', 'subject_id', name='uq_student_semester_subject')
    )
    op.create_index(op.f('ix_student_semester_subjects_student_id'), 'student_semester_subjects', ['student_id'], unique=False)
    op.create_index(op.f('ix_student_semester_subjects_semester_id'), 'student_semester_subjects', ['semester_id'], unique=False)
    op.create_index(op.f('ix_student_semester_subjects_subject_id'), 'student_semester_subjects', ['subject_id'], unique=False)

    # 6. Create question_sources table
    op.create_table(
        'question_sources',
        sa.Column('id', sa.Uuid(), nullable=False),
        sa.Column('student_id', sa.Uuid(), nullable=False),
        sa.Column('semester_id', sa.Uuid(), nullable=False),
        sa.Column('subject_id', sa.Uuid(), nullable=False),
        sa.Column('name', sa.String(length=150), nullable=False),
        sa.Column('publisher', sa.String(length=100), nullable=True),
        sa.Column('source_type', sa.String(length=20), nullable=False, server_default='BOOK'),
        sa.Column('is_archived', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.ForeignKeyConstraint(['student_id'], ['users.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['semester_id'], ['semesters.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['subject_id'], ['subjects.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(
            ['student_id', 'semester_id', 'subject_id'],
            ['student_semester_subjects.student_id', 'student_semester_subjects.semester_id', 'student_semester_subjects.subject_id'],
            ondelete='CASCADE',
            name='fk_student_semester_subject_binding'
        ),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_question_sources_student_id'), 'question_sources', ['student_id'], unique=False)
    op.create_index(op.f('ix_question_sources_semester_id'), 'question_sources', ['semester_id'], unique=False)
    op.create_index(op.f('ix_question_sources_subject_id'), 'question_sources', ['subject_id'], unique=False)
    op.create_index(op.f('ix_question_sources_is_archived'), 'question_sources', ['is_archived'], unique=False)

    # 7. Alter notes table columns
    # First, drop index on old columns
    op.drop_index('ix_notes_source_book_id', table_name='notes')
    op.drop_index('ix_notes_subject', table_name='notes')
    op.drop_index('ix_notes_semester_snapshot', table_name='notes')

    # Drop old columns
    op.drop_column('notes', 'source_book_id')
    op.drop_column('notes', 'subject')
    op.drop_column('notes', 'semester_snapshot')

    # Add new columns
    op.add_column('notes', sa.Column('semester_id', sa.Uuid(), nullable=False))
    op.add_column('notes', sa.Column('subject_id', sa.Uuid(), nullable=False))
    op.add_column('notes', sa.Column('question_source_id', sa.Uuid(), nullable=True))

    # Add foreign keys for new columns
    op.create_foreign_key('fk_notes_semester_id', 'notes', 'semesters', ['semester_id'], ['id'], ondelete='CASCADE')
    op.create_foreign_key('fk_notes_subject_id', 'notes', 'subjects', ['subject_id'], ['id'], ondelete='CASCADE')
    op.create_foreign_key('fk_notes_question_source_id', 'notes', 'question_sources', ['question_source_id'], ['id'], ondelete='SET NULL')

    # Add indexes for new columns
    op.create_index(op.f('ix_notes_semester_id'), 'notes', ['semester_id'], unique=False)
    op.create_index(op.f('ix_notes_subject_id'), 'notes', ['subject_id'], unique=False)
    op.create_index(op.f('ix_notes_question_source_id'), 'notes', ['question_source_id'], unique=False)

    # 8. Seeding default semesters
    semesters_data = [
        "小学一年级上学期", "小学一年级下学期",
        "小学二年级上学期", "小学二年级下学期",
        "小学三年级上学期", "小学三年级下学期",
        "小学四年级上学期", "小学四年级下学期",
        "小学五年级上学期", "小学五年级下学期",
        "小学六年级上学期", "小学六年级下学期",
        "初中一年级上学期", "初中一年级下学期",
        "初中二年级上学期", "初中二年级下学期",
        "初中三年级上学期", "初中三年级下学期",
        "高中一年级上学期", "高中一年级下学期",
        "高中二年级上学期", "高中二年级下学期",
        "高中三年级上学期", "高中三年级下学期"
    ]
    for name in semesters_data:
        id_val = str(uuid.uuid4())
        op.execute(
            sa.text("INSERT INTO semesters (id, name, is_active, created_at, updated_at) VALUES (:id, :name, true, NOW(), NOW())").bindparams(
                id=id_val, name=name
            )
        )

    # 9. Seeding default global subjects
    subjects_data = [("语文", 1), ("数学", 2), ("英语", 3)]
    for name, sort in subjects_data:
        id_val = str(uuid.uuid4())
        op.execute(
            sa.text("INSERT INTO subjects (id, subject_name, sort_order, created_at) VALUES (:id, :subject_name, :sort_order, NOW())").bindparams(
                id=id_val, subject_name=name, sort_order=sort
            )
        )


def downgrade() -> None:
    # 1. Drop foreign keys and indexes on notes table
    op.drop_constraint('fk_notes_question_source_id', 'notes', type_='foreignkey')
    op.drop_constraint('fk_notes_subject_id', 'notes', type_='foreignkey')
    op.drop_constraint('fk_notes_semester_id', 'notes', type_='foreignkey')

    op.drop_index(op.f('ix_notes_question_source_id'), table_name='notes')
    op.drop_index(op.f('ix_notes_subject_id'), table_name='notes')
    op.drop_index(op.f('ix_notes_semester_id'), table_name='notes')

    op.drop_column('notes', 'question_source_id')
    op.drop_column('notes', 'subject_id')
    op.drop_column('notes', 'semester_id')

    # 2. Re-add old notes columns
    op.add_column('notes', sa.Column('source_book_id', sa.Uuid(), nullable=True))
    op.add_column('notes', sa.Column('subject', sa.String(length=20), nullable=False))
    op.add_column('notes', sa.Column('semester_snapshot', sa.String(length=50), nullable=False))
    
    op.create_index(sa.text('ix_notes_source_book_id'), 'notes', ['source_book_id'], unique=False)
    op.create_index(sa.text('ix_notes_subject'), 'notes', ['subject'], unique=False)
    op.create_index(sa.text('ix_notes_semester_snapshot'), 'notes', ['semester_snapshot'], unique=False)

    # 3. Drop new tables
    op.drop_table('question_sources')
    op.drop_table('student_semester_subjects')
    op.drop_table('subjects')
    op.drop_table('semesters')

    # 4. Re-create old tables
    op.create_table('subjects',
        sa.Column('id', sa.Uuid(), nullable=False),
        sa.Column('student_id', sa.Uuid(), nullable=False),
        sa.Column('subject_name', sa.String(length=50), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(['student_id'], ['users.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_table('source_books',
        sa.Column('id', sa.Uuid(), nullable=False),
        sa.Column('student_id', sa.Uuid(), nullable=False),
        sa.Column('grade_semester', sa.String(length=50), nullable=True),
        sa.Column('book_name', sa.String(length=150), nullable=False),
        sa.Column('publisher', sa.String(length=100), nullable=True),
        sa.Column('is_archived', sa.Boolean(), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(['student_id'], ['students.user_id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_table('student_semester_subjects',
        sa.Column('id', sa.Uuid(), nullable=False),
        sa.Column('student_id', sa.Uuid(), nullable=False),
        sa.Column('semester', sa.String(length=50), nullable=False),
        sa.Column('subject_id', sa.Uuid(), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(['student_id'], ['users.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['subject_id'], ['subjects.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_table('student_semester_subject_books',
        sa.Column('id', sa.Uuid(), nullable=False),
        sa.Column('student_semester_subject_id', sa.Uuid(), nullable=False),
        sa.Column('source_book_id', sa.Uuid(), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(['source_book_id'], ['source_books.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['student_semester_subject_id'], ['student_semester_subjects.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id')
    )

    # Re-create foreign key on notes table pointing to source_books.id
    op.create_foreign_key('notes_source_book_id_fkey', 'notes', 'source_books', ['source_book_id'], ['id'], ondelete='SET NULL')
