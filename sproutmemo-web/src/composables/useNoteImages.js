import { ref } from 'vue';
import { showAlert, showConfirm } from '../services/dialog.js';
import { compressImage } from '../utils/imageCompressor.js';

export function useNoteImages() {
  const uploadFiles = ref([]);
  const previewUrls = ref([]);
  const imageTitles = ref([]);
  const imageRotations = ref([]);

  // ── 图片附件选择与压缩 ──
  async function handleFileChange(event) {
    const files = event.target.files;
    if (!files) return;

    for (let i = 0; i < files.length; i++) {
      if (uploadFiles.value.length >= 9) {
        showAlert('最多只能添加 9 张图片！');
        break;
      }
      const file = files[i];
      try {
        const compressedFile = await compressImage(file);
        uploadFiles.value.push(compressedFile);
        previewUrls.value.push(URL.createObjectURL(compressedFile));
        imageTitles.value.push('');
        imageRotations.value.push(0);
      } catch (err) {
        console.error('Image compression failed, using original:', err);
        uploadFiles.value.push(file);
        previewUrls.value.push(URL.createObjectURL(file));
        imageTitles.value.push('');
        imageRotations.value.push(0);
      }
    }
    event.target.value = '';
  }

  async function removeImage(index) {
    const ok = await showConfirm('确定要移除这张新添加的图片吗？');
    if (!ok) return;
    uploadFiles.value.splice(index, 1);
    previewUrls.value.splice(index, 1);
    imageTitles.value.splice(index, 1);
    imageRotations.value.splice(index, 1);
  }

  // ── 拖拽排序逻辑 ──
  const dragIndex = ref(null);
  const dragOverIndex = ref(null);
  const isInputActive = ref(false);

  function handleDragStart(event, idx) {
    event.dataTransfer.effectAllowed = 'move';
    const imgEl = event.currentTarget.querySelector('.preview-card img');
    if (imgEl) {
      event.dataTransfer.setDragImage(imgEl, 50, 50);
    }
    setTimeout(() => {
      dragIndex.value = idx;
    }, 0);
  }

  function handleDragOver(event, idx) {
    if (dragOverIndex.value !== idx) {
      dragOverIndex.value = idx;
    }
  }

  function handleDragEnter(event, idx) {
    dragOverIndex.value = idx;
  }

  function handleDragLeave(event, idx) {
    if (dragOverIndex.value === idx) {
      dragOverIndex.value = null;
    }
  }

  function handleDrop(event, targetIdx) {
    dragOverIndex.value = null;
    if (dragIndex.value === null || dragIndex.value === targetIdx) return;

    const movedUrl = previewUrls.value[dragIndex.value];
    const movedFile = uploadFiles.value[dragIndex.value];
    const movedTitle = imageTitles.value[dragIndex.value];
    const movedRotation = imageRotations.value[dragIndex.value];

    previewUrls.value.splice(dragIndex.value, 1);
    uploadFiles.value.splice(dragIndex.value, 1);
    imageTitles.value.splice(dragIndex.value, 1);
    imageRotations.value.splice(dragIndex.value, 1);

    previewUrls.value.splice(targetIdx, 0, movedUrl);
    uploadFiles.value.splice(targetIdx, 0, movedFile);
    imageTitles.value.splice(targetIdx, 0, movedTitle);
    imageRotations.value.splice(targetIdx, 0, movedRotation);
  }

  function handleDragEnd() {
    dragIndex.value = null;
    dragOverIndex.value = null;
  }

  function formatFileSize(bytes) {
    if (bytes === undefined || bytes === null) return '';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }

  // ── 大图弹层预览与缩放旋转 ──
  const previewIndex = ref(null);
  const zoomScale = ref(1);

  function rotateLeft() {
    const idx = previewIndex.value;
    if (idx === null) return;
    imageRotations.value[idx] = ((imageRotations.value[idx] || 0) - 90) % 360;
  }

  function rotateRight() {
    const idx = previewIndex.value;
    if (idx === null) return;
    imageRotations.value[idx] = ((imageRotations.value[idx] || 0) + 90) % 360;
  }

  function rotateImageAt(idx, deltaAngle = 90) {
    if (idx === null || idx === undefined || idx < 0 || idx >= imageRotations.value.length) return;
    imageRotations.value[idx] = ((imageRotations.value[idx] || 0) + deltaAngle) % 360;
  }

  function openPreview(index) {
    previewIndex.value = index;
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
    event.preventDefault();
    if (event.deltaY < 0) {
      zoomIn();
    } else {
      zoomOut();
    }
  }

  function prevPreview() {
    if (previewIndex.value !== null && previewIndex.value > 0) {
      previewIndex.value--;
      resetZoom();
    }
  }

  function nextPreview() {
    if (previewIndex.value !== null && previewIndex.value < uploadFiles.value.length - 1) {
      previewIndex.value++;
      resetZoom();
    }
  }

  function resetImages() {
    uploadFiles.value = [];
    previewUrls.value = [];
    imageTitles.value = [];
    imageRotations.value = [];
  }

  return {
    uploadFiles,
    previewUrls,
    imageTitles,
    imageRotations,
    handleFileChange,
    removeImage,
    dragIndex,
    dragOverIndex,
    isInputActive,
    handleDragStart,
    handleDragOver,
    handleDragEnter,
    handleDragLeave,
    handleDrop,
    handleDragEnd,
    formatFileSize,
    previewIndex,
    zoomScale,
    rotateLeft,
    rotateRight,
    rotateImageAt,
    openPreview,
    closePreview,
    zoomIn,
    zoomOut,
    resetZoom,
    handleZoomWheel,
    prevPreview,
    nextPreview,
    resetImages
  };
}
