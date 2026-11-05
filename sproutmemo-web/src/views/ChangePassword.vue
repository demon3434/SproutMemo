<template>
  <div class="password-container" style="max-width: 500px; margin: 0 auto;">
    <div class="card">
      <h3 class="card-title">🔐 修改登录密码</h3>
      <p class="card-subtitle">为了您的账户安全，建议定期更换高强度的密码。</p>

      <form @submit.prevent="handleSavePassword">
        <div class="form-group">
          <label class="form-label">新登录密码</label>
          <input
            type="password"
            v-model="password"
            class="form-control"
            placeholder="输入至少 6 位的新密码"
            required
          />
        </div>

        <div class="form-group" style="margin-top: 15px;">
          <label class="form-label">确认新密码</label>
          <input
            type="password"
            v-model="confirmPassword"
            class="form-control"
            placeholder="请再次输入新密码"
            required
          />
        </div>

        <!-- 按钮 -->
        <div style="margin-top: 32px; display: flex;">
          <button type="submit" class="btn btn-primary" style="flex: 1; height: 42px;">确认修改密码</button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { api } from '../services/api.js';
import { showAlert, showConfirm } from '../services/dialog.js';

const password = ref('');
const confirmPassword = ref('');

async function handleSavePassword() {
  if (password.value.length < 6) {
    await showAlert('新密码长度必须至少为 6 位！');
    return;
  }
  if (password.value !== confirmPassword.value) {
    await showAlert('两次输入的新密码不一致！');
    return;
  }

  const ok = await showConfirm('确定要更新您的登录密码吗？');
  if (!ok) return;

  try {
    const res = await api.put('/auth/profile', {
      password: password.value
    });
    if (res.ok) {
      password.value = '';
      confirmPassword.value = '';
      await showAlert('密码修改成功！');
    } else {
      await showAlert((await res.json()).msg || '密码修改失败');
    }
  } catch (err) {
    await showAlert('网络错误，请稍后再试');
  }
}
</script>

<style scoped>
.password-container {
  padding: 20px 0;
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

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 13.5px;
  font-weight: 700;
  color: var(--text-secondary);
}
</style>
