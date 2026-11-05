import os
import shutil
import uuid
import datetime
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from app.db.session import Base
from app.models.note import Attachment
from app.services.migrate_attachments import migrate_attachments
from app.core.config import settings


def test_migration_logic(tmp_path):
    # Override settings.UPLOAD_DIR to a temporary folder
    original_upload_dir = settings.UPLOAD_DIR
    temp_upload_dir = str(tmp_path / "uploads")
    settings.UPLOAD_DIR = temp_upload_dir
    os.makedirs(temp_upload_dir, exist_ok=True)
    
    # Create SQLite engine
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)
    Session = sessionmaker(bind=engine)
    db = Session()
    
    try:
        # Create mock attachment files in old structure
        # Relative path: {student_id}/{semester_slug}/{note_id}/{file_type}/{file_name}
        student_id = str(uuid.uuid4())
        semester_slug = "2026_spring"
        note_id = str(uuid.uuid4())
        
        old_rel_path_1 = f"{student_id}/{semester_slug}/{note_id}/images/file1.png"
        old_rel_path_2 = f"{student_id}/{semester_slug}/{note_id}/audios/file2.mp3"
        
        # Absolute paths
        old_abs_path_1 = os.path.join(temp_upload_dir, old_rel_path_1)
        old_abs_path_2 = os.path.join(temp_upload_dir, old_rel_path_2)
        
        os.makedirs(os.path.dirname(old_abs_path_1), exist_ok=True)
        os.makedirs(os.path.dirname(old_abs_path_2), exist_ok=True)
        
        with open(old_abs_path_1, "w") as f:
            f.write("mock image content")
        with open(old_abs_path_2, "w") as f:
            f.write("mock audio content")
            
        # Add DB records
        created_time = datetime.datetime(2026, 8, 1, 12, 0, 0, tzinfo=datetime.timezone.utc)
        
        att1 = Attachment(
            id=uuid.uuid4(),
            note_id=uuid.UUID(note_id),
            file_type="IMAGE",
            storage_path=old_rel_path_1,
            original_name="file1.png",
            file_size=18,
            created_at=created_time
        )
        att2 = Attachment(
            id=uuid.uuid4(),
            note_id=uuid.UUID(note_id),
            file_type="AUDIO",
            storage_path=old_rel_path_2,
            original_name="file2.mp3",
            file_size=18,
            created_at=created_time
        )
        db.add(att1)
        db.add(att2)
        db.commit()
        
        # Verify old structure exists
        assert os.path.exists(old_abs_path_1)
        assert os.path.exists(old_abs_path_2)
        
        # Run migration
        migrate_attachments(db)
        
        # Reload records
        db.refresh(att1)
        db.refresh(att2)
        
        # Assert new DB paths
        assert att1.storage_path == "2026/08/01/file1.png"
        assert att2.storage_path == "2026/08/01/file2.mp3"
        
        # Assert new physical paths
        new_abs_path_1 = os.path.join(temp_upload_dir, "2026/08/01/file1.png")
        new_abs_path_2 = os.path.join(temp_upload_dir, "2026/08/01/file2.mp3")
        
        assert os.path.exists(new_abs_path_1)
        assert os.path.exists(new_abs_path_2)
        
        # Assert old physical files are deleted/moved
        assert not os.path.exists(old_abs_path_1)
        assert not os.path.exists(old_abs_path_2)
        
        # Assert old directories are removed recursively
        assert not os.path.exists(os.path.dirname(old_abs_path_1))
        assert not os.path.exists(os.path.dirname(old_abs_path_2))
        assert not os.path.exists(os.path.join(temp_upload_dir, student_id))
        
    finally:
        db.close()
        settings.UPLOAD_DIR = original_upload_dir
