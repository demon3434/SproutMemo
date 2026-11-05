<template>
  <div style="display: grid; grid-template-columns: 1fr 2fr; gap: 32px; position: relative;">
    
    <!-- Create Account Form -->
    <div style="background-color: var(--bg-card); padding: 24px; border-radius: var(--radius-md); border: 1px solid var(--border); height: fit-content;">
      <h3 style="margin-bottom: 20px; font-weight: 700;">新建用户账号</h3>
      <div v-if="errorMsg" style="color: var(--danger); margin-bottom: 15px; font-weight: bold; font-size: 14px;">
        {{ errorMsg }}
      </div>
      
      <form @submit.prevent="handleAddUser">
        <div class="form-group">
          <label class="form-label">用户名</label>
          <input type="text" v-model="newUsername" class="form-control" placeholder="输入登录用户名" required autocomplete="off" />
        </div>
        <div class="form-group">
          <label class="form-label">登录密码</label>
          <input type="password" v-model="newPassword" class="form-control" placeholder="设置初始密码 (留空默认为 123456)" autocomplete="new-password" />
        </div>
        <div class="form-group">
          <label class="form-label">显示昵称</label>
          <input type="text" v-model="newNickname" class="form-control" placeholder="例如：爸爸/张晓明" required />
        </div>
        <div class="form-group">
          <label class="form-label">系统角色</label>
          <select v-model="newRole" class="form-control" required>
            <option value="PARENT">家长 (Parent)</option>
            <option value="STUDENT">学生/孩子 (Student)</option>
          </select>
        </div>

        <!-- Student-specific fields -->
        <div v-if="newRole === 'STUDENT'" style="border-left: 3px solid var(--primary); padding-left: 12px; margin-bottom: 16px;">
          <div class="form-group">
            <label class="form-label">入学年份</label>
            <input type="number" v-model.number="newEnrollmentYear" class="form-control" placeholder="例如: 2026" required />
          </div>
          <div class="form-group">
            <label class="form-label">学期修正量 (偏置数)</label>
            <input type="number" v-model.number="newSemesterOffset" class="form-control" required />
            <small style="color: var(--text-muted); font-size: 11px;">0为默认值。+1表示跳级，-1表示休学一学期</small>
          </div>
        </div>

        <!-- Parent-specific fields (select children) -->
        <div v-if="newRole === 'PARENT'" style="border-left: 3px solid var(--primary); padding-left: 12px; margin-bottom: 16px;">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
            <label class="form-label" style="margin-bottom: 0;">关联的学生孩子</label>
            <button 
              type="button" 
              class="btn btn-secondary select-child-btn" 
              @click="showCreateChildModal = true"
            >
              🔍 选择孩子 (已选 {{ selectedStudentIds.length }})
            </button>
          </div>
          
          <div class="selected-children-preview">
            <span v-if="selectedStudentIds.length === 0" style="color: var(--text-muted); font-size: 13px;">尚未关联任何孩子</span>
            <div v-else class="preview-badge-container">
              <span v-for="id in selectedStudentIds" :key="id" class="badge-child">
                {{ getStudentNameById(id) }}
                <span class="remove-btn" @click.stop="removeSelectedStudent(id)">×</span>
              </span>
            </div>
          </div>
        </div>

        <button type="submit" class="btn btn-primary" style="margin-top: 12px; width: 100%;">创建用户</button>
      </form>
    </div>

    <!-- Users List -->
    <div style="background-color: var(--bg-card); padding: 24px; border-radius: var(--radius-md); border: 1px solid var(--border);">
      <h3 style="margin-bottom: 20px; font-weight: 700;">账号列表</h3>
      <div style="overflow-x: auto;">
        <table style="width: 100%; border-collapse: collapse; text-align: left;">
          <thead>
            <tr style="border-bottom: 2px solid var(--border); font-weight: bold; color: var(--text-muted);">
              <th style="padding: 12px 8px;">用户名</th>
              <th style="padding: 12px 8px;">角色</th>
              <th style="padding: 12px 8px;">昵称 / 属性</th>
              <th style="padding: 12px 8px;">启用状态</th>
              <th style="padding: 12px 8px; text-align: right;">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in usersList" :key="u.id" style="border-bottom: 1px solid var(--border); font-size: 14px;">
              <td style="padding: 12px 8px; font-weight: 600;">{{ u.username }}</td>
              <td style="padding: 12px 8px;">
                <span class="badge" :style="getRoleBadgeStyle(u.role)">
                  {{ u.role }}
                </span>
              </td>
              <td style="padding: 12px 8px;">
                <div style="font-weight: 600;">{{ u.nickname || '-' }}</div>
                <div v-if="u.role === 'STUDENT' && u.student_profile" style="font-size: 12px; color: var(--text-muted); margin-top: 4px;">
                  入学: {{ u.student_profile.enrollment_year }} | 偏置: {{ u.student_profile.semester_offset }}
                </div>
                <div v-if="u.role === 'PARENT'" style="font-size: 12px; color: var(--text-muted); margin-top: 4px;">
                  关联孩子: {{ formatChildNames(u.student_ids) }}
                </div>
              </td>
              <td style="padding: 12px 8px;">
                <span style="font-weight: 600;" :style="{ color: u.is_active ? 'var(--primary)' : 'var(--danger)' }">
                  {{ u.is_active ? '启用' : '禁用' }}
                </span>
              </td>
              <td style="padding: 12px 8px; text-align: right; display: flex; gap: 8px; justify-content: flex-end;">
                <!-- 内置 admin 账号隐藏「修改」按钮 (方案 A) -->
                <button 
                  v-if="u.role !== 'ADMIN'" 
                  class="btn btn-secondary" 
                  @click="handleEdit(u)" 
                  style="padding: 4px 8px; font-size: 12px; width: auto;"
                >
                  ✏️ 修改
                </button>
                <button 
                  class="btn btn-secondary" 
                  @click="handleResetPassword(u)" 
                  style="padding: 4px 8px; font-size: 12px; width: auto;"
                >
                  🔑 重置
                </button>
                <button 
                  v-if="u.role !== 'ADMIN'" 
                  class="btn" 
                  :class="u.is_active ? 'btn-danger' : 'btn-primary'" 
                  @click="handleToggleActive(u)" 
                  style="padding: 4px 8px; font-size: 12px; width: auto;"
                >
                  {{ u.is_active ? '禁用' : '启用' }}
                </button>
              </td>
            </tr>
            <tr v-if="usersList.length === 0">
              <td colspan="5" style="text-align: center; padding: 24px; color: var(--text-muted);">暂无可用用户数据</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 后端分页器 -->
      <div class="pagination-bar" v-if="totalPages > 1">
        <button 
          class="btn btn-secondary pagination-btn" 
          :disabled="currentPage === 1" 
          @click="fetchUsers(currentPage - 1)"
        >上一页</button>
        <span class="pagination-info">第 {{ currentPage }} / {{ totalPages }} 页 (共 {{ totalUsers }} 个账号)</span>
        <button 
          class="btn btn-secondary pagination-btn" 
          :disabled="currentPage === totalPages" 
          @click="fetchUsers(currentPage + 1)"
        >下一页</button>
      </div>
    </div>

    <!-- 新建账号关联孩子弹出选择层 -->
    <ChildSelectModal 
      :visible="showCreateChildModal" 
      v-model="selectedStudentIds" 
      :all-students="allStudents" 
      @close="showCreateChildModal = false" 
    />

    <!-- 编辑用户模态弹窗 -->
    <UserEditModal 
      :visible="showEditModal" 
      :user="selectedUserForEdit" 
      :all-students="allStudents" 
      @close="showEditModal = false" 
      @saved="onUserSaved" 
    />

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { api } from '../services/api.js';
import { showAlert, showConfirm, showPrompt } from '../services/dialog.js';
import ChildSelectModal from '../components/ChildSelectModal.vue';
import UserEditModal from '../components/UserEditModal.vue';

// 1. 用户分页列表数据
const usersList = ref([]);
const totalUsers = ref(0);
const currentPage = ref(1);
const limit = 10;

const totalPages = computed(() => {
  return Math.ceil(totalUsers.value / limit);
});

// 2. 表单新建账号数据
const newUsername = ref('');
const newPassword = ref('');
const newNickname = ref('');
const newRole = ref('PARENT');
const newEnrollmentYear = ref(new Date().getFullYear());
const newSemesterOffset = ref(0);
const selectedStudentIds = ref([]);
const errorMsg = ref('');

// 3. 所有学生数据库列表 (不分页，用于选择孩子映射)
const allStudents = ref([]);

// 4. 弹出框模态层控制
const showCreateChildModal = ref(false);
const showEditModal = ref(false);
const selectedUserForEdit = ref(null);

async function fetchUsers(page = 1) {
  currentPage.value = page;
  try {
    const res = await api.get(`/users?page=${page}&limit=${limit}`);
    if (res.ok) {
      const resData = (await res.json()).data;
      usersList.value = resData.items || [];
      totalUsers.value = resData.total || 0;
    }
  } catch (err) {
    console.error('Failed to fetch users page:', err);
  }
}

// 专门拉取所有学生（不分页）供选择绑定
async function fetchAllStudents() {
  try {
    const res = await api.get('/users?role=STUDENT');
    if (res.ok) {
      allStudents.value = (await res.json()).data.items || [];
    }
  } catch (err) {
    console.error('Failed to fetch all students list:', err);
  }
}

function getStudentNameById(id) {
  const s = allStudents.value.find(item => item.id === id);
  return s ? (s.nickname || s.username) : '未加载';
}

function formatChildNames(ids) {
  if (!ids || ids.length === 0) return '尚未绑定';
  const names = ids.map(id => getStudentNameById(id));
  return names.join(', ');
}

function removeSelectedStudent(id) {
  selectedStudentIds.value = selectedStudentIds.value.filter(item => item !== id);
}

function getRoleBadgeStyle(role) {
  if (role === 'ADMIN') return { backgroundColor: '#fee2e2', color: '#991b1b' };
  if (role === 'PARENT') return { backgroundColor: '#dbeafe', color: '#1e40af' };
  return { backgroundColor: '#dcfce7', color: '#166534' };
}

async function handleAddUser() {
  errorMsg.value = '';
  const pwdVal = newPassword.value.trim() || '123456';
  const body = {
    username: newUsername.value,
    password: pwdVal,
    nickname: newNickname.value,
    role: newRole.value
  };

  if (newRole.value === 'STUDENT') {
    body.enrollment_year = newEnrollmentYear.value;
    body.semester_offset = newSemesterOffset.value;
  } else if (newRole.value === 'PARENT') {
    body.student_ids = selectedStudentIds.value;
  }

  try {
    const res = await api.post('/users', body);
    if (res.ok) {
      newUsername.value = '';
      newPassword.value = '';
      newNickname.value = '';
      selectedStudentIds.value = [];
      
      // 刷新数据
      fetchUsers(1);
      fetchAllStudents();
    } else {
      errorMsg.value = (await res.json()).msg || '创建失败';
    }
  } catch (err) {
    errorMsg.value = '连接服务器失败';
  }
}

function handleEdit(u) {
  selectedUserForEdit.value = u;
  showEditModal.value = true;
}

function onUserSaved() {
  fetchUsers(currentPage.value);
  fetchAllStudents();
}

async function handleResetPassword(u) {
  const newPwd = await showPrompt(`请输入用户 "${u.username}" 的新密码 (留空则默认为 123456):`);
  if (newPwd === null) return;
  const pwdVal = newPwd.trim() || '123456';
  try {
    const res = await api.put(`/users/${u.id}/reset_password`, { new_password: pwdVal });
    if (res.ok) {
      await showAlert(`密码已重置为: ${pwdVal}`);
    } else {
      await showAlert((await res.json()).msg || '重置失败');
    }
  } catch (err) {
    await showAlert('网络错误');
  }
}

async function handleToggleActive(u) {
  const isConfirmed = await showConfirm(`确定要${u.is_active ? '禁用' : '启用'}此账号吗？`);
  if (!isConfirmed) return;
  try {
    const res = await api.put(`/users/${u.id}/toggle_active`, { is_active: !u.is_active });
    if (res.ok) fetchUsers(currentPage.value);
  } catch (err) {
    await showAlert('网络错误');
  }
}

onMounted(() => {
  fetchUsers(1);
  fetchAllStudents();
});
</script>

<style scoped>
/* 选择孩子按钮 */
.select-child-btn {
  width: auto;
  padding: 4px 10px;
  font-size: 12px;
  margin-bottom: 0;
}

/* 预览孩子徽章 */
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
  border: 1px solid rgba(16,185,129,0.2);
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

/* 分页器 */
.pagination-bar {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}

.pagination-btn {
  width: auto;
  padding: 6px 16px;
  font-size: 13.5px;
  font-weight: 700;
}

.pagination-info {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-muted);
}
</style>
