<template>
  <div class="daily-review-view">
    <!-- 顶部 Tab 栏 -->
    <div class="view-tabs">
      <!-- 家长专属：选择孩子账号 -->
      <div v-if="currentUser?.role === 'PARENT'" class="review-child-select-group">
        <span class="child-select-label">学生账号:</span>
        <select class="child-select-input" v-model="activeChildId">
          <option v-for="student in currentUser.bound_students || []" :key="student.id" :value="student.id">
            {{ student.nickname || student.username }}
          </option>
        </select>
      </div>

      <button 
        :class="['tab-btn', { active: activeTab === 'review' }]" 
        @click="activeTab = 'review'"
      >
        {{ currentUser?.role === 'PARENT' ? '📊 复习总览' : '🎯 开始复习' }}
      </button>
      <button 
        :class="['tab-btn', { active: activeTab === 'history' }]" 
        @click="activeTab = 'history'"
      >
        📋 复习记录
      </button>
    </div>

    <!-- TAB 1: 开始复习 / 复习总览 -->
    <div v-if="activeTab === 'review'" class="tab-content review-tab">
      <!-- 家长专属：复习总览与近期统计 -->
      <div v-if="currentUser?.role === 'PARENT'" class="review-main-content">
        <ReviewPresetsStats :stats="presets.stats" :show-guide="false" />

        <!-- 近期复习统计表 -->
        <div class="recent-stats-section">
          <h3 class="recent-stats-title">📈 近期复习统计</h3>
          <div class="recent-stats-table-wrapper">
            <table class="recent-stats-table">
              <thead>
                <tr>
                  <th>时间范围</th>
                  <th>复习次数</th>
                  <th>完成次数</th>
                  <th>完成率</th>
                  <th>刷题数量</th>
                  <th>做对数量</th>
                  <th>正确率</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in recentStats || []" :key="row.range">
                  <td class="range-cell">{{ row.range }}</td>
                  <td class="font-mono">{{ row.review_count }}</td>
                  <td class="font-mono">{{ row.completed_count }}</td>
                  <td class="font-mono ratio-cell">{{ row.session_ratio }}</td>
                  <td class="font-mono">{{ row.total_questions }}</td>
                  <td class="font-mono">{{ row.correct_questions }}</td>
                  <td class="font-mono ratio-cell correct">{{ row.correct_ratio }}</td>
                </tr>
                <tr v-if="!recentStats || recentStats.length === 0">
                  <td colspan="7" class="empty-cell">暂无统计数据</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- 学生专属：抽题与练习流程 -->
      <template v-else>
        <ReviewFilterHeader
          ref="filterHeaderRef"
          :subjects="subjects"
          :units="units"
          :presets="presets"
          :loading="generating"
          :has-active-session="!!session"
          :completed-count="session ? session.completed_count : 0"
          :total-count="session ? session.target_count : 0"
          :initial-semester-id="selectedSemesterId"
          @generate="onGeneratePreview"
          @semester-change="onSemesterChange"
          @subject-change="onSubjectChange"
        />
        <div class="review-main-content">
          <!-- 状态 A: 初始空引导页 (包含复习库概览统计看板与近期统计表) -->
          <template v-if="!session && previewNotes.length === 0">
            <ReviewPresetsStats :stats="presets.stats" />

            <!-- 近期复习统计表 -->
            <div class="recent-stats-section">
              <h3 class="recent-stats-title">📈 近期复习统计</h3>
              <div class="recent-stats-table-wrapper">
                <table class="recent-stats-table">
                  <thead>
                    <tr>
                      <th>时间范围</th>
                      <th>复习次数</th>
                      <th>完成次数</th>
                      <th>完成率</th>
                      <th>刷题数量</th>
                      <th>做对数量</th>
                      <th>正确率</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="row in recentStats || []" :key="row.range">
                      <td class="range-cell">{{ row.range }}</td>
                      <td class="font-mono">{{ row.review_count }}</td>
                      <td class="font-mono">{{ row.completed_count }}</td>
                      <td class="font-mono ratio-cell">{{ row.session_ratio }}</td>
                      <td class="font-mono">{{ row.total_questions }}</td>
                      <td class="font-mono">{{ row.correct_questions }}</td>
                      <td class="font-mono ratio-cell correct">{{ row.correct_ratio }}</td>
                    </tr>
                    <tr v-if="!recentStats || recentStats.length === 0">
                      <td colspan="7" class="empty-cell">暂无统计数据</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <!-- 准备就绪引导卡片 -->
            <div class="stats-footer-guide-bar">
              <div class="guide-lightbulb">💡</div>
              <div class="guide-text">
                <h4>准备就绪！</h4>
                <p>请在上方选择学期、科目与策略，然后点击右侧的 <strong>“🚀 抽题复习”</strong> 按钮来规划当前复习任务。</p>
              </div>
            </div>
          </template>

          <!-- 状态 B: 生成了预览，但还未正式“开始答题” -->
          <div v-else-if="!session && previewNotes.length > 0" class="preview-state">
            <div class="preview-card">
              <div class="preview-header">
                <h3>📋 本次复习题目大纲 (共 {{ previewNotes.length }} 题)</h3>
                <p>题目已准备就绪，请让孩子准备好纸笔，点击“开始答题”进入计时解题环节。</p>
              </div>
              <div class="preview-list">
                <div v-for="(note, idx) in previewNotes" :key="note.id" class="preview-item">
                  <span class="preview-index">{{ idx + 1 }}</span>
                  <div class="preview-info">
                    <span class="preview-source">{{ note.source_paper_name || '习题笔记' }}</span>
                    <span class="preview-desc">{{ note.page_number || '' }} {{ note.unit_name || '' }}</span>
                  </div>
                  <span class="preview-stars">⭐ {{ note.difficulty_stars }}</span>
                </div>
              </div>
              <div class="preview-actions">
                <button class="btn-start" :disabled="starting" @click="startFormalSession">
                  {{ starting ? '正在建立任务...' : '✍️ 开始答题' }}
                </button>
                <button class="btn-cancel-preview" @click="cancelPreview">重新选择</button>
              </div>
            </div>
          </div>

          <!-- 状态 C: 会话已完成打卡总结 -->
          <div v-else-if="session && (session.status === 'COMPLETED' || isSessionFinished)" class="completed-state">
            <div class="summary-card">
              <div class="summary-icon">🏆</div>
              <h2>恭喜！完成本次复习打卡！</h2>
              <p class="summary-meta">
                本次共完成 <strong>{{ session.target_count }}</strong> 道题目复习打卡，复习轨迹已存入数据库。
              </p>
              <button class="restart-btn" @click="resetSession">
                🔄 再次抽题复习
              </button>
            </div>
          </div>

          <!-- 状态 D: 单题沉浸式卡片练习中 -->
          <div v-else-if="currentNote" class="card-viewport">
            <ReviewCard
              :note="currentNote"
              :current-index="activeNoteIndex"
              :total-count="session.target_count"
              :completed-count="session.completed_count"
              :notes-list="session.notes"
              :submitting="submitting"
              :outcomes="presets.outcomes"
              @submit-item="handleSubmitItem"
              @select-note="onSelectNote"
            />
          </div>
        </div>
      </template>
    </div>

    <!-- TAB 2: 复习记录 -->
    <div v-else class="tab-content history-tab">
      <ReviewHistoryPane 
        :subjects="subjects"
        :presets="presets"
        :active-child-id="activeChildId"
        @preview-note="previewQuestion"
      />
    </div>

    <!-- 详情弹窗 -->
    <NoteDetailModal
      :visible="!!previewingNote"
      :note="previewingNote"
      :review-log="previewingNote ? previewingNote.review_log : null"
      :outcomes="presets.outcomes"
      @close="previewingNote = null"
    />

    <!-- 继续答题提示弹窗 -->
    <ResumeSessionModal
      :visible="showResumeModal"
      :session="detectedActiveSession"
      @confirm="onResumeSession"
      @cancel="onAbandonSession"
    />
  </div>
</template>

<script setup>
import { ref, computed, inject, onMounted, watch } from 'vue';
import { api } from '../services/api.js';
import { reviewService } from '../services/reviewService.js';
import ReviewFilterHeader from '../components/ReviewFilterHeader.vue';
import ReviewCard from '../components/ReviewCard.vue';
import NoteDetailModal from './NoteDetailModal.vue';
import ResumeSessionModal from '../components/ResumeSessionModal.vue';
import ReviewPresetsStats from '../components/ReviewPresetsStats.vue';
import ReviewHistoryPane from '../components/ReviewHistoryPane.vue';
import { calculateCurrentSemester } from '../utils/semester.js';
import { showAlert } from '../services/dialog.js';

const activeChildId = inject('activeChildId', ref(''));
const currentUser = inject('currentUser', ref(null));

const activeTab = ref('review'); // review or history
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

// 活动任务找回
const showResumeModal = ref(false);
const detectedActiveSession = ref(null);

// 详情弹窗
const previewingNote = ref(null);
const activeNoteIndex = ref(0);

// 家长近期复习统计数据
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

// 监听当前切换的学生/孩子 (包含首次加载)
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

async function loadInitialData() {
  try {
    presets.value = await reviewService.getPresets(currentStudentId.value || null);
    
    // 加载近期统计（学生和家长均加载）
    await loadRecentStats();
    
    // 加载全部学期
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
    
    // 直接用服务器返回的最新完整会话数据更新本地状态
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
</script>

<style scoped>
.daily-review-view {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 100px);
  margin: -32px;
  background: var(--bg-main);
  overflow: hidden;
}

.view-tabs {
  display: flex;
  gap: 8px;
  padding: 16px 24px 0 24px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border);
}

.tab-btn {
  padding: 10px 20px;
  font-size: 13.5px;
  font-weight: 700;
  color: var(--text-muted);
  background: transparent;
  border: none;
  border-bottom: 3px solid transparent;
  cursor: pointer;
  transition: var(--transition);
}

.tab-btn:hover {
  color: var(--primary);
}

.tab-btn.active {
  color: var(--primary);
  border-bottom-color: var(--primary);
}

.tab-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.review-tab {
  height: 100%;
}

.review-main-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: var(--bg-main);
}

/* 预览卡片 */
.preview-state {
  max-width: 800px;
  margin: 0 auto;
}

.preview-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
  padding: 24px;
}

.preview-header h3 {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-main);
  margin-bottom: 6px;
}

.preview-header p {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 20px;
}

.preview-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 360px;
  overflow-y: auto;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 12px;
  background: var(--bg-main);
  margin-bottom: 20px;
}

.preview-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
}

.preview-index {
  font-weight: 700;
  color: var(--text-muted);
  width: 20px;
}

.preview-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.preview-source {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-main);
}

.preview-desc {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 2px;
}

.preview-stars {
  font-size: 12px;
  color: var(--primary);
  font-weight: 600;
}

.preview-actions {
  display: flex;
  gap: 12px;
}

.btn-start {
  flex: 2;
  height: 38px;
  background: var(--primary);
  color: #ffffff;
  border: none;
  border-radius: var(--radius-sm);
  font-weight: 700;
  cursor: pointer;
  transition: var(--transition);
}

.btn-start:hover {
  background: var(--primary-hover);
}

.btn-cancel-preview {
  flex: 1;
  height: 38px;
  background: var(--bg-main);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  color: var(--text-main);
  cursor: pointer;
  transition: var(--transition);
}

.btn-cancel-preview:hover {
  background: var(--border);
}

/* 总结完成卡片 */
.completed-state {
  max-width: 500px;
  margin: 40px auto;
}

.completed-state .summary-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  padding: 40px;
  text-align: center;
}

.summary-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.summary-card h2 {
  font-size: 18px;
  font-weight: 800;
  color: var(--text-main);
  margin-bottom: 8px;
}

.summary-meta {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 24px;
  line-height: 1.5;
}

.restart-btn {
  padding: 10px 24px;
  background: var(--primary);
  color: #ffffff;
  border: none;
  border-radius: var(--radius-sm);
  font-weight: 700;
  cursor: pointer;
  transition: var(--transition);
}

.restart-btn:hover {
  background: var(--primary-hover);
}

.card-viewport {
  height: 100%;
}

.history-tab {
  flex: 1;
  overflow: hidden;
}

/* 家长端孩子切换下拉框 */
.review-child-select-group {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-right: 20px;
}

.child-select-label {
  font-size: 13.5px;
  font-weight: 700;
  color: var(--text-main);
}

.child-select-input {
  height: 34px;
  padding: 0 32px 0 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background-color: var(--bg-input);
  color: var(--text-main);
  font-size: 13px;
  font-weight: 600;
  outline: none;
  cursor: pointer;
  appearance: none;
  -webkit-appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' fill='none' stroke='%23475569' stroke-width='2' viewBox='0 0 24 24'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' d='M19 9l-7 7-7-7'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  background-size: 12px;
  transition: var(--transition);
}

.child-select-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 2px var(--primary-light);
}

/* 近期复习统计板块 */
.recent-stats-section {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  padding: 24px;
  margin: 20px auto 0 auto;
  max-width: 1000px;
  width: 100%;
}

.recent-stats-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-main);
  margin-bottom: 16px;
  border-left: 4px solid var(--primary);
  padding-left: 10px;
}

.recent-stats-table-wrapper {
  overflow-x: auto;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border);
}

.recent-stats-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 13.5px;
}

.recent-stats-table th {
  background: var(--bg-main);
  color: var(--text-muted);
  font-weight: 700;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
}

.recent-stats-table td {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
  color: var(--text-main);
  font-weight: 500;
}

.recent-stats-table tr:last-child td {
  border-bottom: none;
}

.recent-stats-table tr:hover {
  background: var(--bg-main);
}

.range-cell {
  font-weight: 700;
  color: var(--text-main);
}

.ratio-cell {
  font-weight: 700;
  color: var(--text-muted);
}

.ratio-cell.correct {
  color: var(--primary);
}

.empty-cell {
  text-align: center;
  color: var(--text-muted);
  padding: 24px !important;
}

/* 底部引导栏 */
.stats-footer-guide-bar {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 20px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: var(--shadow-sm);
  margin: 20px auto 0 auto;
  max-width: 1000px;
  width: 100%;
}

.guide-lightbulb {
  font-size: 32px;
}

.guide-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.guide-text h4 {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-main);
}

.guide-text p {
  font-size: 12.5px;
  color: var(--text-muted);
  line-height: 1.4;
}
</style>
