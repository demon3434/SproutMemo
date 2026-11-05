<template>
  <div class="auth-container">
    <div class="auth-card">
      <div style="text-align: center; margin-bottom: 24px;">
        <img src="/icon.png" style="width: 80px; height: 80px; border-radius: 16px; margin-bottom: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.1);" />
        <h1 class="auth-title" style="margin: 0; font-size: 32px; font-weight: 700; color: var(--text-primary); letter-spacing: 4px;">知拾</h1>
        <p class="auth-subtitle" style="margin: 4px 0 0 0; font-size: 13px; color: var(--text-secondary);">SproutMemo · 知识如种子发芽成长</p>
      </div>
      
      <div v-if="errorMsg" style="color: var(--danger); margin-bottom: 15px; font-weight: bold; text-align: center;">
        {{ errorMsg }}
      </div>
      
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label class="form-label">用户名</label>
          <input type="text" v-model="username" class="form-control" placeholder="请输入用户名" required autocomplete="username" />
        </div>
        <div class="form-group">
          <label class="form-label">密码</label>
          <input type="password" v-model="password" class="form-control" placeholder="请输入密码" required autocomplete="current-password" />
        </div>
        <button type="submit" class="btn btn-primary" style="width: 100%; margin-top: 24px; height: 48px; border-radius: 12px; font-size: 16px; font-weight: 600; background: linear-gradient(135deg, var(--primary), var(--primary-hover)); color: #ffffff; border: none; cursor: pointer;">
          {{ loggingIn ? '登录中...' : '立即登录' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, inject } from 'vue';
import { useRouter } from 'vue-router';
import { api, tokens } from '../services/api.js';

const router = useRouter();
const refreshAuth = inject('refreshAuth');

const username = ref('');
const password = ref('');
const errorMsg = ref('');
const loggingIn = ref(false);

async function handleLogin() {
  errorMsg.value = '';
  loggingIn.value = true;
  try {
    const res = await api.post('/auth/login', {
      username: username.value,
      password: password.value
    });
    const data = await res.json();
    if (res.ok && data.code === 200) {
      tokens.set(data.data.access_token, data.data.refresh_token);
      await refreshAuth();
    } else {
      errorMsg.value = data.msg || '登录失败，请检查用户名或密码';
    }
  } catch (err) {
    errorMsg.value = '无法连接到服务器，请稍后再试';
  } finally {
    loggingIn.value = false;
  }
}
</script>
