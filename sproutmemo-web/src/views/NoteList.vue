<template>
  <div class="note-list-container">
    
    <!-- ── 筛选工具栏 (高级悬浮筛选) ── -->
    <div class="filter-toolbar">
      <!-- 孩子选择下拉框 (仅限家长账号) -->
      <div v-if="currentUser?.role === 'PARENT' && currentUser.bound_students && currentUser.bound_students.length > 0" class="quick-child-select-group">
        <StationerySelect
          v-model="activeChildId"
          :options="currentUser.bound_students.map(c => ({ value: c.id, label: '学生: ' + (c.nickname || c.username) }))"
          compact
        />
      </div>

      <!-- 搜索输入框 -->
      <div class="search-bar-group">
        <span class="search-icon"><AppIcon name="search" :size="15" /></span>
        <input
          type="text"
          v-model="searchQuery"
          class="search-input"
          placeholder="搜索笔记正文、来源或单元，按回车键过滤..."
          @keyup.enter="currentPage = 1; fetchNotes();"
        />
      </div>

      <!-- 右侧动作按钮 -->
      <div class="toolbar-actions">

        <!-- 悬浮高级筛选开关 -->
        <div class="popover-filter-wrapper">
          <button
            class="filter-trigger-btn"
            :class="{ active: hasActiveFilters || isFilterExpanded }"
            @click.stop="toggleFilterCard"
          >
            <AppIcon name="settings" :size="15" />
            <span>筛选条件</span>
            <span v-if="activeFilterCount > 0" class="filter-count-badge">{{ activeFilterCount }}</span>
            <AppIcon :name="isFilterExpanded ? 'chevron-down' : 'chevron-down'" :size="13" />
          </button>

          <!-- 绝对定位的悬浮卡片 -->
          <div v-if="isFilterExpanded" class="floating-filter-card" @click.stop>
            <div class="floating-filter-grid">
              
              <!-- 学习阶段 -->
              <div class="filter-group">
                <span class="filter-label">学习阶段</span>
                <StationerySelect
                  v-model="tempStageId"
                  :options="[{ value: '', label: '全部阶段' }, ...(activeStages || []).map(s => ({ value: s.id, label: s.name }))]"
                  compact
                  @change="onTempStageChange"
                />
              </div>

              <!-- 学期 -->
              <div class="filter-group">
                <span class="filter-label">学期</span>
                <StationerySelect
                  v-model="tempSemesterId"
                  :options="[{ value: '', label: '全部学期' }, ...(filteredSemesters || []).map(s => ({ value: s.id, label: s.name }))]"
                  compact
                  @change="onTempSemesterChange"
                />
              </div>

              <!-- 学科 -->
              <div class="filter-group">
                <span class="filter-label">学科</span>
                <StationerySelect
                  v-model="tempSubjectId"
                  :options="[{ value: '', label: '全部学科' }, ...(subjects || []).map(s => ({ value: s.subject_id, label: s.subject?.subject_name || '未知' }))]"
                  compact
                  @change="onTempSubjectChange"
                />
              </div>

              <!-- 章节/单元 -->
              <div class="filter-group">
                <span class="filter-label">章节/单元</span>
                <StationerySelect
                  v-model="tempUnitCode"
                  :options="[{ value: '', label: '全部单元' }, ...(systemUnits || []).map(u => ({ value: u.id, label: u.name }))]"
                  compact
                />
              </div>

              <!-- 难度等级 -->
              <div class="filter-group">
                <span class="filter-label">难度星级</span>
                <StationerySelect
                  v-model="tempDifficulty"
                  :options="[
                    { value: '', label: '全部难度' },
                    { value: 1, label: '1 星难度' },
                    { value: 2, label: '2 星难度' },
                    { value: 3, label: '3 星难度' },
                    { value: 4, label: '4 星难度' },
                    { value: 5, label: '5 星难度' }
                  ]"
                  compact
                />
              </div>

              <!-- 掌握程度 -->
              <div class="filter-group">
                <span class="filter-label">掌握程度</span>
                <StationerySelect
                  v-model="tempMastery"
                  :options="[
                    { value: '', label: '全部掌握度' },
                    { value: 'CLUELESS', label: '完全不会' },
                    { value: 'PARTIAL', label: '一知半解' },
                    { value: 'MASTERED', label: '熟练掌握' }
                  ]"
                  compact
                />
              </div>

              <!-- 来源类型 -->
              <div class="filter-group span-2">
                <span class="filter-label">来源类型</span>
                <div class="source-type-filter-segment">
                  <button
                    type="button"
                    class="segment-filter-btn"
                    :class="{ active: tempSourceType === 'ALL' }"
                    @click="tempSourceType = 'ALL'; tempBookId = ''; tempPaperCategory = '';"
                  >
                    全部
                  </button>
                  <button
                    type="button"
                    class="segment-filter-btn"
                    :class="{ active: tempSourceType === 'BOOK' }"
                    @click="tempSourceType = 'BOOK'; tempPaperCategory = '';"
                  >
                    <AppIcon name="book" :size="14" /> 习题册
                  </button>
                  <button
                    type="button"
                    class="segment-filter-btn"
                    :class="{ active: tempSourceType === 'PAPER' }"
                    @click="tempSourceType = 'PAPER'; tempBookId = '';"
                  >
                    <AppIcon name="file" :size="14" /> 试卷/其它
                  </button>
                </div>
              </div>

              <!-- 关联细项 (根据来源类型展示) -->
              <div v-if="tempSourceType === 'BOOK'" class="filter-group span-2">
                <span class="filter-label">关联习题册</span>
                <StationerySelect
                  v-model="tempBookId"
                  :options="[{ value: '', label: '全部习题册' }, ...(availableBooks || []).map(b => ({ value: b.id, label: b.name }))]"
                  compact
                />
              </div>

              <div v-if="tempSourceType === 'PAPER'" class="filter-group span-2">
                <span class="filter-label">试卷分类</span>
                <StationerySelect
                  v-model="tempPaperCategory"
                  :options="[{ value: '', label: '全部试卷分类' }, ...(paperCategories || []).map(c => ({ value: c.code, label: c.name }))]"
                  compact
                />
              </div>

            </div>

            <!-- 底部控制按钮 -->
            <div class="floating-filter-footer">
              <button class="footer-btn btn-reset" @click="resetFilters">重置条件</button>
              <button class="footer-btn btn-confirm" @click="applyFilters">确定</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ── 笔记网格 (双列平分铺满) ── -->
    <div v-if="loading" class="loading-state">
      <span class="spinner"></span> 正在载入错题集...
    </div>

    <div v-else-if="notes.length === 0" class="empty-state">
      <div class="empty-icon"><AppIcon name="folder" :size="36" /></div>
      <h3>没有找到符合筛选条件的错题笔记</h3>
      <p>请尝试调整筛选项，或点击侧栏“录入习题”开始录入。</p>
    </div>

    <div v-else style="display: flex; flex-direction: column; gap: 20px;">
      <div class="notes-grid">
        <NoteListCard
          v-for="note in notes"
          :key="note.id"
          :note="note"
          :can-delete="currentUser?.role !== 'PARENT'"
          @open="openDetail"
          @delete="handleDelete"
        />
      </div>

      <!-- ── 后端分页器 ── -->
      <div class="pagination-container">
        <div class="pagination-left-info">
          共 {{ totalItems }} 条记录 | 页次 {{ currentPage }}/{{ totalPages }}
        </div>
        <div class="pagination-right-controls">
          <!-- 每页容量设置 -->
          <div class="page-size-selector">
            <span>每页</span>
            <StationerySelect
              v-model="pageSize"
              :options="[10, 20, 30, 40, 50].map(n => ({ value: n, label: n + ' 条' }))"
              compact
              style="width: 96px;"
              @change="handlePageSizeChange"
            />
          </div>

          <!-- 页码按钮 -->
          <div class="page-numbers-bar">
            <button class="page-btn" :disabled="currentPage === 1" @click="prevPage" title="上一页">‹</button>
            <button
              v-for="page in totalPages"
              :key="page"
              class="page-btn"
              :class="{ active: currentPage === page }"
              @click="selectPage(page)"
            >
              {{ page }}
            </button>
            <button class="page-btn" :disabled="currentPage === totalPages" @click="nextPage" title="下一页">›</button>
          </div>

          <!-- 直接跳转 -->
          <div class="jump-to-page-box">
            <span>前往</span>
            <input
              type="text"
              v-model="jumpPageInput"
              class="form-control-input jump-input"
              @keyup.enter="handleJumpPage"
            />
            <span>页</span>
          </div>
        </div>
      </div>
    </div>

    <!-- ── 详情与编辑一体化弹出框 ── -->
    <NoteDetailModal
      :key="detailKey"
      :visible="detailModalVisible"
      :note="activeNote"
      @close="closeDetail"
      @refresh="refreshDetailAndList"
    />

  </div>
</template>

<script setup>
import AppIcon from '../components/AppIcon.vue';
import StationerySelect from '../components/StationerySelect.vue';
import NoteDetailModal from './NoteDetailModal.vue';
import NoteListCard from './NoteListCard.vue';
import { useNoteList } from './useNoteList.js';

const {
  currentUser,
  isFilterExpanded,
  activeStages,
  subjects,
  availableBooks,
  systemUnits,
  paperCategories,
  searchQuery,
  tempStageId,
  tempSemesterId,
  tempSubjectId,
  tempUnitCode,
  tempMastery,
  tempDifficulty,
  tempSourceType,
  tempBookId,
  tempPaperCategory,
  currentPage,
  pageSize,
  totalItems,
  jumpPageInput,
  notes,
  loading,
  detailModalVisible,
  activeNote,
  detailKey,
  filteredSemesters,
  activeFilterCount,
  hasActiveFilters,
  totalPages,
  onTempStageChange,
  onTempSemesterChange,
  onTempSubjectChange,
  fetchNotes,
  toggleFilterCard,
  resetFilters,
  applyFilters,
  prevPage,
  nextPage,
  selectPage,
  handlePageSizeChange,
  handleJumpPage,
  handleDelete,
  openDetail,
  closeDetail,
  refreshDetailAndList
} = useNoteList();
</script>

<style scoped src="./NoteList.css"></style>
