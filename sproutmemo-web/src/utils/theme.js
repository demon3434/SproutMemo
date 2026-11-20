import generatedThemes from '../assets/themes.generated.json';

// 由 design-system/themes.json 自动同步生成，保持双端配色 100% 一致
export const themeList = generatedThemes.map((t) => ({
  id: t.id,
  name: t.name,
  category: t.category,
  isDark: false,
  darkBg: t.paper,
  paper2: t.paper2,
  paper3: t.paper3,
  cardBg: t.card,
  rule: t.rule,
  edge: t.edge,
  textPrimary: t.ink,
  textSecondary: t.muted,
  neonBlue: t.accent,
  accentDeep: t.accentDeep
}));

export const defaultTheme = themeList.find((t) => t.id === 18) || themeList[0];

export function applyTheme(themeId) {
  let theme = themeList.find((t) => t.id === Number(themeId));
  if (!theme) {
    // 历史暗色主题 ID 或无效 ID 统一安全回退至默认亮色手账主题
    theme = defaultTheme;
  }

  const root = document.documentElement;
  root.style.setProperty('--color-paper', theme.darkBg);
  root.style.setProperty('--color-paper-2', theme.paper2);
  root.style.setProperty('--color-paper-3', theme.paper3);
  root.style.setProperty('--color-card', theme.cardBg);
  root.style.setProperty('--color-ink', theme.textPrimary);
  root.style.setProperty('--color-ink-2', theme.textPrimary);
  root.style.setProperty('--color-muted', theme.textSecondary);
  root.style.setProperty('--color-rule', theme.rule);
  root.style.setProperty('--color-edge', theme.edge);
  root.style.setProperty('--color-accent', theme.neonBlue);
  root.style.setProperty('--color-accent-deep', theme.accentDeep);
  root.style.setProperty('--color-accent-ink', '#FEFDF9');
  root.style.setProperty('--color-focus', theme.neonBlue);

  // 兼容现有视图与组件引用的语义变量
  root.style.setProperty('--primary', theme.neonBlue);
  root.style.setProperty('--primary-hover', theme.accentDeep);
  root.style.setProperty('--primary-light', `${theme.neonBlue}1A`);
  root.style.setProperty('--bg-main', theme.darkBg);
  root.style.setProperty('--bg-card', theme.cardBg);
  root.style.setProperty('--bg-input', theme.cardBg);
  root.style.setProperty('--text-main', theme.textPrimary);
  root.style.setProperty('--text-muted', theme.textSecondary);
  root.style.setProperty('--text-primary', theme.textPrimary);
  root.style.setProperty('--text-secondary', theme.textSecondary);
  root.style.setProperty('--border', theme.rule);
  root.style.setProperty('--bg-sidebar', theme.paper2);
  root.style.setProperty('--text-sidebar', theme.textSecondary);
  root.style.setProperty('--text-sidebar-hover', theme.neonBlue);
  root.style.setProperty('--bg-sidebar-hover', `${theme.neonBlue}12`);

  document.body.classList.remove('dark-mode');
}
