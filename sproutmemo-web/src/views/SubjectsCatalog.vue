<template>
  <div style="max-width: 800px; background-color: var(--bg-card); padding: 28px; border-radius: var(--radius-md); border: 1px solid var(--border);">
    <h3 style="margin-bottom: 8px; font-weight: 700;">学科管理</h3>
    <p style="color: var(--text-muted); font-size: 13px; margin-bottom: 24px;">管理孩子可选的学科，如语文、数学、英语等。新增学科自动追加到末尾，可通过箭头调整顺序。</p>

    <div v-if="errorMsg" style="color: var(--danger); margin-bottom: 15px; font-weight: bold; font-size: 14px;">
      {{ errorMsg }}
    </div>

    <!-- Add Form: name only, sort_order auto-assigned on backend -->
    <form @submit.prevent="handleAddSubject" style="margin-bottom: 32px; display: flex; gap: 12px; align-items: flex-end;">
      <div class="form-group" style="flex: 1; margin-bottom: 0;">
        <label class="form-label" style="font-size: 13px; font-weight: 600;">学科名称</label>
        <input type="text" v-model="newSubName" class="form-control" placeholder="输入学科名称，如：物理" required style="margin-bottom: 0;" />
      </div>
      <button type="submit" class="btn btn-primary" style="width: auto; height: 42px; white-space: nowrap;">＋ 添加新学科</button>
    </form>

    <h4 style="margin-bottom: 16px; font-weight: 600; color: var(--text-main);">已配置学科</h4>
    <div style="display: flex; flex-direction: column; gap: 10px;">
      <div v-if="subjectsList.length === 0" style="color: var(--text-muted); padding: 12px 0;">
        暂无学科数据
      </div>
      <div
        v-for="(sub, idx) in subjectsList"
        :key="sub.id"
        class="subject-row"
        :style="{ opacity: sub.is_active ? 1 : 0.65 }"
      >
        <!-- Left: name + badge -->
        <div style="display: flex; align-items: center; gap: 10px; flex: 1; overflow: hidden;">
          <span class="status-dot" :class="{ active: sub.is_active }"></span>
          <span style="font-weight: 700; font-size: 15px; color: var(--text-main); overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">{{ sub.subject_name }}</span>
          <span class="badge" :style="{ backgroundColor: sub.is_active ? 'var(--primary-light)' : '#fee2e2', color: sub.is_active ? 'var(--primary)' : '#991b1b' }">
            {{ sub.is_active ? '已启用' : '已停用' }}
          </span>
        </div>
        <!-- Right: reorder + edit + toggle + delete -->
        <div style="display: flex; gap: 6px; align-items: center; flex-shrink: 0;">
          <button class="btn-arrow" :disabled="idx === 0" @click="handleMove(sub, 'up')" title="上移">▲</button>
          <button class="btn-arrow" :disabled="idx === subjectsList.length - 1" @click="handleMove(sub, 'down')" title="下移">▼</button>
          <button class="btn btn-secondary" @click="handleEdit(sub)" style="width: auto; padding: 5px 12px; font-size: 13px; height: 32px;">编辑</button>
          <button
            class="btn" :class="sub.is_active ? 'btn-danger' : 'btn-primary'"
            @click="handleToggleActive(sub)"
            style="width: auto; padding: 5px 12px; font-size: 13px; height: 32px;"
          >{{ sub.is_active ? '停用' : '启用' }}</button>
          <button class="btn btn-danger" @click="handleDelete(sub)" style="width: auto; padding: 5px 12px; font-size: 13px; height: 32px;">删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, inject } from 'vue';
import { api } from '../services/api.js';
import { showAlert, showConfirm, showPrompt } from '../services/dialog.js';

const subjectsList = inject('subjectsList');

const newSubName = ref('');
const errorMsg = ref('');

async function fetchSubjects() {
  try {
    const res = await api.get('/subjects?only_active=false');
    if (res.ok) {
      subjectsList.value = (await res.json()).data.items || [];
    }
  } catch (err) {
    console.error(err);
  }
}

async function handleAddSubject() {
  errorMsg.value = '';
  try {
    // sort_order not sent — backend auto-assigns max+1
    const res = await api.post('/subjects', { subject_name: newSubName.value });
    if (res.ok) {
      newSubName.value = '';
      fetchSubjects();
    } else {
      errorMsg.value = (await res.json()).msg || '无法添加学科';
    }
  } catch (err) {
    errorMsg.value = '网络连接错误';
  }
}

async function handleMove(sub, direction) {
  try {
    const res = await api.post(`/subjects/${sub.id}/move?direction=${direction}`, {});
    if (res.ok) fetchSubjects();
  } catch (err) { console.error(err); }
}

async function handleEdit(sub) {
  const newName = await showPrompt('请输入新的学科名称:', sub.subject_name);
  if (newName === null) return;
  try {
    const res = await api.put(`/subjects/${sub.id}`, { subject_name: newName });
    if (res.ok) fetchSubjects();
  } catch (err) { console.error(err); }
}

async function handleToggleActive(sub) {
  try {
    const res = await api.put(`/subjects/${sub.id}`, { is_active: !sub.is_active });
    if (res.ok) fetchSubjects();
  } catch (err) { console.error(err); }
}

async function handleDelete(sub) {
  const isConfirmed = await showConfirm(`确定要删除学科 "${sub.subject_name}" 吗？`);
  if (!isConfirmed) return;
  try {
    const res = await api.delete(`/subjects/${sub.id}`);
    if (res.ok) fetchSubjects();
  } catch (err) { console.error(err); }
}

onMounted(() => {
  fetchSubjects();
});
</script>

<style scoped>
.subject-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-main);
  transition: var(--transition);
}
.subject-row:hover { border-color: var(--primary); box-shadow: 0 2px 8px rgba(0,0,0,.05); }

.status-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: #94a3b8;
  flex-shrink: 0;
}
.status-dot.active { background: #10b981; }

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
</style>
