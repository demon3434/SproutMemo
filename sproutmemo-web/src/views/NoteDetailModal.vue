<template>
  <div v-if="visible" class="modal-overlay" @click.self="handleCloseWithPrompt">
    <div class="modal-content-wrapper">
      
      <!-- ── 头部控制栏 ── -->
      <div class="modal-header">
        <div class="modal-title-box">
          <h2 class="title-text">{{ isEditMode ? '编辑习题笔记' : '习题详情' }}</h2>
        </div>
        <div class="action-buttons">
          <!-- 预览模式按钮 -->
          <template v-if="!isEditMode">
            <button v-if="currentUser?.role !== 'PARENT'" class="action-icon-btn edit" @click="enterEditMode" title="编辑笔记">
              <AppIcon name="pen" :size="16" />
            </button>
            <button class="action-icon-btn close-red" @click="handleClose" title="关闭">
              <AppIcon name="close" :size="16" />
            </button>
          </template>
          <!-- 编辑模式按钮 -->
          <template v-else>
            <!-- 仅保留存盘图标，去掉“保存修改”文字 -->
            <button class="action-icon-btn save" @click="handleEditSave" :disabled="saving" title="保存修改">
              <AppIcon name="check" :size="16" />
            </button>
            <!-- 关闭X叉按钮，调用保存/放弃确认提示 -->
            <button class="action-icon-btn close-red" @click="handleCloseWithPrompt" :disabled="saving" title="放弃/退出">
              <AppIcon name="close" :size="16" />
            </button>
          </template>
        </div>
      </div>

      <!-- ── 模态框主体 (左右双栏布局) ── -->
      <div class="modal-body-columns" :class="{ 'edit-mode-active': isEditMode }">
        
        <!-- ======================= 左栏：核心内容区 (自动拉伸) ======================= -->
        <div class="column-left-main">
          
          <!-- ── A. 预览模式左栏 ── -->
          <div v-if="!isEditMode" class="preview-left-content">
            <!-- 解析与笔记正文 -->
            <div class="detail-section">
              <h4 class="section-title"><AppIcon name="pen" :size="16" /> 解析与笔记正文：</h4>
              <div class="note-text-area">
                <p v-if="note.auxiliary_text" class="text-content">{{ note.auxiliary_text }}</p>
                <p v-else class="text-content empty-text">暂无文字描述。</p>
              </div>
            </div>

            <!-- 多媒体展示区 -->
            <div class="detail-section media-section">
              <h4 class="section-title"><AppIcon name="paperclip" :size="16" /> 关联多媒体附件：</h4>
              
              <!-- 图片附件网格 -->
              <div v-if="images.length > 0" class="image-gallery-block">
                <span class="sub-section-title" style="display:inline-flex; align-items:center; gap:5px;"><AppIcon name="image" :size="14" /> 图片列表 (共 {{ images.length }} 张)</span>
                <div class="preview-images-grid">
                  <div
                    v-for="(img, idx) in images"
                    :key="img.id"
                    class="img-thumb-card"
                    @click="openFullScreen(getAbsoluteUrl(img.storage_path), idx)"
                  >
                    <img :src="getAbsoluteUrl(img.storage_path)" alt="thumb" />
                    <div class="img-title-overlay" :title="img.title || img.original_name">
                      {{ img.title || img.original_name }}
                    </div>
                  </div>
                </div>
              </div>

              <!-- 音频附件播放列表 -->
              <div v-if="audios.length > 0" class="audio-gallery-block">
                <span class="sub-section-title" style="display:inline-flex; align-items:center; gap:5px;"><AppIcon name="music" :size="14" /> 录音附件 (共 {{ audios.length }} 个)</span>
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
              <div v-if="videos.length > 0" class="video-gallery-block">
                <span class="sub-section-title" style="display:inline-flex; align-items:center; gap:5px;"><AppIcon name="video" :size="14" /> 视频附件 (共 {{ videos.length }} 个)</span>
                <div class="video-players-list">
                  <div v-for="vid in videos" :key="vid.id" class="video-player-item">
                    <div class="video-info">
                      <span class="video-filename" :title="vid.title || vid.original_name" style="display:inline-flex; align-items:center; gap:5px;">
                        <AppIcon name="video" :size="14" /> {{ vid.title || vid.original_name }}
                      </span>
                    </div>
                    <video controls class="custom-video" :src="getAbsoluteUrl(vid.storage_path)"></video>
                  </div>
                </div>
              </div>

              <div v-if="images.length === 0 && audios.length === 0 && videos.length === 0" class="no-attachments-prompt">
                无任何关联多媒体附件。
              </div>
            </div>
          </div>

          <!-- ── B. 编辑模式左栏 ── -->
          <div v-else class="edit-left-content">
            <div class="form-group">
              <label class="form-label font-bold" style="font-size: 14px; color: var(--text-main);">题目详情与笔记解析</label>
              <!-- 文本域样式与 NoteCreate 相同 -->
              <textarea
                v-model="editAuxiliaryText"
                class="form-control edit-textarea"
                rows="12"
                placeholder="在此编辑题目正文、解析过程、解题思路或注意的陷阱..."
              ></textarea>
            </div>

            <!-- 附件管理区 (与 NoteCreate 结构一致) -->
            <div class="form-group edit-attachments-group">
              <label class="form-label font-bold" style="font-size: 14px; color: var(--text-main);">
                题目与解答图片 (最多 9 张)
                <span class="hint-label" v-if="totalImagesCount > 0">（已添加 {{ totalImagesCount }} 张图片）</span>
              </label>

              <!-- 已上传与新增混合媒体轨道 -->
              <div class="image-uploader-grid" v-if="combinedMediaList.length > 0">
                <div
                  v-for="(item, idx) in combinedMediaList"
                  :key="item.isNew ? idx : item.id"
                  class="preview-item"
                  :class="{ dragging: dragIndex === idx, 'drag-over': dragOverIndex === idx }"
                  :draggable="!isInputActive"
                  @dragstart="handleDragStart($event, idx)"
                  @dragover.prevent="handleDragOver($event, idx)"
                  @dragenter.prevent="handleDragEnter($event, idx)"
                  @dragleave="handleDragLeave($event, idx)"
                  @drop="handleDrop($event, idx)"
                  @dragend="handleDragEnd"
                >
                  <!-- A. 图片渲染 -->
                  <div v-if="item.file_type === 'IMAGE'" class="preview-card" @click="openFullScreen(null, getEditImageIndex(idx))" style="cursor: pointer;">
                    <img :src="item.isNew ? item.blobUrl : getAbsoluteUrl(item.storage_path)" alt="preview" :style="{ transform: `rotate(${(item.rotation || 0)}deg)` }" />
                    <button type="button" class="rotate-card-btn" @click.stop="rotateCombinedMediaAt(idx, 90)" title="顺时针旋转 90°">
                      <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                        <rect x="3" y="8" width="12" height="13" rx="2" />
                        <path d="M11 4h4a5 5 0 0 1 5 5v4" />
                        <path d="M17 10l3 3 3-3" />
                      </svg>
                    </button>
                    <!-- 红叉位置与 NoteCreate 一致，直接放置在 card 内部，取消遮罩层 -->
                    <button type="button" class="remove-btn" @click.stop="removeMedia(idx)" title="删除此图">×</button>
                  </div>

                  <!-- B. 音视频渲染 -->
                  <div v-else class="media-file-icon-box">
                    <span class="media-type-icon"><AppIcon :name="item.file_type === 'AUDIO' ? 'music' : 'video'" :size="20" /></span>
                    <span class="media-file-name" :title="item.title || item.original_name">{{ item.title || item.original_name }}</span>
                    <!-- 红叉位置与图片一致 -->
                    <button type="button" class="remove-btn" @click.stop="removeMedia(idx)" title="删除此附件">×</button>
                  </div>

                  <!-- 显示文件大小 -->
                  <div class="file-size-label">
                    {{ formatFileSize(item.isNew ? item.file?.size : item.file_size) }}
                  </div>
                  
                  <!-- 输入文件标题 -->
                  <input
                    type="text"
                    v-model="item.title"
                    class="image-title-input"
                    placeholder="请给附件命名"
                    @click.stop
                    @mousedown.stop
                    @dragstart.stop.prevent
                    @mouseenter="isInputActive = true"
                    @mouseleave="isInputActive = false"
                    @focus="isInputActive = true"
                    @blur="isInputActive = false"
                  />
                </div>

                <!-- 📷 新上传图片按钮 -->
                <label class="upload-trigger-btn" v-if="totalImagesCount < 9">
                  <span class="upload-icon"><AppIcon name="camera" :size="24" /></span>
                  <span class="upload-text">添加图片</span>
                  <input
                    type="file"
                    multiple
                    accept="image/*"
                    @change="handleEditFileChange"
                    style="display: none;"
                  />
                </label>
              </div>

              <div v-else class="no-attachments-prompt" style="margin-top: 10px;">
                暂无附件。点击下方按钮添加图片。
                <div style="margin-top: 12px; display: flex; justify-content: center;">
                  <label class="upload-trigger-btn" style="width: 110px; height: 110px;">
                    <span class="upload-icon"><AppIcon name="camera" :size="24" /></span>
                    <span class="upload-text">添加图片</span>
                    <input
                      type="file"
                      multiple
                      accept="image/*"
                      @change="handleEditFileChange"
                      style="display: none;"
                    />
                  </label>
                </div>
              </div>

            </div>
          </div>
        </div>

        <!-- ======================= 右栏：属性控制面板 (固定宽度) ======================= -->
        <NoteDetailRightPane :detail='detail' :note='note' :review-log='reviewLog' :outcomes='outcomes' :current-user='currentUser' />
      </div>
    </div>

    <!-- ── 大图全屏查看叠层 ── -->
    <ImageLightbox
      :visible="previewIndex !== null"
      :images="previewImages"
      v-model:index="previewIndex"
      :is-edit-mode="isEditMode"
      @close="closePreview"
      @rotate-left="rotateLeft"
      @rotate-right="rotateRight"
    />
  </div>
</template>

<script setup>
import { inject, onUnmounted, watch } from 'vue';
import { useNoteDetail } from './useNoteDetail.js';
import ImageLightbox from '../components/ImageLightbox.vue';
import AppIcon from '../components/AppIcon.vue';
import NoteDetailRightPane from './NoteDetailRightPane.vue';

const props = defineProps({
  visible: { type: Boolean, required: true },
  note: { type: Object, default: null },
  reviewLog: { type: Object, default: null },
  outcomes: { type: Array, default: () => [] }
});

function getOutcomeName(code) {
  const item = (props.outcomes || []).find(o => o.code === code);
  return item ? item.name : code;
}

function getOutcomeColor(code) {
  const item = (props.outcomes || []).find(o => o.code === code);
  return item ? item.color_hex : 'var(--text-muted)';
}

function translateMastery(level) {
  const map = {
    'CLUELESS': '未掌握',
    'PARTIAL': '略微掌握',
    'FAMILIAR': '基本掌握',
    'MASTERED': '熟练掌握'
  };
  return map[level] || level;
}

const emit = defineEmits(['close', 'refresh']);
const currentUser = inject('currentUser');
const detail = useNoteDetail(props, emit, currentUser);

const {
  isEditMode,
  saving,
  submitError,
  activeStages,
  allSemesters,
  systemUnits,
  filteredEditSystemUnits,
  paperCategories,
  editStageId,
  editSemesterId,
  editSubjectId,
  editSourceType,
  editBookId,
  editPaperCategory,
  editSourcePaperName,
  editUnitCode,
  editPageNumber,
  editDifficultyStars,
  editMasteryLevel,
  editIsInReviewPool,
  editAuxiliaryText,
  editSubjects,
  editBooks,
  combinedMediaList,
  deleteAttachmentIds,
  dragIndex,
  dragOverIndex,
  isInputActive,
  previewIndex,
  hoverStars,
  images,
  previewImages,
  getEditImageIndex,
  rotateLeft,
  rotateRight,
  rotateCombinedMediaAt,
  audios,
  videos,
  totalImagesCount,
  editFilteredSemesters,
  handleDirectMasteryChange,
  handleDirectStarsChange,
  handleDirectReviewPoolChange,
  openFullScreen,
  closePreview,
  enterEditMode,
  exitEditMode,
  handleCloseWithPrompt,
  onEditStageChange,
  onEditSemesterChange,
  onEditSubjectChange,
  setEditSourceType,
  removeMedia,
  handleDragStart,
  handleDragOver,
  handleDragEnter,
  handleDragLeave,
  handleDrop,
  handleDragEnd,
  handleEditFileChange,
  handleEditSave,
  masteryName,
  editPaperSuggestions,
  editShowSuggestions,
  selectEditPaperSuggestion,
  hideEditSuggestionsWithDelay
} = detail;

function getAbsoluteUrl(relPath) {
  if (!relPath) return '';
  const baseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8000/api/v1';
  return `${baseUrl.replace('/api/v1', '')}/uploads/${relPath}`;
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function formatFileSize(bytes) {
  if (bytes === undefined || bytes === null) return '';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

function handleClose() {
  emit('close');
}

function handleEscKey(e) {
  if (e.key === 'Escape' && previewIndex.value === null) {
    handleCloseWithPrompt();
  }
}

watch(() => props.visible, (newVal) => {
  if (newVal) {
    window.addEventListener('keydown', handleEscKey);
  } else {
    window.removeEventListener('keydown', handleEscKey);
  }
}, { immediate: true });

onUnmounted(() => {
  window.removeEventListener('keydown', handleEscKey);
});
</script>

<style scoped src="./NoteDetailModal.css"></style>


