<template>
  <div class="review-card-container" v-if="note">
    <!-- 左右分栏布局 -->
    <div class="split-layout">
      
      <!-- 左栏：正文内容与多媒体附件区 (宽度 65%) -->
      <div class="layout-left-pane">
        <div class="pane-content-wrapper">
          <!-- 试卷来源及页码 -->
          <div class="source-header" v-if="sourceDisplayName">
            <span class="source-icon">📖</span>
            <span class="source-text">{{ sourceDisplayName }}</span>
            <span class="page-badge" v-if="note.page_number">P. {{ note.page_number }}</span>
          </div>

          <!-- 题目文本正文 -->
          <div class="problem-body" v-if="note.auxiliary_text">
            <div class="body-label">习题正文</div>
            <p class="body-text">{{ note.auxiliary_text }}</p>
          </div>

          <!-- 附件多媒体展示托盘 (参照查看习题页面样式) -->
          <div class="attachments-body" v-if="images.length > 0 || audios.length > 0 || videos.length > 0">
            <div class="body-label">📎 关联多媒体附件：</div>

            <!-- 图片附件网格 -->
            <div v-if="images.length > 0" class="image-gallery-block" style="margin-top: 8px;">
              <span class="sub-section-title" style="font-size: 11.5px; font-weight: 700; color: var(--text-muted); display: block; margin-bottom: 6px;">🖼️ 图片列表 (共 {{ images.length }} 张，点击进入大图预览)</span>
              <div class="preview-images-grid">
                <div
                  v-for="img in images"
                  :key="img.id"
                  class="img-thumb-card"
                  @click="openMedia(img)"
                >
                  <img :src="getAbsoluteUrl(img.storage_path)" alt="thumb" />
                  <div class="img-title-overlay" :title="img.title || img.original_name">
                    {{ img.title || img.original_name }}
                  </div>
                </div>
              </div>
            </div>

            <!-- 音频附件播放列表 -->
            <div v-if="audios.length > 0" class="audio-gallery-block" style="margin-top: 14px;">
              <span class="sub-section-title" style="font-size: 11.5px; font-weight: 700; color: var(--text-muted); display: block; margin-bottom: 6px;">🎵 录音附件 (共 {{ audios.length }} 个)</span>
              <div class="audio-players-list">
                <div v-for="aud in audios" :key="aud.id" class="audio-player-item">
                  <div class="audio-info">
                    <span class="audio-icon-status">🔊</span>
                    <span class="audio-filename" :title="aud.title || aud.original_name">
                      {{ aud.title || aud.original_name }}
                    </span>
                  </div>
                  <audio controls class="custom-audio" :src="getAbsoluteUrl(aud.storage_path)"></audio>
                </div>
              </div>
            </div>

            <!-- 视频附件播放列表 -->
            <div v-if="videos.length > 0" class="video-gallery-block" style="margin-top: 14px;">
              <span class="sub-section-title" style="font-size: 11.5px; font-weight: 700; color: var(--text-muted); display: block; margin-bottom: 6px;">🎥 视频附件 (共 {{ videos.length }} 个)</span>
              <div class="video-players-list">
                <div v-for="vid in videos" :key="vid.id" class="video-player-item">
                  <div class="video-info">
                    <span class="video-filename" :title="vid.title || vid.original_name">
                      🎥 {{ vid.title || vid.original_name }}
                    </span>
                  </div>
                  <video controls class="custom-video" :src="getAbsoluteUrl(vid.storage_path)"></video>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右栏：计时器、题号面包屑导航与回单卡片控制区 (宽度 35%) -->
      <div class="layout-right-pane">
        <!-- 进度与计时器汇总栏 -->
        <div class="summary-header">
          <div class="session-progress">
            🎯 复习进度: <strong>{{ completedCount }}</strong> / <strong>{{ totalCount }}</strong>
          </div>
          <div class="timer-display" :class="{ paused: isTimerPaused }">
            ⏱️ {{ formattedTime }}
          </div>
        </div>

        <!-- 题号面包屑数字导航栏 -->
        <div class="breadcrumbs-container">
          <div class="breadcrumb-label">题号快速切换</div>
          <div class="breadcrumb-tiles">
            <button
              v-for="(n, idx) in notesList"
              :key="n.id"
              :class="[
                'breadcrumb-tile', 
                { active: idx === currentIndex, completed: n.is_completed }
              ]"
              @click="emit('select-note', idx)"
            >
              {{ idx + 1 }}
              <span class="completed-dot" v-if="n.is_completed">✓</span>
            </button>
          </div>
        </div>

        <!-- 打卡控制回单卡片 -->
        <div class="feedback-card">
          <div class="feedback-title">
            📝 本次练习打卡回单 <span class="required-hint">(必选结果)</span>
          </div>

          <!-- 三选一反馈胶囊 -->
          <div class="outcomes-button-group">
            <button
              v-for="out in [...outcomes].reverse()"
              :key="out.code"
              class="outcome-btn-capsule"
              :class="{ selected: selectedOutcomeCode === out.code }"
              :style="{ '--btn-theme': out.color_hex }"
              @click="selectOutcome(out)"
            >
              {{ out.name }}
            </button>
          </div>

          <!-- 属性就地调整 -->
          <div class="controls-vertical-group">
            <div class="vertical-item">
              <label>掌握度修改</label>
              <div class="mastery-buttons-group">
                <button
                  type="button"
                  class="mastery-btn-item clueless"
                  :class="{ active: form.newMastery === 'CLUELESS' }"
                  @click="form.newMastery = 'CLUELESS'"
                >
                  🔴 完全不会
                </button>
                <button
                  type="button"
                  class="mastery-btn-item partial"
                  :class="{ active: form.newMastery === 'PARTIAL' }"
                  @click="form.newMastery = 'PARTIAL'"
                >
                  🟡 一知半解
                </button>
                <button
                  type="button"
                  class="mastery-btn-item mastered"
                  :class="{ active: form.newMastery === 'MASTERED' }"
                  @click="form.newMastery = 'MASTERED'"
                >
                  🟢 熟练掌握
                </button>
              </div>
            </div>

            <div class="vertical-item">
              <label>难度星级</label>
              <div class="star-rating-box" style="margin-top: 4px;" @mouseleave="hoverStars = 0">
                <span
                  v-for="star in 5"
                  :key="star"
                  class="star-item"
                  :class="{ filled: hoverStars > 0 ? star <= hoverStars : star <= form.newStars }"
                  @mouseenter="hoverStars = star"
                  @click="form.newStars = star"
                >★</span>
              </div>
            </div>

            <div class="form-group-toggle" style="padding: 8px 12px; margin-top: 2px;">
              <div class="toggle-info">
                <span class="toggle-title" style="font-size: 13px;">纳入复习题库</span>
                <span class="toggle-desc" style="font-size: 10.5px;">开启后自动加入复习推荐范围</span>
              </div>
              <label class="switch" style="width: 42px; height: 22px;">
                <input type="checkbox" v-model="isInReviewPool" />
                <span class="slider round" style="border-radius: 22px;"></span>
              </label>
            </div>
          </div>

          <!-- 确认提交并切题 -->
          <div class="submit-action-area">
            <button
              class="btn-action-submit"
              :disabled="!selectedOutcomeCode || submitting"
              @click="handleSubmit"
            >
              {{ submitting ? '正在提交...' : (isNoteCompletedAlready ? '💾 更新打卡反馈' : (isLastItem ? '🏁 提交并完成复习' : '✓ 提交打卡，切下一题 ➔')) }}
            </button>
          </div>
        </div>
      </div>

    </div>

    <!-- 全屏多媒体 Lightbox 浮层 -->
    <MediaLightbox
      :visible="lightboxVisible"
      :media-url="activeMediaUrl"
      :file-type="activeFileType"
      :title="activeMediaTitle"
      @close="lightboxVisible = false"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue';
import MediaLightbox from './MediaLightbox.vue';

const props = defineProps({
  note: { type: Object, required: true },
  currentIndex: { type: Number, default: 0 },
  totalCount: { type: Number, default: 1 },
  completedCount: { type: Number, default: 0 },
  notesList: { type: Array, default: () => [] },
  outcomes: { type: Array, default: () => [] },
  submitting: { type: Boolean, default: false }
});

const emit = defineEmits(['submit-item', 'select-note']);

// 秒表计时器
const seconds = ref(0);
let timerId = null;
const isTimerPaused = ref(false);

// 多媒体 Lightbox
const lightboxVisible = ref(false);
const activeMediaUrl = ref('');
const activeFileType = ref('IMAGE');
const activeMediaTitle = ref('');

// 反馈表单
const selectedOutcomeCode = ref('');
const form = reactive({
  newMastery: 'PARTIAL',
  newStars: 1.0,
  excludeFromReview: false
});

const hoverStars = ref(0);
const isInReviewPool = computed({
  get() {
    return !form.excludeFromReview;
  },
  set(val) {
    form.excludeFromReview = !val;
  }
});

const isLastItem = computed(() => props.currentIndex >= props.totalCount - 1);
const isNoteCompletedAlready = computed(() => props.note?.is_completed || false);

const sourceDisplayName = computed(() => {
  if (!props.note) return '';
  return props.note.source_paper_name || props.note.question_source_name || '';
});

const formattedTime = computed(() => {
  const m = Math.floor(seconds.value / 60).toString().padStart(2, '0');
  const s = (seconds.value % 60).toString().padStart(2, '0');
  return `${m}:${s}`;
});

const images = computed(() => {
  return props.note?.attachments?.filter(a => a.file_type === 'IMAGE') || [];
});

const audios = computed(() => {
  return props.note?.attachments?.filter(a => a.file_type === 'AUDIO') || [];
});

const videos = computed(() => {
  return props.note?.attachments?.filter(a => a.file_type === 'VIDEO') || [];
});

function initCard() {
  seconds.value = 0;
  startTimer();

  if (props.note) {
    form.newMastery = props.note.mastery_level || 'PARTIAL';
    form.newStars = Number(props.note.difficulty_stars) || 1.0;
    form.excludeFromReview = Boolean(props.note.exclude_from_review);
    
    // 如果已经答过卡，回显当时的打卡反馈
    if (props.note.is_completed && props.note.review_log) {
      selectedOutcomeCode.value = props.note.review_log.outcome_code;
      form.newMastery = props.note.review_log.new_mastery;
      form.newStars = Number(props.note.review_log.new_stars);
    } else {
      selectedOutcomeCode.value = '';
    }
  }
}

watch(() => props.note?.id, () => {
  initCard();
});

onMounted(() => {
  initCard();
  document.addEventListener('visibilitychange', handleVisibilityChange);
});

onUnmounted(() => {
  stopTimer();
  document.removeEventListener('visibilitychange', handleVisibilityChange);
});

function startTimer() {
  stopTimer();
  timerId = setInterval(() => {
    seconds.value++;
  }, 1000);
}

function stopTimer() {
  if (timerId) {
    clearInterval(timerId);
    timerId = null;
  }
}

function handleVisibilityChange() {
  if (document.hidden) {
    isTimerPaused.value = true;
    stopTimer();
  } else {
    isTimerPaused.value = false;
    startTimer();
  }
}

function selectOutcome(out) {
  selectedOutcomeCode.value = out.code;
  if (out.suggested_mastery) {
    form.newMastery = out.suggested_mastery;
  }
}

function getAbsoluteUrl(relPath) {
  if (!relPath) return '';
  if (relPath.startsWith('http://') || relPath.startsWith('https://')) {
    return relPath;
  }
  const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8000/api/v1';
  const serverHost = API_BASE.replace('/api/v1', '');
  return `${serverHost}/uploads/${relPath}`;
}

function openMedia(att) {
  activeMediaUrl.value = getAbsoluteUrl(att.storage_path);
  activeFileType.value = att.file_type;
  activeMediaTitle.value = att.original_name || '多媒体预览';
  lightboxVisible.value = true;
}

function handleSubmit() {
  if (!selectedOutcomeCode.value) return;

  emit('submit-item', {
    noteId: props.note.id,
    durationSeconds: seconds.value,
    outcomeCode: selectedOutcomeCode.value,
    newMastery: form.newMastery,
    newStars: form.newStars,
    excludeFromReview: form.excludeFromReview,
    sequenceIndex: props.currentIndex + 1
  });
}
</script>

<style scoped>
.review-card-container {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  height: 100%;
  overflow: hidden;
}

.split-layout {
  display: flex;
  height: 100%;
}

/* 左侧栏样式 */
.layout-left-pane {
  flex: 1.6; /* 占比约 65% */
  border-right: 1px solid var(--border);
  overflow-y: auto;
  background: var(--bg-card);
}

.pane-content-wrapper {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.source-header {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--bg-main);
  padding: 10px 16px;
  border-radius: var(--radius-sm);
  border: 1px dashed var(--border);
}

.source-text {
  font-size: 13.5px;
  font-weight: 700;
  color: var(--text-main);
}

.page-badge {
  font-size: 11px;
  font-weight: 700;
  background: var(--primary-light);
  color: var(--primary);
  padding: 1px 6px;
  border-radius: 4px;
}

.body-label {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-muted);
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.body-text {
  font-size: 15px;
  color: var(--text-main);
  line-height: 1.6;
  background: var(--bg-main);
  padding: 16px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border);
  white-space: pre-wrap;
}

/* 图片附件网格 */
.image-gallery-block {
  margin-bottom: 12px;
}

.preview-images-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
  gap: 12px;
}

.img-thumb-card {
  aspect-ratio: 1;
  border-radius: var(--radius-sm);
  overflow: hidden;
  border: 1px solid var(--border);
  background: #f1f5f9;
  cursor: pointer;
  position: relative;
  transition: var(--transition);
}

.img-thumb-card:hover {
  transform: translateY(-2px);
  border-color: var(--primary);
}

.img-thumb-card img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.img-title-overlay {
  position: absolute;
  bottom: 0; left: 0; right: 0;
  background: rgba(15, 23, 42, 0.75);
  color: #fff;
  font-size: 10px;
  padding: 3px 6px;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 音频列表 */
.audio-gallery-block {
  margin-bottom: 12px;
}

.audio-players-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.audio-player-item {
  background: var(--bg-main);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.audio-info {
  display: flex;
  align-items: center;
  gap: 6px;
}

.audio-filename {
  font-size: 11.5px;
  font-weight: 700;
  color: var(--text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.custom-audio {
  width: 100%;
  height: 32px;
}

/* 视频列表 */
.video-gallery-block {
  margin-bottom: 12px;
}

.video-players-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.video-player-item {
  background: var(--bg-main);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.custom-video {
  width: 100%;
  border-radius: var(--radius-sm);
  background: #000;
  max-height: 240px;
  object-fit: contain;
}

/* 右侧栏样式 */
.layout-right-pane {
  flex: 0.9; /* 占比约 35% */
  background: var(--bg-main);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

/* 进度及计时器 */
.summary-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px;
  border-bottom: 1px solid var(--border);
  background: var(--bg-card);
}

.session-progress {
  font-size: 13.5px;
  color: var(--text-main);
}

.timer-display {
  font-family: monospace;
  font-size: 16px;
  font-weight: 700;
  color: var(--primary);
  background: var(--primary-light);
  padding: 4px 12px;
  border-radius: var(--radius-sm);
}

.timer-display.paused {
  color: var(--text-muted);
  background: var(--border);
}

/* 面包屑导航 */
.breadcrumbs-container {
  padding: 16px 24px;
  border-bottom: 1px solid var(--border);
  background: var(--bg-card);
}

.breadcrumb-label, .feedback-title {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-muted);
  margin-bottom: 12px;
  text-transform: uppercase;
}

.breadcrumb-tiles {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.breadcrumb-tile {
  width: 38px;
  height: 38px;
  border-radius: 6px;
  border: 1px solid var(--border);
  background: var(--bg-main);
  color: var(--text-main);
  font-weight: 700;
  font-size: 13px;
  cursor: pointer;
  position: relative;
  transition: var(--transition);
}

.breadcrumb-tile:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.breadcrumb-tile.active {
  background: var(--primary);
  color: #ffffff;
  border-color: var(--primary);
}

.breadcrumb-tile.completed {
  border-color: var(--primary);
  background: rgba(16, 185, 129, 0.05);
  color: var(--primary);
}

.breadcrumb-tile.completed.active {
  background: var(--primary);
  color: #ffffff;
}

.completed-dot {
  position: absolute;
  top: -2px;
  right: 1px;
  font-size: 8px;
  color: var(--primary);
  font-weight: bold;
}

.breadcrumb-tile.active .completed-dot {
  color: #ffffff;
}

/* 打卡卡片 */
.feedback-card {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.required-hint {
  color: var(--danger);
  font-size: 10px;
}

.outcomes-button-group {
  display: flex;
  flex-direction: row;
  gap: 8px;
}

.outcome-btn-capsule {
  flex: 1;
  height: 40px;
  border-radius: var(--radius-sm);
  border: 1.5px solid var(--btn-theme);
  background: var(--bg-card);
  color: var(--btn-theme);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: var(--transition);
}

.outcome-btn-capsule:hover {
  background: var(--btn-theme);
  color: #ffffff;
  opacity: 0.9;
}

.outcome-btn-capsule.selected {
  background: var(--btn-theme);
  border-color: var(--btn-theme);
  color: #ffffff;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.15);
}

/* 星级评分 */
.star-rating-box {
  display: flex;
  gap: 4px;
}

.star-item {
  font-size: 28px;
  color: #cbd5e1;
  cursor: pointer;
  transition: var(--transition);
}

.star-item:hover, .star-item.filled {
  color: #f59e0b;
}

/* Switch Slider */
.switch {
  position: relative;
  display: inline-block;
  width: 42px;
  height: 22px;
  flex-shrink: 0;
}

.switch input { opacity: 0; width: 0; height: 0; }

.slider {
  position: absolute;
  cursor: pointer;
  top: 0; left: 0; right: 0; bottom: 0;
  background-color: #cbd5e1;
  transition: .4s;
}

.slider:before {
  position: absolute;
  content: "";
  height: 16px; width: 16px;
  left: 3px; bottom: 3px;
  background-color: white;
  transition: .4s;
}

input:checked + .slider { background-color: var(--primary); }
input:checked + .slider:before { transform: translateX(20px); }
.slider.round { border-radius: 22px; }
.slider.round:before { border-radius: 50%; }

.form-group-toggle {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--bg-main);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 8px 12px;
  margin-top: 4px;
}

.toggle-info {
  display: flex;
  flex-direction: column;
}

.toggle-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-main);
}

.toggle-desc {
  font-size: 10.5px;
  color: var(--text-muted);
}

/* 垂直属性表 */
.controls-vertical-group {
  display: flex;
  flex-direction: column;
  gap: 14px;
  background: var(--bg-card);
  padding: 16px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border);
}

.vertical-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.vertical-item label {
  font-size: 11.5px;
  font-weight: 700;
  color: var(--text-muted);
}

.form-select {
  height: 34px;
  padding: 0 10px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--bg-main);
  color: var(--text-main);
  font-size: 12.5px;
  outline: none;
}

.form-select:focus {
  border-color: var(--primary);
}

.checkbox-row {
  flex-direction: row;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}

.checkbox-row input {
  width: 14px;
  height: 14px;
}

.checkbox-row label {
  margin-bottom: 0;
  cursor: pointer;
}

/* 确认操作 */
.submit-action-area {
  margin-top: 10px;
}

.btn-action-submit {
  width: 100%;
  height: 42px;
  background: var(--primary);
  color: #ffffff;
  border: none;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: var(--transition);
}

.btn-action-submit:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.btn-action-submit:hover:not(:disabled) {
  background: var(--primary-hover);
  transform: translateY(-1px);
}

/* 掌握度按钮组 */
.mastery-buttons-group {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}

.mastery-btn-item {
  flex: 1;
  height: 36px;
  border-radius: var(--radius-sm);
  border: 1.5px solid var(--border);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  transition: var(--transition);
}

.mastery-btn-item.clueless:hover {
  border-color: #ef4444;
  color: #ef4444;
}
.mastery-btn-item.clueless.active {
  background: #ef4444;
  border-color: #ef4444;
  color: #ffffff;
  box-shadow: 0 3px 8px rgba(239, 68, 68, 0.2);
}

.mastery-btn-item.partial:hover {
  border-color: #f59e0b;
  color: #f59e0b;
}
.mastery-btn-item.partial.active {
  background: #f59e0b;
  border-color: #f59e0b;
  color: #ffffff;
  box-shadow: 0 3px 8px rgba(245, 158, 11, 0.2);
}

.mastery-btn-item.mastered:hover {
  border-color: #10b981;
  color: #10b981;
}
.mastery-btn-item.mastered.active {
  background: #10b981;
  border-color: #10b981;
  color: #ffffff;
  box-shadow: 0 3px 8px rgba(16, 185, 129, 0.2);
}
</style>
