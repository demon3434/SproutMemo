<template>
  <div class="history-container">
    <!-- 左侧：历史会话列表 -->
    <div class="history-list-pane">
      <div class="pane-title-row">
        <span class="pane-title-text" style="display:inline-flex; align-items:center; gap:6px;"><AppIcon name="list" :size="16" /> 往期复习任务</span>
        <div class="history-filters-bar">
          <!-- 自定义年月微调/选择器 -->
          <div class="month-selector-widget">
            <button 
              type="button"
              class="widget-arrow-btn" 
              @click="shiftMonth(-1)"
              title="前一个月"
            >
              <AppIcon name="chevron-left" :size="14" />
            </button>
            <div class="widget-current-text" @click.stop="toggleMonthPopup" style="display:inline-flex; align-items:center; gap:5px;">
              <AppIcon name="calendar" :size="14" />
              <span>{{ formatFilterYearMonth(historyFilterYearMonth) }}</span>
              <AppIcon name="chevron-down" :size="13" />
            </div>
            <button 
              type="button"
              class="widget-arrow-btn" 
              :disabled="isNextMonthDisabled" 
              @click="shiftMonth(1)"
              title="后一个月"
            >
              <AppIcon name="chevron-right" :size="14" />
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
          <StationerySelect
            v-model="historyFilterSubject"
            :options="[{ value: '', label: '全部学科' }, ...(subjects || []).map(s => ({ value: s.subject_id, label: s.subject?.subject_name || s.subject_name }))]"
            compact
            style="width: 140px;"
            @change="resetAndReloadHistory"
          />
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
              <span style="display:inline-flex; align-items:center; gap:4px;"><AppIcon name="book" :size="13" /> {{ h.semester_name }}</span>
              <span>学科: {{ h.subject_name }}</span>
            </div>
            <div class="row-footer">
              <span style="display:inline-flex; align-items:center; gap:4px;"><AppIcon name="target" :size="13" /> 策略: {{ h.strategy_name }}</span>
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
            {{ loadingHistory ? '加载中...' : '点击加载更多' }}
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
        <span style="display:inline-flex; align-items:center; gap:6px;"><AppIcon name="search" :size="16" /> 复习题目明细</span>
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
import StationerySelect from './StationerySelect.vue';
import AppIcon from './AppIcon.vue';

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

<style scoped src='./ReviewHistoryPane.css'></style>
