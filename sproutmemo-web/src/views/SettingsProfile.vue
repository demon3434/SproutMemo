<template>
  <div class="profile-container" style="max-width: 600px; margin: 0 auto;">
    <div class="card">
      <h3 class="card-title">👤 个人中心与账号设置</h3>
      <p class="card-subtitle">在此修改您的账户基本信息、安全密码与学期偏移配置。</p>

      <form @submit.prevent="handleSave">
        <!-- 基础信息 -->
        <div class="form-group">
          <label class="form-label">账号用户名</label>
          <input type="text" :value="currentUser.username" class="form-control" disabled style="background:#f1f5f9; cursor:not-allowed;" />
        </div>

        <div class="form-group">
          <label class="form-label">账户角色</label>
          <span class="badge-role" :class="currentUser.role.toLowerCase()">
            {{ roleName(currentUser.role) }}
          </span>
        </div>

        <div class="form-group" style="margin-top: 15px;">
          <label class="form-label">修改账户昵称</label>
          <input type="text" v-model="nickname" class="form-control" placeholder="设置一个方便称呼的昵称" />
        </div>

        <!-- 学生学期偏移配置 -->
        <div v-if="currentUser.role === 'STUDENT'" class="student-offset-card">
          <div class="divider">学年与学期配置</div>

          <div class="form-group">
            <label class="form-label">入学年份</label>
            <input type="number" :value="enrollmentYear" class="form-control" disabled style="background:#f1f5f9; cursor:not-allowed;" />
          </div>

          <div class="form-group">
            <label class="form-label">当前学期偏移量</label>
            <div style="font-size: 13px; color: var(--text-muted); margin-bottom: 8px;">
              {{ offsetHint(semesterOffset) }}
            </div>
            
            <div class="offset-control">
              <button type="button" class="btn-step" @click="changeOffset(-1)" title="减少一个学期">－</button>
              <div class="offset-display" :class="offsetClass(semesterOffset)">
                <span class="offset-value">{{ semesterOffset > 0 ? '+' : '' }}{{ semesterOffset }}</span>
                <span class="offset-unit">学期</span>
              </div>
              <button type="button" class="btn-step" @click="changeOffset(1)" title="增加一个学期">＋</button>
              <button
                v-if="semesterOffset !== 0"
                type="button"
                class="btn-reset"
                @click="changeOffset(-semesterOffset)"
                title="重置为 0"
              >重置</button>
            </div>

            <div class="current-sem-tip">
              <span>📅 当前推算学期：</span>
              <strong>{{ computedSemester }}</strong>
            </div>
          </div>
        </div>

        <!-- 按钮组 -->
        <div style="margin-top: 32px; display: flex; gap: 12px;">
          <button type="submit" class="btn btn-primary" style="flex: 1; height: 42px;">保存更改</button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, inject, computed, onMounted } from 'vue';
import { api } from '../services/api.js';
import { showAlert, showConfirm } from '../services/dialog.js';
import { calculateCurrentSemester } from '../utils/semester.js';

const currentUser = inject('currentUser');
const setCurrentUser = inject('setCurrentUser');

const nickname = ref('');
const semesterOffset = ref(0);

const enrollmentYear = computed(() => {
  return currentUser.value.student_profile?.enrollment_year || 2024;
});

const computedSemester = computed(() => {
  return calculateCurrentSemester(enrollmentYear.value, semesterOffset.value);
});

function roleName(role) {
  const map = { ADMIN: '系统管理员', PARENT: '家长账号', STUDENT: '学生账号' };
  return map[role] || role;
}

function offsetHint(offset) {
  if (offset === 0) return '无调整（与系统自然推算一致）';
  if (offset > 0) return `超前 ${offset} 学期（跳级或提前学习）`;
  return `延后 ${Math.abs(offset)} 学期（休学或往届重读）`;
}

function offsetClass(offset) {
  if (offset === 0) return 'neutral';
  return offset > 0 ? 'positive' : 'negative';
}

function changeOffset(delta) {
  semesterOffset.value += delta;
}

async function handleSave() {
  const okToSave = await showConfirm('确定要保存上述个人信息修改吗？');
  if (!okToSave) return;

  const body = {
    nickname: nickname.value.trim() || null
  };

  if (currentUser.value.role === 'STUDENT') {
    body.semester_offset = semesterOffset.value;
  }

  try {
    const res = await api.put('/auth/profile', body);
    if (res.ok) {
      const data = await res.json();
      setCurrentUser(data.data); // 更新全局用户上下文
      await showAlert('个人设置更新成功！');
    } else {
      await showAlert((await res.json()).msg || '更新失败');
    }
  } catch (err) {
    await showAlert('网络错误');
  }
}

onMounted(() => {
  nickname.value = currentUser.value.nickname || '';
  if (currentUser.value.role === 'STUDENT') {
    semesterOffset.value = currentUser.value.student_profile?.semester_offset || 0;
  }
});
</script>

<style scoped>
.profile-container {
  padding: 10px 0;
}

.card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 32px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.02);
}

.card-title {
  font-size: 20px;
  font-weight: 800;
  color: var(--text-main);
  margin-bottom: 4px;
}

.card-subtitle {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 24px;
}

.badge-role {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 700;
}
.badge-role.admin { background: #fee2e2; color: #ef4444; }
.badge-role.parent { background: #dbeafe; color: #2563eb; }
.badge-role.student { background: var(--primary-light); color: var(--primary); }

.divider {
  border-bottom: 1.5px solid var(--border);
  font-size: 13px;
  font-weight: 700;
  color: var(--text-muted);
  padding-bottom: 6px;
  margin: 28px 0 16px 0;
}

.student-offset-card {
  margin-top: 15px;
}

/* 偏移调节样式 */
.offset-control { display: flex; align-items: center; gap: 12px; margin-top: 8px; }

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
  margin-top: 12px;
}
.current-sem-tip strong { color: var(--primary); font-weight: 700; }
</style>
