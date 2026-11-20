import { ref, onMounted, computed, inject, watch, onUnmounted } from 'vue';
import { api } from '../services/api.js';
import { showConfirm, showAlert } from '../services/dialog.js';

export function useNoteList() {
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
  const sourceType = ref('ALL');
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
  const pageSize = ref(10);
  const totalItems = ref(0);
  const jumpPageInput = ref('');

  // ── 列表数据与加载状态 ──
  const notes = ref([]);
  const loading = ref(true);

  // ── 详情/编辑模态框状态 ──
  const detailModalVisible = ref(false);
  const activeNote = ref(null);
  const detailKey = ref(0);

  const filteredSemesters = computed(() => {
    if (!tempStageId.value) return [];
    return allSemesters.value.filter(sem => sem.stage_id === tempStageId.value);
  });

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
  const totalPages = computed(() => Math.ceil(totalItems.value / pageSize.value) || 1);

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
            subject: { subject_name: sub.subject_name }
          }));
        }
      } catch (err) {
        console.error(err);
      }
      return;
    }
    try {
      const res = await api.get(`/semester_settings/subjects?semester_id=${tempSemesterId.value}&only_active=true`);
      if (res.ok) subjects.value = (await res.json()).data.items || [];
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
      if (res.ok) availableBooks.value = (await res.json()).data.items || [];
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

  function toggleFilterCard() {
    if (!isFilterExpanded.value) {
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
        if (res.ok) activeNote.value = (await res.json()).data;
      } catch (err) {
        console.error(err);
      }
    }
    await fetchNotes();
  }

  onMounted(async () => {
    await loadFilters();
    await fetchSubjectsForTemp();
    await fetchNotes();
  });

  onUnmounted(() => {
    document.removeEventListener('click', handleOutsideClick);
  });

  return {
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
  };
}

