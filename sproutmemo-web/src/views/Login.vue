<template>
  <div class="auth-container login-stage">
    <!-- 可爱手账 SVG 贴纸装饰（纯装饰，不占内容宽度） -->
    <svg class="sticker-float sticker-tl" width="76" height="76" viewBox="0 0 80 80" aria-hidden="true">
      <circle cx="40" cy="40" r="34" fill="#fef6d8" stroke="#e6be4c" stroke-width="3" stroke-dasharray="6 4" />
      <path d="M40 18 L46 32 L61 34 L50 44 L53 59 L40 52 L27 59 L30 44 L19 34 L34 32 Z" fill="#f6c945" stroke="#c28e12" stroke-width="2.5" stroke-linejoin="round" />
    </svg>
    <svg class="sticker-float sticker-br" width="90" height="90" viewBox="0 0 96 96" aria-hidden="true">
      <rect x="8" y="8" width="80" height="80" rx="22" fill="#e4f6ec" stroke="#68c196" stroke-width="3" />
      <path d="M48 68 V42 M48 44 C48 30 60 22 70 22 C70 34 60 44 48 44 Z M48 50 C48 38 37 31 27 31 C27 42 36 50 48 50 Z" fill="#169863" stroke="#0e6f47" stroke-width="2.5" stroke-linejoin="round" />
    </svg>

    <div class="auth-card notebook-cover">
      <div class="washi-tape cover-tape" aria-hidden="true"></div>
      <div class="binder-rings" aria-hidden="true">
        <div class="ring"></div>
        <div class="ring"></div>
        <div class="ring"></div>
        <div class="ring"></div>
        <div class="ring"></div>
      </div>

      <div class="cover-badge">我的专属错题手账本</div>

      <div class="login-brand">
        <div class="login-logo-wrap">
          <img src="/icon.png" alt="知拾 SproutMemo" class="login-logo" />
        </div>
        <div class="login-title-group">
          <h1 class="auth-title">知拾</h1>
          <p class="auth-subtitle">翻开本子，把错题变成小星星</p>
        </div>
      </div>

      <div v-if="errorMsg" class="login-error" role="alert">
        {{ errorMsg }}
      </div>

      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label class="form-label" for="login-username">小主人名字（用户名）</label>
          <input
            id="login-username"
            type="text"
            v-model="username"
            class="form-control"
            placeholder="请输入用户名"
            required
            autocomplete="username"
          />
        </div>
        <div class="form-group">
          <label class="form-label" for="login-password">秘密钥匙（密码）</label>
          <input
            id="login-password"
            type="password"
            v-model="password"
            class="form-control"
            placeholder="请输入密码"
            required
            autocomplete="current-password"
          />
        </div>
        <button type="submit" class="btn btn-primary login-submit" :disabled="loggingIn">
          <AppIcon name="sprout" :size="18" />
          <span>{{ loggingIn ? '正在翻开错题本...' : '翻开我的错题本' }}</span>
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, inject } from 'vue';
import { useRouter } from 'vue-router';
import { api, tokens } from '../services/api.js';
import AppIcon from '../components/AppIcon.vue';

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

<style scoped>
.login-stage {
  position: relative;
  overflow: hidden;
}

.sticker-float {
  position: absolute;
  pointer-events: none;
  filter: drop-shadow(0 3px 0 rgba(44, 39, 35, 0.1));
}

.sticker-tl {
  top: 44px;
  left: 64px;
  transform: rotate(-12deg);
}

.sticker-br {
  bottom: 48px;
  right: 76px;
  transform: rotate(10deg);
}

@media (max-width: 720px) {
  .sticker-float { display: none; }
}

.notebook-cover {
  position: relative;
  padding: 36px 34px 34px 48px;
  text-align: left;
}

.cover-tape {
  top: -10px;
  right: 28px;
  transform: rotate(12deg);
}

.binder-rings {
  position: absolute;
  left: -13px;
  top: 38px;
  bottom: 38px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.ring {
  width: 26px;
  height: 15px;
  border-radius: 999px;
  background: linear-gradient(180deg, #fffdf8 0%, #d8cbb0 55%, #b3a386 100%);
  border: 2px solid #8c7b60;
  box-shadow: 2px 2px 0 rgba(0, 0, 0, 0.12);
}

.cover-badge {
  display: inline-block;
  background: var(--sunny-soft);
  border: 1.5px dashed var(--sunny-border);
  color: var(--sunny-ink);
  font-size: 12px;
  font-weight: 800;
  padding: 4px 12px;
  border-radius: 999px;
  margin-bottom: 14px;
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 24px;
}

.login-logo-wrap {
  width: 64px;
  height: 64px;
  padding: 5px;
  background: var(--color-paper-2);
  border: 2px solid var(--border);
  border-radius: 18px;
  box-shadow: 0 3px 0 0 var(--color-edge);
  transform: rotate(-4deg);
  flex-shrink: 0;
}

.login-logo {
  width: 100%;
  height: 100%;
  border-radius: 12px;
  object-fit: cover;
}

.login-title-group .auth-title {
  margin: 0;
  font-size: 28px;
  text-align: left;
}

.login-title-group .auth-subtitle {
  margin: 2px 0 0;
  text-align: left;
}

.login-error {
  padding: var(--space-xs) var(--space-sm);
  margin-bottom: var(--space-md);
  background: #fee2e2;
  color: var(--danger);
  border: 1.5px solid #fecaca;
  border-radius: var(--radius-input);
  font-size: var(--text-sm);
  font-weight: 700;
  text-align: center;
}

.login-submit {
  margin-top: var(--space-xs);
  min-height: 50px;
  font-size: 16px;
}
</style>
