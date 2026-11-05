<template>
  <div v-if="visible" class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-card">
      <h3 class="modal-title">✏️ 编辑用户账号</h3>
      <p class="modal-subtitle">修改账号基础信息、学籍档案或关联绑定的学生孩子。</p>

      <div v-if="errorMsg" class="error-banner">
        {{ errorMsg }}
      </div>

      <form @submit.prevent="handleSave">
        <div class="form-group">
          <label class="form-label">用户名</label>
          <input 
            type="text" 
            v-model="form.username" 
            class="form-control" 
            placeholder="输入登录用户名" 
            required 
            autocomplete="off" 
          />
        </div>

        <div class="form-group">
          <label class="form-label">显示昵称</label>
          <input 
            type="text" 
            v-model="form.nickname" 
            class="form-control" 
            placeholder="例如：爸爸/张晓明" 
            required 
          />
        </div>

        <div class="form-group">
          <label class="form-label">系统角色</label>
          <select v-model="form.role" class="form-control" required>
            <option value="PARENT">家长 (Parent)</option>
            <option value="STUDENT">学生/孩子 (Student)</option>
          </select>
          <small style="color: var(--text-muted); font-size: 11px;">
            可根据实际需要切换角色。注意：已录入错题的学生无法切换为家长。
          </small>
        </div>

        <!-- 学生专属属性 -->
        <div v-if="form.role === 'STUDENT'" class="role-specific-box">
          <div class="form-group">
            <label class="form-label">入学年份</label>
            <input 
              type="number" 
              v-model.number="form.enrollment_year" 
              class="form-control" 
              placeholder="例如: 2026" 
              required 
            />
          </div>
          <div class="form-group" style="margin-bottom: 0;">
            <label class="form-label">学期修正量 (偏置数)</label>
            <input 
              type="number" 
              v-model.number="form.semester_offset" 
              class="form-control" 
              required 
            />
            <small style="color: var(--text-muted); font-size: 11px;">0为默认值。+1表示跳级，-1表示休学一学期</small>
          </div>
        </div>

        <!-- 家长专属属性 -->
        <div v-if="form.role === 'PARENT'" class="role-specific-box">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
            <label class="form-label" style="margin-bottom: 0;">关联的学生孩子</label>
            <button 
              type="button" 
              class="btn btn-secondary select-child-btn" 
              @click="showChildModal = true"
            >
              🔍 选择孩子 (已选 {{ form.student_ids.length }})
            </button>
          </div>
          
          <div class="selected-children-preview">
            <span v-if="form.student_ids.length === 0" style="color: var(--text-muted); font-size: 13px;">尚未关联任何孩子</span>
            <div v-else class="preview-badge-container">
              <span v-for="id in form.student_ids" :key="id" class="badge-child">
                {{ getStudentNameById(id) }}
                <span class="remove-btn" @click.stop="removeStudent(id)">×</span>
              </span>
            </div>
          </div>
        </div>

        <div class="modal-actions" style="margin-top: 24px;">
          <button type="button" class="btn btn-secondary action-btn" @click="$emit('close')">取消</button>
          <button type="submit" class="btn btn-primary action-btn" :disabled="isSubmitting">
            {{ isSubmitting ? '保存中...' : '💾 保存修改' }}
          </button>
        </div>
      </form>
    </div>

    <!-- 子弹窗：选择孩子 -->
    <ChildSelectModal 
      :visible="showChildModal" 
      v-model="form.student_ids" 
      :all-students="allStudents" 
      @close="showChildModal = false" 
    />
  </div>
</template>

<script setup>
import { ref, reactive, watch } from 'vue';
import { api } from '../services/api.js';
import { showAlert } from '../services/dialog.js';
import ChildSelectModal from './ChildSelectModal.vue';

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  user: {
    type: Object,
    default: null
  },
  allStudents: {
    type: Array,
    default: () => []
  }
});

const emit = defineEmits(['close', 'saved']);

const errorMsg = ref('');
const isSubmitting = ref(false);
const showChildModal = ref(false);

const form = reactive({
  username: '',
  nickname: '',
  role: 'PARENT',
  enrollment_year: new Date().getFullYear(),
  semester_offset: 0,
  student_ids: []
});

watch(
  () => props.user,
  (u) => {
    if (u) {
      errorMsg.value = '';
      form.username = u.username || '';
      form.nickname = u.nickname || '';
      form.role = u.role === 'ADMIN' ? 'PARENT' : u.role;
      form.enrollment_year = u.student_profile?.enrollment_year || new Date().getFullYear();
      form.semester_offset = u.student_profile?.semester_offset ?? 0;
      form.student_ids = u.student_ids ? [...u.student_ids] : [];
    }
  },
  { immediate: true }
);

function getStudentNameById(id) {
  const s = props.allStudents.find(item => item.id === id);
  return s ? (s.nickname || s.username) : '已选学生';
}

function removeStudent(id) {
  form.student_ids = form.student_ids.filter(item => item !== id);
}

async function handleSave() {
  if (!props.user) return;
  errorMsg.value = '';
  isSubmitting.value = true;

  const body = {
    username: form.username.trim(),
    nickname: form.nickname.trim(),
    role: form.role
  };

  if (form.role === 'STUDENT') {
    body.enrollment_year = form.enrollment_year;
    body.semester_offset = form.semester_offset;
  } else if (form.role === 'PARENT') {
    body.student_ids = form.student_ids;
  }

  try {
    const res = await api.put(`/users/${props.user.id}`, body);
    const data = await res.json();
    if (res.ok) {
      await showAlert('用户信息修改成功！');
      emit('saved');
      emit('close');
    } else {
      errorMsg.value = data.msg || '修改失败';
    }
  } catch (err) {
    errorMsg.value = '网络错误，修改失败';
  } finally {
    isSubmitting.value = false;
  }
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(4px);
  z-index: 1000;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
}

.modal-card {
  width: 100%;
  max-width: 520px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 26px;
  box-shadow: var(--shadow-lg);
  animation: modal-fadeIn 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  max-height: 90vh;
  overflow-y: auto;
}

.modal-title {
  font-size: 19px;
  font-weight: 800;
  color: var(--text-main);
  margin-bottom: 4px;
}

.modal-subtitle {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 16px;
}

.error-banner {
  color: var(--danger);
  background: rgba(239, 68, 68, 0.08);
  border: 1px solid rgba(239, 68, 68, 0.2);
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  margin-bottom: 16px;
  font-weight: 700;
  font-size: 13px;
}

.role-specific-box {
  border-left: 3px solid var(--primary);
  padding-left: 14px;
  margin-bottom: 16px;
  margin-top: 14px;
}

.select-child-btn {
  width: auto;
  padding: 4px 10px;
  font-size: 12px;
  margin-bottom: 0;
}

.selected-children-preview {
  min-height: 42px;
  background: var(--bg-main);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 10px;
}

.preview-badge-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.badge-child {
  background: var(--primary-light);
  color: var(--primary);
  border: 1px solid rgba(16, 185, 129, 0.2);
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12.5px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.remove-btn {
  cursor: pointer;
  font-weight: 900;
  font-size: 14px;
  opacity: 0.7;
  transition: var(--transition);
}
.remove-btn:hover {
  opacity: 1;
  color: var(--danger);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.action-btn {
  width: auto;
  padding: 0 18px;
  height: 38px;
  font-weight: 700;
}

@keyframes modal-fadeIn {
  from { opacity: 0; transform: scale(0.96); }
  to { opacity: 1; transform: scale(1); }
}
</style>
