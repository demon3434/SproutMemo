<template>
  <div class="books-manage">

    <!-- ── 控制面板 ── -->
    <div class="control-panel">
      <!-- 孩子切换 Tab -->
      <div class="child-selector" v-if="children.length > 0">
        <span class="panel-label">关联孩子：</span>
        <div class="child-tabs">
          <button
            v-for="child in children"
            :key="child.id"
            class="child-tab"
            :class="{ active: selectedChildId === child.id }"
            @click="selectChild(child)"
          >
            <span class="tab-avatar">{{ (child.nickname || child.username).charAt(0).toUpperCase() }}</span>
            <span>{{ child.nickname || child.username }}</span>
          </button>
        </div>
      </div>

      <!-- 阶段与学期联动下拉框 -->
      <div class="filter-selectors">
        <div class="selector-group">
          <span class="panel-label">学习阶段：</span>
          <select v-model="selectedStageId" class="custom-select" @change="onStageChange">
            <option v-for="stage in activeStages" :key="stage.id" :value="stage.id">
              {{ stage.name }}
            </option>
          </select>
        </div>

        <div class="selector-group">
          <span class="panel-label">学期：</span>
          <select v-model="selectedSemesterId" class="custom-select" @change="onSemesterChange">
            <option value="">-- 选择学期 --</option>
            <option v-for="sem in filteredSemesters" :key="sem.id" :value="sem.id">
              {{ sem.name }}
            </option>
          </select>
        </div>
      </div>
    </div>

    <!-- ── 学科选项卡 ── -->
    <div class="subject-bar">
      <div v-if="subjects.length > 0" class="subject-tabs">
        <button
          v-for="sub in subjects"
          :key="sub.subject_id"
          class="subject-tab"
          :class="{ active: selectedSubjectId === sub.subject_id }"
          @click="selectSubject(sub.subject_id)"
        >
          <span class="subject-name">{{ sub.subject?.subject_name || '未知' }}</span>
          <span class="book-badge" v-if="books.length > 0 && selectedSubjectId === sub.subject_id">{{ books.length }}</span>
        </button>
      </div>
      <div v-else-if="selectedSemesterId" class="empty-subjects-tip">
        ⚠️ 该学期暂未配置任何关联学科，请切换学期或联系管理员。
      </div>
      <div v-else class="empty-subjects-tip">
        💡 请选择学习阶段和学期以查看学科列表。
      </div>
    </div>

    <!-- ── 习题册主内容区 ── -->
    <div v-if="selectedSubjectId" class="main-card">
      <!-- 快捷添加表单 -->
      <BookQuickAdd :error="addError" @add="handleAdd" />

      <!-- 习题册列表 -->
      <div class="books-list-section">
        <div class="section-title-row">
          <span class="icon">📋</span>
          <h3 class="title">已录入的习题册</h3>
          <span class="subtitle">（支持上下调整顺序、停用或修改）</span>
        </div>

        <div v-if="books.length === 0" class="empty-books-box">
          <div class="empty-icon">📖</div>
          <p class="empty-text">该学科下暂无录入的习题册，请在上方快速添加。</p>
        </div>

        <div v-else class="books-grid">
          <BookItemCard
            v-for="(book, idx) in books"
            :key="book.id"
            :book="book"
            :index="idx"
            :totalCount="books.length"
            @move="handleMove"
            @edit="handleEdit"
            @toggle="handleToggle"
            @delete="handleDelete"
          />
        </div>
      </div>
    </div>

    <!-- 未选择学科时的友好提示 -->
    <div v-else class="welcome-box">
      <div class="welcome-icon">📘</div>
      <h3>请选择学期以及上方的学科选项卡</h3>
      <p>选择完成后即可在此录入并整理孩子的习题册，错题笔记将与之关联。</p>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { api } from '../services/api.js';
import { useBookShelf } from '../composables/useBookShelf.js';
import BookItemCard from '../components/BookItemCard.vue';
import BookQuickAdd from '../components/BookQuickAdd.vue';

const children = ref([]);
const selectedChildId = ref('');

const {
  activeStages,
  filteredSemesters,
  subjects,
  books,
  selectedStageId,
  selectedSemesterId,
  selectedSubjectId,
  addError,
  loadInitData,
  setStudent,
  onStageChange,
  onSemesterChange,
  selectSubject,
  handleAdd,
  handleMove,
  handleEdit,
  handleToggle,
  handleDelete
} = useBookShelf();

async function loadChildren() {
  try {
    const res = await api.get('/parent/children');
    if (res.ok) {
      children.value = (await res.json()).data.items || [];
      if (children.value.length > 0) {
        selectChild(children.value[0]);
      }
    }
  } catch (err) {
    console.error('Failed to load children:', err);
  }
}

function selectChild(child) {
  selectedChildId.value = child.id;
  const profile = child.student_profile;
  const enrollYear = profile?.enrollment_year || child.enrollment_year;
  const semOffset = profile?.semester_offset ?? child.semester_offset ?? 0;
  if (!activeStages.value.length) {
    loadInitData(child.id, enrollYear, semOffset);
  } else {
    setStudent(child.id, enrollYear, semOffset);
  }
}

onMounted(() => {
  loadChildren();
});
</script>

<style scoped>
@import '../assets/styles/books-manage.css';
</style>
