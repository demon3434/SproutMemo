<template>
  <div style="display: flex; flex-direction: column; gap: 20px; height: calc(100vh - 120px);">
    
    <!-- Top Action Panel -->
    <div style="background-color: var(--bg-card); padding: 16px 24px; border-radius: var(--radius-md); border: 1px solid var(--border); display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 16px;">
      <div style="display: flex; gap: 12px; align-items: center;">
        <button @click="handleCreateStage" class="btn btn-primary" style="width: auto;">
          ➕ 新建学习阶段
        </button>
      </div>

      <!-- Add Semester Form -->
      <form @submit.prevent="handleAddSemester" style="display: flex; gap: 12px; align-items: center;">
        <div style="display: flex; align-items: center; gap: 8px;">
          <label style="font-weight: 600; font-size: 13px; white-space: nowrap;">学期名称</label>
          <input type="text" v-model="newSemName" class="form-control" placeholder="如：小一上" required style="width: 180px; margin-bottom: 0;" />
        </div>
        <div style="display: flex; align-items: center; gap: 8px;">
          <label style="font-weight: 600; font-size: 13px; white-space: nowrap;">隶属阶段</label>
          <select v-model="selectedStageId" class="form-control" required style="width: 140px; margin-bottom: 0;">
            <option value="">-- 选择阶段 --</option>
            <option v-for="stage in stagesList" :key="stage.id" :value="stage.id">
              {{ stage.name }}
            </option>
          </select>
        </div>
        <button type="submit" class="btn btn-primary" style="width: auto; height: 38px; padding: 0 16px;">
          添加学期
        </button>
      </form>
    </div>

    <!-- Kanban Board Layout -->
    <div class="stages-board" style="display: flex; gap: 20px; overflow-x: auto; flex: 1; align-items: flex-start; padding-bottom: 8px;">
      <div v-if="stagesList.length === 0" style="color: var(--text-muted); text-align: center; width: 100%; padding: 48px;">
        暂无阶段目录数据，请先创建学习阶段。
      </div>

      <!-- Columns -->
      <div v-for="(stage, stageIdx) in stagesList" :key="stage.id" class="stage-column" 
           style="flex: 0 0 360px; background-color: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius-md); max-height: 100%; display: flex; flex-direction: column; overflow: hidden; transition: var(--transition);">
        
        <!-- Column Header -->
        <div class="column-header" style="padding: 14px 16px; border-bottom: 1px solid var(--border); display: flex; align-items: center; justify-content: space-between; background-color: var(--bg-main);">
          <div style="display: flex; align-items: center; gap: 8px; overflow: hidden;">
            <span style="font-weight: 700; font-size: 15px; text-overflow: ellipsis; overflow: hidden; white-space: nowrap;">
              {{ stage.name }}
            </span>
            <span class="badge" :style="{ backgroundColor: stage.is_active ? 'var(--primary-light)' : '#fee2e2', color: stage.is_active ? 'var(--primary)' : '#991b1b' }">
              {{ stage.is_active ? '启用' : '禁用' }}
            </span>
          </div>
          <!-- Stage ordering and editing buttons -->
          <div style="display: flex; gap: 4px;">
            <button @click="handleMoveStage(stage, 'up')" class="btn-arrow" title="左移" :disabled="stageIdx === 0">◀</button>
            <button @click="handleMoveStage(stage, 'down')" class="btn-arrow" title="右移" :disabled="stageIdx === stagesList.length - 1">▶</button>
            <button @click="handleEditStage(stage)" class="btn-icon" title="重命名" style="padding: 2px 6px;">✏️</button>
            <button @click="handleToggleStage(stage)" class="btn-icon" :title="stage.is_active ? '停用' : '启用'" style="padding: 2px 6px;">
              {{ stage.is_active ? '🚫' : '✅' }}
            </button>
            <button @click="handleDeleteStage(stage)" class="btn-icon" title="删除" style="padding: 2px 6px;">🗑️</button>
          </div>
        </div>

        <!-- Column Body -->
        <div class="column-body" style="padding: 16px; overflow-y: auto; flex: 1; display: flex; flex-direction: column; gap: 10px;">
          <div v-if="getSemestersByStage(stage.id).length === 0" style="color: var(--text-muted); font-size: 13px; text-align: center; padding: 32px 0;">
            暂无学期
          </div>
          
          <!-- Semester Cards -->
          <div v-for="(sem, semIdx) in getSemestersByStage(stage.id)" :key="sem.id" class="semester-card" 
               style="padding: 12px; border: 1px solid var(--border); border-radius: var(--radius-sm); background-color: var(--bg-card); display: flex; justify-content: space-between; align-items: center; transition: var(--transition);">
            <div style="overflow: hidden; padding-right: 8px;">
              <div style="font-weight: 600; font-size: 14px; text-overflow: ellipsis; overflow: hidden; white-space: nowrap;" :style="{ color: sem.is_active ? 'var(--text-main)' : 'var(--text-muted)' }">
                {{ formatSemesterName(sem.name, stage.name) }}
              </div>
              <span v-if="!sem.is_active" style="font-size: 11px; color: var(--danger); font-weight: bold;">(已停用)</span>
            </div>
            
            <!-- Semester Controls -->
            <div style="display: flex; gap: 4px; flex-shrink: 0;">
              <button @click="handleMoveSemester(sem, 'up')" class="btn-arrow-sm" title="上移" :disabled="semIdx === 0">▲</button>
              <button @click="handleMoveSemester(sem, 'down')" class="btn-arrow-sm" title="下移" :disabled="semIdx === getSemestersByStage(stage.id).length - 1">▼</button>
              <button @click="handleEditSemester(sem)" class="btn-icon-sub" title="重命名">✏️</button>
              <button @click="handleToggleSemester(sem)" class="btn-icon-sub" :title="sem.is_active ? '停用' : '启用'">
                {{ sem.is_active ? '🚫' : '✅' }}
              </button>
              <button @click="handleDeleteSemester(sem)" class="btn-icon-sub" title="删除">🗑️</button>
            </div>
          </div>
        </div>
        
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { api } from '../services/api.js';
import { showAlert, showConfirm, showPrompt } from '../services/dialog.js';

const stagesList = ref([]);
const semestersList = ref([]);
const newSemName = ref('');
const selectedStageId = ref('');

async function fetchStages() {
  try {
    const res = await api.get('/stages?only_active=false');
    if (res.ok) {
      stagesList.value = (await res.json()).data.items || [];
    }
  } catch (err) {
    console.error(err);
  }
}

async function fetchSemesters() {
  try {
    const res = await api.get('/semesters?only_active=false');
    if (res.ok) {
      semestersList.value = (await res.json()).data.items || [];
    }
  } catch (err) {
    console.error(err);
  }
}

function getSemestersByStage(stageId) {
  return semestersList.value.filter(sem => sem.stage_id === stageId);
}

function formatSemesterName(name, stageName) {
  // Strip stage name prefix to keep display compact
  if (name.startsWith(stageName)) {
    return name.replace(stageName, '').trim();
  }
  return name;
}

// Stage Actions
async function handleCreateStage() {
  const name = await showPrompt('请输入新学习阶段的名称 (如：高中):');
  if (!name || !name.trim()) return;
  try {
    const res = await api.post('/stages', {
      name: name.trim(),
      sort_order: stagesList.value.length + 1
    });
    if (res.ok) {
      fetchStages();
    } else {
      await showAlert((await res.json()).msg || '创建失败');
    }
  } catch (err) {
    await showAlert('网络错误');
  }
}

async function handleEditStage(stage) {
  const newName = await showPrompt('请输入阶段新名称:', stage.name);
  if (!newName || !newName.trim() || newName === stage.name) return;
  try {
    const res = await api.put(`/stages/${stage.id}`, { name: newName.trim() });
    if (res.ok) {
      fetchStages();
      fetchSemesters();
    } else {
      await showAlert((await res.json()).msg || '修改失败');
    }
  } catch (err) {
    await showAlert('网络错误');
  }
}

async function handleToggleStage(stage) {
  const isConfirmed = await showConfirm(`确定要${stage.is_active ? '停用' : '启用'}阶段 "${stage.name}" 吗？`);
  if (!isConfirmed) return;
  try {
    const res = await api.put(`/stages/${stage.id}`, { is_active: !stage.is_active });
    if (res.ok) {
      fetchStages();
    }
  } catch (err) {
    await showAlert('网络错误');
  }
}

async function handleMoveStage(stage, direction) {
  try {
    const res = await api.post(`/stages/${stage.id}/move?direction=${direction}`, {});
    if (res.ok) {
      fetchStages();
    }
  } catch (err) {
    console.error(err);
  }
}

// Semester Actions
async function handleAddSemester() {
  if (!newSemName.value.trim() || !selectedStageId.value) return;
  
  const stage = stagesList.value.find(s => s.id === selectedStageId.value);
  // Standardize full semester name prefix if not present
  let fullName = newSemName.value.trim();
  if (stage && !fullName.startsWith(stage.name)) {
    fullName = stage.name + fullName;
  }

  const semsInStage = getSemestersByStage(selectedStageId.value);

  try {
    const res = await api.post('/semesters', {
      name: fullName,
      stage_id: selectedStageId.value,
      sort_order: semsInStage.length + 1
    });
    if (res.ok) {
      newSemName.value = '';
      fetchSemesters();
    } else {
      await showAlert((await res.json()).msg || '添加失败');
    }
  } catch (err) {
    await showAlert('网络错误');
  }
}

async function handleEditSemester(sem) {
  const newName = await showPrompt('请输入学期新名称:', sem.name);
  if (!newName || !newName.trim() || newName === sem.name) return;
  try {
    const res = await api.put(`/semesters/${sem.id}`, { name: newName.trim() });
    if (res.ok) {
      fetchSemesters();
    } else {
      await showAlert((await res.json()).msg || '修改失败');
    }
  } catch (err) {
    await showAlert('网络错误');
  }
}

async function handleToggleSemester(sem) {
  const isConfirmed = await showConfirm(`确定要${sem.is_active ? '停用' : '启用'}学期 "${sem.name}" 吗？`);
  if (!isConfirmed) return;
  try {
    const res = await api.put(`/semesters/${sem.id}`, { is_active: !sem.is_active });
    if (res.ok) {
      fetchSemesters();
    }
  } catch (err) {
    await showAlert('网络错误');
  }
}

async function handleMoveSemester(sem, direction) {
  try {
    const res = await api.post(`/semesters/${sem.id}/move?direction=${direction}`, {});
    if (res.ok) {
      fetchSemesters();
    }
  } catch (err) {
    console.error(err);
  }
}

async function handleDeleteStage(stage) {
  const activeSems = getSemestersByStage(stage.id);
  if (activeSems.length > 0) {
    await showAlert(`无法删除：学习阶段 "${stage.name}" 下还有 ${activeSems.length} 个学期。必须先删除所属学期。`);
    return;
  }
  const isConfirmed = await showConfirm(`确定要删除学习阶段 "${stage.name}" 吗？`);
  if (!isConfirmed) return;
  try {
    const res = await api.delete(`/stages/${stage.id}`);
    if (res.ok) {
      fetchStages();
    } else {
      await showAlert((await res.json()).msg || '删除失败');
    }
  } catch (err) {
    await showAlert('网络错误');
  }
}

async function handleDeleteSemester(sem) {
  const isConfirmed = await showConfirm(`确定要删除学期 "${sem.name}" 吗？`);
  if (!isConfirmed) return;
  try {
    const res = await api.delete(`/semesters/${sem.id}`);
    if (res.ok) {
      fetchSemesters();
    } else {
      await showAlert((await res.json()).msg || '删除失败');
    }
  } catch (err) {
    await showAlert('网络错误');
  }
}

onMounted(() => {
  fetchStages();
  fetchSemesters();
});
</script>

<style scoped>
.btn-arrow {
  background: transparent;
  border: 1px solid var(--border);
  border-radius: 3px;
  cursor: pointer;
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: var(--text-secondary);
  transition: var(--transition);
  padding: 0;
  flex-shrink: 0;
}
.btn-arrow:hover:not(:disabled) {
  background: var(--primary-light);
  color: var(--primary);
  border-color: var(--primary);
}
.btn-arrow:disabled {
  color: #cbd5e1;
  cursor: not-allowed;
  background: transparent;
  border-color: var(--border);
}

.btn-arrow-sm {
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
  flex-shrink: 0;
}
.btn-arrow-sm:hover:not(:disabled) {
  background: var(--primary-light);
  color: var(--primary);
  border-color: var(--primary);
}
.btn-arrow-sm:disabled {
  color: #cbd5e1;
  cursor: not-allowed;
  background: transparent;
  border-color: var(--border);
}

.btn-icon {
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 4px;
  font-size: 13px;
  color: var(--text-secondary);
  border-radius: var(--radius-sm);
  transition: var(--transition);
}
.btn-icon:hover {
  background-color: var(--bg-hover);
  color: var(--primary);
}

.btn-icon-sub {
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 2px 4px;
  font-size: 11px;
  color: var(--text-muted);
  border-radius: 3px;
  transition: var(--transition);
}
.btn-icon-sub:hover {
  background-color: var(--bg-hover);
  color: var(--primary);
}

/* Custom Scrollbar for columns */
.column-body::-webkit-scrollbar { width: 5px; }
.column-body::-webkit-scrollbar-thumb { background: var(--border); border-radius: 4px; }
.column-body::-webkit-scrollbar-thumb:hover { background: var(--text-muted); }
</style>
