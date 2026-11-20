<template>
        <div class="column-right-sidebar">
          
          <!-- ── A. 预览模式右栏 (快捷修改) ── -->
          <div v-if="!isEditMode" class="preview-right-sidebar">
            <div class="sidebar-section-title">习题分类与属性</div>
            
            <div v-if="currentUser?.role === 'PARENT'" class="attribute-item-row">
              <span class="attr-label">学生账号</span>
              <span class="attr-value">{{ note.student_name || '未知' }}</span>
            </div>

            <div class="attribute-item-row">
              <span class="attr-label">学习阶段</span>
              <span class="attr-value">{{ note.stage_name || '未知' }}</span>
            </div>

            <div class="attribute-item-row">
              <span class="attr-label">学期</span>
              <span class="attr-value">{{ note.semester_name || '未绑定' }}</span>
            </div>

            <div class="attribute-item-row">
              <span class="attr-label">学科</span>
              <span class="attr-value">{{ note.subject_name }}</span>
            </div>

            <!-- 题目来源类型 -->
            <div class="attribute-item-row">
              <span class="attr-label">来源类型</span>
              <span class="attr-value" style="display:inline-flex; align-items:center; gap:6px;">
                <AppIcon :name="note.source_type === 'PAPER' ? 'file' : 'book'" :size="15" />
                <span>{{ note.source_type === 'PAPER' ? '试卷/其它' : '习题册' }}</span>
              </span>
            </div>

            <!-- A. 来源是试卷时的行序列 -->
            <template v-if="note.source_type === 'PAPER'">
              <div class="attribute-item-row">
                <span class="attr-label">试卷分类</span>
                <span class="attr-value">{{ note.paper_category_name || '日常作业/练习' }}</span>
              </div>

              <div class="attribute-item-row">
                <span class="attr-label">章节单元</span>
                <span class="attr-value">{{ note.unit_name || '未设置' }}</span>
              </div>

              <div class="attribute-item-row">
                <span class="attr-label">试卷名称</span>
                <span class="attr-value">{{ note.question_source_name || note.source_paper_name || '自定义来源' }}</span>
              </div>
            </template>

            <!-- B. 来源是习题册时的行序列 -->
            <template v-else>
              <div class="attribute-item-row">
                <span class="attr-label">习题册</span>
                <span class="attr-value">{{ note.question_source_name || '未绑定' }}</span>
              </div>

              <div class="attribute-item-row">
                <span class="attr-label">章节单元</span>
                <span class="attr-value">{{ note.unit_name || '未设置' }}</span>
              </div>

              <div class="attribute-item-row">
                <span class="attr-label">页码</span>
                <span class="attr-value">
                  <span v-if="note.page_number" class="page-badge">P.{{ note.page_number }}</span>
                  <span v-else>未设置</span>
                </span>
              </div>
            </template>

            <!-- Parent-only Read-only Attributes (Aligned as single-row key-values) -->
            <template v-if="currentUser?.role === 'PARENT'">
              <div class="attribute-item-row">
                <span class="attr-label">掌握程度</span>
                <span class="attr-value" style="display:inline-flex; align-items:center; gap:6px;">
                  <AppIcon
                    :name="note.mastery_level === 'CLUELESS' ? 'mastery-clueless' : (note.mastery_level === 'MASTERED' ? 'mastery-mastered' : 'mastery-partial')"
                    :size="16"
                  />
                  <span>{{ masteryName(note.mastery_level) }}</span>
                </span>
              </div>

              <div class="attribute-item-row">
                <span class="attr-label">难度星级</span>
                <span class="attr-value" style="display:inline-flex; align-items:center; gap:2px;">
                  <AppIcon
                    v-for="star in 5"
                    :key="star"
                    :name="star <= Math.round(note.difficulty_stars) ? 'star-filled' : 'star-outline'"
                    :size="16"
                  />
                </span>
              </div>

              <div class="attribute-item-row">
                <span class="attr-label">纳入复习</span>
                <span class="attr-value" style="display:inline-flex; align-items:center; gap:4px;">
                  <AppIcon :name="note.exclude_from_review ? 'close' : 'check'" :size="14" />
                  <span>{{ note.exclude_from_review ? '排除' : '纳入' }}</span>
                </span>
              </div>
            </template>

            <!-- Student-only Interactive Attributes -->
            <template v-else>
              <div class="divider-line"></div>

              <!-- 直接修改：掌握程度 -->
              <div class="interactive-attr-block">
                <label class="form-label font-bold">掌握程度评估（点击即时修改）</label>
                <div class="mastery-buttons-group">
                  <button
                    type="button"
                    class="mastery-btn-item clueless"
                    :class="{ active: note.mastery_level === 'CLUELESS' }"
                    @click="handleDirectMasteryChange('CLUELESS')"
                  >
                    <AppIcon name="mastery-clueless" :size="18" />
                    <span>完全不会</span>
                  </button>
                  <button
                    type="button"
                    class="mastery-btn-item partial"
                    :class="{ active: note.mastery_level === 'PARTIAL' }"
                    @click="handleDirectMasteryChange('PARTIAL')"
                  >
                    <AppIcon name="mastery-partial" :size="18" />
                    <span>一知半解</span>
                  </button>
                  <button
                    type="button"
                    class="mastery-btn-item mastered"
                    :class="{ active: note.mastery_level === 'MASTERED' }"
                    @click="handleDirectMasteryChange('MASTERED')"
                  >
                    <AppIcon name="mastery-mastered" :size="18" />
                    <span>熟练掌握</span>
                  </button>
                </div>
              </div>

              <!-- 直接修改：难度星级 -->
              <div class="interactive-attr-block">
                <label class="form-label font-bold">难度星级评分（点击星星修改）</label>
                <div class="star-rating-box-interactive" @mouseleave="hoverStars = 0">
                  <span
                    v-for="star in 5"
                    :key="star"
                    class="star-item-large"
                    :class="{ filled: hoverStars > 0 ? star <= hoverStars : star <= Math.round(note.difficulty_stars) }"
                    @mouseenter="hoverStars = star"
                    @click="handleDirectStarsChange(star)"
                  >
                    <AppIcon
                      :name="(hoverStars > 0 ? star <= hoverStars : star <= Math.round(note.difficulty_stars)) ? 'star-filled' : 'star-outline'"
                      :size="26"
                    />
                  </span>
                </div>
              </div>

              <!-- 纳入复习推荐范围 (胶囊按钮控件) -->
              <div class="interactive-attr-block">
                <div class="form-group-toggle-quick">
                  <span class="toggle-title-quick">纳入复习推荐范围</span>
                  <label class="switch switch-quick">
                    <input
                      type="checkbox"
                      :checked="!note.exclude_from_review"
                      @change="handleDirectReviewPoolChange($event.target.checked)"
                    />
                    <span class="slider round"></span>
                  </label>
                </div>
              </div>
            </template>
            
            <!-- 往期历史打卡反馈快照 -->
            <div v-if="reviewLog" class="review-feedback-snapshot-box">
              <div class="feedback-snapshot-title">⏱️ 历史复习打卡反馈</div>
              <div class="feedback-rows">
                <div class="feedback-row-item">
                  <span class="fb-label">打卡耗时</span>
                  <span class="fb-value font-mono">{{ reviewLog.duration_seconds.toFixed(0) }} 秒</span>
                </div>
                <div class="feedback-row-item">
                  <span class="fb-label">本次反馈</span>
                  <span 
                    class="outcome-badge"
                    :style="{ background: getOutcomeColor(reviewLog.outcome_code) }"
                  >
                    {{ getOutcomeName(reviewLog.outcome_code) }}
                  </span>
                </div>
                <div class="feedback-row-item" v-if="reviewLog.new_mastery">
                  <span class="fb-label">调整后掌握</span>
                  <span class="fb-value">{{ translateMastery(reviewLog.new_mastery) }}</span>
                </div>
                <div class="feedback-row-item" v-if="reviewLog.new_stars">
                  <span class="fb-label">调整后难度</span>
                  <span class="fb-value">⭐ {{ reviewLog.new_stars }}</span>
                </div>
              </div>
            </div>

            <div class="attribute-footer-info">
              <p>录入时间: {{ formatDate(note.created_at) }}</p>
              <p>最后更新: {{ formatDate(note.updated_at) }}</p>
            </div>
          </div>

          <!-- ── B. 编辑模式右栏 (完全与 NoteCreate 一致) ── -->
          <div v-else class="form-sidebar-panel" style="padding: 16px 20px; gap: 14px;">
            <div class="sidebar-section-title">编辑习题属性</div>

            <div class="form-row">
              <div class="form-group flex-select-col">
                <label class="form-label">学习阶段<span class="required-star">*</span></label>
                <StationerySelect
                  v-model="editStageId"
                  :options="(activeStages || []).map(s => ({ value: s.id, label: s.name }))"
                  @change="onEditStageChange"
                />
              </div>

              <div class="form-group flex-select-col">
                <label class="form-label">学期<span class="required-star">*</span></label>
                <StationerySelect
                  v-model="editSemesterId"
                  :options="[{ value: '', label: '选择学期' }, ...(editFilteredSemesters || []).map(s => ({ value: s.id, label: s.name }))]"
                  @change="onEditSemesterChange"
                />
              </div>

              <div class="form-group flex-select-col">
                <label class="form-label">学科<span class="required-star">*</span></label>
                <StationerySelect
                  v-model="editSubjectId"
                  :options="[{ value: '', label: '选择学科' }, ...(editSubjects || []).map(s => ({ value: s.subject_id, label: s.subject?.subject_name || '未知' }))]"
                  @change="onEditSubjectChange"
                />
              </div>
            </div>

            <!-- 来源类型分段切换 -->
            <div class="form-group" style="margin-bottom: 2px;">
              <label class="form-label">来源类型</label>
              <div class="source-type-segment">
                <button
                  type="button"
                  class="segment-btn"
                  :class="{ active: editSourceType === 'BOOK' }"
                  @click="setEditSourceType('BOOK')"
                >
                  <AppIcon name="book" :size="15" />
                  <span>习题册</span>
                </button>
                <button
                  type="button"
                  class="segment-btn"
                  :class="{ active: editSourceType === 'PAPER' }"
                  @click="setEditSourceType('PAPER')"
                >
                  <AppIcon name="file" :size="15" />
                  <span>试卷/其它</span>
                </button>
              </div>
            </div>

            <!-- A. 习题册联动 -->
            <template v-if="editSourceType === 'BOOK'">
              <div class="form-group">
                <label class="form-label">关联习题册<span class="required-star">*</span></label>
                <StationerySelect
                  v-model="editBookId"
                  :options="[{ value: '', label: '-- 选择已有习题册 --' }, ...(editBooks || []).map(b => ({ value: b.id, label: b.name }))]"
                />
              </div>

              <div class="form-row">
                <div class="form-group flex-1">
                  <label class="form-label">章节/单元<span class="required-star">*</span></label>
                  <StationerySelect
                    v-model="editUnitCode"
                    :options="(systemUnits || []).map(u => ({ value: u.id, label: u.name }))"
                  />
                </div>
                <div class="form-group flex-1">
                  <label class="form-label">页码<span class="required-star">*</span></label>
                  <input type="text" v-model="editPageNumber" class="form-control" placeholder="如：第 45 页" required />
                </div>
              </div>
            </template>

            <!-- B. 试卷分类联动 -->
            <template v-else-if="editSourceType === 'PAPER'">
              <div class="form-group">
                <label class="form-label">试卷分类<span class="required-star">*</span></label>
                <StationerySelect
                  v-model="editPaperCategory"
                  :options="(paperCategories || []).map(c => ({ value: c.code, label: c.name }))"
                />
              </div>

              <div class="form-group">
                <label class="form-label">章节/单元<span class="required-star">*</span></label>
                <StationerySelect
                  v-model="editUnitCode"
                  :options="(filteredEditSystemUnits || []).map(u => ({ value: u.id, label: u.name }))"
                />
              </div>

              <div class="form-group">
                <label class="form-label">自定义来源/试卷名称<span class="required-star">*</span></label>
                <div class="autocomplete-container">
                  <input
                    type="text"
                    v-model="editSourcePaperName"
                    class="form-control"
                    placeholder="如：期中模拟卷A"
                    required
                    @focus="editShowSuggestions = true"
                    @blur="hideEditSuggestionsWithDelay"
                  />
                  <div v-if="editShowSuggestions && editPaperSuggestions.length > 0" class="autocomplete-dropdown">
                    <div
                      v-for="item in editPaperSuggestions"
                      :key="item.id"
                      class="autocomplete-item"
                      @mousedown="selectEditPaperSuggestion(item)"
                    >
                      <span class="suggestion-name">{{ item.name }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </template>
            <!-- 难度星级评分 & 掌握程度评估 (垂直排列) -->
            <div class="form-group">
              <label class="form-label">难度星级评分</label>
              <div class="star-rating-box-interactive" @mouseleave="hoverStars = 0" style="margin-top: 4px;">
                <span
                  v-for="star in 5"
                  :key="star"
                  class="star-item-large"
                  :class="{ filled: hoverStars > 0 ? star <= hoverStars : star <= editDifficultyStars }"
                  @mouseenter="hoverStars = star"
                  @click="editDifficultyStars = star"
                >
                  <AppIcon
                    :name="(hoverStars > 0 ? star <= hoverStars : star <= editDifficultyStars) ? 'star-filled' : 'star-outline'"
                    :size="26"
                  />
                </span>
              </div>
            </div>
            
            <div class="form-group">
              <label class="form-label">掌握程度评估</label>
              <div class="mastery-buttons-group">
                <button
                  type="button"
                  class="mastery-btn-item clueless"
                  :class="{ active: editMasteryLevel === 'CLUELESS' }"
                  @click="editMasteryLevel = 'CLUELESS'"
                >
                  <AppIcon name="mastery-clueless" :size="18" />
                  <span>完全不会</span>
                </button>
                <button
                  type="button"
                  class="mastery-btn-item partial"
                  :class="{ active: editMasteryLevel === 'PARTIAL' }"
                  @click="editMasteryLevel = 'PARTIAL'"
                >
                  <AppIcon name="mastery-partial" :size="18" />
                  <span>一知半解</span>
                </button>
                <button
                  type="button"
                  class="mastery-btn-item mastered"
                  :class="{ active: editMasteryLevel === 'MASTERED' }"
                  @click="editMasteryLevel = 'MASTERED'"
                >
                  <AppIcon name="mastery-mastered" :size="18" />
                  <span>熟练掌握</span>
                </button>
              </div>
            </div>

            <!-- 纳入复习推荐范围开关 -->
            <div class="form-group-toggle" style="padding: 10px 14px; margin-top: 2px;">
              <div class="toggle-info">
                <span class="toggle-title" style="font-size: 13px;">纳入复习推荐范围</span>
              </div>
              <label class="switch">
                <input type="checkbox" v-model="editIsInReviewPool" />
                <span class="slider round"></span>
              </label>
            </div>

            <div v-if="submitError" class="error-box" style="margin-top: 4px;">{{ submitError }}</div>
          </div>
        </div>
</template>

<script setup>
import AppIcon from '../components/AppIcon.vue';
import StationerySelect from '../components/StationerySelect.vue';

const props = defineProps({
  detail: { type: Object, required: true },
  note: { type: Object, required: true },
  reviewLog: { type: Object, default: null },
  outcomes: { type: Array, default: () => [] },
  currentUser: { type: Object, default: null }
});

const {
  isEditMode,
  submitError,
  activeStages,
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
  editSubjects,
  editBooks,
  hoverStars,
  editFilteredSemesters,
  handleDirectMasteryChange,
  handleDirectStarsChange,
  handleDirectReviewPoolChange,
  onEditStageChange,
  onEditSemesterChange,
  onEditSubjectChange,
  setEditSourceType,
  masteryName,
  editPaperSuggestions,
  editShowSuggestions,
  selectEditPaperSuggestion,
  hideEditSuggestionsWithDelay
} = props.detail;

function getOutcomeName(code) {
  const item = (props.outcomes || []).find(o => o.code === code);
  return item ? item.name : code;
}

function getOutcomeColor(code) {
  const item = (props.outcomes || []).find(o => o.code === code);
  return item ? item.color_hex : 'var(--text-muted)';
}

function translateMastery(level) {
  const map = { CLUELESS: '未掌握', PARTIAL: '略微掌握', FAMILIAR: '基本掌握', MASTERED: '熟练掌握' };
  return map[level] || level;
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}
</script>

<style scoped src="./NoteDetailModal.css"></style>
