import { ref, computed } from 'vue';
import { api } from '../services/api.js';
import { showConfirm, showAlert } from '../services/dialog.js';
import { compressImage } from '../utils/imageCompressor.js';

export function useNoteDetailMedia(props, emit, isEditMode) {
  const combinedMediaList = ref([]);
  const deleteAttachmentIds = ref([]);

  const dragIndex = ref(null);
  const dragOverIndex = ref(null);
  const isInputActive = ref(false);

  const previewIndex = ref(null);
  const zoomScale = ref(1);

  const images = computed(() => {
    return props.note?.attachments?.filter(a => a.file_type === 'IMAGE') || [];
  });

  const audios = computed(() => {
    return props.note?.attachments?.filter(a => a.file_type === 'AUDIO') || [];
  });

  const videos = computed(() => {
    return props.note?.attachments?.filter(a => a.file_type === 'VIDEO') || [];
  });

  const totalImagesCount = computed(() => {
    return combinedMediaList.value.filter(a => a.file_type === 'IMAGE').length;
  });

  // ── 大图轮播预览控制 ──
  function openFullScreen(url, idx) {
    previewIndex.value = idx;
    zoomScale.value = 1;
  }

  function closePreview() {
    previewIndex.value = null;
    zoomScale.value = 1;
  }

  function zoomIn() {
    if (zoomScale.value < 4) {
      zoomScale.value = parseFloat((zoomScale.value + 0.15).toFixed(2));
    }
  }

  function zoomOut() {
    if (zoomScale.value > 0.3) {
      zoomScale.value = parseFloat((zoomScale.value - 0.15).toFixed(2));
    }
  }

  function resetZoom() {
    zoomScale.value = 1;
  }

  function handleZoomWheel(event) {
    if (event.deltaY < 0) {
      zoomIn();
    } else {
      zoomOut();
    }
  }

  const previewImages = computed(() => {
    if (isEditMode.value) {
      return combinedMediaList.value.filter(a => a.file_type === 'IMAGE');
    }
    return props.note?.attachments?.filter(a => a.file_type === 'IMAGE') || [];
  });

  function getEditImageIndex(combinedIdx) {
    const list = combinedMediaList.value.filter(a => a.file_type === 'IMAGE');
    const targetItem = combinedMediaList.value[combinedIdx];
    return list.indexOf(targetItem);
  }

  async function persistViewModeRotation(item, deltaAngle) {
    if (!props.note?.id || !item?.id) return;
    try {
      const res = await api.post(`/notes/${props.note.id}/attachments/${item.id}/rotate`, {
        angle: deltaAngle
      });
      if (res.data?.code === 200 && res.data.data) {
        const updatedNote = res.data.data;
        const updatedAtt = (updatedNote.attachments || []).find(a => a.id === item.id);
        if (updatedAtt) {
          item.storage_path = updatedAtt.storage_path;
          item.file_size = updatedAtt.file_size;
          item.rotation = 0;
        }
        props.note.updated_at = updatedNote.updated_at;
        props.note.sync_base_time = updatedNote.sync_base_time;
        emit('refresh');
      }
    } catch (err) {
      console.error('Failed to persist image rotation:', err);
    }
  }

  function rotateLeft() {
    const idx = previewIndex.value;
    if (idx === null) return;
    const item = previewImages.value[idx];
    if (item) {
      if (isEditMode.value) {
        item.rotation = ((item.rotation || 0) - 90) % 360;
      } else {
        persistViewModeRotation(item, -90);
      }
    }
  }

  function rotateRight() {
    const idx = previewIndex.value;
    if (idx === null) return;
    const item = previewImages.value[idx];
    if (item) {
      if (isEditMode.value) {
        item.rotation = ((item.rotation || 0) + 90) % 360;
      } else {
        persistViewModeRotation(item, 90);
      }
    }
  }

  function rotateCombinedMediaAt(combinedIdx, deltaAngle = 90) {
    const item = combinedMediaList.value[combinedIdx];
    if (item && item.file_type === 'IMAGE') {
      item.rotation = ((item.rotation || 0) + deltaAngle) % 360;
    }
  }

  function prevPreview() {
    if (previewImages.value.length === 0) return;
    previewIndex.value = (previewIndex.value - 1 + previewImages.value.length) % previewImages.value.length;
    zoomScale.value = 1;
  }

  function nextPreview() {
    if (previewImages.value.length === 0) return;
    previewIndex.value = (previewIndex.value + 1) % previewImages.value.length;
    zoomScale.value = 1;
  }

  // ── 附件顺序调整 & 移除 ──
  function moveMediaLeft(idx) {
    if (idx === 0) return;
    const list = [...combinedMediaList.value];
    const temp = list[idx];
    list[idx] = list[idx - 1];
    list[idx - 1] = temp;
    combinedMediaList.value = list;
  }

  function moveMediaRight(idx) {
    if (idx === combinedMediaList.value.length - 1) return;
    const list = [...combinedMediaList.value];
    const temp = list[idx];
    list[idx] = list[idx + 1];
    list[idx + 1] = temp;
    combinedMediaList.value = list;
  }

  async function removeMedia(idx) {
    const item = combinedMediaList.value[idx];
    const ok = await showConfirm(`确定要删除此${item.file_type === 'IMAGE' ? '图片' : item.file_type === 'AUDIO' ? '录音' : '视频'}附件吗？`);
    if (!ok) return;

    if (!item.isNew) {
      deleteAttachmentIds.value.push(item.id);
    }
    combinedMediaList.value.splice(idx, 1);
  }

  function handleDragStart(event, idx) {
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
      const imgEl = event.currentTarget.querySelector('.preview-card img') || event.currentTarget.querySelector('.media-file-icon-box');
      if (imgEl) {
        event.dataTransfer.setDragImage(imgEl, 50, 50);
      }
    }
    setTimeout(() => {
      dragIndex.value = idx;
    }, 0);
  }

  function handleDragOver(e, index) {
    dragOverIndex.value = index;
  }

  function handleDragEnter(e, index) {
    dragOverIndex.value = index;
  }

  function handleDragLeave(e, index) {
    if (dragOverIndex.value === index) {
      dragOverIndex.value = null;
    }
  }

  function handleDrop(e, index) {
    if (dragIndex.value === null || dragIndex.value === index) return;
    const list = [...combinedMediaList.value];
    const draggedItem = list[dragIndex.value];
    list.splice(dragIndex.value, 1);
    list.splice(index, 0, draggedItem);
    combinedMediaList.value = list;
    dragIndex.value = null;
    dragOverIndex.value = null;
  }

  function handleDragEnd() {
    dragIndex.value = null;
    dragOverIndex.value = null;
  }

  async function handleEditFileChange(event) {
    const files = event.target.files;
    if (!files) return;

    for (let i = 0; i < files.length; i++) {
      if (totalImagesCount.value >= 9) {
        await showAlert('总图片数量不能超过 9 张！');
        break;
      }
      const file = files[i];
      try {
        const compressedFile = await compressImage(file);
        combinedMediaList.value.push({
          isNew: true,
          file: compressedFile,
          file_type: 'IMAGE',
          blobUrl: URL.createObjectURL(compressedFile),
          original_name: file.name,
          title: '',
          rotation: 0
        });
      } catch (err) {
        console.error(err);
        combinedMediaList.value.push({
          isNew: true,
          file: file,
          file_type: 'IMAGE',
          blobUrl: URL.createObjectURL(file),
          original_name: file.name,
          title: '',
          rotation: 0
        });
      }
    }
    event.target.value = '';
  }

  return {
    combinedMediaList,
    deleteAttachmentIds,
    dragIndex,
    dragOverIndex,
    isInputActive,
    previewIndex,
    zoomScale,
    images,
    audios,
    videos,
    totalImagesCount,
    previewImages,
    getEditImageIndex,
    rotateLeft,
    rotateRight,
    rotateCombinedMediaAt,
    openFullScreen,
    closePreview,
    zoomIn,
    zoomOut,
    resetZoom,
    handleZoomWheel,
    prevPreview,
    nextPreview,
    moveMediaLeft,
    moveMediaRight,
    removeMedia,
    handleDragStart,
    handleDragOver,
    handleDragEnter,
    handleDragLeave,
    handleDrop,
    handleDragEnd,
    handleEditFileChange
  };
}

