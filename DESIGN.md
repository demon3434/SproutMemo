# Design: SproutMemo (知拾)

The locked system. Web (`sproutmemo-web`) and Android (`sproutmemo-android`) share this warm stationery / paper-notebook design language. Light-only themes; no dark mode.

## System
- Genre: playful
- Shape of the first page: app-workbench (error-book workspace for elementary school students & parents)
- Theme: Hum (warm cream paper, card-stock controls that press down, blue-black ink, mint for growth)
- Axes: light/rounded-sans/chromatic-yellow · posture: full-palette

## Tokens
`sproutmemo-web/src/assets/styles/tokens.css` is the Web source of truth; `sproutmemo-android/app/src/main/java/com/sproutmemo/android/ui/theme/AppThemeColors.kt` mirrors these semantic roles for Jetpack Compose:

```css
:root {
  color-scheme: light;
  --color-paper: oklch(97% 0.012 95);       /* #FAF8F2 暖手账纸底色 */
  --color-paper-2: oklch(94.5% 0.018 95);   /* #F3EFE3 次级纸面/表头 */
  --color-paper-3: oklch(91% 0.022 95);     /* #EAE4D3 便签/分区底色 */
  --color-card: oklch(98.8% 0.007 95);      /* #FEFDF9 卡片亮纸面 */
  --color-ink: oklch(20% 0.014 255);        /* #1B2432 蓝黑墨水主字色 */
  --color-ink-2: oklch(29% 0.014 255);      /* #333E4F 次级正文 */
  --color-muted: oklch(47% 0.014 255);      /* #5F6A7A 辅助说明字色 */
  --color-rule: oklch(87% 0.016 95);        /* #DED7C6 纸张装订线/边框 */
  --color-edge: oklch(83% 0.04 88);         /* #D5C9AD 卡纸立体底边 */
  --color-accent: oklch(87% 0.175 96);      /* #F5CF3B 默认主题主强调 */
  --color-accent-deep: oklch(72% 0.16 92);  /* #C99D1A 主按钮立体底边 */
  --color-accent-ink: oklch(20% 0.014 255); /* #1B2432 主按钮文字 */
  --color-accent-2: oklch(50% 0.17 250);    /* #1D68D6 链接/选中焦点蓝 */
  --color-accent-3: oklch(70% 0.2 22);      /* #F26449 错题警示珊瑚红 */
  --color-mint: oklch(82% 0.15 152);        /* #7CE0A6 掌握/成长薄荷绿 */
  --color-mint-deep: oklch(48% 0.12 155);   /* #1E8550 成长深绿 */
  --color-focus: oklch(50% 0.17 250);       /* #1D68D6 键盘/无障碍焦点环 */
  --radius-card: 20px;
  --radius-stock: 14px;
  --radius-input: 12px;
  --radius-pill: 999px;
  --rule-hair: 1px;
  --shadow-card: 0 1px 2px oklch(20% 0.014 255 / 0.06), 0 10px 24px -14px oklch(20% 0.014 255 / 0.16);
}
```

## Controls
- **Primary (Card-stock tactile button)**: Fill `--color-accent` (or active light theme's accent), text `--color-accent-ink` (or high-contrast ink), `--radius-stock`, min-height `44px`–`48px`, `1px` rule + solid `3px`–`4px` bottom edge (`--color-accent-deep`) that presses down on `:active`.
- **Secondary**: `--color-card` fill, `1px` `--color-rule` border, `3px` `--color-edge` bottom lip; same radius and height.
- **Inputs**: `--color-card` fill, `1px` `--color-rule` border, `--radius-input`, `--color-focus` focus ring.
- **Iconography**: Unified rounded vector icons (`AppIcon.vue` on Web, `Icons.Rounded.*` on Android). No raw Unicode emojis as functional UI icons.
- **Dialogs**: Custom modal overlays only; never browser-native `alert()`, `confirm()`, or `prompt()`.

## Motion
- Character: brisk & tactile (`--ease-spring` on primary card-stock press only; `--ease-snap` elsewhere). Never `transition: all`. Under `prefers-reduced-motion: reduce`, transitions collapse immediately.

## Exports
- Web: `sproutmemo-web/src/assets/styles/tokens.css`, `sproutmemo-web/src/assets/styles/base.css`, `sproutmemo-web/src/utils/theme.js`
- Android: `sproutmemo-android/app/src/main/java/com/sproutmemo/android/ui/theme/AppThemeColors.kt`, `Theme.kt`

