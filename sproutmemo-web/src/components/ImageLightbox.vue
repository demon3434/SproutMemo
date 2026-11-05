<template>
  <div
    v-if="visible && index !== null && images[index]"
    class="img-preview-overlay"
    @click="handleClose"
    @wheel.prevent="handleZoomWheel"
  >
    <button class="preview-close-btn" @click="handleClose">×</button>

    <div class="preview-zoom-controls" @click.stop>
      <button class="zoom-btn" @click="zoomOut" title="缩小">➖</button>
      <span class="zoom-text">{{ Math.round(zoomScale * 100) }}%</span>
      <button class="zoom-btn" @click="zoomIn" title="放大">➕</button>
      <button class="zoom-btn" @click="resetZoom" title="重置">🔍</button>
      <span class="control-divider">|</span>
      <button class="zoom-btn rotate-btn" type="button" @click="handleRotateLeft" title="左旋转 90°">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.1" stroke-linecap="round" stroke-linejoin="round">
          <rect x="9" y="8" width="12" height="13" rx="2" />
          <path d="M13 4H9a5 5 0 0 0-5 5v4" />
          <path d="M7 10l-3 3-3-3" />
        </svg>
      </button>
      <button class="zoom-btn rotate-btn" type="button" @click="handleRotateRight" title="右旋转 90°">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.1" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="8" width="12" height="13" rx="2" />
          <path d="M11 4h4a5 5 0 0 1 5 5v4" />
          <path d="M17 10l3 3 3-3" />
        </svg>
      </button>
    </div>

    <button
      v-if="images.length > 1"
      class="nav-btn nav-left-btn"
      @click.stop="prevPreview"
      title="上一张"
    >
      ‹
    </button>

    <div class="preview-img-wrapper" @click.stop>
      <img
        :src="images[index].isNew ? images[index].blobUrl : getAbsoluteUrl(images[index].storage_path)"
        alt="large preview"
        :style="{ transform: `scale(${zoomScale}) rotate(${(images[index].rotation || 0)}deg)` }"
        class="preview-large-img"
      />
    </div>

    <button
      v-if="images.length > 1"
      class="nav-btn nav-right-btn"
      @click.stop="nextPreview"
      title="下一张"
    >
      ›
    </button>
  </div>
</template>

<script setup>
import { ref, watch, onUnmounted } from 'vue';

const props = defineProps({
  visible: { type: Boolean, required: true },
  images: { type: Array, required: true, default: () => [] },
  index: { type: Number, default: null },
  isEditMode: { type: Boolean, default: false }
});

const emit = defineEmits(['close', 'update:index', 'rotate-left', 'rotate-right']);

const zoomScale = ref(1);

function getAbsoluteUrl(relPath) {
  if (!relPath) return '';
  const baseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8000/api/v1';
  return `${baseUrl.replace('/api/v1', '')}/uploads/${relPath}`;
}

function handleClose() {
  emit('close');
}

function zoomIn() {
  if (zoomScale.value < 5) {
    zoomScale.value = parseFloat((zoomScale.value + 0.25).toFixed(2));
  }
}

function zoomOut() {
  if (zoomScale.value > 0.25) {
    zoomScale.value = parseFloat((zoomScale.value - 0.25).toFixed(2));
  }
}

function resetZoom() {
  zoomScale.value = 1;
}

function handleZoomWheel(e) {
  if (e.deltaY < 0) {
    zoomIn();
  } else {
    zoomOut();
  }
}

function handleRotateLeft() {
  emit('rotate-left', props.index);
}

function handleRotateRight() {
  emit('rotate-right', props.index);
}

function prevPreview() {
  if (props.images.length <= 1) return;
  const newIndex = (props.index - 1 + props.images.length) % props.images.length;
  emit('update:index', newIndex);
  resetZoom();
}

function nextPreview() {
  if (props.images.length <= 1) return;
  const newIndex = (props.index + 1) % props.images.length;
  emit('update:index', newIndex);
  resetZoom();
}

function handleEscKey(e) {
  if (e.key === 'Escape') {
    handleClose();
  } else if (e.key === 'ArrowLeft') {
    prevPreview();
  } else if (e.key === 'ArrowRight') {
    nextPreview();
  }
}

watch(() => props.visible, (newVal) => {
  if (newVal) {
    window.addEventListener('keydown', handleEscKey);
    resetZoom();
  } else {
    window.removeEventListener('keydown', handleEscKey);
  }
}, { immediate: true });

onUnmounted(() => {
  window.removeEventListener('keydown', handleEscKey);
});
</script>

<style scoped>
.img-preview-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background-color: rgba(15, 23, 42, 0.95);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
  user-select: none;
}

.preview-close-btn {
  position: absolute;
  top: 24px; right: 24px;
  font-size: 36px;
  color: #f1f5f9;
  background: none;
  border: none;
  cursor: pointer;
  width: 44px; height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background-color 0.2s, color 0.2s;
  z-index: 10;
}

.preview-close-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

.preview-zoom-controls {
  position: absolute;
  bottom: 24px; left: 50%;
  transform: translateX(-50%);
  background: rgba(30, 41, 59, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.15);
  padding: 6px 16px;
  border-radius: 30px;
  display: flex;
  align-items: center;
  gap: 12px;
  z-index: 10;
}

.zoom-btn {
  color: #fff;
  background: none;
  border: none;
  font-size: 14px;
  cursor: pointer;
  transition: transform 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.zoom-btn:hover {
  transform: scale(1.2);
}

.zoom-text {
  color: #f1f5f9;
  font-size: 13px;
  font-weight: 700;
  min-width: 48px;
  text-align: center;
}

.nav-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 50px; height: 50px;
  background: rgba(30, 41, 59, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  color: #f1f5f9;
  font-size: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s;
  z-index: 10;
}

.nav-btn:hover {
  background: #3b82f6;
  border-color: #3b82f6;
  color: white;
}

.nav-left-btn { left: 24px; }
.nav-right-btn { right: 24px; }

.preview-img-wrapper {
  width: 100%;
  height: 100%;
  max-width: 100%;
  max-height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  padding: 80px 100px;
  box-sizing: border-box;
  z-index: 1;
}

.preview-large-img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  transition: transform 0.15s ease-out;
}

.control-divider {
  color: rgba(255, 255, 255, 0.3);
  margin: 0 4px;
}
</style>
