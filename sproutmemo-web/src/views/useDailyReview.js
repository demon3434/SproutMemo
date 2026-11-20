import { ref, computed, inject, watch } from 'vue';
import { api } from '../services/api.js';
import { reviewService } from '../services/reviewService.js';
import { calculateCurrentSemester } from '../utils/semester.js';
import { showAlert } from '../services/dialog.js';

export function useDailyReview() {
  const activeChildId = inject('activeChildId', ref(''));
  const currentUser = inject('currentUser', ref(null));

  const activeTab = ref('review');
  const filterHeaderRef = ref(null);

  const semesters = ref([]);
  const subjects = ref([]);
  const units = ref([]);
  const presets = ref({ strategies: [], count_presets: [], outcomes: [], stages: [], semesters: [], stats: null });

  const selectedSemesterId = ref('');
  const session = ref(null);
  const previewNotes = ref([]);
  const generating = ref(false);
  const starting = ref(false);
  const submitting = ref(false);

  const showResumeModal = ref(false);
  const detectedActiveSession = ref(null);

  const previewingNote = ref(null);
  const activeNoteIndex = ref(0);

  const recentStats = ref(null);
  const loadingRecentStats = ref(false);

  const currentStudentId = computed(() => {
    if (currentUser.value?.role === 'PARENT') {
      return activeChildId.value;
    }
    return currentUser.value?.id || '';
  });

  const isSessionFinished = computed(() => {
    if (!session.value || !session.value.notes) return false;
    return session.value.notes.every(n => n.is_completed);
  });

  const currentNote = computed(() => {
    if (!session.value || !session.value.notes) return null;
    const idx = activeNoteIndex.value;
    if (idx < 0 || idx >= session.value.notes.length) return null;
    return session.value.notes[idx];
  });

  function onSelectNote(idx) {
    if (session.value && session.value.notes && idx >= 0 && idx < session.value.notes.length) {
      activeNoteIndex.value = idx;
    }
  }

  function previewQuestion(note) {
    previewingNote.value = note;
  }

  function initSemesterId() {
    let profile = null;
    if (currentUser.value) {
      if (currentUser.value.role === 'STUDENT') {
        profile = currentUser.value.student_profile;
      } else if (currentUser.value.role === 'PARENT' && currentUser.value.bound_students) {
        const child = currentUser.value.bound_students.find(c => c.id === activeChildId.value);
        if (child) {
          profile = child.student_profile;
        }
      }
    }

    const semsList = presets.value.semesters && presets.value.semesters.length > 0
      ? presets.value.semesters
      : semesters.value;

    if (profile && semsList.length > 0) {
      const semName = calculateCurrentSemester(profile.enrollment_year, profile.semester_offset);
      const matchedSem = semsList.find(s => s.name === semName);
      if (matchedSem) {
        selectedSemesterId.value = matchedSem.id;
        return;
      }
    }

    if (semsList.length > 0) {
      selectedSemesterId.value = semsList[0].id;
    } else {
      selectedSemesterId.value = '';
    }
  }

  async function loadRecentStats() {
    if (!currentStudentId.value) return;
    loadingRecentStats.value = true;
    try {
      const res = await api.get(`/review/recent-stats?student_id=${currentStudentId.value}`);
      const json = await res.json();
      if (json.code === 200) {
        recentStats.value = json.data;
      }
    } catch (err) {
      console.error('加载近期复习统计失败:', err);
    } finally {
      loadingRecentStats.value = false;
    }
  }

  async function loadSubjects(semesterId) {
    try {
      const res = await api.get(`/semester_settings/subjects?semester_id=${semesterId}`);
      const json = await res.json();
      if (json.code === 200 && json.data) {
        subjects.value = json.data.items || [];
      }
    } catch (err) {
      console.error('加载科目失败:', err);
    }
  }

  async function loadUnits() {
    try {
      const res = await api.get('/system/units');
      const json = await res.json();
      if (json.code === 200 && json.data) {
        units.value = json.data.items || [];
      }
    } catch (err) {
      console.error('加载单元失败:', err);
    }
  }

  async function loadInitialData() {
    try {
      presets.value = await reviewService.getPresets(currentStudentId.value || null);
      await loadRecentStats();

      const semRes = await api.get('/semesters');
      const semJson = await semRes.json();
      if (semJson.code === 200 && semJson.data) {
        semesters.value = semJson.data.items || [];
        initSemesterId();
        if (selectedSemesterId.value) {
          await loadSubjects(selectedSemesterId.value);
          await loadUnits();
        }
      }
    } catch (err) {
      console.error('加载复习初始化数据失败:', err);
    }
  }

  async function checkActiveSession() {
    if (currentUser.value?.role === 'PARENT') return;
    try {
      const activeSession = await reviewService.getActiveSession(currentStudentId.value || null);
      if (activeSession) {
        detectedActiveSession.value = activeSession;
        showResumeModal.value = true;
      }
    } catch (err) {
      console.error('探测活跃会话失败:', err);
    }
  }

  watch(currentStudentId, async (newStudentId) => {
    if (newStudentId) {
      session.value = null;
      previewNotes.value = [];
      recentStats.value = null;
      initSemesterId();
      if (selectedSemesterId.value) {
        await loadSubjects(selectedSemesterId.value);
      }
      await loadInitialData();
      if (currentUser.value?.role !== 'PARENT') {
        await checkActiveSession();
      }
    }
  }, { immediate: true });

  async function onSemesterChange(semesterId) {
    selectedSemesterId.value = semesterId;
    subjects.value = [];
    units.value = [];
    if (semesterId) {
      await loadSubjects(semesterId);
      await loadUnits();
    }
  }

  function onSubjectChange() {
    loadUnits();
  }

  async function onGeneratePreview(filters) {
    if (generating.value) return;
    generating.value = true;
    try {
      const data = await reviewService.previewNotes({
        studentId: currentStudentId.value || null,
        semesterId: selectedSemesterId.value,
        subjectId: filters.subjectId || null,
        unitCode: filters.unitCode || null,
        countPresetCode: filters.countPresetCode,
        strategyCode: filters.strategyCode
      });
      previewNotes.value = data || [];
      if (previewNotes.value.length === 0) {
        showAlert('在当前筛选条件下，没有找到符合要求的错题，请调整条件后再试！', '提示');
      }
    } catch (err) {
      console.error('生成复习预览失败:', err);
      showAlert(err.message || '生成复习预览失败', '提示');
    } finally {
      generating.value = false;
    }
  }

  function cancelPreview() {
    previewNotes.value = [];
  }

  async function startFormalSession() {
    if (starting.value || previewNotes.value.length === 0) return;
    starting.value = true;
    try {
      const filters = filterHeaderRef.value ? filterHeaderRef.value.getFilters() : {};
      const noteIds = previewNotes.value.map(n => n.id);
      session.value = await reviewService.createSession({
        studentId: currentStudentId.value || null,
        semesterId: selectedSemesterId.value,
        subjectId: filters.subjectId || null,
        unitCode: filters.unitCode || null,
        countPresetCode: filters.countPresetCode,
        strategyCode: filters.strategyCode,
        noteIds
      });
      activeNoteIndex.value = 0;
    } catch (err) {
      console.error('开始正式复习失败:', err);
      showAlert(err.message || '开始答题失败，请重试', '提示');
    } finally {
      starting.value = false;
    }
  }

  async function onResumeSession() {
    if (!detectedActiveSession.value) return;
    try {
      const details = await reviewService.getSessionDetails(detectedActiveSession.value.id);
      session.value = details;
      const firstUncompletedIdx = details.notes.findIndex(n => !n.is_completed);
      activeNoteIndex.value = firstUncompletedIdx !== -1 ? firstUncompletedIdx : 0;
    } catch (err) {
      console.error('恢复会话失败:', err);
      showAlert(err.message || '恢复答题失败', '提示');
    } finally {
      showResumeModal.value = false;
      detectedActiveSession.value = null;
    }
  }

  async function onAbandonSession() {
    if (!detectedActiveSession.value) return;
    try {
      await reviewService.abandonSession(detectedActiveSession.value.id);
      await loadInitialData();
    } catch (err) {
      console.error('放弃会话失败:', err);
    } finally {
      showResumeModal.value = false;
      detectedActiveSession.value = null;
    }
  }

  async function handleSubmitItem(payload) {
    if (submitting.value || !session.value) return;
    submitting.value = true;
    try {
      const updatedSession = await reviewService.submitItem(session.value.id, payload);
      session.value = updatedSession;

      if (updatedSession.status === 'COMPLETED' || isSessionFinished.value) {
        await loadInitialData();
      } else {
        const nextUncompletedIdx = session.value.notes.findIndex((n, idx) => idx > activeNoteIndex.value && !n.is_completed);
        if (nextUncompletedIdx !== -1) {
          activeNoteIndex.value = nextUncompletedIdx;
        } else {
          const firstUncompletedIdx = session.value.notes.findIndex(n => !n.is_completed);
          if (firstUncompletedIdx !== -1) {
            activeNoteIndex.value = firstUncompletedIdx;
          } else if (activeNoteIndex.value < session.value.notes.length - 1) {
            activeNoteIndex.value++;
          }
        }
      }
    } catch (err) {
      console.error('提交复习打卡失败:', err);
      showAlert(err.message || '提交打卡失败，请重试', '提示');
    } finally {
      submitting.value = false;
    }
  }

  function resetSession() {
    session.value = null;
    previewNotes.value = [];
  }

  return {
    currentUser,
    activeTab,
    filterHeaderRef,
    semesters,
    subjects,
    units,
    presets,
    selectedSemesterId,
    session,
    previewNotes,
    generating,
    starting,
    submitting,
    showResumeModal,
    detectedActiveSession,
    previewingNote,
    activeNoteIndex,
    recentStats,
    loadingRecentStats,
    currentStudentId,
    isSessionFinished,
    currentNote,
    onSelectNote,
    previewQuestion,
    onSemesterChange,
    onSubjectChange,
    onGeneratePreview,
    cancelPreview,
    startFormalSession,
    onResumeSession,
    onAbandonSession,
    handleSubmitItem,
    resetSession
  };
}

