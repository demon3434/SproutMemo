import os
import re
import shutil
import logging
from sqlalchemy.orm import Session
from app.db.session import SessionLocal
from app.models.note import Attachment
from app.core.config import settings

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")
logger = logging.getLogger("migrate_attachments")

# Regular expression to match YYYY/MM/DD/filename format
NEW_PATH_PATTERN = re.compile(r"^\d{4}/\d{2}/\d{2}/[^/]+$")


def clean_empty_dirs(path: str, base_dir: str):
    """Recursively delete empty directories from path up to base_dir (bottom-up)"""
    current = os.path.abspath(path)
    base = os.path.abspath(base_dir)
    
    while current and current != base and current.startswith(base):
        if os.path.exists(current) and os.path.isdir(current):
            try:
                # If directory is empty, remove it
                if not os.listdir(current):
                    logger.info(f"Removing empty directory: {current}")
                    os.rmdir(current)
                else:
                    break  # Stop climbing if directory is not empty
            except Exception as e:
                logger.warning(f"Failed to check/remove directory {current}: {e}")
                break
        current = os.path.dirname(current)


def migrate_attachments(db: Session):
    """Scan and migrate all legacy attachments to the date-partitioned storage format"""
    logger.info("Starting attachment migration...")
    
    import datetime
    # 1. Fetch all attachments
    attachments = db.query(Attachment).all()
    logger.info(f"Found {len(attachments)} total attachment records in database.")
    
    to_migrate = []
    for att in attachments:
        # Standardize path separators just in case
        normalized_path = att.storage_path.replace("\\", "/")
        match = NEW_PATH_PATTERN.match(normalized_path)
        
        # Calculate expected local date prefix
        created_date = att.created_at
        if created_date:
            created_date = created_date.astimezone()
        else:
            created_date = datetime.datetime.now()
        expected_prefix = created_date.strftime("%Y/%m/%d")
        
        # Migrate if path doesn't match format OR date prefix doesn't match the local timezone creation date
        if not match or not normalized_path.startswith(expected_prefix):
            to_migrate.append((att, normalized_path))
            
    logger.info(f"Identified {len(to_migrate)} attachments requiring migration or date correction.")
    
    if not to_migrate:
        logger.info("No legacy attachments found. Migration is already complete.")
        return
        
    migrated_count = 0
    missing_count = 0
    old_dirs_to_clean = set()
    
    # 2. Iterate and process in batches
    batch_size = 100
    for idx, (att, old_rel_path) in enumerate(to_migrate):
        # Resolve dates using attachment creation date (created_at)
        created_date = att.created_at
        if created_date:
            # Convert UTC datetime to server local timezone to match real-world date
            created_date = created_date.astimezone()
        else:
            # Fallback to current time if created_at is somehow null
            logger.warning(f"Attachment {att.id} has no created_at timestamp. Using file timestamp or current time.")
            old_abs_path = os.path.join(settings.UPLOAD_DIR, old_rel_path)
            if os.path.exists(old_abs_path):
                mtime = os.path.getmtime(old_abs_path)
                import datetime
                created_date = datetime.datetime.fromtimestamp(mtime)  # local time
            else:
                import datetime
                created_date = datetime.datetime.now()  # local time

                
        # Format new paths
        year = created_date.strftime("%Y")
        month = created_date.strftime("%m")
        day = created_date.strftime("%d")
        filename = os.path.basename(old_rel_path)
        
        new_rel_path = f"{year}/{month}/{day}/{filename}"
        
        # Absolute paths
        old_abs_path = os.path.abspath(os.path.join(settings.UPLOAD_DIR, old_rel_path))
        new_abs_dir = os.path.abspath(os.path.join(settings.UPLOAD_DIR, year, month, day))
        new_abs_path = os.path.abspath(os.path.join(new_abs_dir, filename))
        
        # Physical File Move
        file_moved = False
        if os.path.exists(old_abs_path):
            try:
                # Keep track of old parent folder for directory cleanup
                old_dirs_to_clean.add(os.path.dirname(old_abs_path))
                
                # Make new directory structure
                os.makedirs(new_abs_dir, exist_ok=True)
                
                # Move the file (overwriting if destination already exists)
                if old_abs_path != new_abs_path:
                    shutil.move(old_abs_path, new_abs_path)
                file_moved = True
            except Exception as e:
                logger.error(f"Error moving file for Attachment {att.id} ({old_abs_path} -> {new_abs_path}): {e}")
                continue
        else:
            logger.warning(f"Physical file not found for Attachment {att.id} at: {old_abs_path}")
            missing_count += 1
            # Even if the physical file is missing, we update the DB so we don't attempt to process it next time.
            file_moved = True 
            
        if file_moved:
            # Update DB record
            att.storage_path = new_rel_path
            migrated_count += 1
            
        # Commit batch
        if migrated_count % batch_size == 0 and migrated_count > 0:
            db.commit()
            logger.info(f"Successfully migrated {migrated_count}/{len(to_migrate)} attachments...")
            
    # Final database commit
    db.commit()
    logger.info(f"Database update complete. Total successfully migrated: {migrated_count} (Missing files: {missing_count})")
    
    # 3. Clean up empty old directories
    logger.info("Cleaning up empty legacy directories...")
    for old_dir in sorted(list(old_dirs_to_clean), key=len, reverse=True):
        clean_empty_dirs(old_dir, settings.UPLOAD_DIR)
        
    logger.info("Attachment migration successfully completed!")


if __name__ == "__main__":
    db = SessionLocal()
    try:
        migrate_attachments(db)
    finally:
        db.close()
