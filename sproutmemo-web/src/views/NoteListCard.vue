<template>
  <div class="note-card" @click="$emit('open', note)">
    <div class="washi-tape note-card-tape" aria-hidden="true"></div>
    <!-- 标签行 (学期 + 学科) -->
    <div class="card-header-tags">
      <div style="display: flex; gap: 8px; align-items: center;">
        <span class="subject-badge">{{ note.subject_name }}</span>
        <span class="semester-badge-tag">{{ note.semester_name }}</span>
      </div>
      <span class="mastery-badge-dot">
        <AppIcon :name="masteryIcon(note.mastery_level)" :size="16" />
        <span class="dot-indicator">{{ masteryName(note.mastery_level) }}</span>
      </span>
    </div>

    <!-- 来源与页码 -->
    <div class="note-source-info">
      <AppIcon :name="note.source_paper_name ? 'file' : 'book'" :size="14" />
      <span>{{ note.question_source_name || note.source_paper_name || '自定义来源' }}</span>
      <span v-if="note.unit_name" class="page-indicator-badge">{{ note.unit_name }}</span>
      <span v-if="note.page_number" class="page-indicator-badge">P.{{ note.page_number }}</span>
    </div>

    <!-- 难度与星级 -->
    <div class="difficulty-rating" style="display:flex; align-items:center; gap:2px;">
      <AppIcon
        v-for="star in 5"
        :key="star"
        :name="star <= Math.round(note.difficulty_stars) ? 'star-filled' : 'star-outline'"
        :size="15"
      />
    </div>

    <!-- 题目正文摘要 -->
    <p class="note-summary" :class="{ empty: !note.auxiliary_text }">
      {{ note.auxiliary_text || '(暂无文字描述)' }}
    </p>

    <!-- 缩略图栏 -->
    <div class="thumbnail-bar" v-if="imageAttachments.length > 0">
      <div
        v-for="attach in imageAttachments.slice(0, 4)"
        :key="attach.id"
        class="thumb-wrapper"
      >
        <img :src="getAbsoluteUrl(attach.storage_path)" alt="thumb" />
      </div>
      <span class="more-badge" v-if="imageAttachments.length > 4">+{{ imageAttachments.length - 4 }}</span>
    </div>

    <!-- 卡片底部时间与删除 -->
    <div class="card-footer">
      <span class="date">{{ formatDate(note.created_at) }}</span>
      <div class="card-media-counts" v-if="note.attachments && note.attachments.length > 0">
        <span v-if="imageAttachments.length > 0" class="media-count-item" title="图片">
          <AppIcon name="image" :size="13" /> {{ imageAttachments.length }}
        </span>
        <span v-if="audioCount > 0" class="media-count-item" title="音频">
          <AppIcon name="music" :size="13" /> {{ audioCount }}
        </span>
        <span v-if="videoCount > 0" class="media-count-item" title="视频">
          <AppIcon name="video" :size="13" /> {{ videoCount }}
        </span>
      </div>
      <button v-if="canDelete" class="delete-btn" @click.stop="$emit('delete', note)" title="删除此题">
        <AppIcon name="trash" :size="13" /> 删除
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import AppIcon from '../components/AppIcon.vue';

const props = defineProps({
  note: { type: Object, required: true },
  canDelete: { type: Boolean, default: false }
});

defineEmits(['open', 'delete']);

const imageAttachments = computed(() =>
  (props.note.attachments || []).filter(a => a.file_type === 'IMAGE')
);
const audioCount = computed(() =>
  (props.note.attachments || []).filter(a => a.file_type === 'AUDIO').length
);
const videoCount = computed(() =>
  (props.note.attachments || []).filter(a => a.file_type === 'VIDEO').length
);

function masteryName(level) {
  const map = { CLUELESS: '完全不会', PARTIAL: '一知半解', MASTERED: '熟练掌握' };
  return map[level] || level;
}

function masteryIcon(level) {
  const map = {
    CLUELESS: 'mastery-clueless',
    PARTIAL: 'mastery-partial',
    MASTERED: 'mastery-mastered'
  };
  return map[level] || 'mastery-partial';
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function getAbsoluteUrl(relPath) {
  if (!relPath) return '';
  const baseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8000/api/v1';
  return `${baseUrl.replace('/api/v1', '')}/uploads/${relPath}`;
}
</script>

<style scoped src="./NoteListCard.css"></style>

