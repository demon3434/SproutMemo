import { ref, computed } from 'vue';
import { api } from '../services/api.js';
import { calculateCurrentSemester } from '../utils/semester.js';
import { showConfirm, showPrompt, showAlert } from '../services/dialog.js';

export function useBookShelf() {
  const activeStages = ref([]);
  const allSemesters = ref([]);
  const subjects = ref([]);
  const books = ref([]);

  const selectedStageId = ref('');
  const selectedSemesterId = ref('');
  const selectedSubjectId = ref(null);

  const newName = ref('');
  const addError = ref('');
  const currentStudentId = ref(null);

  const filteredSemesters = computed(() => {
    if (!selectedStageId.value) return [];
    return allSemesters.value.filter(sem => sem.stage_id === selectedStageId.value);
  });

  async function loadInitData(studentId, enrollmentYear, semesterOffset = 0) {
    currentStudentId.value = studentId;
    try {
      const [stageRes, semRes] = await Promise.all([
        api.get('/stages?only_active=true'),
        api.get('/semesters?only_active=true')
      ]);

      if (stageRes.ok) {
        activeStages.value = (await stageRes.json()).data.items || [];
      }
      if (semRes.ok) {
        allSemesters.value = (await semRes.json()).data.items || [];
      }

      if (enrollmentYear) {
        const semName = calculateCurrentSemester(enrollmentYear, semesterOffset);
        const matchedSem = allSemesters.value.find(s => s.name === semName);

        if (matchedSem) {
          selectedStageId.value = matchedSem.stage_id || '';
          selectedSemesterId.value = matchedSem.id;
        } else if (activeStages.value.length > 0) {
          selectedStageId.value = activeStages.value[0].id;
          const sems = allSemesters.value.filter(s => s.stage_id === selectedStageId.value);
          selectedSemesterId.value = sems.length > 0 ? sems[0].id : '';
        }
      } else if (activeStages.value.length > 0) {
        selectedStageId.value = activeStages.value[0].id;
        const sems = allSemesters.value.filter(s => s.stage_id === selectedStageId.value);
        selectedSemesterId.value = sems.length > 0 ? sems[0].id : '';
      }

      await fetchSubjects();
    } catch (err) {
      console.error('Failed to load initial bookshelf data:', err);
    }
  }

  function setStudent(studentId, enrollmentYear, semesterOffset = 0) {
    currentStudentId.value = studentId;
    if (enrollmentYear && allSemesters.value.length > 0) {
      const semName = calculateCurrentSemester(enrollmentYear, semesterOffset);
      const matchedSem = allSemesters.value.find(s => s.name === semName);
      if (matchedSem) {
        selectedStageId.value = matchedSem.stage_id || '';
        selectedSemesterId.value = matchedSem.id;
      }
    }
    fetchSubjects();
  }

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
    selectedSubjectId.value = null;
    books.value = [];

    if (!selectedSemesterId.value) return;
    try {
      const res = await api.get(`/semester_settings/subjects?semester_id=${selectedSemesterId.value}&only_active=true`);
      if (res.ok) {
        subjects.value = (await res.json()).data.items || [];
        if (subjects.value.length > 0) {
          selectSubject(subjects.value[0].subject_id);
        }
      }
    } catch (err) {
      console.error('Failed to fetch subjects:', err);
    }
  }

  function selectSubject(subjectId) {
    selectedSubjectId.value = subjectId;
    fetchBooks();
  }

  async function fetchBooks() {
    if (!currentStudentId.value || !selectedSemesterId.value || !selectedSubjectId.value) {
      books.value = [];
      return;
    }
    try {
      const url = `/source_books?student_id=${currentStudentId.value}&semester_id=${selectedSemesterId.value}&subject_id=${selectedSubjectId.value}&only_active=false&include_deleted=false`;
      const res = await api.get(url);
      if (res.ok) {
        books.value = (await res.json()).data.items || [];
      }
    } catch (err) {
      console.error('Failed to fetch books:', err);
    }
  }

  async function handleAdd(bookName) {
    addError.value = '';
    const nameToUse = (bookName || newName.value || '').trim();
    if (!nameToUse) return false;

    const body = {
      student_id: currentStudentId.value,
      semester_id: selectedSemesterId.value,
      subject_id: selectedSubjectId.value,
      name: nameToUse
    };

    try {
      const res = await api.post('/source_books', body);
      if (res.ok) {
        newName.value = '';
        await fetchBooks();
        return true;
      } else {
        const data = await res.json();
        addError.value = data.msg || '添加失败';
        return false;
      }
    } catch (err) {
      addError.value = '网络错误';
      return false;
    }
  }

  async function handleMove(book, direction) {
    try {
      const res = await api.post(`/source_books/${book.id}/move?direction=${direction}`, {});
      if (res.ok) {
        await fetchBooks();
      }
    } catch (err) {
      console.error('Failed to move book:', err);
    }
  }

  async function handleEdit(book) {
    const name = await showPrompt('请输入习题册新名称:', book.name);
    if (name === null || !name.trim()) return;
    try {
      const res = await api.put(`/source_books/${book.id}`, { name: name.trim() });
      if (res.ok) {
        await fetchBooks();
      }
    } catch (err) {
      console.error('Failed to rename book:', err);
    }
  }

  async function handleToggle(book) {
    try {
      const res = await api.put(`/source_books/${book.id}`, { is_active: !book.is_active });
      if (res.ok) {
        await fetchBooks();
      }
    } catch (err) {
      console.error('Failed to toggle book active status:', err);
    }
  }

  async function handleDelete(book) {
    const ok = await showConfirm(`确定删除习题册 "${book.name}" 吗？\n删除后关联此习题册的错题记录将解除绑定。`);
    if (!ok) return;
    try {
      const res = await api.delete(`/source_books/${book.id}`);
      if (res.ok) {
        await fetchBooks();
      } else {
        await showAlert('删除失败');
      }
    } catch (err) {
      console.error('Failed to delete book:', err);
    }
  }

  return {
    activeStages,
    allSemesters,
    filteredSemesters,
    subjects,
    books,
    selectedStageId,
    selectedSemesterId,
    selectedSubjectId,
    newName,
    addError,
    loadInitData,
    setStudent,
    onStageChange,
    onSemesterChange,
    selectSubject,
    fetchBooks,
    handleAdd,
    handleMove,
    handleEdit,
    handleToggle,
    handleDelete
  };
}
