<template>
  <Teleport to="body">
    <div v-if="visible" class="lightbox-overlay" @click.self="close">
      <div class="lightbox-toolbar">
        <span class="lightbox-title">{{ title || '多媒体预览' }}</span>
        <div class="zoom-controls" v-if="isImage">
          <button @click="zoomOut" title="缩小">-</button>
          <span class="zoom-level">{{ Math.round(scale * 100) }}%</span>
          <button @click="zoomIn" title="放大">+</button>
          <button @click="resetZoom" title="重置">100%</button>
        </div>
        <button class="close-btn" @click="close">✕</button>
      </div>

      <div 
        class="lightbox-content" 
        ref="containerRef"
        @wheel.prevent="onWheel"
        @mousedown="onMouseDown"
        @mousemove="onMouseMove"
        @mouseup="onMouseUp"
        @mouseleave="onMouseUp"
      >
        <!-- 图片展示 -->
        <img
          v-if="isImage"
          :src="mediaUrl"
          :alt="title"
          class="lightbox-image"
          :style="{
            transform: `translate(${translateX}px, ${translateY}px) scale(${scale})`,
            cursor: isDragging ? 'grabbing' : 'grab'
          }"
          draggable="false"
        />

        <!-- 音频展示 -->
        <div v-else-if="isAudio" class="lightbox-audio-container">
          <audio controls autoplay :src="mediaUrl"></audio>
        </div>

        <!-- 视频展示 -->
        <div v-else-if="isVideo" class="lightbox-video-container">
          <video controls autoplay :src="mediaUrl"></video>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch } from 'vue';

const props = defineProps({
  visible: { type: Boolean, default: false },
  mediaUrl: { type: String, default: '' },
  fileType: { type: String, default: 'IMAGE' },
  title: { type: String, default: '' }
});

const emit = defineEmits(['close']);

const scale = ref(1);
const translateX = ref(0);
const translateY = ref(0);
const isDragging = ref(false);
let startX = 0;
let startY = 0;

const isImage = computed(() => props.fileType === 'IMAGE' || props.fileType === 'IMAGE_ATTACHMENT');
const isAudio = computed(() => props.fileType === 'AUDIO');
const isVideo = computed(() => props.fileType === 'VIDEO');

watch(() => props.visible, (val) => {
  if (val) {
    resetZoom();
  }
});

function close() {
  emit('close');
}

function zoomIn() {
  scale.value = Math.min(scale.value + 0.25, 4);
}

function zoomOut() {
  scale.value = Math.max(scale.value - 0.25, 0.5);
}

function resetZoom() {
  scale.value = 1;
  translateX.value = 0;
  translateY.value = 0;
}

function onWheel(e) {
  if (!isImage.value) return;
  if (e.deltaY < 0) {
    zoomIn();
  } else {
    zoomOut();
  }
}

function onMouseDown(e) {
  if (!isImage.value) return;
  isDragging.value = true;
  startX = e.clientX - translateX.value;
  startY = e.clientY - translateY.value;
}

function onMouseMove(e) {
  if (!isDragging.value || !isImage.value) return;
  translateX.value = e.clientX - startX;
  translateY.value = e.clientY - startY;
}

function onMouseUp() {
  isDragging.value = false;
}
</script>

<style scoped>
.lightbox-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(15, 23, 42, 0.9);
  z-index: 9999;
  display: flex;
  flex-direction: column;
  backdrop-filter: blur(4px);
}

.lightbox-toolbar {
  height: 48px;
  background: rgba(30, 41, 59, 0.8);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  color: #fff;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}

.lightbox-title {
  font-size: 14px;
  font-weight: 500;
  color: #f1f5f9;
}

.zoom-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.zoom-controls button {
  background: rgba(255,255,255,0.15);
  border: none;
  color: #fff;
  border-radius: 4px;
  padding: 4px 10px;
  cursor: pointer;
  font-size: 13px;
}

.zoom-controls button:hover {
  background: rgba(255,255,255,0.3);
}

.zoom-level {
  font-size: 12px;
  color: #94a3b8;
  min-width: 42px;
  text-align: center;
}

.close-btn {
  background: transparent;
  border: none;
  color: #cbd5e1;
  font-size: 20px;
  cursor: pointer;
  padding: 4px 8px;
}

.close-btn:hover {
  color: #fff;
}

.lightbox-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
  user-select: none;
}

.lightbox-image {
  max-width: 90vw;
  max-height: 85vh;
  object-fit: contain;
  transition: transform 0.05s ease-out;
  transform-origin: center center;
}

.lightbox-audio-container, .lightbox-video-container {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 80%;
  max-width: 800px;
}

.lightbox-video-container video {
  max-width: 100%;
  max-height: 80vh;
  border-radius: 8px;
}

.lightbox-audio-container audio {
  width: 100%;
}
</style>
