<template>
  <div style="max-width: 760px;">

    <!-- Children List -->
    <div v-if="children.length === 0 && !loading" style="background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius-md); padding: 48px; text-align: center; color: var(--text-muted);">
      <div style="font-size: 36px; margin-bottom: 12px;">👶</div>
      <p>您的账号下暂未关联任何孩子账号。<br>请联系管理员绑定孩子账号。</p>
    </div>

    <div style="display: flex; flex-direction: column; gap: 20px;">
      <div
        v-for="child in children"
        :key="child.id"
        class="child-card"
      >
        <!-- Card Header -->
        <div class="child-header">
          <div style="display: flex; align-items: center; gap: 12px;">
            <div class="avatar">{{ (child.nickname || child.username).charAt(0).toUpperCase() }}</div>
            <div>
              <div class="child-name">{{ child.nickname || child.username }}</div>
              <div class="child-sub">账号：{{ child.username }} · 入学年份：{{ child.enrollment_year }}</div>
            </div>
          </div>
          <span class="badge" :style="child.is_active ? 'background:var(--primary-light);color:var(--primary)' : 'background:#fee2e2;color:#991b1b'">
            {{ child.is_active ? '正常' : '已停用' }}
          </span>
        </div>

        <!-- Semester Offset Control -->
        <div class="offset-section">
          <div class="offset-label">
            <span style="font-weight: 600; font-size: 14px;">学期偏移量</span>
            <span class="hint-text">{{ offsetHint(child.semester_offset) }}</span>
          </div>
          <div class="offset-control">
            <button class="btn-step" @click="changeOffset(child, -1)" title="减少一个学期">－</button>
            <div class="offset-display" :class="offsetClass(child.semester_offset)">
              <span class="offset-value">{{ child.semester_offset > 0 ? '+' : '' }}{{ child.semester_offset }}</span>
              <span class="offset-unit">学期</span>
            </div>
            <button class="btn-step" @click="changeOffset(child, 1)" title="增加一个学期">＋</button>
            <button
              v-if="child.semester_offset !== 0"
              class="btn-reset"
              @click="changeOffset(child, -child.semester_offset)"
              title="重置为 0"
            >重置</button>
          </div>
          <div class="current-sem-tip">
            <span>📅 当前推算学期：</span>
            <strong>{{ computedSemester(child) }}</strong>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted, inject } from 'vue';
import { api } from '../services/api.js';
import { showAlert, showConfirm } from '../services/dialog.js';
import { calculateCurrentSemester } from '../utils/semester.js';

const semestersList = inject('semestersList');
const children = ref([]);
const loading = ref(true);

async function fetchChildren() {
  loading.value = true;
  try {
    const res = await api.get('/parent/children');
    if (res.ok) children.value = (await res.json()).data.items || [];
  } catch (err) { console.error(err); }
  finally { loading.value = false; }
}

function offsetHint(offset) {
  if (offset === 0) return '无调整（默认）';
  if (offset > 0) return `超前 ${offset} 学期（跳级/提前）`;
  return `延后 ${Math.abs(offset)} 学期（休学/重读）`;
}

function offsetClass(offset) {
  if (offset === 0) return 'neutral';
  return offset > 0 ? 'positive' : 'negative';
}

function computedSemester(child) {
  return calculateCurrentSemester(child.enrollment_year, child.semester_offset);
}

async function changeOffset(child, delta) {
  const newVal = child.semester_offset + delta;
  const label = newVal === 0 ? '重置为默认（0）' : `调整为 ${newVal > 0 ? '+' : ''}${newVal} 学期`;
  const ok = await showConfirm(`确认将 "${child.nickname || child.username}" 的学期偏移量${label}？`);
  if (!ok) return;

  try {
    const res = await api.put(`/parent/children/${child.id}/semester_offset`, { semester_offset: newVal });
    if (res.ok) {
      child.semester_offset = newVal;
    } else {
      await showAlert((await res.json()).msg || '更新失败');
    }
  } catch (err) {
    await showAlert('网络错误');
  }
}

onMounted(() => {
  fetchChildren();
});
</script>

<style scoped>
.child-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  transition: var(--transition);
}
.child-card:hover { border-color: var(--primary); box-shadow: 0 4px 16px rgba(0,0,0,.06); }

.child-header { display: flex; justify-content: space-between; align-items: center; }

.avatar {
  width: 48px; height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary), #059669);
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.child-name { font-size: 18px; font-weight: 700; color: var(--text-main); }
.child-sub { font-size: 13px; color: var(--text-muted); margin-top: 2px; }

.offset-section {
  border-top: 1px solid var(--border);
  padding-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.offset-label { display: flex; align-items: center; gap: 10px; }
.hint-text { font-size: 13px; color: var(--text-muted); background: var(--bg-main); border: 1px solid var(--border); border-radius: 20px; padding: 2px 10px; }

.offset-control { display: flex; align-items: center; gap: 12px; }

.btn-step {
  width: 40px; height: 40px;
  border-radius: 50%;
  border: 2px solid var(--border);
  background: var(--bg-main);
  font-size: 20px;
  font-weight: 700;
  cursor: pointer;
  color: var(--primary);
  transition: var(--transition);
  display: flex; align-items: center; justify-content: center;
}
.btn-step:hover { background: var(--primary-light); border-color: var(--primary); }

.offset-display {
  min-width: 100px;
  height: 52px;
  border-radius: var(--radius-md);
  border: 2px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: var(--bg-main);
  transition: var(--transition);
}
.offset-display.positive { border-color: #10b981; background: rgba(16,185,129,.06); }
.offset-display.negative { border-color: #f59e0b; background: rgba(245,158,11,.06); }
.offset-display.neutral  { border-color: var(--border); }

.offset-value { font-size: 24px; font-weight: 800; color: var(--text-main); }
.offset-unit  { font-size: 13px; color: var(--text-muted); }

.btn-reset {
  background: #fee2e2;
  color: #991b1b;
  border: none;
  border-radius: var(--radius-sm);
  padding: 6px 14px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: var(--transition);
}
.btn-reset:hover { background: #fca5a5; }

.current-sem-tip {
  font-size: 13px;
  color: var(--text-muted);
  background: var(--bg-main);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 8px 14px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.current-sem-tip strong { color: var(--primary); font-weight: 700; }
</style>
