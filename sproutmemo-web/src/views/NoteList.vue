<template>
  <div class="note-list-container">
    
    <!-- ── 筛选工具栏 (高级悬浮筛选) ── -->
    <div class="filter-toolbar">
      <!-- 孩子选择下拉框 (仅限家长账号) -->
      <div v-if="currentUser?.role === 'PARENT' && currentUser.bound_students && currentUser.bound_students.length > 0" class="quick-child-select-group">
        <select v-model="activeChildId" class="quick-child-select">
          <option v-for="child in currentUser.bound_students" :key="child.id" :value="child.id">
            学生: {{ child.nickname || child.username }}
          </option>
        </select>
      </div>

      <!-- 搜索输入框 -->
      <div class="search-bar-group">
        <span class="search-icon">🔍</span>
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
            <span>⚙️ 筛选条件</span>
            <span v-if="activeFilterCount > 0" class="filter-count-badge">{{ activeFilterCount }}</span>
            <span class="arrow-indicator">{{ isFilterExpanded ? '▲' : '▼' }}</span>
          </button>

          <!-- 绝对定位的悬浮卡片 -->
          <div v-if="isFilterExpanded" class="floating-filter-card" @click.stop>
            <div class="floating-filter-grid">
              
              <!-- 学习阶段 -->
              <div class="filter-group">
                <span class="filter-label">学习阶段</span>
                <select v-model="tempStageId" class="custom-select" @change="onTempStageChange">
                  <option value="">全部阶段</option>
                  <option v-for="stage in activeStages" :key="stage.id" :value="stage.id">
                    {{ stage.name }}
                  </option>
                </select>
              </div>

              <!-- 学期 -->
              <div class="filter-group">
                <span class="filter-label">学期</span>
                <select v-model="tempSemesterId" class="custom-select" @change="onTempSemesterChange">
                  <option value="">全部学期</option>
                  <option v-for="sem in filteredSemesters" :key="sem.id" :value="sem.id">
                    {{ sem.name }}
                  </option>
                </select>
              </div>

              <!-- 学科 -->
              <div class="filter-group">
                <span class="filter-label">学科</span>
                <select v-model="tempSubjectId" class="custom-select" @change="onTempSubjectChange">
                  <option value="">全部学科</option>
                  <option v-for="sub in subjects" :key="sub.subject_id" :value="sub.subject_id">
                    {{ sub.subject?.subject_name || '未知' }}
                  </option>
                </select>
              </div>

              <!-- 章节/单元 -->
              <div class="filter-group">
                <span class="filter-label">章节/单元</span>
                <select v-model="tempUnitCode" class="custom-select">
                  <option value="">全部单元</option>
                  <option v-for="unit in systemUnits" :key="unit.id" :value="unit.id">
                    {{ unit.name }}
                  </option>
                </select>
              </div>

              <!-- 难度等级 -->
              <div class="filter-group">
                <span class="filter-label">难度星级</span>
                <select v-model="tempDifficulty" class="custom-select">
                  <option value="">全部难度</option>
                  <option :value="1">★☆☆☆☆ (1星)</option>
                  <option :value="2">★★☆☆☆ (2星)</option>
                  <option :value="3">★★★☆☆ (3星)</option>
                  <option :value="4">★★★★☆ (4星)</option>
                  <option :value="5">★★★★★ (5星)</option>
                </select>
              </div>

              <!-- 掌握程度 -->
              <div class="filter-group">
                <span class="filter-label">掌握程度</span>
                <select v-model="tempMastery" class="custom-select">
                  <option value="">全部掌握度</option>
                  <option value="CLUELESS">🔴 完全不会</option>
                  <option value="PARTIAL">🟡 一知半解</option>
                  <option value="MASTERED">🟢 熟练掌握</option>
                </select>
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
                    📖 习题册
                  </button>
                  <button
                    type="button"
                    class="segment-filter-btn"
                    :class="{ active: tempSourceType === 'PAPER' }"
                    @click="tempSourceType = 'PAPER'; tempBookId = '';"
                  >
                    📄 试卷/其它
                  </button>
                </div>
              </div>

              <!-- 关联细项 (根据来源类型展示) -->
              <div v-if="tempSourceType === 'BOOK'" class="filter-group span-2">
                <span class="filter-label">关联习题册</span>
                <select v-model="tempBookId" class="custom-select">
                  <option value="">全部习题册</option>
                  <option v-for="book in availableBooks" :key="book.id" :value="book.id">
                    {{ book.name }}
                  </option>
                </select>
              </div>

              <div v-if="tempSourceType === 'PAPER'" class="filter-group span-2">
                <span class="filter-label">试卷分类</span>
                <select v-model="tempPaperCategory" class="custom-select">
                  <option value="">全部试卷分类</option>
                  <option v-for="cat in paperCategories" :key="cat.code" :value="cat.code">
                    {{ cat.name }}
                  </option>
                </select>
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
      <div class="empty-icon">📂</div>
      <h3>没有找到符合筛选条件的错题笔记</h3>
      <p>请尝试调整筛选项，或点击侧栏“录入习题”开始录入。</p>
    </div>

    <div v-else style="display: flex; flex-direction: column; gap: 20px;">
      <div class="notes-grid">
        <div
          v-for="note in notes"
          :key="note.id"
          class="note-card"
          @click="openDetail(note)"
        >
          <!-- 标签行 (学期 + 学科) -->
          <div class="card-header-tags">
            <div style="display: flex; gap: 8px; align-items: center;">
              <span class="subject-badge">{{ note.subject_name }}</span>
              <span class="semester-badge-tag">{{ note.semester_name }}</span>
            </div>
            <span class="mastery-badge-dot">
              <span class="dot-indicator">{{ masteryName(note.mastery_level) }}</span>
            </span>
          </div>

          <!-- 来源与页码 -->
          <div class="note-source-info">
            <span>{{ note.source_paper_name ? '📄' : '📖' }} {{ note.question_source_name || note.source_paper_name || '自定义来源' }}</span>
            <span v-if="note.unit_name" class="page-indicator-badge">{{ note.unit_name }}</span>
            <span v-if="note.page_number" class="page-indicator-badge">P.{{ note.page_number }}</span>
          </div>

          <!-- 难度与星级 -->
          <div class="difficulty-rating">
            <span v-for="star in 5" :key="star" class="star" :class="{ active: star <= Math.round(note.difficulty_stars) }">★</span>
          </div>

          <!-- 题目正文摘要 -->
          <p class="note-summary" :class="{ empty: !note.auxiliary_text }">
            {{ note.auxiliary_text || '(暂无文字描述)' }}
          </p>

          <!-- 缩略图栏 -->
          <div class="thumbnail-bar" v-if="note.attachments && note.attachments.filter(a => a.file_type === 'IMAGE').length > 0">
            <div
              v-for="attach in note.attachments.filter(a => a.file_type === 'IMAGE').slice(0, 4)"
              :key="attach.id"
              class="thumb-wrapper"
            >
              <img :src="getAbsoluteUrl(attach.storage_path)" alt="thumb" />
            </div>
            <span class="more-badge" v-if="note.attachments.filter(a => a.file_type === 'IMAGE').length > 4">+{{ note.attachments.filter(a => a.file_type === 'IMAGE').length - 4 }}</span>
          </div>

          <!-- 卡片底部时间与删除 -->
          <div class="card-footer">
            <span class="date">{{ formatDate(note.created_at) }}</span>
            <div class="card-media-counts" v-if="note.attachments && note.attachments.length > 0">
              <span v-if="note.attachments.filter(a => a.file_type === 'IMAGE').length > 0" class="media-count-item" title="图片">
                🖼️ {{ note.attachments.filter(a => a.file_type === 'IMAGE').length }}
              </span>
              <span v-if="note.attachments.filter(a => a.file_type === 'AUDIO').length > 0" class="media-count-item" title="音频">
                🎵 {{ note.attachments.filter(a => a.file_type === 'AUDIO').length }}
              </span>
              <span v-if="note.attachments.filter(a => a.file_type === 'VIDEO').length > 0" class="media-count-item" title="视频">
                📹 {{ note.attachments.filter(a => a.file_type === 'VIDEO').length }}
              </span>
            </div>
            <button v-if="currentUser?.role !== 'PARENT'" class="delete-btn" @click.stop="handleDelete(note)" title="删除此题">🗑️ 删除</button>
          </div>
        </div>
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
            <select v-model="pageSize" class="custom-select select-size" @change="handlePageSizeChange">
              <option :value="10">10 条</option>
              <option :value="20">20 条</option>
              <option :value="30">30 条</option>
              <option :value="40">40 条</option>
              <option :value="50">50 条</option>
            </select>
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
import { ref, onMounted, computed, inject, watch, onUnmounted } from 'vue';
import { api } from '../services/api.js';
import { showConfirm, showAlert } from '../services/dialog.js';
import NoteDetailModal from './NoteDetailModal.vue';

const currentUser = inject('currentUser');
const activeChildId = inject('activeChildId');

const currentStudentId = computed(() => {
  if (currentUser.value?.role === 'PARENT') {
    return activeChildId.value || '';
  }
  return currentUser.value?.id || '';
});

// ── 悬浮筛选层显示状态 ──
const isFilterExpanded = ref(false);

// ── 筛选下拉列表数据缓存 ──
const activeStages = ref([]);
const allSemesters = ref([]);
const subjects = ref([]);
const availableBooks = ref([]);
const systemUnits = ref([]);
const paperCategories = ref([]);

// ── 筛选值绑定 ──
const selectedStageId = ref('');
const selectedSemesterId = ref('');
const selectedSubjectId = ref('');
const selectedMastery = ref('');
const sourceType = ref('ALL'); // ALL, BOOK, PAPER
const selectedBookId = ref('');
const selectedPaperCategory = ref('');
const selectedUnitCode = ref('');
const selectedDifficulty = ref('');
const searchQuery = ref('');

// ── 暂存筛选值绑定 ──
const tempStageId = ref('');
const tempSemesterId = ref('');
const tempSubjectId = ref('');
const tempUnitCode = ref('');
const tempMastery = ref('');
const tempDifficulty = ref('');
const tempSourceType = ref('ALL');
const tempBookId = ref('');
const tempPaperCategory = ref('');

// ── 分页状态 ──
const currentPage = ref(1);
const pageSize = ref(10); // 默认一页10个
const totalItems = ref(0);

// ── 列表数据与加载状态 ──
const notes = ref([]);
const loading = ref(true);

// ── 详情/编辑模态框状态 ──
const detailModalVisible = ref(false);
const activeNote = ref(null);
const detailKey = ref(0); // 刷新子组件用

// ── 联动逻辑 ──
const filteredSemesters = computed(() => {
  if (!tempStageId.value) return [];
  return allSemesters.value.filter(sem => sem.stage_id === tempStageId.value);
});

// 统计激活的高级筛选条件数量
const activeFilterCount = computed(() => {
  let count = 0;
  if (selectedStageId.value) count++;
  if (selectedSemesterId.value) count++;
  if (selectedSubjectId.value) count++;
  if (selectedMastery.value) count++;
  if (selectedUnitCode.value) count++;
  if (selectedDifficulty.value) count++;
  if (sourceType.value !== 'ALL') count++;
  if (selectedBookId.value || selectedPaperCategory.value) count++;
  return count;
});

const hasActiveFilters = computed(() => activeFilterCount.value > 0);

// 计算总页数
const totalPages = computed(() => Math.ceil(totalItems.value / pageSize.value) || 1);

// 跳转输入页数
const jumpPageInput = ref('');

async function loadFilters() {
  try {
    const stageRes = await api.get('/stages?only_active=true');
    if (stageRes.ok) activeStages.value = (await stageRes.json()).data.items || [];

    const semRes = await api.get('/semesters?only_active=true');
    if (semRes.ok) allSemesters.value = (await semRes.json()).data.items || [];

    const unitsRes = await api.get('/system/units');
    if (unitsRes.ok) systemUnits.value = (await unitsRes.json()).data.items || [];

    const catsRes = await api.get('/system/paper-categories');
    if (catsRes.ok) paperCategories.value = (await catsRes.json()).data.items || [];
  } catch (err) {
    console.error(err);
  }
}

async function fetchSubjectsForTemp() {
  subjects.value = [];
  tempSubjectId.value = '';
  availableBooks.value = [];
  tempBookId.value = '';

  if (!tempSemesterId.value) {
    try {
      const res = await api.get('/subjects?only_active=true');
      if (res.ok) {
        const items = (await res.json()).data.items || [];
        subjects.value = items.map(sub => ({
          subject_id: sub.id,
          subject: {
            subject_name: sub.subject_name
          }
        }));
      }
    } catch (err) {
      console.error(err);
    }
    return;
  }
  try {
    const res = await api.get(`/semester_settings/subjects?semester_id=${tempSemesterId.value}&only_active=true`);
    if (res.ok) {
      subjects.value = (await res.json()).data.items || [];
    }
  } catch (err) {
    console.error(err);
  }
}

async function fetchBooksForTemp() {
  availableBooks.value = [];
  tempBookId.value = '';

  if (!tempSemesterId.value || !tempSubjectId.value || !currentStudentId.value) return;
  try {
    const res = await api.get(`/source_books?student_id=${currentStudentId.value}&semester_id=${tempSemesterId.value}&subject_id=${tempSubjectId.value}&only_active=true&include_deleted=false`);
    if (res.ok) {
      availableBooks.value = (await res.json()).data.items || [];
    }
  } catch (err) {
    console.error(err);
  }
}

function onTempStageChange() {
  tempSemesterId.value = '';
  tempSubjectId.value = '';
  tempBookId.value = '';
  subjects.value = [];
  availableBooks.value = [];
}

async function onTempSemesterChange() {
  tempSubjectId.value = '';
  tempBookId.value = '';
  availableBooks.value = [];
  subjects.value = [];
  await fetchSubjectsForTemp();
}

async function onTempSubjectChange() {
  tempBookId.value = '';
  availableBooks.value = [];
  await fetchBooksForTemp();
}

// ── 核心列表获取（后端分页 + 联动过滤） ──
async function fetchNotes() {
  if (currentUser.value?.role === 'PARENT' && !currentStudentId.value) {
    notes.value = [];
    totalItems.value = 0;
    loading.value = false;
    return;
  }
  loading.value = true;
  try {
    let url = `/notes?student_id=${currentStudentId.value}&include_deleted=false`;
    url += `&page=${currentPage.value}&size=${pageSize.value}`;

    if (selectedSemesterId.value) url += `&semester_id=${selectedSemesterId.value}`;
    if (selectedSubjectId.value) url += `&subject_id=${selectedSubjectId.value}`;
    if (selectedMastery.value) url += `&mastery_level=${selectedMastery.value}`;
    if (selectedUnitCode.value) url += `&unit_code=${selectedUnitCode.value}`;
    if (selectedDifficulty.value) url += `&difficulty_stars=${selectedDifficulty.value}`;
    
    if (sourceType.value === 'BOOK') {
      url += `&source_type=BOOK`;
      if (selectedBookId.value) url += `&question_source_id=${selectedBookId.value}`;
    } else if (sourceType.value === 'PAPER') {
      url += `&source_type=PAPER`;
      if (selectedPaperCategory.value) url += `&paper_category=${selectedPaperCategory.value}`;
    }

    const res = await api.get(url);
    if (res.ok) {
      const responseData = await res.json();
      const rawItems = responseData.data.items || [];
      totalItems.value = responseData.data.total || 0;

      const query = searchQuery.value.trim();
      if (query) {
        notes.value = rawItems.filter(item => 
          (item.auxiliary_text && item.auxiliary_text.includes(query)) ||
          (item.source_paper_name && item.source_paper_name.includes(query)) ||
          (item.unit_name && item.unit_name.includes(query))
        );
      } else {
        notes.value = rawItems;
      }
    }
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
}

// ── 悬浮筛选控制 ──
function toggleFilterCard() {
  if (!isFilterExpanded.value) {
    // Copy active filters to temp variables
    tempStageId.value = selectedStageId.value;
    tempSemesterId.value = selectedSemesterId.value;
    tempSubjectId.value = selectedSubjectId.value;
    tempUnitCode.value = selectedUnitCode.value;
    tempMastery.value = selectedMastery.value;
    tempDifficulty.value = selectedDifficulty.value;
    tempSourceType.value = sourceType.value;
    tempBookId.value = selectedBookId.value;
    tempPaperCategory.value = selectedPaperCategory.value;
    isFilterExpanded.value = true;
  } else {
    isFilterExpanded.value = false;
  }
}

function resetFilters() {
  tempStageId.value = '';
  tempSemesterId.value = '';
  tempSubjectId.value = '';
  tempUnitCode.value = '';
  tempMastery.value = '';
  tempDifficulty.value = '';
  tempSourceType.value = 'ALL';
  tempBookId.value = '';
  tempPaperCategory.value = '';
  
  selectedStageId.value = '';
  selectedSemesterId.value = '';
  selectedSubjectId.value = '';
  selectedUnitCode.value = '';
  selectedMastery.value = '';
  selectedDifficulty.value = '';
  sourceType.value = 'ALL';
  selectedBookId.value = '';
  selectedPaperCategory.value = '';
  searchQuery.value = '';
  
  subjects.value = [];
  availableBooks.value = [];
  
  currentPage.value = 1;
  fetchNotes();
}

function applyFilters() {
  selectedStageId.value = tempStageId.value;
  selectedSemesterId.value = tempSemesterId.value;
  selectedSubjectId.value = tempSubjectId.value;
  selectedUnitCode.value = tempUnitCode.value;
  selectedMastery.value = tempMastery.value;
  selectedDifficulty.value = tempDifficulty.value;
  sourceType.value = tempSourceType.value;
  selectedBookId.value = tempBookId.value;
  selectedPaperCategory.value = tempPaperCategory.value;

  isFilterExpanded.value = false;
  currentPage.value = 1;
  fetchNotes();
}

function closeFilterPopover() {
  isFilterExpanded.value = false;
}



// 点击空白处关闭悬浮窗
function handleOutsideClick(e) {
  const wrapper = document.querySelector('.popover-filter-wrapper');
  if (wrapper && !wrapper.contains(e.target)) {
    closeFilterPopover();
  }
}

watch(currentStudentId, async (newVal, oldVal) => {
  if (newVal !== oldVal) {
    await resetFilters();
  }
});

watch(isFilterExpanded, (newVal) => {
  if (newVal) {
    document.addEventListener('click', handleOutsideClick);
  } else {
    document.removeEventListener('click', handleOutsideClick);
  }
});

// ── 分页动作 ──
function prevPage() {
  if (currentPage.value > 1) {
    currentPage.value--;
    fetchNotes();
  }
}

function nextPage() {
  if (currentPage.value < totalPages.value) {
    currentPage.value++;
    fetchNotes();
  }
}

function selectPage(page) {
  currentPage.value = page;
  fetchNotes();
}

function handlePageSizeChange() {
  currentPage.value = 1;
  fetchNotes();
}

function handleJumpPage() {
  const p = parseInt(jumpPageInput.value);
  if (p >= 1 && p <= totalPages.value) {
    currentPage.value = p;
    fetchNotes();
    jumpPageInput.value = '';
  } else {
    showAlert(`请输入 1 到 ${totalPages.value} 之间的有效页码`);
  }
}

// ── 错题卡片删除 ──
async function handleDelete(note) {
  const ok = await showConfirm(`确定要删除此习题笔记吗？`);
  if (!ok) return;
  try {
    const res = await api.delete(`/notes/${note.id}`);
    if (res.ok) {
      await fetchNotes();
    } else {
      await showAlert('删除失败');
    }
  } catch (err) {
    console.error(err);
  }
}

// ── 错题大图模态框展开 ──
function openDetail(note) {
  activeNote.value = note;
  detailKey.value += 1;
  detailModalVisible.value = true;
}

function closeDetail() {
  detailModalVisible.value = false;
}

async function refreshDetailAndList() {
  if (activeNote.value) {
    try {
      const res = await api.get(`/notes/${activeNote.value.id}`);
      if (res.ok) {
        activeNote.value = (await res.json()).data;
      }
    } catch (err) {
      console.error(err);
    }
  }
  await fetchNotes();
}

function masteryName(level) {
  const map = { CLUELESS: '🔴 完全不会', PARTIAL: '🟡 一知半解', MASTERED: '🟢 熟练掌握' };
  return map[level] || level;
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

onMounted(async () => {
  await loadFilters();
  await fetchSubjectsForTemp();
  await fetchNotes();
});

onUnmounted(() => {
  document.removeEventListener('click', handleOutsideClick);
});
</script>

<style scoped src="./NoteList.css"></style>
