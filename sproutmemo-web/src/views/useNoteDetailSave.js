import { api } from '../services/api.js';
import { showConfirm, showAlert } from '../services/dialog.js';
import { rotateImageFile } from '../utils/imageCompressor.js';

export function getAbsoluteUrl(relPath) {
  if (!relPath) return '';
  const baseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8000/api/v1';
  return `${baseUrl.replace('/api/v1', '')}/uploads/${relPath}`;
}

export function masteryName(level) {
  const map = { CLUELESS: '完全不会', PARTIAL: '一知半解', MASTERED: '熟练掌握' };
  return map[level] || level;
}

export async function saveNoteDetailEdits({
  props,
  emit,
  getAbsoluteUrl,
  isEditMode,
  saving,
  submitError,
  editSemesterId,
  editSubjectId,
  editSourceType,
  editBookId,
  editPageNumber,
  editSourcePaperName,
  editPaperCategory,
  editUnitCode,
  editDifficultyStars,
  editMasteryLevel,
  editIsInReviewPool,
  editAuxiliaryText,
  combinedMediaList,
  deleteAttachmentIds
}) {
  submitError.value = '';

  if (!editSemesterId.value) {
    submitError.value = '请选择学期';
    return;
  }
  if (!editSubjectId.value) {
    submitError.value = '请选择学科';
    return;
  }

  if (editSourceType.value === 'BOOK') {
    if (!editBookId.value) {
      submitError.value = '请选择关联的习题册';
      return;
    }
    if (!editPageNumber.value.trim()) {
      submitError.value = '关联习题册时，页码为必填项';
      return;
    }
  } else {
    if (!editSourcePaperName.value.trim()) {
      submitError.value = '请输入自定义来源/试卷名称';
      return;
    }
  }

  const ok = await showConfirm('确定要保存对该习题笔记的修改吗？');
  if (!ok) return;

  saving.value = true;

  try {
    for (let i = 0; i < combinedMediaList.value.length; i++) {
      const item = combinedMediaList.value[i];
      if (item.file_type === 'IMAGE' && item.rotation && item.rotation !== 0) {
        try {
          if (item.isNew) {
            const rotatedFile = await rotateImageFile(item.file, item.rotation);
            item.file = rotatedFile;
            if (item.blobUrl) {
              URL.revokeObjectURL(item.blobUrl);
            }
            item.blobUrl = URL.createObjectURL(rotatedFile);
            item.rotation = 0;
          } else {
            const imageUrl = getAbsoluteUrl(item.storage_path);
            const rotatedFile = await rotateImageFile(imageUrl, item.rotation);
            deleteAttachmentIds.value.push(item.id);
            combinedMediaList.value[i] = {
              isNew: true,
              file: rotatedFile,
              file_type: 'IMAGE',
              blobUrl: URL.createObjectURL(rotatedFile),
              original_name: item.original_name,
              title: item.title || '',
              rotation: 0
            };
          }
        } catch (err) {
          console.error('Failed to rotate image before save:', err);
        }
      }
    }

    const formData = new FormData();
    formData.append('semester_id', editSemesterId.value);
    formData.append('subject_id', editSubjectId.value);
    formData.append('mastery_level', editMasteryLevel.value);
    formData.append('difficulty_stars', editDifficultyStars.value.toFixed(1));
    formData.append('exclude_from_review', (!editIsInReviewPool.value).toString());
    formData.append('unit_code', editUnitCode.value.toString());
    formData.append('auxiliary_text', editAuxiliaryText.value.trim());
    formData.append('sync_base_time', props.note.updated_at);

    if (editSourceType.value === 'BOOK') {
      formData.append('question_source_id', editBookId.value);
      formData.append('page_number', editPageNumber.value.trim());
      formData.append('source_paper_name', '');
    } else {
      formData.append('question_source_id', '');
      formData.append('source_paper_name', editSourcePaperName.value.trim());
      formData.append('paper_category', editPaperCategory.value);
      formData.append('page_number', '');
    }

    for (const attId of deleteAttachmentIds.value) {
      formData.append('delete_attachment_ids', attId);
    }

    const existingMeta = [];
    let sortOrderCounter = 0;
    combinedMediaList.value.forEach((item) => {
      if (!item.isNew) {
        existingMeta.push({
          id: item.id,
          title: item.title,
          sort_order: sortOrderCounter++
        });
      }
    });
    formData.append('attachment_metadata', JSON.stringify(existingMeta));

    combinedMediaList.value.forEach((item) => {
      if (item.isNew) {
        formData.append('images', item.file);
        formData.append('image_titles', item.title || '');
      }
    });

    const res = await api.put(`/notes/${props.note.id}`, formData);
    if (res.ok) {
      await showAlert('习题笔记更新成功！');
      isEditMode.value = false;
      emit('refresh');
    } else {
      const errRes = await res.json();
      submitError.value = errRes.msg || '保存失败';
    }
  } catch (err) {
    submitError.value = '网络保存失败，请检查连接';
  } finally {
    saving.value = false;
  }
}
