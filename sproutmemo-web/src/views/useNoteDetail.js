import { ref, computed, watch } from 'vue';
import { api } from '../services/api.js';
import { showConfirm, showAlert, showThreeButtonConfirm } from '../services/dialog.js';
import { compressImage, rotateImageFile } from '../utils/imageCompressor.js';

function getAbsoluteUrl(relPath) {
  if (!relPath) return '';
  const baseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8000/api/v1';
  return `${baseUrl.replace('/api/v1', '')}/uploads/${relPath}`;
}

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

  // 统一的媒体附件列表 (合并已有和新增，支持排序和统一删除管理)
  // 项结构:
  // - 已有附件: { id, isNew: false, file_type, storage_path, original_name, title }
  // - 新增附件: { isNew: true, file, file_type: 'IMAGE', blobUrl, original_name, title }
  const combinedMediaList = ref([]);
  const deleteAttachmentIds = ref([]);

  // 拖拽排序状态
  const dragIndex = ref(null);
  const dragOverIndex = ref(null);
  const isInputActive = ref(false);

  // 大图轮播预览状态
  const previewIndex = ref(null);
  const zoomScale = ref(1);

  // 难度评分悬浮状态
  const hoverStars = ref(0);

  // 计算属性
  const images = computed(() => {
    return props.note?.attachments?.filter(a => a.file_type === 'IMAGE') || [];
  });

  const audios = computed(() => {
    return props.note?.attachments?.filter(a => a.file_type === 'AUDIO') || [];
  });

  const videos = computed(() => {
    return props.note?.attachments?.filter(a => a.file_type === 'VIDEO') || [];
  });

  const totalImagesCount = computed(() => {
    return combinedMediaList.value.filter(a => a.file_type === 'IMAGE').length;
  });

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

  // ── 大图轮播预览控制 ──
  function openFullScreen(url, idx) {
    previewIndex.value = idx;
    zoomScale.value = 1;
  }

  function closePreview() {
    previewIndex.value = null;
    zoomScale.value = 1;
  }

  function zoomIn() {
    if (zoomScale.value < 4) {
      zoomScale.value = parseFloat((zoomScale.value + 0.15).toFixed(2));
    }
  }

  function zoomOut() {
    if (zoomScale.value > 0.3) {
      zoomScale.value = parseFloat((zoomScale.value - 0.15).toFixed(2));
    }
  }

  function resetZoom() {
    zoomScale.value = 1;
  }

  function handleZoomWheel(event) {
    if (event.deltaY < 0) {
      zoomIn();
    } else {
      zoomOut();
    }
  }

  const previewImages = computed(() => {
    if (isEditMode.value) {
      return combinedMediaList.value.filter(a => a.file_type === 'IMAGE');
    }
    return props.note?.attachments?.filter(a => a.file_type === 'IMAGE') || [];
  });

  function getEditImageIndex(combinedIdx) {
    const list = combinedMediaList.value.filter(a => a.file_type === 'IMAGE');
    const targetItem = combinedMediaList.value[combinedIdx];
    return list.indexOf(targetItem);
  }

  async function persistViewModeRotation(item, deltaAngle) {
    if (!props.note?.id || !item?.id) return;
    try {
      const res = await api.post(`/notes/${props.note.id}/attachments/${item.id}/rotate`, {
        angle: deltaAngle
      });
      if (res.data?.code === 200 && res.data.data) {
        const updatedNote = res.data.data;
        const updatedAtt = (updatedNote.attachments || []).find(a => a.id === item.id);
        if (updatedAtt) {
          item.storage_path = updatedAtt.storage_path;
          item.file_size = updatedAtt.file_size;
          item.rotation = 0;
        }
        props.note.updated_at = updatedNote.updated_at;
        props.note.sync_base_time = updatedNote.sync_base_time;
        emit('refresh');
      }
    } catch (err) {
      console.error('Failed to persist image rotation:', err);
    }
  }

  function rotateLeft() {
    const idx = previewIndex.value;
    if (idx === null) return;
    const item = previewImages.value[idx];
    if (item) {
      if (isEditMode.value) {
        item.rotation = ((item.rotation || 0) - 90) % 360;
      } else {
        persistViewModeRotation(item, -90);
      }
    }
  }

  function rotateRight() {
    const idx = previewIndex.value;
    if (idx === null) return;
    const item = previewImages.value[idx];
    if (item) {
      if (isEditMode.value) {
        item.rotation = ((item.rotation || 0) + 90) % 360;
      } else {
        persistViewModeRotation(item, 90);
      }
    }
  }

  function rotateCombinedMediaAt(combinedIdx, deltaAngle = 90) {
    const item = combinedMediaList.value[combinedIdx];
    if (item && item.file_type === 'IMAGE') {
      item.rotation = ((item.rotation || 0) + deltaAngle) % 360;
    }
  }

  function prevPreview() {
    if (previewImages.value.length === 0) return;
    previewIndex.value = (previewIndex.value - 1 + previewImages.value.length) % previewImages.value.length;
    zoomScale.value = 1;
  }

  // 仅供大图查看定位 index 映射
  const imageIndexMapping = computed(() => {
    return combinedMediaList.value
      .map((item, idx) => ({ item, originalIndex: idx }))
      .filter(x => x.item.file_type === 'IMAGE');
  });

  function nextPreview() {
    if (previewImages.value.length === 0) return;
    previewIndex.value = (previewIndex.value + 1) % previewImages.value.length;
    zoomScale.value = 1;
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

  // ── 附件顺序调整 & 移除 ──
  function moveMediaLeft(idx) {
    if (idx === 0) return;
    const list = [...combinedMediaList.value];
    const temp = list[idx];
    list[idx] = list[idx - 1];
    list[idx - 1] = temp;
    combinedMediaList.value = list;
  }

  function moveMediaRight(idx) {
    if (idx === combinedMediaList.value.length - 1) return;
    const list = [...combinedMediaList.value];
    const temp = list[idx];
    list[idx] = list[idx + 1];
    list[idx + 1] = temp;
    combinedMediaList.value = list;
  }

  async function removeMedia(idx) {
    const item = combinedMediaList.value[idx];
    const ok = await showConfirm(`确定要删除此${item.file_type === 'IMAGE' ? '图片' : item.file_type === 'AUDIO' ? '录音' : '视频'}附件吗？`);
    if (!ok) return;

    if (!item.isNew) {
      deleteAttachmentIds.value.push(item.id);
    }
    combinedMediaList.value.splice(idx, 1);
  }

  // 拖拽附件排序
  function handleDragStart(event, idx) {
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
      
      // 获取当前卡片中的 img 元素作为拖拽快照（如果是音视频，就使用图标卡片）
      const imgEl = event.currentTarget.querySelector('.preview-card img') || event.currentTarget.querySelector('.media-file-icon-box');
      if (imgEl) {
        event.dataTransfer.setDragImage(imgEl, 50, 50);
      }
    }
    
    setTimeout(() => {
      dragIndex.value = idx;
    }, 0);
  }

  function handleDragOver(e, index) {
    dragOverIndex.value = index;
  }

  function handleDragEnter(e, index) {
    dragOverIndex.value = index;
  }

  function handleDragLeave(e, index) {
    if (dragOverIndex.value === index) {
      dragOverIndex.value = null;
    }
  }

  function handleDrop(e, index) {
    if (dragIndex.value === null || dragIndex.value === index) return;
    const list = [...combinedMediaList.value];
    const draggedItem = list[dragIndex.value];
    
    // 移出原位置并插入新位置
    list.splice(dragIndex.value, 1);
    list.splice(index, 0, draggedItem);
    
    combinedMediaList.value = list;
    dragIndex.value = null;
    dragOverIndex.value = null;
  }

  function handleDragEnd() {
    dragIndex.value = null;
    dragOverIndex.value = null;
  }

  // 图片压缩与添加至统一轨道
  async function handleEditFileChange(event) {
    const files = event.target.files;
    if (!files) return;

    for (let i = 0; i < files.length; i++) {
      if (totalImagesCount.value >= 9) {
        await showAlert('总图片数量不能超过 9 张！');
        break;
      }
      const file = files[i];
      try {
        const compressedFile = await compressImage(file);
        combinedMediaList.value.push({
          isNew: true,
          file: compressedFile,
          file_type: 'IMAGE',
          blobUrl: URL.createObjectURL(compressedFile),
          original_name: file.name,
          title: '',
          rotation: 0
        });
      } catch (err) {
        console.error(err);
        combinedMediaList.value.push({
          isNew: true,
          file: file,
          file_type: 'IMAGE',
          blobUrl: URL.createObjectURL(file),
          original_name: file.name,
          title: '',
          rotation: 0
        });
      }
    }
    event.target.value = '';
  }

  // ── 保存编辑表单 ──
  async function handleEditSave() {
    submitError.value = '';

    if (!editSemesterId.value) {
      submitError.value = '请选择学期';
      return;
    }
    if (!editSubjectId.value) {
      submitError.value = '请选择学科';
      return;
    }

    if (editSourceType.value === 'BOOK') {
      if (!editBookId.value) {
        submitError.value = '请选择关联的习题册';
        return;
      }
      if (!editPageNumber.value.trim()) {
        submitError.value = '关联习题册时，页码为必填项';
        return;
      }
    } else {
      if (!editSourcePaperName.value.trim()) {
        submitError.value = '请输入自定义来源/试卷名称';
        return;
      }
    }

    const ok = await showConfirm('确定要保存对该习题笔记的修改吗？');
    if (!ok) return;

    saving.value = true;

    try {
      // 保存前对有旋转的图片附件进行 Canvas 物理旋转
      for (let i = 0; i < combinedMediaList.value.length; i++) {
        const item = combinedMediaList.value[i];
        if (item.file_type === 'IMAGE' && item.rotation && item.rotation !== 0) {
          try {
            if (item.isNew) {
              const rotatedFile = await rotateImageFile(item.file, item.rotation);
              item.file = rotatedFile;
              if (item.blobUrl) {
                URL.revokeObjectURL(item.blobUrl);
              }
              item.blobUrl = URL.createObjectURL(rotatedFile);
              item.rotation = 0;
            } else {
              const imageUrl = getAbsoluteUrl(item.storage_path);
              const rotatedFile = await rotateImageFile(imageUrl, item.rotation);
              deleteAttachmentIds.value.push(item.id);
              combinedMediaList.value[i] = {
                isNew: true,
                file: rotatedFile,
                file_type: 'IMAGE',
                blobUrl: URL.createObjectURL(rotatedFile),
                original_name: item.original_name,
                title: item.title || '',
                rotation: 0
              };
            }
          } catch (err) {
            console.error('Failed to rotate image before save:', err);
          }
        }
      }

      const formData = new FormData();
      formData.append('semester_id', editSemesterId.value);
      formData.append('subject_id', editSubjectId.value);
      formData.append('mastery_level', editMasteryLevel.value);
      formData.append('difficulty_stars', editDifficultyStars.value.toFixed(1));
      formData.append('exclude_from_review', (!editIsInReviewPool.value).toString());
      formData.append('unit_code', editUnitCode.value.toString());
      formData.append('auxiliary_text', editAuxiliaryText.value.trim());
      formData.append('sync_base_time', props.note.updated_at); // 乐观锁

      if (editSourceType.value === 'BOOK') {
        formData.append('question_source_id', editBookId.value);
        formData.append('page_number', editPageNumber.value.trim());
        formData.append('source_paper_name', '');
      } else {
        formData.append('question_source_id', '');
        formData.append('source_paper_name', editSourcePaperName.value.trim());
        formData.append('paper_category', editPaperCategory.value);
        formData.append('page_number', '');
      }

      // 待物理删除列表
      for (const attId of deleteAttachmentIds.value) {
        formData.append('delete_attachment_ids', attId);
      }

      // 序列化现有附件的新排序 metadata
      const existingMeta = [];
      let sortOrderCounter = 0;
      
      combinedMediaList.value.forEach((item) => {
        if (!item.isNew) {
          existingMeta.push({
            id: item.id,
            title: item.title,
            sort_order: sortOrderCounter++
          });
        }
      });
      formData.append('attachment_metadata', JSON.stringify(existingMeta));

      // 追加新增图片文件及自定义标题 (按目前列表顺序)
      combinedMediaList.value.forEach((item) => {
        if (item.isNew) {
          formData.append('images', item.file);
          formData.append('image_titles', item.title || '');
        }
      });

      const res = await api.put(`/notes/${props.note.id}`, formData);
      if (res.ok) {
        await showAlert('习题笔记更新成功！');
        isEditMode.value = false;
        emit('refresh');
      } else {
        const errRes = await res.json();
        submitError.value = errRes.msg || '保存失败';
      }
    } catch (err) {
      submitError.value = '网络保存失败，请检查连接';
    } finally {
      saving.value = false;
    }
  }

  function masteryName(level) {
    const map = { CLUELESS: '🔴 完全不会', PARTIAL: '🟡 一知半解', MASTERED: '🟢 熟练掌握' };
    return map[level] || level;
  }

  return {
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
    zoomScale,
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
    hasChanges,
    handleDirectMasteryChange,
    handleDirectStarsChange,
    handleDirectReviewPoolChange,
    openFullScreen,
    closePreview,
    zoomIn,
    zoomOut,
    resetZoom,
    handleZoomWheel,
    prevPreview,
    nextPreview,
    enterEditMode,
    exitEditMode,
    handleCloseWithPrompt,
    onEditStageChange,
    onEditSemesterChange,
    onEditSubjectChange,
    setEditSourceType,
    moveMediaLeft,
    moveMediaRight,
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
  };
}
