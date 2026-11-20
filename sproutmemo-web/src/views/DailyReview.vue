<template>
  <div class="daily-review-view">
    <!-- 顶部 Tab 栏 -->
    <div class="view-tabs">
      <!-- 家长专属：选择孩子账号 -->
      <div v-if="currentUser?.role === 'PARENT'" class="review-child-select-group">
        <span class="child-select-label">学生账号:</span>
        <StationerySelect
          v-model="activeChildId"
          :options="(currentUser.bound_students || []).map(s => ({ value: s.id, label: s.nickname || s.username }))"
          compact
          style="width: 150px;"
        />
      </div>

      <button 
        :class="['tab-btn', { active: activeTab === 'review' }]" 
        @click="activeTab = 'review'"
      >
        <AppIcon :name="currentUser?.role === 'PARENT' ? 'chart' : 'target'" :size="15" />
        <span>{{ currentUser?.role === 'PARENT' ? '复习总览' : '开始复习' }}</span>
      </button>
      <button 
        :class="['tab-btn', { active: activeTab === 'history' }]" 
        @click="activeTab = 'history'"
      >
        <AppIcon name="list" :size="15" />
        <span>复习记录</span>
      </button>
    </div>

    <!-- TAB 1: 开始复习 / 复习总览 -->
    <div v-if="activeTab === 'review'" class="tab-content review-tab">
      <!-- 家长专属：复习总览与近期统计 -->
      <div v-if="currentUser?.role === 'PARENT'" class="review-main-content">
        <ReviewPresetsStats :stats="presets.stats" :show-guide="false" />

        <!-- 近期复习统计表 -->
        <div class="recent-stats-section">
          <h3 class="recent-stats-title" style="display:flex; align-items:center; gap:6px;"><AppIcon name="chart" :size="18" /> 近期复习统计</h3>
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
              <h3 class="recent-stats-title" style="display:flex; align-items:center; gap:6px;"><AppIcon name="chart" :size="18" /> 近期复习统计</h3>
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
              <div class="guide-lightbulb"><AppIcon name="mastery-partial" :size="26" /></div>
              <div class="guide-text">
                <h4>准备就绪！</h4>
                <p>请在上方选择学期、科目与策略，然后点击右侧的 <strong>“抽题复习”</strong> 按钮来规划当前复习任务。</p>
              </div>
            </div>
          </template>

          <!-- 状态 B: 生成了预览，但还未正式“开始答题” -->
          <div v-else-if="!session && previewNotes.length > 0" class="preview-state">
            <div class="preview-card">
              <div class="preview-header">
                <h3 style="display:flex; align-items:center; gap:6px;"><AppIcon name="list" :size="18" /> 本次复习题目大纲 (共 {{ previewNotes.length }} 题)</h3>
                <p>题目已准备就绪，请让孩子准备好纸笔，点击“开始答题”进入计时解题环节。</p>
              </div>
              <div class="preview-list">
                <div v-for="(note, idx) in previewNotes" :key="note.id" class="preview-item">
                  <span class="preview-index">{{ idx + 1 }}</span>
                  <div class="preview-info">
                    <span class="preview-source">{{ note.source_paper_name || '习题笔记' }}</span>
                    <span class="preview-desc">{{ note.page_number || '' }} {{ note.unit_name || '' }}</span>
                  </div>
                  <span class="preview-stars" style="display:inline-flex; align-items:center; gap:4px;"><AppIcon name="star-filled" :size="15" /> {{ note.difficulty_stars }}</span>
                </div>
              </div>
              <div class="preview-actions">
                <button class="btn-start" :disabled="starting" @click="startFormalSession" style="display:inline-flex; align-items:center; gap:6px; justify-content:center;">
                  <AppIcon name="pen" :size="16" />
                  <span>{{ starting ? '正在建立任务...' : '开始答题' }}</span>
                </button>
                <button class="btn-cancel-preview" @click="cancelPreview">重新选择</button>
              </div>
            </div>
          </div>

          <!-- 状态 C: 会话已完成打卡总结 -->
          <div v-else-if="session && (session.status === 'COMPLETED' || isSessionFinished)" class="completed-state">
            <div class="summary-card">
              <div class="summary-icon"><AppIcon name="mastery-mastered" :size="48" /></div>
              <h2>恭喜！完成本次复习打卡！</h2>
              <p class="summary-meta">
                本次共完成 <strong>{{ session.target_count }}</strong> 道题目复习打卡，复习轨迹已存入数据库。
              </p>
              <button class="restart-btn" @click="resetSession" style="display:inline-flex; align-items:center; gap:6px; justify-content:center;">
                <AppIcon name="target" :size="16" />
                <span>再次抽题复习</span>
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
import ReviewFilterHeader from '../components/ReviewFilterHeader.vue';
import ReviewCard from '../components/ReviewCard.vue';
import NoteDetailModal from './NoteDetailModal.vue';
import ResumeSessionModal from '../components/ResumeSessionModal.vue';
import ReviewPresetsStats from '../components/ReviewPresetsStats.vue';
import ReviewHistoryPane from '../components/ReviewHistoryPane.vue';
import AppIcon from '../components/AppIcon.vue';
import StationerySelect from '../components/StationerySelect.vue';
import { useDailyReview } from './useDailyReview.js';

const {
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
} = useDailyReview();
</script>

<style scoped src='./DailyReview.css'></style>
