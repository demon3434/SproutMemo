<template>
  <div class="settings-container" style="max-width: 800px; margin: 0 auto;">
    <div class="tab-content">
      <!-- 主题配色页面 -->
      <div class="card">
        <div class="card-header-row">
          <span class="header-icon">🎨</span>
          <div>
            <h3 class="card-title">系统主题与配色设定</h3>
            <p class="card-subtitle">点击任意主题卡片即可直接换肤应用。我们为您精心适配了 12 套明亮主题与 6 套极客暗色主题。</p>
          </div>
        </div>

        <!-- 亮色主题网格 -->
        <div class="theme-section-title">☀️ 明亮暖色系列</div>
        <div class="theme-grid">
          <div 
            v-for="t in lightThemes" 
            :key="t.id" 
            class="theme-card" 
            :class="{ active: activeThemeId === t.id }"
            @click="selectTheme(t.id)"
          >
            <div class="color-preview-bar" :style="{ backgroundColor: t.darkBg }">
              <span class="color-dot" :style="{ backgroundColor: t.neonBlue }" title="强调高亮色"></span>
              <span class="color-dot" :style="{ backgroundColor: t.textPrimary }" title="正文文字色"></span>
              <span class="color-dot" :style="{ backgroundColor: t.textSecondary }" title="副栏文字色"></span>
            </div>
            <div class="theme-info">
              <span class="theme-name">{{ t.name }}</span>
              <span class="check-mark" v-if="activeThemeId === t.id">✔</span>
            </div>
          </div>
        </div>

        <!-- 暗色主题网格 -->
        <div class="theme-section-title" style="margin-top: 28px;">🌙 极客暗夜系列</div>
        <div class="theme-grid">
          <div 
            v-for="t in darkThemes" 
            :key="t.id" 
            class="theme-card dark-theme-card" 
            :class="{ active: activeThemeId === t.id }"
            @click="selectTheme(t.id)"
          >
            <div class="color-preview-bar" :style="{ backgroundColor: t.darkBg }">
              <span class="color-dot" :style="{ backgroundColor: t.neonBlue }" title="强调高亮色"></span>
              <span class="color-dot" :style="{ backgroundColor: t.textPrimary }" title="正文文字色"></span>
              <span class="color-dot" :style="{ backgroundColor: t.textSecondary }" title="副栏文字色"></span>
            </div>
            <div class="theme-info">
              <span class="theme-name">{{ t.name }}</span>
              <span class="check-mark" v-if="activeThemeId === t.id">✔</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { themeList, applyTheme } from '../utils/theme.js';

// 1. 主题状态
const activeThemeId = ref(Number(localStorage.getItem('theme-id')) || 8);

const lightThemes = computed(() => themeList.filter(t => !t.isDark));
const darkThemes = computed(() => themeList.filter(t => t.isDark));

// ── 切换主题逻辑 (即时触发) ──
function selectTheme(id) {
  activeThemeId.value = id;
  localStorage.setItem('theme-id', id);
  // 执行动态 CSS 变量注入与暗色类变更
  applyTheme(id);
  // 全局广播主题变动通知
  window.dispatchEvent(new Event('theme-updated'));
}
</script>

<style scoped>
.settings-container {
  display: flex;
  flex-direction: column;
}

/* 顶部标签页样式 */
.tabs-header {
  display: flex;
  gap: 8px;
  border-bottom: 2px solid var(--border);
  margin-bottom: 24px;
}

.tab-item {
  padding: 12px 20px;
  cursor: pointer;
  font-weight: 700;
  font-size: 15px;
  color: var(--text-muted);
  border-bottom: 3px solid transparent;
  margin-bottom: -2px;
  transition: var(--transition);
}

.tab-item:hover {
  color: var(--primary);
}

.tab-item.active {
  color: var(--primary);
  border-bottom-color: var(--primary);
}

/* 卡片样式 */
.card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 28px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.02);
}

.card-header-row {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 24px;
}

.header-icon {
  font-size: 24px;
}

.card-title {
  font-size: 18px;
  font-weight: 800;
  color: var(--text-main);
  margin: 0 0 4px 0;
}

.card-subtitle {
  font-size: 13px;
  color: var(--text-muted);
  margin: 0;
}

.theme-section-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-secondary);
  border-left: 3px solid var(--primary);
  padding-left: 8px;
  margin-bottom: 12px;
}

/* 主题选择网格 */
.theme-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 16px;
}

.theme-card {
  background: var(--bg-main);
  border: 2px solid var(--border);
  border-radius: var(--radius-md);
  padding: 12px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 8px;
  position: relative;
  transition: var(--transition);
}

.theme-card:hover {
  transform: translateY(-2px);
  border-color: var(--primary);
}

.theme-card.active {
  border-color: var(--primary);
  background: var(--bg-card);
  box-shadow: 0 4px 12px var(--primary-light);
}

.color-preview-bar {
  height: 38px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.color-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 1px solid rgba(0,0,0,0.1);
  display: inline-block;
}

.theme-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.theme-name {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.check-mark {
  font-size: 12px;
  color: var(--primary);
  font-weight: bold;
}

/* 暗色主题卡片定制 */
.dark-theme-card .color-dot {
  border-color: rgba(255,255,255,0.1);
}

/* 广播表单 */
.discover-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group-checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
}

.form-group-checkbox input {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.form-group-checkbox label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
  cursor: pointer;
}

.form-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.flex-1 { flex: 1; min-width: 140px; }
.flex-2 { flex: 2; min-width: 260px; }

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-secondary);
}

.alert {
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 16px;
}
.alert-success { background: var(--primary-light); color: var(--primary); border: 1px solid var(--primary); }
.alert-danger { background: #fee2e2; color: #ef4444; border: 1px solid #fca5a5; }
</style>
