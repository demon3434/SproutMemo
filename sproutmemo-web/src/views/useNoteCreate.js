import { ref, onMounted, computed, inject, watch } from 'vue';
import { api } from '../services/api.js';
import { calculateCurrentSemester } from '../utils/semester.js';
import { showAlert } from '../services/dialog.js';
import { rotateImageFile } from '../utils/imageCompressor.js';
import { useNoteImages } from '../composables/useNoteImages.js';

export function useNoteCreate() {
  const currentUser = inject('currentUser');

  // 下拉及联动状态
  const activeStages = ref([]);
  const allSemesters = ref([]);
  const subjects = ref([]);
  const availableBooks = ref([]);

  const selectedStageId = ref('');
  const selectedSemesterId = ref('');
  const selectedSubjectId = ref('');
  const selectedQuestionSourceId = ref('');

  // 录入数据字段
  const sourcePaperName = ref('');
  const selectedUnitCode = ref(999);
  const selectedPaperCategory = ref('DAILY_HW');
  const pageNumber = ref('');
  const masteryLevel = ref('CLUELESS'); // 默认完全不会

  // 元数据缓存
  const systemUnits = ref([]);
  const paperCategories = ref([]);
  const difficultyStars = ref(3); // 默认 3 星
  const isInReviewPool = ref(true); // 默认纳入复习
  const auxiliaryText = ref('');

  // 试卷来源联想建议
  const paperSuggestions = ref([]);
  const showSuggestions = ref(false);
  let fetchSuggestionsTimer = null;

  function selectPaperSuggestion(item) {
    sourcePaperName.value = item.name;
    showSuggestions.value = false;
  }

  function hideSuggestionsWithDelay() {
    setTimeout(() => {
      showSuggestions.value = false;
    }, 200);
  }

  async function fetchPaperSuggestions() {
    paperSuggestions.value = [];
    if (!selectedSemesterId.value || !selectedSubjectId.value || !selectedPaperCategory.value) return;

    if (fetchSuggestionsTimer) {
      clearTimeout(fetchSuggestionsTimer);
    }

    fetchSuggestionsTimer = setTimeout(async () => {
      try {
        const studentId = currentUser.value.id;
        const semId = selectedSemesterId.value;
        const subId = selectedSubjectId.value;
        const catCode = selectedPaperCategory.value;
        const unitCode = selectedUnitCode.value;
        const queryVal = sourcePaperName.value.trim();

        let url = `/source_books?student_id=${studentId}&semester_id=${semId}&subject_id=${subId}&source_type=PAPER&category_code=${catCode}&only_active=true&include_deleted=false`;
        if (unitCode) {
          url += `&unit_code=${unitCode}`;
        }
        if (queryVal) {
          url += `&name_query=${encodeURIComponent(queryVal)}`;
        }

        const res = await api.get(url);
        if (res.ok) {
          paperSuggestions.value = (await res.json()).data.items || [];
        }
      } catch (err) {
        console.error(err);
      }
    }, 300);
  }

  // 引入图片与预览管理
  const {
    uploadFiles,
    previewUrls,
    imageTitles,
    imageRotations,
    handleFileChange,
    removeImage,
    dragIndex,
    dragOverIndex,
    isInputActive,
    handleDragStart,
    handleDragOver,
    handleDragEnter,
    handleDragLeave,
    handleDrop,
    handleDragEnd,
    formatFileSize,
    previewIndex,
    zoomScale,
    rotateLeft,
    rotateRight,
    rotateImageAt,
    openPreview,
    closePreview,
    zoomIn,
    zoomOut,
    resetZoom,
    handleZoomWheel,
    prevPreview,
    nextPreview,
    resetImages
  } = useNoteImages();

  const submitting = ref(false);
  const submitError = ref('');

  const sourceType = ref('BOOK'); // 'BOOK' or 'PAPER'

  function setSourceType(type) {
    sourceType.value = type;
    if (type === 'BOOK') {
      sourcePaperName.value = '';
    } else if (type === 'PAPER') {
      selectedQuestionSourceId.value = '';
      selectedUnitCode.value = 999;
      pageNumber.value = '';
    }
  }

  watch(
    [selectedSemesterId, selectedSubjectId, selectedPaperCategory, selectedUnitCode, sourcePaperName, sourceType],
    () => {
      if (sourceType.value === 'PAPER') {
        fetchPaperSuggestions();
      } else {
        paperSuggestions.value = [];
      }
    }
  );

  const filteredSemesters = computed(() => {
    if (!selectedStageId.value) return [];
    return allSemesters.value.filter(sem => sem.stage_id === selectedStageId.value);
  });

  const filteredSystemUnits = computed(() => {
    if (sourceType.value === 'BOOK') {
      return systemUnits.value;
    }
    const cat = selectedPaperCategory.value;
    if (cat === 'DAILY_HW') {
      return systemUnits.value;
    } else if (cat === 'UNIT_TEST') {
      return systemUnits.value.filter(u => u.id <= 12 || u.id === 999);
    } else if (cat === 'MIDTERM') {
      return systemUnits.value.filter(u => u.id === 101);
    } else if (cat === 'FINAL') {
      return systemUnits.value.filter(u => u.id === 102);
    } else if (cat === 'OTHER') {
      return systemUnits.value.filter(u => u.id === 999);
    }
    return systemUnits.value;
  });

  watch(filteredSystemUnits, (newUnits) => {
    if (newUnits && newUnits.length > 0) {
      const isCurrentValid = newUnits.some(u => u.id === selectedUnitCode.value);
      if (!isCurrentValid) {
        selectedUnitCode.value = newUnits[0].id;
      }
    }
  });

  // ── 数据初始化 ──
  async function loadData() {
    try {
      const stageRes = await api.get('/stages?only_active=true');
      if (stageRes.ok) activeStages.value = (await stageRes.json()).data.items || [];

      const semRes = await api.get('/semesters?only_active=true');
      if (semRes.ok) allSemesters.value = (await semRes.json()).data.items || [];

      const unitsRes = await api.get('/system/units');
      if (unitsRes.ok) systemUnits.value = (await unitsRes.json()).data.items || [];

      const catsRes = await api.get('/system/paper-categories');
      if (catsRes.ok) paperCategories.value = (await catsRes.json()).data.items || [];

      // 默认回显当前学生的当前推算学期
      const profile = currentUser.value.student_profile;
      if (profile) {
        const semName = calculateCurrentSemester(profile.enrollment_year, profile.semester_offset);
        const matchedSem = allSemesters.value.find(s => s.name === semName);
        if (matchedSem) {
          selectedStageId.value = matchedSem.stage_id || '';
          selectedSemesterId.value = matchedSem.id;
        }
      }
      fetchSubjects();
    } catch (err) {
      console.error(err);
    }
  }

  // ── 联动逻辑 ──
  function onStageChange() {
    const sems = filteredSemesters.value;
    selectedSemesterId.value = sems.length > 0 ? sems[0].id : '';
    fetchSubjects();
  }

  function onSemesterChange() {
    fetchSubjects();
  }

  async function fetchSubjects() {
    subjects.value = [];
    selectedSubjectId.value = '';
    availableBooks.value = [];
    selectedQuestionSourceId.value = '';
    
    if (!selectedSemesterId.value) return;
    try {
      const res = await api.get(`/semester_settings/subjects?semester_id=${selectedSemesterId.value}&only_active=true`);
      if (res.ok) {
        subjects.value = (await res.json()).data.items || [];
        if (subjects.value.length > 0) {
          selectedSubjectId.value = subjects.value[0].subject_id;
          fetchBooks();
        }
      }
    } catch (err) {
      console.error(err);
    }
  }

  function onSubjectChange() {
    fetchBooks();
  }

  async function fetchBooks() {
    availableBooks.value = [];
    selectedQuestionSourceId.value = '';
    if (!selectedSemesterId.value || !selectedSubjectId.value) return;

    try {
      const res = await api.get(`/source_books?student_id=${currentUser.value.id}&semester_id=${selectedSemesterId.value}&subject_id=${selectedSubjectId.value}&only_active=true&include_deleted=false`);
      if (res.ok) {
        availableBooks.value = (await res.json()).data.items || [];
      }
    } catch (err) {
      console.error(err);
    }
  }


  // ── 表单提交 ──
  async function handleSubmit() {
    submitError.value = '';
    
    if (sourceType.value === 'BOOK') {
      if (!selectedQuestionSourceId.value) {
        submitError.value = '请选择关联的习题册';
        return;
      }
      if (!pageNumber.value.trim()) {
        submitError.value = '关联已有资料时，页码为必填项';
        return;
      }
    } else if (sourceType.value === 'PAPER') {
      if (!sourcePaperName.value.trim()) {
        submitError.value = '请输入自定义来源/试卷名称';
        return;
      }
    }

    const ok = await showConfirm('确定要录入这笔习题笔记吗？');
    if (!ok) return;

    submitting.value = true;

    try {
      // 提交前对有旋转的图片进行画布物理旋转
      const processedFiles = [];
      for (let i = 0; i < uploadFiles.value.length; i++) {
        const rotation = imageRotations.value[i] || 0;
        if (rotation !== 0) {
          try {
            const rotatedFile = await rotateImageFile(uploadFiles.value[i], rotation);
            processedFiles.push(rotatedFile);
          } catch (err) {
            console.error('Failed to rotate image before upload:', err);
            processedFiles.push(uploadFiles.value[i]);
          }
        } else {
          processedFiles.push(uploadFiles.value[i]);
        }
      }

      const formData = new FormData();
      formData.append('semester_id', selectedSemesterId.value);
      formData.append('subject_id', selectedSubjectId.value);
      formData.append('mastery_level', masteryLevel.value);
      formData.append('difficulty_stars', difficultyStars.value.toFixed(1));
      formData.append('exclude_from_review', (!isInReviewPool.value).toString());

      if (sourceType.value === 'BOOK') {
        formData.append('unit_code', selectedUnitCode.value.toString());
        formData.append('question_source_id', selectedQuestionSourceId.value);
        formData.append('page_number', pageNumber.value.trim());
      } else {
        formData.append('unit_code', selectedUnitCode.value.toString());
        formData.append('source_paper_name', sourcePaperName.value.trim());
        formData.append('paper_category', selectedPaperCategory.value);
      }
      if (auxiliaryText.value.trim()) {
        formData.append('auxiliary_text', auxiliaryText.value.trim());
      }

      for (let i = 0; i < processedFiles.length; i++) {
        formData.append('images', processedFiles[i]);
        formData.append('image_titles', imageTitles.value[i] || '');
      }

      const res = await api.post('/notes', formData);
      if (res.ok) {
        sourcePaperName.value = '';
        selectedUnitCode.value = 999;
        selectedPaperCategory.value = 'DAILY_HW';
        pageNumber.value = '';
        auxiliaryText.value = '';
        resetImages();
        difficultyStars.value = 3;
        masteryLevel.value = 'CLUELESS';
        isInReviewPool.value = true;
        
        await showAlert('习题笔记录入成功！');
      } else {
        submitError.value = (await res.json()).msg || '录入失败';
      }
    } catch (err) {
      submitError.value = '网络连接异常';
    } finally {
      submitting.value = false;
    }
  }

  onMounted(() => {
    loadData();
  });

  return {
    currentUser,
    activeStages,
    allSemesters,
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
    systemUnits,
    filteredSystemUnits,
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
    zoomScale,
    imageRotations,
    openPreview,
    closePreview,
    zoomIn,
    zoomOut,
    resetZoom,
    handleZoomWheel,
    prevPreview,
    nextPreview,
    rotateLeft,
    rotateRight,
    rotateImageAt,
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
  };
}
