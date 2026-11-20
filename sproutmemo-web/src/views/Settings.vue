<template>
  <div class="settings-container">
    <div class="card settings-paper-card">
      <div class="card-header-row">
        <div class="header-icon-box">
          <AppIcon name="palette" :size="22" />
        </div>
        <div>
          <h3 class="card-title">手账纸张与配色设定</h3>
          <p class="card-subtitle">精选 17 款护眼亮色手账配色（朴实纸张 / 温暖治愈 / 马卡龙 / 多巴胺活力），点击即可实时换肤。</p>
        </div>
      </div>

      <div v-for="group in themeGroups" :key="group.category" class="theme-group-block">
        <div class="theme-section-title">{{ group.category }}</div>
        <div class="theme-grid">
          <button
            type="button"
            v-for="t in group.items"
            :key="t.id"
            class="theme-card"
            :class="{ active: activeThemeId === t.id }"
            @click="selectTheme(t.id)"
          >
            <div class="color-preview-bar" :style="{ backgroundColor: t.darkBg, borderBottomColor: t.rule }">
              <span class="color-swatch" :style="{ backgroundColor: t.neonBlue }" title="主强调色"></span>
              <span class="color-swatch" :style="{ backgroundColor: t.textPrimary }" title="墨水主字色"></span>
              <span class="color-swatch" :style="{ backgroundColor: t.paper3 }" title="便签辅色"></span>
            </div>
            <div class="theme-info">
              <span class="theme-name">{{ t.name }}</span>
              <span class="check-mark" v-if="activeThemeId === t.id">
                <AppIcon name="check" :size="14" />
              </span>
            </div>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { themeList, defaultTheme, applyTheme } from '../utils/theme.js';
import AppIcon from '../components/AppIcon.vue';

const savedId = Number(localStorage.getItem('theme-id'));
const validTheme = themeList.find(t => t.id === savedId) || defaultTheme;
const activeThemeId = ref(validTheme.id);

const themeGroups = computed(() => {
  const order = ['朴实纸张', '温暖治愈', '马卡龙', '多巴胺活力'];
  return order
    .map(cat => ({
      category: cat,
      items: themeList.filter(t => t.category === cat)
    }))
    .filter(g => g.items.length > 0);
});

function selectTheme(id) {
  activeThemeId.value = id;
  localStorage.setItem('theme-id', String(id));
  applyTheme(id);
  window.dispatchEvent(new Event('theme-updated'));
}
</script>

<style scoped>
.settings-container {
  max-width: 960px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
}

.settings-paper-card {
  background: var(--bg-card);
  border: 2px solid var(--border);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  box-shadow: 0 4px 0 0 var(--color-edge), var(--shadow-sm);
}

.card-header-row {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
  padding-bottom: var(--space-md);
  border-bottom: 2px dashed var(--border);
}

.header-icon-box {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: var(--primary-light);
  color: var(--primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.card-title {
  margin: 0;
  font-size: 19px;
  font-weight: 800;
  color: var(--text-main);
}

.card-subtitle {
  margin: 4px 0 0;
  font-size: var(--text-sm);
  color: var(--text-muted);
}

.theme-group-block + .theme-group-block {
  margin-top: var(--space-lg);
}

.theme-section-title {
  display: inline-block;
  padding: 3px 12px;
  border-radius: 999px;
  background: var(--color-paper-2);
  border: 1.5px solid var(--border);
  font-size: var(--text-sm);
  font-weight: 800;
  color: var(--text-main);
  margin-bottom: var(--space-sm);
}

.theme-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(175px, 1fr));
  gap: var(--space-md);
}

.theme-card {
  appearance: none;
  text-align: left;
  background: var(--bg-card);
  border: 2px solid var(--border);
  border-radius: var(--radius-md);
  padding: 0;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 3px 0 0 var(--color-edge);
  transition: transform var(--dur-micro) var(--ease-ui), border-color var(--dur-micro) var(--ease-ui), box-shadow var(--dur-micro) var(--ease-ui);
}

.theme-card:hover {
  transform: translateY(-2px);
  border-color: var(--primary);
  box-shadow: 0 5px 0 0 var(--color-edge);
}

.theme-card:active {
  transform: translateY(1px);
}

.theme-card.active {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light), 0 3px 0 0 var(--primary);
}

.color-preview-bar {
  height: 54px;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  border-bottom: 1.5px dashed var(--border);
}

.color-swatch {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 1.5px solid rgba(27, 36, 50, 0.16);
}

.theme-info {
  padding: 10px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}

.theme-name {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-main);
}

.check-mark {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--primary);
  color: var(--text-light);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
</style>
