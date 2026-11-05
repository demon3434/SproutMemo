<template>
  <div style="display: flex; flex-direction: column; gap: 16px; height: calc(100vh - 120px);">

    <!-- Stage Tabs Navigation -->
    <div style="display: flex; border-bottom: 2px solid var(--border); gap: 8px;">
      <button
        v-for="stage in activeStages"
        :key="stage.id"
        @click="activeStageId = stage.id"
        class="tab-btn"
        :class="{ active: activeStageId === stage.id }"
      >
        🎓 {{ stage.name }} 阶段
      </button>
    </div>

    <!-- Semesters Grid -->
    <div class="grid-container" style="flex: 1; overflow-y: auto; padding-bottom: 12px;">
      <div v-if="!activeStageId" style="color: var(--text-muted); text-align: center; padding: 48px;">
        暂无可用的学习阶段数据。
      </div>
      <div v-else-if="getSemestersForActiveStage().length === 0" style="color: var(--text-muted); text-align: center; padding: 48px;">
        该阶段下暂未配置学期名录。请先在"学期管理"中配置。
      </div>
      <div v-else class="semesters-grid">
        <div
          v-for="sem in getSemestersForActiveStage()"
          :key="sem.id"
          class="semester-card"
        >
          <!-- Card Header -->
          <div class="card-header">
            <span class="card-title">{{ formatSemesterName(sem.name) }}</span>
            <span class="badge" :style="{ backgroundColor: sem.is_active ? 'var(--primary-light)' : '#fee2e2', color: sem.is_active ? 'var(--primary)' : '#991b1b' }">
              {{ sem.is_active ? '在用' : '停用' }}
            </span>
          </div>

          <!-- Subject Rows -->
          <div class="subjects-list">
            <div v-if="getMappingsForSemester(sem.id).length === 0" class="empty-hint">
              暂未关联任何学科
            </div>
            <div
              v-for="(mapping, idx) in getMappingsForSemester(sem.id)"
              :key="mapping.id"
              class="mapping-row"
              :style="{ opacity: mapping.is_active ? 1 : 0.6 }"
            >
              <!-- Left: status dot + name -->
              <div style="display: flex; align-items: center; gap: 8px; flex: 1; overflow: hidden;">
                <span class="status-dot" :class="{ active: mapping.is_active }"></span>
                <span class="subject-name">{{ mapping.subject?.subject_name }}</span>
                <span v-if="!mapping.is_active" class="inactive-tag">停用</span>
              </div>
              <!-- Right: reorder + toggle + delete -->
              <div class="row-actions">
                <button
                  class="btn-arrow"
                  :disabled="idx === 0"
                  @click="handleMove(mapping, 'up')"
                  title="上移"
                >▲</button>
                <button
                  class="btn-arrow"
                  :disabled="idx === getMappingsForSemester(sem.id).length - 1"
                  @click="handleMove(mapping, 'down')"
                  title="下移"
                >▼</button>
                <button class="btn-icon-sub" @click="handleToggleActive(mapping)" :title="mapping.is_active ? '停用' : '启用'">
                  {{ mapping.is_active ? '🚫' : '✅' }}
                </button>
                <button class="btn-icon-sub danger" @click="handleDelete(mapping)" title="删除关联">
                  🗑️
                </button>
              </div>
            </div>
          </div>

          <!-- Footer: Add New Association -->
          <div class="card-footer">
            <button
              v-if="!showAddMode[sem.id]"
              @click="enterAddMode(sem.id)"
              class="btn-add"
            >
              ＋ 关联新学科
            </button>
            <div v-else class="add-panel">
              <div class="add-panel-title">选择需要关联的学科：</div>
              <div v-if="getAvailableSubjects(sem.id).length === 0" class="empty-hint" style="text-align: center; padding: 4px 0;">
                所有学科均已关联
              </div>
              <div v-else class="checkbox-list">
                <label
                  v-for="sub in getAvailableSubjects(sem.id)"
                  :key="sub.id"
                  class="checkbox-item"
                >
                  <input type="checkbox" :value="sub.id" v-model="selectedSubjectsToAdd[sem.id]" />
                  {{ sub.subject_name }}
                </label>
              </div>
              <div class="add-panel-actions">
                <button
                  @click="saveAssociations(sem.id)"
                  class="btn btn-primary"
                  style="font-size: 12px; padding: 5px 14px; height: 30px; width: auto;"
                  :disabled="!selectedSubjectsToAdd[sem.id] || selectedSubjectsToAdd[sem.id].length === 0"
                >保存</button>
                <button
                  @click="cancelAddMode(sem.id)"
                  class="btn btn-secondary"
                  style="font-size: 12px; padding: 5px 14px; height: 30px; width: auto;"
                >取消</button>
              </div>
            </div>
          </div>

        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, reactive } from 'vue';
import { api } from '../services/api.js';
import { showAlert, showConfirm } from '../services/dialog.js';

const stagesList = ref([]);
const semestersList = ref([]);
const subjectsList = ref([]);
const mappingsList = ref([]);
const activeStageId = ref('');
const showAddMode = ref({});
const selectedSubjectsToAdd = reactive({});

const activeStages = computed(() =>
  stagesList.value.filter(s => s.is_active)
);

async function fetchStages() {
  try {
    const res = await api.get('/stages?only_active=false');
    if (res.ok) {
      stagesList.value = (await res.json()).data.items || [];
      if (!activeStageId.value) {
        const active = stagesList.value.find(s => s.is_active);
        if (active) activeStageId.value = active.id;
      }
    }
  } catch (err) { console.error(err); }
}

async function fetchSemesters() {
  try {
    const res = await api.get('/semesters?only_active=false');
    if (res.ok) semestersList.value = (await res.json()).data.items || [];
  } catch (err) { console.error(err); }
}

async function fetchSubjects() {
  try {
    const res = await api.get('/subjects?only_active=false');
    if (res.ok) subjectsList.value = (await res.json()).data.items || [];
  } catch (err) { console.error(err); }
}

async function fetchMappings() {
  try {
    const res = await api.get('/semester_settings/subjects?only_active=false');
    if (res.ok) mappingsList.value = (await res.json()).data.items || [];
  } catch (err) { console.error(err); }
}

function getSemestersForActiveStage() {
  return semestersList.value.filter(s => s.stage_id === activeStageId.value);
}

function formatSemesterName(name) {
  const stage = stagesList.value.find(s => s.id === activeStageId.value);
  if (stage && name.startsWith(stage.name)) return name.slice(stage.name.length).trim();
  return name;
}

function getMappingsForSemester(semesterId) {
  // Already sorted server-side by sort_order; maintain that order
  return mappingsList.value
    .filter(m => m.semester_id === semesterId)
    .sort((a, b) => a.sort_order - b.sort_order);
}

function getAvailableSubjects(semesterId) {
  const mapped = new Set(getMappingsForSemester(semesterId).map(m => m.subject_id));
  return subjectsList.value.filter(s => s.is_active && !mapped.has(s.id));
}

// Add Mode
function enterAddMode(semId) {
  showAddMode.value[semId] = true;
  selectedSubjectsToAdd[semId] = [];
}
function cancelAddMode(semId) {
  showAddMode.value[semId] = false;
  selectedSubjectsToAdd[semId] = [];
}
async function saveAssociations(semId) {
  const toAdd = selectedSubjectsToAdd[semId] || [];
  if (!toAdd.length) return;
  try {
    for (const subId of toAdd) {
      await api.post('/semester_settings/subjects', {
        semester_id: semId,
        subject_id: subId,
        is_active: true
      });
    }
    await fetchMappings();
    cancelAddMode(semId);
  } catch (err) {
    await showAlert('网络错误，部分学科关联失败');
  }
}

// Move
async function handleMove(mapping, direction) {
  try {
    const res = await api.post(`/semester_settings/subjects/${mapping.id}/move?direction=${direction}`, {});
    if (res.ok) await fetchMappings();
  } catch (err) { console.error(err); }
}

// Toggle / Delete
async function handleToggleActive(mapping) {
  try {
    const res = await api.put(`/semester_settings/subjects/${mapping.id}`, { is_active: !mapping.is_active });
    if (res.ok) await fetchMappings();
  } catch (err) { await showAlert('网络错误'); }
}

async function handleDelete(mapping) {
  const ok = await showConfirm(`确定要取消此学期与 "${mapping.subject?.subject_name}" 的关联吗？`);
  if (!ok) return;
  try {
    const res = await api.delete(`/semester_settings/subjects/${mapping.id}`);
    if (res.ok) await fetchMappings();
    else await showAlert((await res.json()).msg || '操作失败');
  } catch (err) { await showAlert('网络错误'); }
}

onMounted(() => {
  fetchStages();
  fetchSemesters();
  fetchSubjects();
  fetchMappings();
});
</script>

<style scoped>
.tab-btn {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-bottom: none;
  cursor: pointer;
  padding: 12px 20px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
  border-top-left-radius: var(--radius-sm);
  border-top-right-radius: var(--radius-sm);
  transition: var(--transition);
  margin-bottom: -2px;
}
.tab-btn:hover { background: var(--bg-hover); color: var(--primary); }
.tab-btn.active {
  background: var(--bg-main);
  color: var(--primary);
  border-top: 3px solid var(--primary);
  border-bottom: 2px solid var(--bg-main);
}

.semesters-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.semester-card {
  background-color: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  transition: var(--transition);
}
.semester-card:hover { border-color: var(--primary); box-shadow: 0 4px 12px rgba(0,0,0,.06); }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--border);
  padding-bottom: 10px;
}
.card-title { font-weight: 700; font-size: 16px; color: var(--primary); }

.subjects-list { display: flex; flex-direction: column; gap: 6px; min-height: 80px; }

.mapping-row {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 10px;
  background: var(--bg-main);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  transition: var(--transition);
}
.mapping-row:hover { border-color: var(--primary); }

.status-dot {
  width: 7px; height: 7px;
  border-radius: 50%;
  background: #94a3b8;
  flex-shrink: 0;
}
.status-dot.active { background: #10b981; }

.subject-name { font-weight: 600; font-size: 14px; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.inactive-tag { font-size: 11px; color: var(--danger); font-weight: bold; flex-shrink: 0; }

.row-actions { display: flex; gap: 4px; flex-shrink: 0; }

.btn-arrow {
  background: transparent;
  border: 1px solid var(--border);
  border-radius: 3px;
  cursor: pointer;
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  color: var(--text-secondary);
  transition: var(--transition);
  padding: 0;
}
.btn-arrow:hover:not(:disabled) { background: var(--primary-light); color: var(--primary); border-color: var(--primary); }
.btn-arrow:disabled {
  color: #cbd5e1;
  cursor: not-allowed;
  background: transparent;
  border-color: var(--border);
}

.btn-icon-sub {
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 2px 5px;
  font-size: 12px;
  border-radius: 3px;
  transition: var(--transition);
}
.btn-icon-sub:hover { background: var(--bg-hover); }
.btn-icon-sub.danger:hover { color: var(--danger); }

.empty-hint { color: var(--text-muted); font-size: 13px; text-align: center; padding: 8px 0; }

.card-footer { border-top: 1px solid var(--border); padding-top: 12px; }

.btn-add {
  width: 100%;
  height: 36px;
  background: var(--bg-main);
  border: 1px dashed var(--border);
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 600;
  color: var(--primary);
  cursor: pointer;
  transition: var(--transition);
}
.btn-add:hover { border-color: var(--primary); background: var(--primary-light); }

.add-panel {
  border: 1px dashed var(--primary);
  border-radius: var(--radius-sm);
  padding: 12px;
  background: rgba(16,185,129,.02);
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.add-panel-title { font-size: 12px; font-weight: 700; color: var(--primary); }

.checkbox-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-height: 90px;
  overflow-y: auto;
}
.checkbox-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 4px;
  padding: 3px 8px;
  user-select: none;
}
.add-panel-actions { display: flex; gap: 8px; }

.grid-container::-webkit-scrollbar { width: 6px; }
.grid-container::-webkit-scrollbar-thumb { background: var(--border); border-radius: 4px; }
</style>
