<template>
  <div class="review-card-container" v-if="note">
    <!-- 左右分栏布局 -->
    <div class="split-layout">
      
      <!-- 左栏：正文内容与多媒体附件区 (宽度 65%) -->
      <div class="layout-left-pane">
        <div class="pane-content-wrapper">
          <!-- 试卷来源及页码 -->
          <div class="source-header" v-if="sourceDisplayName">
            <span class="source-icon"><AppIcon name="book" :size="16" /></span>
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
            <div class="body-label" style="display:flex; align-items:center; gap:6px;"><AppIcon name="paperclip" :size="15" /> 关联多媒体附件：</div>

            <!-- 图片附件网格 -->
            <div v-if="images.length > 0" class="image-gallery-block" style="margin-top: 8px;">
              <span class="sub-section-title" style="font-size: 12.5px; font-weight: 700; color: var(--text-muted); display: flex; align-items: center; gap: 5px; margin-bottom: 6px;"><AppIcon name="image" :size="14" /> 图片列表 (共 {{ images.length }} 张，点击进入大图预览)</span>
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
              <span class="sub-section-title" style="font-size: 12.5px; font-weight: 700; color: var(--text-muted); display: flex; align-items: center; gap: 5px; margin-bottom: 6px;"><AppIcon name="music" :size="14" /> 录音附件 (共 {{ audios.length }} 个)</span>
              <div class="audio-players-list">
                <div v-for="aud in audios" :key="aud.id" class="audio-player-item">
                  <div class="audio-info">
                    <span class="audio-icon-status"><AppIcon name="volume" :size="15" /></span>
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
              <span class="sub-section-title" style="font-size: 12.5px; font-weight: 700; color: var(--text-muted); display: flex; align-items: center; gap: 5px; margin-bottom: 6px;"><AppIcon name="video" :size="14" /> 视频附件 (共 {{ videos.length }} 个)</span>
              <div class="video-players-list">
                <div v-for="vid in videos" :key="vid.id" class="video-player-item">
                  <div class="video-info">
                    <span class="video-filename" :title="vid.title || vid.original_name" style="display:flex; align-items:center; gap:5px;">
                      <AppIcon name="video" :size="14" /> {{ vid.title || vid.original_name }}
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
          <div class="session-progress" style="display:flex; align-items:center; gap:6px;">
            <AppIcon name="target" :size="15" /> 复习进度: <strong>{{ completedCount }}</strong> / <strong>{{ totalCount }}</strong>
          </div>
          <div class="timer-display" :class="{ paused: isTimerPaused }">
            {{ formattedTime }}
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
          <div class="feedback-title" style="display:flex; align-items:center; gap:6px;">
            <AppIcon name="pen" :size="15" /> 本次练习打卡回单 <span class="required-hint">(必选结果)</span>
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
                  <AppIcon name="mastery-clueless" :size="17" />
                  <span>完全不会</span>
                </button>
                <button
                  type="button"
                  class="mastery-btn-item partial"
                  :class="{ active: form.newMastery === 'PARTIAL' }"
                  @click="form.newMastery = 'PARTIAL'"
                >
                  <AppIcon name="mastery-partial" :size="17" />
                  <span>一知半解</span>
                </button>
                <button
                  type="button"
                  class="mastery-btn-item mastered"
                  :class="{ active: form.newMastery === 'MASTERED' }"
                  @click="form.newMastery = 'MASTERED'"
                >
                  <AppIcon name="mastery-mastered" :size="17" />
                  <span>熟练掌握</span>
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
                >
                  <AppIcon
                    :name="(hoverStars > 0 ? star <= hoverStars : star <= form.newStars) ? 'star-filled' : 'star-outline'"
                    :size="24"
                  />
                </span>
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
              {{ submitting ? '正在提交...' : (isNoteCompletedAlready ? '更新打卡反馈' : (isLastItem ? '提交并完成复习' : '提交打卡，切下一题')) }}
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
import AppIcon from './AppIcon.vue';

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

<style scoped src='./ReviewCard.css'></style>
