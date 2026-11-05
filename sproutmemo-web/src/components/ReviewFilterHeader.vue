<template>
  <div class="review-filter-header" :class="{ collapsed: isCollapsed }">
    <!-- 收缩极简状态栏 -->
    <div v-if="isCollapsed" class="collapsed-bar">
      <div class="status-tags">
        <span class="tag-pill current-tag">📚 {{ currentSemesterName }}</span>
        <span class="tag-pill" v-if="currentSubjectName">学科: {{ currentSubjectName }}</span>
        <span class="tag-pill" v-if="currentUnitName">单元: {{ currentUnitName }}</span>
        <span class="tag-pill strategy-tag">🎯 {{ currentStrategyName }}</span>
        <span class="tag-pill progress-tag">进度: {{ completedCount }} / {{ totalCount }} 题</span>
      </div>
      <button class="expand-btn" @click="toggleCollapse">
        ⚙️ 调整参数
      </button>
    </div>

    <!-- 展开内联单行控制栏 -->
    <div v-else class="expanded-bar">
      <div class="filter-inputs">
        <!-- 学习阶段 (Stage) -->
        <div class="filter-item">
          <label>学习阶段</label>
          <select v-model="form.stageId" @change="onStageChange">
            <option value="">全部阶段</option>
            <option v-for="st in presets.stages" :key="st.id" :value="st.id">{{ st.name }}</option>
          </select>
        </div>

        <!-- 学期 (Semester) -->
        <div class="filter-item">
          <label>学期</label>
          <select v-model="form.semesterId" @change="onSemesterChange">
            <option v-for="s in filteredSemesters" :key="s.id" :value="s.id">{{ s.name }}</option>
          </select>
        </div>

        <!-- 学科 (Subject) -->
        <div class="filter-item">
          <label>学科</label>
          <select v-model="form.subjectId" @change="onSubjectChange">
            <option value="">全部学科</option>
            <option v-for="sub in subjects" :key="sub.id" :value="sub.subject_id">
              {{ sub.subject?.subject_name || '未知' }}
            </option>
          </select>
        </div>

        <!-- 单元/阶段 (Unit) -->
        <div class="filter-item">
          <label>单元/阶段</label>
          <select v-model="form.unitCode">
            <option value="">全部单元</option>
            <option v-for="u in units" :key="u.id" :value="u.id">{{ u.name }}</option>
          </select>
        </div>

        <!-- 题量档位 (自定义高保真下拉选择器，悬浮项出气泡) -->
        <div class="filter-item">
          <label>题量档位</label>
          <div class="custom-select" ref="countSelectRef">
            <div class="select-trigger" @click="toggleCountDropdown">
              <span>{{ getPresetShowName(selectedCountPreset) }}</span>
              <span class="arrow-icon">▼</span>
            </div>
            <div v-show="isCountDropdownOpen" class="select-options">
              <div 
                v-for="p in presets.count_presets" 
                :key="p.code" 
                class="select-option tooltip-box-left"
                :data-tooltip="`复习量: ${p.count_value} 题`"
                :class="{ active: form.countPresetCode === p.code }"
                @click="selectCountPreset(p.code)"
              >
                {{ getPresetShowName(p) }}
              </div>
            </div>
          </div>
        </div>

        <!-- 筛题策略 (自定义高保真下拉选择器，悬浮项出气泡) -->
        <div class="filter-item">
          <label>筛题策略</label>
          <div class="custom-select" ref="strategySelectRef">
            <div class="select-trigger" @click="toggleStrategyDropdown">
              <span>{{ selectedStrategyName }}</span>
              <span class="arrow-icon">▼</span>
            </div>
            <div v-show="isStrategyDropdownOpen" class="select-options">
              <div 
                v-for="st in presets.strategies" 
                :key="st.code" 
                class="select-option tooltip-box-left"
                :data-tooltip="st.description || st.name"
                :class="{ active: form.strategyCode === st.code }"
                @click="selectStrategy(st.code)"
              >
                {{ st.name }}
              </div>
            </div>
          </div>
        </div>

        <button class="generate-btn" :disabled="loading" @click="handleGenerate">
          {{ loading ? '抽题中...' : '🚀 抽题复习' }}
        </button>
      </div>

      <button v-if="hasActiveSession" class="close-expand-btn" @click="toggleCollapse">
        ▲ 收起
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive, watch, onMounted, onUnmounted } from 'vue';

const props = defineProps({
  subjects: { type: Array, default: () => [] },
  units: { type: Array, default: () => [] },
  presets: {
    type: Object,
    default: () => ({ strategies: [], count_presets: [], outcomes: [], stages: [], semesters: [] })
  },
  loading: { type: Boolean, default: false },
  hasActiveSession: { type: Boolean, default: false },
  completedCount: { type: Number, default: 0 },
  totalCount: { type: Number, default: 0 },
  initialSemesterId: { type: String, default: '' }
});

const emit = defineEmits(['generate', 'semester-change', 'subject-change']);

const isCollapsed = ref(false);

const form = reactive({
  stageId: '',
  semesterId: '',
  subjectId: '',
  unitCode: '',
  countPresetCode: 'PRESET_5',
  strategyCode: 'EBBINGHAUS'
});

// 自定义模拟下拉框的状态
const isCountDropdownOpen = ref(false);
const isStrategyDropdownOpen = ref(false);
const countSelectRef = ref(null);
const strategySelectRef = ref(null);

function toggleCountDropdown() {
  isCountDropdownOpen.value = !isCountDropdownOpen.value;
  isStrategyDropdownOpen.value = false;
}

function toggleStrategyDropdown() {
  isStrategyDropdownOpen.value = !isStrategyDropdownOpen.value;
  isCountDropdownOpen.value = false;
}

function selectCountPreset(code) {
  form.countPresetCode = code;
  isCountDropdownOpen.value = false;
}

function selectStrategy(code) {
  form.strategyCode = code;
  isStrategyDropdownOpen.value = false;
}

const selectedCountPreset = computed(() => {
  return (props.presets.count_presets || []).find(p => p.code === form.countPresetCode);
});

const selectedStrategyName = computed(() => {
  const item = (props.presets.strategies || []).find(s => s.code === form.strategyCode);
  return item ? item.name : '选择策略';
});

// 点击外部区域自动收起
function handleOutsideClick(e) {
  if (countSelectRef.value && !countSelectRef.value.contains(e.target)) {
    isCountDropdownOpen.value = false;
  }
  if (strategySelectRef.value && !strategySelectRef.value.contains(e.target)) {
    isStrategyDropdownOpen.value = false;
  }
}

onMounted(() => {
  document.addEventListener('click', handleOutsideClick);
});

onUnmounted(() => {
  document.removeEventListener('click', handleOutsideClick);
});

// 监听初始学期
watch(() => props.initialSemesterId, (val) => {
  if (val && !form.semesterId) {
    form.semesterId = val;
    const currentSem = (props.presets.semesters || []).find(s => s.id === val);
    if (currentSem && currentSem.stage_id) {
      form.stageId = currentSem.stage_id;
    }
  }
}, { immediate: true });

// 根据学习阶段级联过滤学期
const filteredSemesters = computed(() => {
  const list = props.presets.semesters || [];
  if (!form.stageId) return list;
  return list.filter(sem => sem.stage_id === form.stageId);
});

function onStageChange() {
  const sems = filteredSemesters.value;
  if (sems.length > 0) {
    const exists = sems.some(s => s.id === form.semesterId);
    if (!exists) {
      form.semesterId = sems[0].id;
      onSemesterChange();
    }
  } else {
    form.semesterId = '';
  }
}

function getPresetShowName(p) {
  if (!p || !p.name) return '';
  return p.name.split(' ')[0] || p.name;
}

const currentSemesterName = computed(() => {
  const list = props.presets.semesters || [];
  const item = list.find(s => s.id === form.semesterId);
  return item ? item.name : '全部学期';
});

const currentSubjectName = computed(() => {
  if (!form.subjectId) return '';
  const item = props.subjects.find(s => s.id === form.subjectId);
  return item ? item.name : '';
});

const currentUnitName = computed(() => {
  if (!form.unitCode) return '';
  const item = props.units.find(u => u.id === Number(form.unitCode));
  return item ? item.name : '';
});

const currentStrategyName = computed(() => {
  const item = (props.presets.strategies || []).find(st => st.code === form.strategyCode);
  return item ? item.name : '策略';
});

function toggleCollapse() {
  isCollapsed.value = !isCollapsed.value;
}

function collapseHeader() {
  isCollapsed.value = true;
}

function onSemesterChange() {
  form.subjectId = '';
  form.unitCode = '';
  emit('semester-change', form.semesterId);
}

function onSubjectChange() {
  form.unitCode = '';
  emit('subject-change', { semesterId: form.semesterId, subjectId: form.subjectId });
}

function handleGenerate() {
  emit('generate', { ...form });
}

function getFilters() {
  return { ...form };
}

defineExpose({
  collapseHeader,
  getFilters
});
</script>

<style scoped>
.review-filter-header {
  background: var(--bg-card);
  border-bottom: 1px solid var(--border);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: var(--shadow-sm);
  border-radius: var(--radius-md);
  margin-bottom: 16px;
  overflow: visible;
}

/* 收缩单行极简条 */
.collapsed-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 38px;
  padding: 0 16px;
  background: var(--bg-main);
  border-radius: var(--radius-md);
}

.status-tags {
  display: flex;
  align-items: center;
  gap: 10px;
  overflow-x: auto;
}

.tag-pill {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 12px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  color: var(--text-main);
  white-space: nowrap;
}

.tag-pill.current-tag {
  background: rgba(16, 185, 129, 0.1);
  color: var(--primary);
  border-color: rgba(16, 185, 129, 0.2);
  font-weight: 600;
}

.tag-pill.strategy-tag {
  background: rgba(245, 158, 11, 0.1);
  color: var(--warning);
  border-color: rgba(245, 158, 11, 0.2);
}

.tag-pill.progress-tag {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
  border-color: rgba(59, 130, 246, 0.2);
  font-weight: 600;
}

.expand-btn, .close-expand-btn {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 4px 12px;
  font-size: 12px;
  color: var(--text-main);
  cursor: pointer;
  white-space: nowrap;
  transition: var(--transition);
}

.expand-btn:hover, .close-expand-btn:hover {
  background: var(--bg-sidebar-hover);
  border-color: var(--primary);
}

/* 展开单行 Header */
.expanded-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 18px;
  gap: 16px;
  background: var(--bg-card);
  border-radius: var(--radius-md);
  overflow: visible;
}

.filter-inputs {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  flex: 1;
  overflow: visible;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
  position: relative;
}

.filter-item label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-muted);
  white-space: nowrap;
}

.filter-item select {
  height: 34px;
  padding: 0 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  font-size: 13px;
  background: var(--bg-main);
  color: var(--text-main);
  outline: none;
  transition: var(--transition);
}

.filter-item select:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 2px var(--primary-light);
}

/* 自定义下拉选择器 */
.custom-select {
  position: relative;
  width: 120px;
  user-select: none;
}

.select-trigger {
  height: 34px;
  padding: 0 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  font-size: 13px;
  background: var(--bg-main);
  color: var(--text-main);
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  transition: var(--transition);
}

.select-trigger:hover {
  border-color: var(--primary);
}

.arrow-icon {
  font-size: 9px;
  color: var(--text-muted);
  margin-left: 6px;
}

.select-options {
  position: absolute;
  top: 105%;
  left: 0;
  width: 100%;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow-md);
  z-index: 150;
  overflow: visible;
}

.select-option {
  padding: 8px 10px;
  font-size: 12.5px;
  color: var(--text-main);
  cursor: pointer;
  transition: var(--transition);
  white-space: nowrap;
}

.select-option:hover {
  background: var(--bg-sidebar-hover);
  color: var(--primary);
}

.select-option.active {
  background: var(--primary-light);
  color: var(--primary);
  font-weight: 700;
}

.generate-btn {
  height: 34px;
  padding: 0 18px;
  background: var(--primary);
  color: var(--text-light);
  border: none;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: var(--transition);
  white-space: nowrap;
}

.generate-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.generate-btn:hover:not(:disabled) {
  background: var(--primary-hover);
  transform: translateY(-1px);
}

/* 选项水平左向气泡 Tooltip */
.tooltip-box-left {
  position: relative;
}

.tooltip-box-left::after {
  content: attr(data-tooltip);
  position: absolute;
  right: 110%; /* 浮现在选项的左侧 */
  top: 50%;
  transform: translateY(-50%) translateX(6px);
  background: #1e293b;
  color: #ffffff;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.15s ease, transform 0.15s ease;
  z-index: 250; /* 确保盖在所有下拉层级之上 */
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.tooltip-box-left:hover::after {
  opacity: 1;
  transform: translateY(-50%) translateX(0);
}

.dark-mode .tooltip-box-left::after {
  background: #0f172a;
  border-color: var(--border);
}
</style>
