import { ref, computed, watch } from 'vue';
import { api } from '../services/api.js';
import { showConfirm, showAlert, showThreeButtonConfirm } from '../services/dialog.js';
import { useNoteDetailMedia } from './useNoteDetailMedia.js';
import { saveNoteDetailEdits, getAbsoluteUrl, masteryName } from './useNoteDetailSave.js';

export function useNoteDetail(props, emit, currentUser) {
  const isEditMode = ref(false);
  const saving = ref(false);
  const submitError = ref('');

  // 联动列表缓存
  const activeStages = ref([]);
  const allSemesters = ref([]);
  const systemUnits = ref([]);
  const paperCategories = ref([]);

  // 编辑表单字段
  const editStageId = ref('');
  const editSemesterId = ref('');
  const editSubjectId = ref('');
  const editSourceType = ref('BOOK');
  const editBookId = ref('');
  const editPaperCategory = ref('DAILY_HW');
  const editSourcePaperName = ref('');
  const editUnitCode = ref(999);
  const editPageNumber = ref('');
  const editDifficultyStars = ref(3);
  const editMasteryLevel = ref('CLUELESS');
  const editIsInReviewPool = ref(true);
  const editAuxiliaryText = ref('');

  // 编辑联动数据缓存
  const editSubjects = ref([]);
  const editBooks = ref([]);
  const editPaperSuggestions = ref([]);
  const editShowSuggestions = ref(false);

  function selectEditPaperSuggestion(item) {
    editSourcePaperName.value = item.name;
    editShowSuggestions.value = false;
  }

  function hideEditSuggestionsWithDelay() {
    setTimeout(() => {
      editShowSuggestions.value = false;
    }, 200);
  }

  const media = useNoteDetailMedia(props, emit, isEditMode);
  const {
    combinedMediaList,
    deleteAttachmentIds
  } = media;

  // 难度评分悬浮状态
  const hoverStars = ref(0);

  const editFilteredSemesters = computed(() => {
    if (!editStageId.value) return [];
    return allSemesters.value.filter(sem => sem.stage_id === editStageId.value);
  });

  const filteredEditSystemUnits = computed(() => {
    if (editSourceType.value === 'BOOK') {
      return systemUnits.value;
    }
    const cat = editPaperCategory.value;
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

  watch(filteredEditSystemUnits, (newUnits) => {
    if (newUnits && newUnits.length > 0) {
      const isCurrentValid = newUnits.some(u => u.id === editUnitCode.value);
      if (!isCurrentValid) {
        editUnitCode.value = newUnits[0].id;
      }
    }
  });

  // 检测修改发生状态
  const hasChanges = computed(() => {
    if (!isEditMode.value) return false;
    if (!props.note) return false;
    
    // 文本比较
    if ((editAuxiliaryText.value || '') !== (props.note.auxiliary_text || '')) return true;
    
    // 字段比较
    if (editSemesterId.value !== props.note.semester_id) return true;
    if (editSubjectId.value !== props.note.subject_id) return true;
    if (editUnitCode.value !== props.note.unit_code) return true;
    if (editPageNumber.value !== (props.note.page_number || '')) return true;
    if (editDifficultyStars.value !== Math.round(props.note.difficulty_stars)) return true;
    if (editMasteryLevel.value !== props.note.mastery_level) return true;
    if (editIsInReviewPool.value !== !props.note.exclude_from_review) return true;
    
    // 来源比较
    if (editSourceType.value === 'BOOK') {
      if (editBookId.value !== (props.note.question_source_id || '')) return true;
    } else {
      if (editSourcePaperName.value !== (props.note.source_paper_name || '')) return true;
      const originalCat = props.note.paper_category || 'DAILY_HW';
      if (editPaperCategory.value !== originalCat) return true;
    }

    // 附件队列改变比较 (已有项数量或者是否存在新增文件/图片旋转/标题改变)
    if (deleteAttachmentIds.value.length > 0) return true;
    if (combinedMediaList.value.some(item => item.isNew)) return true;
    if (combinedMediaList.value.some(item => item.file_type === 'IMAGE' && ((item.rotation || 0) % 360 !== 0))) return true;

    // 检查顺序或标题是否改变
    const originalAttachments = props.note.attachments || [];
    if (combinedMediaList.value.length !== originalAttachments.length) return true;
    for (let i = 0; i < combinedMediaList.value.length; i++) {
      if (combinedMediaList.value[i].id !== originalAttachments[i].id) return true;
      if ((combinedMediaList.value[i].title || '') !== (originalAttachments[i].title || '')) return true;
    }
    
    return false;
  });

  // 监听 note 改变，自动退出编辑状态
  watch(() => props.note, () => {
    isEditMode.value = false;
  });

  // ── 预览页直接修改快捷更新 ──
  async function handleDirectMasteryChange(newLevel) {
    const confirmOk = await showConfirm(`确定要将当前习题的掌握程度修改为【${masteryName(newLevel)}】吗？`);
    if (!confirmOk) {
      emit('refresh'); // 触发刷新以同步重置 DOM 下拉状态
      return;
    }

    try {
      const formData = new FormData();
      formData.append('mastery_level', newLevel);
      formData.append('sync_base_time', props.note.updated_at);

      const res = await api.put(`/notes/${props.note.id}`, formData);
      if (res.ok) {
        await showAlert('掌握程度更新成功！');
        emit('refresh');
      } else {
        const errMsg = (await res.json()).msg || '更新失败';
        await showAlert(`修改失败：${errMsg}`);
        emit('refresh');
      }
    } catch (err) {
      console.error(err);
      await showAlert('网络故障，更新失败');
      emit('refresh');
    }
  }

  async function handleDirectStarsChange(stars) {
    const confirmOk = await showConfirm(`确定要把这道错题的难度评分调整为 ${stars} 星级吗？`);
    if (!confirmOk) return;

    try {
      const formData = new FormData();
      formData.append('difficulty_stars', stars.toFixed(1));
      formData.append('sync_base_time', props.note.updated_at);

      const res = await api.put(`/notes/${props.note.id}`, formData);
      if (res.ok) {
        await showAlert('难度评分更新成功！');
        emit('refresh');
      } else {
        const errMsg = (await res.json()).msg || '更新失败';
        await showAlert(`修改失败：${errMsg}`);
        emit('refresh');
      }
    } catch (err) {
      console.error(err);
      await showAlert('网络故障，更新失败');
      emit('refresh');
    }
  }

  async function handleDirectReviewPoolChange(checked) {
    const actionText = checked ? '纳入' : '排除出';
    const confirmOk = await showConfirm(`确定要将当前习题【${actionText}】复习推荐范围吗？`);
    if (!confirmOk) {
      emit('refresh');
      return;
    }

    try {
      const formData = new FormData();
      formData.append('exclude_from_review', (!checked).toString());
      formData.append('sync_base_time', props.note.updated_at);

      const res = await api.put(`/notes/${props.note.id}`, formData);
      if (res.ok) {
        await showAlert('复习推荐范围更新成功！');
        emit('refresh');
      } else {
        const errMsg = (await res.json()).msg || '更新失败';
        await showAlert(`修改失败：${errMsg}`);
        emit('refresh');
      }
    } catch (err) {
      console.error(err);
      await showAlert('网络故障，更新失败');
      emit('refresh');
    }
  }

  // ── 进入/退出编辑模式 ──
  async function enterEditMode() {
    submitError.value = '';
    isEditMode.value = true;
    saving.value = false;

    // 加载元数据字典
    await loadMetadataDicts();

    // 初始化编辑表单字段
    editStageId.value = props.note.stage_id || '';
    editSemesterId.value = props.note.semester_id || '';
    editSubjectId.value = props.note.subject_id || '';
    editUnitCode.value = props.note.unit_code || 999;
    editPageNumber.value = props.note.page_number || '';
    editDifficultyStars.value = Math.round(props.note.difficulty_stars) || 3;
    editMasteryLevel.value = props.note.mastery_level || 'CLUELESS';
    editIsInReviewPool.value = !props.note.exclude_from_review;
    editAuxiliaryText.value = props.note.auxiliary_text || '';

    // 判断来源类型并回显
    if (props.note.source_paper_name) {
      editSourceType.value = 'PAPER';
      editSourcePaperName.value = props.note.source_paper_name;
      editPaperCategory.value = props.note.paper_category || 'DAILY_HW';
      editBookId.value = '';
    } else {
      editSourceType.value = 'BOOK';
      editBookId.value = props.note.question_source_id || '';
      editSourcePaperName.value = '';
    }

    // 统一附件列表初始化
    combinedMediaList.value = (props.note.attachments || []).map(att => ({
      id: att.id,
      isNew: false,
      file_type: att.file_type,
      storage_path: att.storage_path,
      original_name: att.original_name,
      file_size: att.file_size,
      title: att.title || '',
      rotation: 0
    }));
    deleteAttachmentIds.value = [];

    // 加载编辑表单联动数据
    await fetchEditSubjects();
    await fetchEditBooks();
  }

  function exitEditMode() {
    isEditMode.value = false;
    submitError.value = '';
  }

  // 点击叉号退出编辑或关闭的确认拦截器
  async function handleCloseWithPrompt() {
    if (hasChanges.value) {
      const result = await showThreeButtonConfirm(
        '您修改了笔记内容，是否保存修改？\n点击【保存修改】以存盘并退出；\n点击【放弃修改】将不保存并退出；\n点击【取消】将留在此页面继续编辑。',
        '确认退出编辑'
      );
      
      if (result === 'save') {
        await handleEditSave();
      } else if (result === 'discard') {
        exitEditMode();
        emit('close');
      }
      // 如果是 'cancel' 则什么都不做，继续保留在编辑页面
    } else {
      exitEditMode();
      emit('close');
    }
  }

  // ── 编辑子联动逻辑 ──
  async function loadMetadataDicts() {
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

  async function fetchEditSubjects() {
    editSubjects.value = [];
    if (!editSemesterId.value) return;
    try {
      const res = await api.get(`/semester_settings/subjects?semester_id=${editSemesterId.value}&only_active=true`);
      if (res.ok) {
        editSubjects.value = (await res.json()).data.items || [];
      }
    } catch (err) {
      console.error(err);
    }
  }

  async function fetchEditBooks() {
    editBooks.value = [];
    if (!editSemesterId.value || !editSubjectId.value) return;
    try {
      const res = await api.get(`/source_books?student_id=${currentUser.value.id}&semester_id=${editSemesterId.value}&subject_id=${editSubjectId.value}&only_active=true&include_deleted=false`);
      if (res.ok) {
        editBooks.value = (await res.json()).data.items || [];
      }
    } catch (err) {
      console.error(err);
    }
  }

  function onEditStageChange() {
    const sems = editFilteredSemesters.value;
    editSemesterId.value = sems.length > 0 ? sems[0].id : '';
    editSubjectId.value = '';
    editBookId.value = '';
    editBooks.value = [];
    editSubjects.value = [];
    fetchEditSubjects();
  }

  function onEditSemesterChange() {
    editSubjectId.value = '';
    editBookId.value = '';
    editBooks.value = [];
    fetchEditSubjects();
  }

  function onEditSubjectChange() {
    editBookId.value = '';
    fetchEditBooks();
  }

  function setEditSourceType(type) {
    editSourceType.value = type;
    if (type === 'BOOK') {
      editSourcePaperName.value = '';
    } else {
      editBookId.value = '';
      editPageNumber.value = '';
    }
  }

  let fetchSuggestionsTimer = null;

  async function fetchEditPaperSuggestions() {
    editPaperSuggestions.value = [];
    if (!editSemesterId.value || !editSubjectId.value || !editPaperCategory.value) return;

    if (fetchSuggestionsTimer) {
      clearTimeout(fetchSuggestionsTimer);
    }

    fetchSuggestionsTimer = setTimeout(async () => {
      try {
        const studentId = currentUser.value.id;
        const semId = editSemesterId.value;
        const subId = editSubjectId.value;
        const catCode = editPaperCategory.value;
        const unitCode = editUnitCode.value;
        const queryVal = editSourcePaperName.value.trim();

        let url = `/source_books?student_id=${studentId}&semester_id=${semId}&subject_id=${subId}&source_type=PAPER&category_code=${catCode}&only_active=true&include_deleted=false`;
        if (unitCode) {
          url += `&unit_code=${unitCode}`;
        }
        if (queryVal) {
          url += `&name_query=${encodeURIComponent(queryVal)}`;
        }

        const res = await api.get(url);
        if (res.ok) {
          editPaperSuggestions.value = (await res.json()).data.items || [];
        }
      } catch (err) {
        console.error(err);
      }
    }, 300);
  }

  watch(
    [editSemesterId, editSubjectId, editPaperCategory, editUnitCode, editSourcePaperName, editSourceType],
    () => {
      if (editSourceType.value === 'PAPER') {
        fetchEditPaperSuggestions();
      } else {
        editPaperSuggestions.value = [];
      }
    }
  );

  // ── 保存编辑表单 ──
  function handleEditSave() {
    return saveNoteDetailEdits({
      props,
      emit,
      getAbsoluteUrl,
      isEditMode,
      saving,
      submitError,
      editSemesterId,
      editSubjectId,
      editSourceType,
      editBookId,
      editPageNumber,
      editSourcePaperName,
      editPaperCategory,
      editUnitCode,
      editDifficultyStars,
      editMasteryLevel,
      editIsInReviewPool,
      editAuxiliaryText,
      combinedMediaList,
      deleteAttachmentIds
    });
  }

  return {
    ...media,
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
    hoverStars,
    editFilteredSemesters,
    hasChanges,
    handleDirectMasteryChange,
    handleDirectStarsChange,
    handleDirectReviewPoolChange,
    enterEditMode,
    exitEditMode,
    handleCloseWithPrompt,
    onEditStageChange,
    onEditSemesterChange,
    onEditSubjectChange,
    setEditSourceType,
    handleEditSave,
    masteryName,
    editPaperSuggestions,
    editShowSuggestions,
    selectEditPaperSuggestion,
    hideEditSuggestionsWithDelay
  };
}
