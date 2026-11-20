<template>
  <div class="review-filter-header" :class="{ collapsed: isCollapsed }">
    <!-- 收缩极简状态栏 -->
    <div v-if="isCollapsed" class="collapsed-bar">
      <div class="status-tags">
        <span class="tag-pill current-tag"><AppIcon name="book" :size="14" /> {{ currentSemesterName }}</span>
        <span class="tag-pill" v-if="currentSubjectName">学科: {{ currentSubjectName }}</span>
        <span class="tag-pill" v-if="currentUnitName">单元: {{ currentUnitName }}</span>
        <span class="tag-pill strategy-tag"><AppIcon name="target" :size="14" /> {{ currentStrategyName }}</span>
        <span class="tag-pill progress-tag">进度: {{ completedCount }} / {{ totalCount }} 题</span>
      </div>
      <button class="expand-btn" @click="toggleCollapse">
        <AppIcon name="settings" :size="14" /> 调整参数
      </button>
    </div>

    <!-- 展开内联单行控制栏 -->
    <div v-else class="expanded-bar">
      <div class="filter-inputs">
        <div class="filter-item">
          <label>学习阶段</label>
          <StationerySelect
            v-model="form.stageId"
            :options="stageOptions"
            compact
            @change="onStageChange"
          />
        </div>

        <div class="filter-item">
          <label>学期</label>
          <StationerySelect
            v-model="form.semesterId"
            :options="semesterOptions"
            compact
            @change="onSemesterChange"
          />
        </div>

        <div class="filter-item">
          <label>学科</label>
          <StationerySelect
            v-model="form.subjectId"
            :options="subjectOptions"
            compact
            @change="onSubjectChange"
          />
        </div>

        <div class="filter-item">
          <label>单元/阶段</label>
          <StationerySelect
            v-model="form.unitCode"
            :options="unitOptions"
            compact
          />
        </div>

        <div class="filter-item">
          <label>题量档位</label>
          <StationerySelect
            v-model="form.countPresetCode"
            :options="countPresetOptions"
            compact
          />
        </div>

        <div class="filter-item">
          <label>筛题策略</label>
          <StationerySelect
            v-model="form.strategyCode"
            :options="strategyOptions"
            compact
          />
        </div>

        <button class="generate-btn" :disabled="loading" @click="handleGenerate">
          <AppIcon name="target" :size="16" />
          <span>{{ loading ? '抽题中...' : '抽题复习' }}</span>
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
import StationerySelect from './StationerySelect.vue';
import AppIcon from './AppIcon.vue';

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

const stageOptions = computed(() => [
  { value: '', label: '全部阶段' },
  ...(props.presets.stages || []).map(st => ({ value: st.id, label: st.name }))
]);

const semesterOptions = computed(() =>
  (filteredSemesters.value || []).map(s => ({ value: s.id, label: s.name }))
);

const subjectOptions = computed(() => [
  { value: '', label: '全部学科' },
  ...(props.subjects || []).map(sub => ({
    value: sub.subject_id,
    label: sub.subject?.subject_name || '未知'
  }))
]);

const unitOptions = computed(() => [
  { value: '', label: '全部单元' },
  ...(props.units || []).map(u => ({ value: u.id, label: u.name }))
]);

const countPresetOptions = computed(() =>
  (props.presets.count_presets || []).map(p => ({
    value: p.code,
    label: getPresetShowName(p),
    tooltip: `${p.count_value} 题`
  }))
);

const strategyOptions = computed(() =>
  (props.presets.strategies || []).map(st => ({
    value: st.code,
    label: st.name,
    tooltip: st.description || ''
  }))
);

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

<style scoped src="./ReviewFilterHeader.css"></style>
