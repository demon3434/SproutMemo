<template>
  <div class="history-container">
    <!-- 左侧：历史会话列表 -->
    <div class="history-list-pane">
      <div class="pane-title-row">
        <span class="pane-title-text">📋 往期复习任务</span>
        <div class="history-filters-bar">
          <!-- 自定义年月微调/选择器 -->
          <div class="month-selector-widget">
            <button 
              type="button"
              class="widget-arrow-btn" 
              @click="shiftMonth(-1)"
              title="前一个月"
            >
              ◀
            </button>
            <div class="widget-current-text" @click.stop="toggleMonthPopup">
              📅 {{ formatFilterYearMonth(historyFilterYearMonth) }}
              <span class="widget-down-arrow">▼</span>
            </div>
            <button 
              type="button"
              class="widget-arrow-btn" 
              :disabled="isNextMonthDisabled" 
              @click="shiftMonth(1)"
              title="后一个月"
            >
              ▶
            </button>

            <!-- 极简年月弹出网格面板 -->
            <div v-if="showMonthPopup" class="month-grid-popup" @click.stop>
              <div class="popup-year-header">
                <button type="button" class="year-arrow" @click="shiftYear(-1)">◀</button>
                <span class="year-text">{{ popupYear }} 年</span>
                <button type="button" class="year-arrow" :disabled="popupYear >= currentRealYear" @click="shiftYear(1)">▶</button>
              </div>
              <div class="popup-months-grid">
                <button 
                  type="button"
                  v-for="m in 12" 
                  :key="m"
                  :class="['month-grid-item', { 
                    active: isCurrentSelected(popupYear, m),
                    disabled: isMonthInFuture(popupYear, m)
                  }]"
                  :disabled="isMonthInFuture(popupYear, m)"
                  @click="selectMonth(popupYear, m)"
                >
                  {{ m }}月
                </button>
              </div>
            </div>
          </div>
          <!-- 学科筛选 -->
          <select v-model="historyFilterSubject" class="history-filter-select" @change="resetAndReloadHistory">
            <option value="">全部学科</option>
            <option v-for="sub in subjects" :key="sub.subject_id" :value="sub.subject_id">
              {{ sub.subject?.subject_name || sub.subject_name }}
            </option>
          </select>
        </div>
      </div>
      <div v-if="historyList.length === 0" class="history-empty">
        该时间或学科下暂无复习记录
      </div>
      <div v-else class="history-items">
        <!-- 按年月倒序大分组展示 -->
        <div v-for="(items, monthGroup) in groupedHistory" :key="monthGroup" class="history-month-group">
          
          <div 
            v-for="h in items" 
            :key="h.id"
            :class="['history-row-card', { selected: selectedHistoryId === h.id }]"
            @click="loadHistoryDetail(h.id)"
          >
            <div class="row-header">
              <span class="row-date">{{ formatDate(h.created_at) }}</span>
              <span :class="['row-status', h.status.toLowerCase()]">
                {{ h.status === 'COMPLETED' ? '已完成' : '已中止' }}
              </span>
            </div>
            <div class="row-meta">
              <span>📚 {{ h.semester_name }}</span>
              <span>学科: {{ h.subject_name }}</span>
            </div>
            <div class="row-footer">
              <span>🎯 策略: {{ h.strategy_name }}</span>
              <span>进度: {{ h.completed_count }}/{{ h.target_count }} 题</span>
            </div>
          </div>
        </div>

        <!-- 加载更多分页控制 -->
        <div class="history-pagination-ctrl">
          <button 
            v-if="historyList.length < historyTotal" 
            class="btn-load-more" 
            :disabled="loadingHistory"
            @click="loadMoreHistory"
          >
            {{ loadingHistory ? '⏳ 加载中...' : '点击加载更多' }}
          </button>
          <div v-else class="no-more-history-tip">
            — 已显示全部复习历史记录 —
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧：选中会话的题目明细 -->
    <div class="history-detail-pane">
      <div class="pane-title">
        <span>🔍 复习题目明细</span>
        <span v-if="selectedHistoryDetail" class="pane-title-sub">
          （复习时间：{{ formatDate(selectedHistoryDetail.created_at) }} | 题量：{{ selectedHistoryDetail.target_count }} 题）
        </span>
      </div>
      <div v-if="!selectedHistoryDetail" class="detail-empty">
        请在左侧选择复习任务以查看详细题目
      </div>
      <div v-else class="detail-content">
        <div class="detail-questions-list">
          <div 
            v-for="(n, idx) in selectedHistoryDetail.notes" 
            :key="n.id" 
            class="detail-question-item"
            @click="$emit('preview-note', n)"
          >
            <div class="item-left">
              <span class="item-idx">{{ idx + 1 }}</span>
              <div class="item-info">
                <span class="item-title">{{ n.source_paper_name || '习题笔记' }}</span>
                <span class="item-desc">{{ n.page_number || '' }} {{ n.unit_name || '' }}</span>
              </div>
            </div>
            <div class="item-right" v-if="n.review_log">
              <span class="item-duration">⏱️ {{ n.review_log.duration_seconds.toFixed(1) }}s</span>
              <span 
                class="outcome-pill" 
                :style="getOutcomeStyle(n.review_log.outcome_code)"
              >
                {{ getOutcomeName(n.review_log.outcome_code) }}
              </span>
            </div>
            <div class="item-right" v-else>
              <span class="outcome-pill uncompleted">未答</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import { reviewService } from '../services/reviewService.js';

const props = defineProps({
  subjects: Array,
  presets: Object,
  activeChildId: String
});

defineEmits(['preview-note']);

const historyList = ref([]);
const selectedHistoryId = ref('');
const selectedHistoryDetail = ref(null);

const historyFilterSubject = ref('');
const historyFilterYearMonth = ref(getCurrentYearMonth());
const historyLimit = 10;
const historyOffset = ref(0);
const historyTotal = ref(0);
const loadingHistory = ref(false);

const showMonthPopup = ref(false);
const popupYear = ref(new Date().getFullYear());
const currentRealYear = new Date().getFullYear();
const currentRealMonth = new Date().getMonth() + 1;

function getCurrentYearMonth() {
  const d = new Date();
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  return `${year}-${month}`;
}

const groupedHistory = computed(() => {
  const groups = {};
  (historyList.value || []).forEach(item => {
    const date = new Date(item.created_at);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const key = `${year}年${month}月`;
    if (!groups[key]) {
      groups[key] = [];
    }
    groups[key].push(item);
  });
  return groups;
});

const isNextMonthDisabled = computed(() => {
  if (!historyFilterYearMonth.value) return true;
  const [yStr, mStr] = historyFilterYearMonth.value.split('-');
  const y = parseInt(yStr);
  const m = parseInt(mStr);
  return y > currentRealYear || (y === currentRealYear && m >= currentRealMonth);
});

function shiftMonth(delta) {
  if (!historyFilterYearMonth.value) return;
  const [yStr, mStr] = historyFilterYearMonth.value.split('-');
  let y = parseInt(yStr);
  let m = parseInt(mStr);
  
  m += delta;
  if (m < 1) {
    m = 12;
    y -= 1;
  } else if (m > 12) {
    m = 1;
    y += 1;
  }
  
  if (y > currentRealYear || (y === currentRealYear && m > currentRealMonth)) {
    return;
  }
  
  historyFilterYearMonth.value = `${y}-${String(m).padStart(2, '0')}`;
  resetAndReloadHistory();
}

function shiftYear(delta) {
  popupYear.value += delta;
  if (popupYear.value > currentRealYear) {
    popupYear.value = currentRealYear;
  }
}

function selectMonth(year, month) {
  historyFilterYearMonth.value = `${year}-${String(month).padStart(2, '0')}`;
  showMonthPopup.value = false;
  window.removeEventListener('click', handleGlobalClick);
  resetAndReloadHistory();
}

function isMonthInFuture(year, month) {
  if (year > currentRealYear) return true;
  if (year === currentRealYear && month > currentRealMonth) return true;
  return false;
}

function isCurrentSelected(year, month) {
  if (!historyFilterYearMonth.value) return false;
  const target = `${year}-${String(month).padStart(2, '0')}`;
  return historyFilterYearMonth.value === target;
}

function formatFilterYearMonth(val) {
  if (!val) return '';
  const [y, m] = val.split('-');
  return `${y}年${m}月`;
}

function toggleMonthPopup() {
  showMonthPopup.value = !showMonthPopup.value;
  if (showMonthPopup.value) {
    if (historyFilterYearMonth.value) {
      const [yStr] = historyFilterYearMonth.value.split('-');
      popupYear.value = parseInt(yStr);
    } else {
      popupYear.value = currentRealYear;
    }
    setTimeout(() => {
      window.addEventListener('click', handleGlobalClick);
    }, 0);
  } else {
    window.removeEventListener('click', handleGlobalClick);
  }
}

function closeMonthPopup() {
  showMonthPopup.value = false;
  window.removeEventListener('click', handleGlobalClick);
}

function handleGlobalClick(e) {
  const widget = document.querySelector('.month-selector-widget');
  if (widget && !widget.contains(e.target)) {
    closeMonthPopup();
  }
}

// 历史记录加载
async function resetAndReloadHistory() {
  historyList.value = [];
  historyOffset.value = 0;
  historyTotal.value = 0;
  selectedHistoryId.value = '';
  selectedHistoryDetail.value = null;
  await loadMoreHistory();
}

async function loadMoreHistory() {
  if (loadingHistory.value) return;
  loadingHistory.value = true;
  try {
    const data = await reviewService.getHistory({
      studentId: props.activeChildId || null,
      subjectId: historyFilterSubject.value || null,
      yearMonth: historyFilterYearMonth.value || null,
      limit: historyLimit,
      offset: historyOffset.value
    });
    const items = data.items || [];
    historyList.value = [...historyList.value, ...items];
    historyTotal.value = data.total || 0;
    historyOffset.value += items.length;
    
    if (historyList.value.length > 0 && !selectedHistoryId.value) {
      await loadHistoryDetail(historyList.value[0].id);
    }
  } catch (err) {
    console.error('加载复习历史失败:', err);
  } finally {
    loadingHistory.value = false;
  }
}

async function loadHistoryDetail(id) {
  selectedHistoryId.value = id;
  selectedHistoryDetail.value = null;
  try {
    selectedHistoryDetail.value = await reviewService.getSessionDetails(id);
  } catch (err) {
    console.error('加载历史会话详情失败:', err);
  }
}

function formatDate(isoStr) {
  if (!isoStr) return '';
  const date = new Date(isoStr);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
}

function getOutcomeName(code) {
  const item = (props.presets.outcomes || []).find(o => o.code === code);
  return item ? item.name : code;
}

function getOutcomeStyle(code) {
  if (!code) return { background: '#f1f5f9', color: '#475569' };
  const upperCode = code.toUpperCase();
  if (upperCode.includes('EASY') || upperCode.includes('MASTERED') || upperCode.includes('PASS')) {
    return { background: '#e6f4ea', color: '#137333' };
  } else if (upperCode.includes('HARD') || upperCode.includes('PARTIAL') || upperCode.includes('DOUBT')) {
    return { background: '#fef7e0', color: '#b06000' };
  } else if (upperCode.includes('WRONG') || upperCode.includes('CLUELESS') || upperCode.includes('FAIL')) {
    return { background: '#fce8e6', color: '#c5221f' };
  }
  return { background: '#e8f0fe', color: '#1a73e8' };
}

// 暴露刷新历史方法给父组件
defineExpose({
  resetAndReloadHistory
});

// 监听孩子切换自动重载
watch(() => props.activeChildId, () => {
  resetAndReloadHistory();
});

onMounted(() => {
  resetAndReloadHistory();
});

onUnmounted(() => {
  window.removeEventListener('click', handleGlobalClick);
});
</script>

<style scoped>
.history-container {
  display: flex;
  height: 100%;
  background: var(--bg-main);
  width: 100%;
}

.history-list-pane {
  width: 340px;
  border-right: 1px solid var(--border);
  background: var(--bg-card);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.pane-title-row {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  border-bottom: 1px solid var(--border);
  background: var(--bg-card);
}

.pane-title-text {
  font-size: 14.5px;
  font-weight: 800;
  color: var(--text-main);
}

.history-filters-bar {
  display: flex;
  gap: 8px;
  align-items: center;
  width: 100%;
}

.history-filter-select {
  flex: 1;
  height: 32px;
  padding: 0 8px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-card);
  color: var(--text-main);
  font-size: 12px;
  font-weight: 600;
}

.month-selector-widget {
  position: relative;
  display: flex;
  align-items: center;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-card);
  overflow: visible;
  height: 32px;
  flex: 1.2;
}

.widget-arrow-btn {
  width: 24px;
  height: 100%;
  border: none;
  background: transparent;
  color: var(--text-muted);
  font-size: 9px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: var(--transition);
}

.widget-arrow-btn:hover:not(:disabled) {
  background: var(--bg-main);
  color: var(--primary);
}

.widget-arrow-btn:disabled {
  color: var(--border);
  cursor: not-allowed;
}

.widget-current-text {
  flex: 1;
  padding: 0 6px;
  height: 100%;
  font-size: 11.5px;
  font-weight: 700;
  color: var(--text-main);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border-left: 1px solid var(--border);
  border-right: 1px solid var(--border);
  user-select: none;
  white-space: nowrap;
}

.widget-current-text:hover {
  background: var(--bg-main);
}

.widget-down-arrow {
  font-size: 8px;
  color: var(--text-muted);
}

.month-grid-popup {
  position: absolute;
  top: 36px;
  left: 0;
  width: 220px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  padding: 12px;
  z-index: 120;
  animation: widgetFadeIn 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.popup-year-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  border-bottom: 1px solid var(--border);
  padding-bottom: 6px;
}

.year-arrow {
  width: 22px;
  height: 22px;
  border: 1px solid var(--border);
  border-radius: var(--radius-xs);
  background: var(--bg-main);
  color: var(--text-main);
  font-size: 9px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.year-arrow:hover:not(:disabled) {
  border-color: var(--primary);
  color: var(--primary);
}

.year-arrow:disabled {
  color: var(--border);
  cursor: not-allowed;
}

.year-text {
  font-size: 12.5px;
  font-weight: 700;
  color: var(--text-main);
}

.popup-months-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
}

.month-grid-item {
  height: 28px;
  border: 1px solid var(--border);
  border-radius: var(--radius-xs);
  background: var(--bg-main);
  color: var(--text-main);
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: var(--transition);
}

.month-grid-item:hover:not(.disabled):not(.active) {
  border-color: var(--primary);
  color: var(--primary);
  background: var(--bg-card);
}

.month-grid-item.active {
  background: var(--primary);
  color: #ffffff;
  border-color: var(--primary);
}

.month-grid-item.disabled {
  background: var(--bg-main);
  color: var(--border);
  border-color: var(--border);
  cursor: not-allowed;
  opacity: 0.6;
}

.history-empty, .detail-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  color: var(--text-muted);
  font-size: 13px;
  padding: 20px;
}

.history-items {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.history-month-group {
  margin-bottom: 24px;
}

.month-group-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-muted);
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding-left: 6px;
  border-left: 3px solid var(--primary);
}

.history-row-card {
  padding: 14px 16px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-card);
  margin-bottom: 12px;
  cursor: pointer;
  transition: var(--transition);
  box-shadow: var(--shadow-sm);
}

.history-row-card:hover {
  border-color: var(--primary);
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.history-row-card.selected {
  border-color: var(--primary);
  background: var(--bg-card);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.08);
  position: relative;
}

.history-row-card.selected::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: var(--primary);
  border-top-left-radius: var(--radius-md);
  border-bottom-left-radius: var(--radius-md);
}

.row-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.row-date {
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 500;
}

.row-status {
  font-size: 10.5px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 12px;
}

.row-status.completed {
  background: #e6f4ea;
  color: #137333;
}

.row-status.abandoned {
  background: #fce8e6;
  color: #c5221f;
}

.row-meta {
  font-size: 12.5px;
  color: var(--text-main);
  font-weight: 700;
  margin-bottom: 6px;
  display: flex;
  gap: 8px;
}

.row-footer {
  font-size: 11.5px;
  color: var(--text-muted);
  display: flex;
  justify-content: space-between;
}

.history-pagination-ctrl {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

.btn-load-more {
  width: 100%;
  height: 34px;
  border: 1px solid var(--border);
  background: var(--bg-card);
  border-radius: var(--radius-sm);
  color: var(--text-main);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: var(--transition);
}

.btn-load-more:hover {
  background: var(--bg-main);
  border-color: var(--primary);
  color: var(--primary);
}

.no-more-history-tip {
  font-size: 11px;
  color: var(--text-muted);
  margin: 10px 0;
}

/* 详情分栏 */
.history-detail-pane {
  flex: 1;
  background: var(--bg-card);
  display: flex;
  flex-direction: column;
}

.pane-title {
  padding: 18px 24px;
  font-size: 14px;
  font-weight: 700;
  border-bottom: 1px solid var(--border);
  color: var(--text-main);
  background: var(--bg-card);
  display: flex;
  align-items: center;
}

.pane-title-sub {
  font-size: 12px;
  font-weight: normal;
  color: var(--text-muted);
  margin-left: 8px;
}

.detail-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: var(--bg-main);
}

.detail-meta-summary {
  margin-bottom: 20px;
  background: var(--bg-card);
  padding: 16px 20px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.detail-meta-summary h3 {
  font-size: 15px;
  color: var(--text-main);
  margin-bottom: 6px;
  font-weight: 700;
}

.detail-meta-summary p {
  font-size: 12px;
  color: var(--text-muted);
}

.detail-questions-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-question-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: var(--transition);
  box-shadow: var(--shadow-sm);
}

.detail-question-item:hover {
  border-color: var(--primary);
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.item-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.item-idx {
  font-weight: 700;
  color: var(--text-muted);
  font-size: 14px;
  width: 16px;
}

.item-info {
  display: flex;
  flex-direction: column;
}

.item-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-main);
}

.item-desc {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 2px;
}

.item-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.item-duration {
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 500;
}

.outcome-pill {
  font-size: 10.5px;
  padding: 2.5px 10px;
  border-radius: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.outcome-pill.uncompleted {
  background: #f1f5f9;
  color: #475569;
}

@keyframes widgetFadeIn {
  from {
    opacity: 0;
    transform: translateY(-8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
