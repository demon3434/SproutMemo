<template>
  <div class="books-manage">

    <!-- ── 控制面板（阶段与学期） ── -->
    <div class="control-panel">
      <div class="filter-selectors">
        <div class="selector-group">
          <span class="panel-label">学习阶段：</span>
          <StationerySelect
            v-model="selectedStageId"
            :options="(activeStages || []).map(s => ({ value: s.id, label: s.name }))"
            compact
            style="min-width: 120px;"
            @change="onStageChange"
          />
        </div>

        <div class="selector-group">
          <span class="panel-label">学期：</span>
          <StationerySelect
            v-model="selectedSemesterId"
            :options="[{ value: '', label: '-- 选择学期 --' }, ...(filteredSemesters || []).map(s => ({ value: s.id, label: s.name }))]"
            compact
            style="min-width: 150px;"
            @change="onSemesterChange"
          />
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
        该学期暂未配置任何关联学科，请切换学期或联系管理员。
      </div>
      <div v-else class="empty-subjects-tip">
        请选择学习阶段和学期以查看学科列表。
      </div>
    </div>

    <!-- ── 习题册主内容区 ── -->
    <div v-if="selectedSubjectId" class="main-card">
      <!-- 快捷添加表单 -->
      <BookQuickAdd :error="addError" @add="handleAdd" />

      <!-- 习题册列表 -->
      <div class="books-list-section">
        <div class="section-title-row">
          <span class="icon"><AppIcon name="list" :size="18" /></span>
          <h3 class="title">已录入的习题册</h3>
          <span class="subtitle">（支持上下调整顺序、停用或修改）</span>
        </div>

        <div v-if="books.length === 0" class="empty-books-box">
          <div class="empty-icon"><AppIcon name="book" :size="36" /></div>
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
      <div class="welcome-icon"><AppIcon name="book" :size="40" /></div>
      <h3>请选择学期以及上方的学科选项卡</h3>
      <p>选择完成后即可在此录入并整理您的个人习题册，错题笔记将与之关联。</p>
    </div>

  </div>
</template>

<script setup>
import { onMounted, inject } from 'vue';
import { useBookShelf } from '../composables/useBookShelf.js';
import BookItemCard from '../components/BookItemCard.vue';
import BookQuickAdd from '../components/BookQuickAdd.vue';
import StationerySelect from '../components/StationerySelect.vue';
import AppIcon from '../components/AppIcon.vue';

const currentUser = inject('currentUser');

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
  onStageChange,
  onSemesterChange,
  selectSubject,
  handleAdd,
  handleMove,
  handleEdit,
  handleToggle,
  handleDelete
} = useBookShelf();

onMounted(() => {
  const profile = currentUser.value?.student_profile;
  const enrollYear = profile?.enrollment_year || currentUser.value?.enrollment_year;
  const semOffset = profile?.semester_offset ?? currentUser.value?.semester_offset ?? 0;
  loadInitData(currentUser.value?.id, enrollYear, semOffset);
});
</script>

<style scoped>
@import '../assets/styles/books-manage.css';
</style>
