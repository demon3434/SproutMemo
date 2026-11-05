<template>
  <div class="note-create-container" style="max-width: 1400px; margin: 0 auto; margin-top: -16px;">
    <div class="card" style="padding: 24px;">
      <form @submit.prevent="handleSubmit" class="note-form redesign-grid">
        <!-- 左侧：核心输入区 (60%) -->
        <div class="form-main-content">
          <!-- 题目正文 -->
          <div class="form-group">
            <label class="form-label font-bold" style="font-size: 14px; color: var(--text-main); text-transform: none;">题目详情与笔记解析</label>
            <textarea
              v-model="auxiliaryText"
              class="form-control locked-textarea"
              rows="12"
              placeholder="在此键入题目正文、解析过程、解题思路或需要注意的陷阱..."
            ></textarea>
          </div>

          <!-- 附件上传（Web端只允许图片） -->
          <div class="form-group">
            <label class="form-label font-bold" style="font-size: 14px; color: var(--text-main); text-transform: none;">
              题目与解答图片 (最多 9 张)
              <span class="hint-label" v-if="uploadFiles.length > 0">（已添加 {{ uploadFiles.length }} 张图片）</span>
            </label>
            
            <div class="image-uploader-grid">
              <!-- 已经选择的图片预览 -->
              <div
                v-for="(img, idx) in previewUrls"
                :key="idx"
                class="preview-item"
                :class="{ dragging: dragIndex === idx, 'drag-over': dragOverIndex === idx }"
                :data-tooltip="uploadFiles[idx]?.name"
                :draggable="!isInputActive"
                @dragstart="handleDragStart($event, idx)"
                @dragover.prevent="handleDragOver($event, idx)"
                @dragenter.prevent="handleDragEnter($event, idx)"
                @dragleave="handleDragLeave($event, idx)"
                @drop="handleDrop($event, idx)"
                @dragend="handleDragEnd"
              >
                <div class="preview-card" @click="openPreview(idx)" style="cursor: pointer;">
                  <img :src="img" alt="preview" :style="{ transform: `rotate(${(imageRotations[idx] || 0)}deg)` }" />
                  <button type="button" class="rotate-card-btn" @click.stop="rotateImageAt(idx, 90)" title="顺时针旋转 90°">
                    <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                      <rect x="3" y="8" width="12" height="13" rx="2" />
                      <path d="M11 4h4a5 5 0 0 1 5 5v4" />
                      <path d="M17 10l3 3 3-3" />
                    </svg>
                  </button>
                  <button type="button" class="remove-btn" @click.stop="removeImage(idx)" title="删除此图">×</button>
                </div>
                <div class="file-size-label">{{ formatFileSize(uploadFiles[idx]?.size) }}</div>
                <input
                  type="text"
                  v-model="imageTitles[idx]"
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

              <!-- 上传按钮 -->
              <label class="upload-trigger-btn" v-if="previewUrls.length < 9">
                <span class="upload-icon">📷</span>
                <span class="upload-text">添加图片</span>
                <input
                  type="file"
                  multiple
                  accept="image/*"
                  @change="handleFileChange"
                  style="display: none;"
                />
              </label>
            </div>
          </div>
        </div>

        <!-- 右侧：属性面板 (40%) -->
        <div class="form-sidebar-panel" style="padding: 16px 20px; gap: 14px;">
          <div class="sidebar-section-title">习题属性配置</div>

          <!-- 联动选择：阶段 & 学期 & 学科 -->
          <div class="form-row">
            <div class="form-group flex-select-col">
              <label class="form-label">学习阶段<span class="required-star">*</span></label>
              <select v-model="selectedStageId" class="custom-select" @change="onStageChange" required>
                <option v-for="stage in activeStages" :key="stage.id" :value="stage.id">
                  {{ stage.name }}
                </option>
              </select>
            </div>

            <div class="form-group flex-select-col">
              <label class="form-label">学期<span class="required-star">*</span></label>
              <select v-model="selectedSemesterId" class="custom-select" @change="onSemesterChange" required>
                <option value="">-- 选择学期 --</option>
                <option v-for="sem in filteredSemesters" :key="sem.id" :value="sem.id">
                  {{ sem.name }}
                </option>
              </select>
            </div>

            <div class="form-group flex-select-col">
              <label class="form-label">学科<span class="required-star">*</span></label>
              <select v-model="selectedSubjectId" class="custom-select" @change="onSubjectChange" required>
                <option value="">-- 选择学科 --</option>
                <option v-for="sub in subjects" :key="sub.subject_id" :value="sub.subject_id">
                  {{ sub.subject?.subject_name || '未知' }}
                </option>
              </select>
            </div>
          </div>

          <!-- 来源类型分段切换 -->
          <div class="form-group" style="margin-bottom: 2px;">
            <label class="form-label">来源类型</label>
            <div class="source-type-segment">
              <button
                type="button"
                class="segment-btn"
                :class="{ active: sourceType === 'BOOK' }"
                @click="setSourceType('BOOK')"
              >
                📖 习题册
              </button>
              <button
                type="button"
                class="segment-btn"
                :class="{ active: sourceType === 'PAPER' }"
                @click="setSourceType('PAPER')"
              >
                📄 试卷/其它
              </button>
            </div>
          </div>

          <!-- A. 习题册来源输入区 -->
          <template v-if="sourceType === 'BOOK'">
            <div class="form-row">
              <div class="form-group flex-1">
                <label class="form-label">关联习题册<span class="required-star">*</span></label>
                <select v-model="selectedQuestionSourceId" class="custom-select" required>
                  <option v-if="availableBooks.length === 0" value="">
                    ⚠️ 该学科暂无可用习题册（请先在「我的习题册」中配置）
                  </option>
                  <option v-else value="">-- 选择已有习题册 --</option>
                  <option v-for="book in availableBooks" :key="book.id" :value="book.id">
                    {{ book.name }}
                  </option>
                </select>
                <div v-if="availableBooks.length === 0 && selectedSubjectId" style="color: #d97706; font-size: 12px; margin-top: 5px;">
                  💡 录题只能选择已建立的习题册。若当前无对应习题册，请先前往左侧「我的习题册」提前录入书架。
                </div>
              </div>
            </div>

            <div class="form-row">
              <div class="form-group flex-1">
                <label class="form-label">章节/单元<span class="required-star">*</span></label>
                <select v-model="selectedUnitCode" class="custom-select" required>
                  <option v-for="unit in systemUnits" :key="unit.id" :value="unit.id">
                    {{ unit.name }}
                  </option>
                </select>
              </div>
              <div class="form-group flex-1">
                <label class="form-label">页码<span class="required-star">*</span></label>
                <input
                  type="text"
                  v-model="pageNumber"
                  class="form-control"
                  placeholder="如：第 45 页"
                  required
                />
              </div>
            </div>
          </template>

          <!-- B. 试卷/其它来源输入区 -->
          <template v-else-if="sourceType === 'PAPER'">
            <div class="form-group">
              <label class="form-label">试卷分类<span class="required-star">*</span></label>
              <select v-model="selectedPaperCategory" class="custom-select" required>
                <option v-for="cat in paperCategories" :key="cat.code" :value="cat.code">
                  {{ cat.name }}
                </option>
              </select>
            </div>

            <div class="form-group">
              <label class="form-label">章节/单元<span class="required-star">*</span></label>
              <select v-model="selectedUnitCode" class="custom-select" required>
                <option v-for="unit in filteredSystemUnits" :key="unit.id" :value="unit.id">
                  {{ unit.name }}
                </option>
              </select>
            </div>

            <div class="form-row">
              <div class="form-group flex-1">
                <label class="form-label">自定义来源/试卷名称<span class="required-star">*</span></label>
                <div class="autocomplete-container">
                  <input
                    type="text"
                    v-model="sourcePaperName"
                    class="form-control"
                    placeholder="如：期中模拟卷A"
                    required
                    @focus="showSuggestions = true"
                    @blur="hideSuggestionsWithDelay"
                  />
                  <div v-if="showSuggestions && paperSuggestions.length > 0" class="autocomplete-dropdown">
                    <div
                      v-for="item in paperSuggestions"
                      :key="item.id"
                      class="autocomplete-item"
                      @mousedown="selectPaperSuggestion(item)"
                    >
                      <span class="suggestion-name">{{ item.name }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <!-- 难度星级评分 & 掌握程度评估 (垂直排列) -->
          <div class="form-group">
            <label class="form-label">难度星级评分</label>
            <div class="star-rating-box" style="margin-top: 4px;" @mouseleave="hoverStars = 0">
              <span
                v-for="star in 5"
                :key="star"
                class="star-item"
                :class="{ filled: hoverStars > 0 ? star <= hoverStars : star <= difficultyStars }"
                @mouseenter="hoverStars = star"
                @click="difficultyStars = star"
              >★</span>
            </div>
          </div>
          
          <div class="form-group">
            <label class="form-label">掌握程度评估</label>
            <div class="mastery-buttons-group">
              <button
                type="button"
                class="mastery-btn-item clueless"
                :class="{ active: masteryLevel === 'CLUELESS' }"
                @click="masteryLevel = 'CLUELESS'"
              >
                🔴 完全不会
              </button>
              <button
                type="button"
                class="mastery-btn-item partial"
                :class="{ active: masteryLevel === 'PARTIAL' }"
                @click="masteryLevel = 'PARTIAL'"
              >
                🟡 一知半解
              </button>
              <button
                type="button"
                class="mastery-btn-item mastered"
                :class="{ active: masteryLevel === 'MASTERED' }"
                @click="masteryLevel = 'MASTERED'"
              >
                🟢 熟练掌握
              </button>
            </div>
          </div>

          <!-- 纳入复习题库 -->
          <div class="form-group-toggle" style="padding: 8px 12px; margin-top: 2px;">
            <div class="toggle-info">
              <span class="toggle-title" style="font-size: 13px;">纳入复习题库</span>
              <span class="toggle-desc" style="font-size: 10.5px;">勾选后自动加入复习推荐范围</span>
            </div>
            <label class="switch" style="width: 42px; height: 22px;">
              <input type="checkbox" v-model="isInReviewPool" />
              <span class="slider round" style="border-radius: 22px;"></span>
            </label>
          </div>

          <div v-if="submitError" class="error-box" style="margin-top: 0;">{{ submitError }}</div>

          <!-- 保存按钮 -->
          <div style="margin-top: 4px;">
            <button type="submit" class="btn btn-primary submit-btn" :disabled="submitting">
              {{ submitting ? '正在压缩并保存...' : '确认录入' }}
            </button>
          </div>
        </div>

      </form>
    </div>
    
    <!-- Image Preview Modal Overlay -->
    <ImageLightbox
      :visible="previewIndex !== null"
      :images="lightboxImages"
      v-model:index="previewIndex"
      :is-edit-mode="true"
      @close="closePreview"
      @rotate-left="rotateLeft"
      @rotate-right="rotateRight"
    />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useNoteCreate } from './useNoteCreate.js';
import ImageLightbox from '../components/ImageLightbox.vue';

const hoverStars = ref(0);

const {
  activeStages,
  allSemesters,
  imageRotations,
  rotateLeft,
  rotateRight,
  rotateImageAt,
  subjects,
  availableBooks,
  selectedStageId,
  selectedSemesterId,
  selectedSubjectId,
  selectedQuestionSourceId,
  sourcePaperName,
  selectedUnitCode,
  selectedPaperCategory,
  pageNumber,
  masteryLevel,
  filteredSystemUnits,
  systemUnits,
  paperCategories,
  difficultyStars,
  isInReviewPool,
  auxiliaryText,
  uploadFiles,
  previewUrls,
  imageTitles,
  submitting,
  submitError,
  sourceType,
  setSourceType,
  filteredSemesters,
  onStageChange,
  onSemesterChange,
  onSubjectChange,
  handleFileChange,
  removeImage,
  formatFileSize,
  previewIndex,
  openPreview,
  closePreview,
  dragIndex,
  dragOverIndex,
  isInputActive,
  handleDragStart,
  handleDragOver,
  handleDragEnter,
  handleDragLeave,
  handleDrop,
  handleDragEnd,
  handleSubmit,
  paperSuggestions,
  showSuggestions,
  selectPaperSuggestion,
  hideSuggestionsWithDelay
} = useNoteCreate();

const lightboxImages = computed(() => {
  return (previewUrls.value || []).map((url, idx) => ({
    isNew: true,
    blobUrl: url,
    rotation: (imageRotations.value && imageRotations.value[idx]) || 0
  }));
});
</script>

<style scoped src="./NoteCreate.css"></style>
