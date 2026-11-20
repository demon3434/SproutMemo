#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const rootDir = path.resolve(__dirname, '..');
const themesPath = path.join(rootDir, 'design-system', 'themes.json');
const iconsPath = path.join(rootDir, 'design-system', 'icons.json');
const fontsSrcDir = path.join(rootDir, 'design-system', 'fonts');

const webTarget = path.join(rootDir, 'sproutmemo-web', 'src', 'assets', 'themes.generated.json');
const webIconsTarget = path.join(rootDir, 'sproutmemo-web', 'src', 'assets', 'icons.generated.json');
const webFontsTarget = path.join(rootDir, 'sproutmemo-web', 'src', 'assets', 'fonts');

const androidThemeTarget = path.join(
  rootDir,
  'sproutmemo-android',
  'app',
  'src',
  'main',
  'java',
  'com',
  'sproutmemo',
  'android',
  'ui',
  'theme',
  'AppThemeColors.kt'
);
const androidIconsTarget = path.join(
  rootDir,
  'sproutmemo-android',
  'app',
  'src',
  'main',
  'java',
  'com',
  'sproutmemo',
  'android',
  'ui',
  'theme',
  'AppIcons.kt'
);
const androidFontsTarget = path.join(
  rootDir,
  'sproutmemo-android',
  'app',
  'src',
  'main',
  'res',
  'font'
);

const themes = JSON.parse(fs.readFileSync(themesPath, 'utf8'));
const icons = JSON.parse(fs.readFileSync(iconsPath, 'utf8'));

// 1. Sync Themes, Icons & Web Fonts to sproutmemo-web
fs.mkdirSync(path.dirname(webTarget), { recursive: true });
fs.writeFileSync(webTarget, JSON.stringify(themes, null, 2) + '\n', 'utf8');
fs.writeFileSync(webIconsTarget, JSON.stringify(icons, null, 2) + '\n', 'utf8');

for (const fontFolder of ['nunito', 'lxgw-wenkai-lite']) {
  const src = path.join(fontsSrcDir, fontFolder);
  const dst = path.join(webFontsTarget, fontFolder);
  if (fs.existsSync(src)) {
    fs.cpSync(src, dst, { recursive: true, force: true });
  }
}

// 2. Sync Android TTF Fonts to res/font/
const androidFontsSrc = path.join(fontsSrcDir, 'android');
if (fs.existsSync(androidFontsSrc)) {
  fs.mkdirSync(androidFontsTarget, { recursive: true });
  for (const file of fs.readdirSync(androidFontsSrc)) {
    if (file.endsWith('.ttf') || file.endsWith('.otf')) {
      fs.copyFileSync(path.join(androidFontsSrc, file), path.join(androidFontsTarget, file));
    }
  }
}

// 3. Generate Android AppThemeColors.kt
function toComposeColor(hex) {
  const clean = hex.replace('#', '').toUpperCase();
  return `Color(0xFF${clean})`;
}

const ktEntries = themes
  .map(
    (t) => `    AppThemeColors(
        id = ${t.id},
        name = "${t.name}",
        category = "${t.category}",
        isDark = false,
        darkBg = ${toComposeColor(t.paper)},
        paper2 = ${toComposeColor(t.paper2)},
        paper3 = ${toComposeColor(t.paper3)},
        cardBg = ${toComposeColor(t.card)},
        rule = ${toComposeColor(t.rule)},
        edge = ${toComposeColor(t.edge)},
        textPrimary = ${toComposeColor(t.ink)},
        textSecondary = ${toComposeColor(t.muted)},
        neonBlue = ${toComposeColor(t.accent)},
        accentDeep = ${toComposeColor(t.accentDeep)},
        neonGreen = ${toComposeColor(t.success)},
        neonRed = ${toComposeColor(t.danger)},
        neonAmber = ${toComposeColor(t.warning)}
    )`
  )
  .join(',\n');

const ktThemeContent = `package com.sproutmemo.android.ui.theme

// AUTO-GENERATED from design-system/themes.json via scripts/sync_design_system.mjs
// Do not edit colors manually here; update design-system/themes.json and re-run sync.

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppThemeColors(
    val id: Int,
    val name: String,
    val category: String,
    val isDark: Boolean,
    val darkBg: Color,          // 主纸面底色 (paper)
    val paper2: Color,          // 次级纸面/顶栏 (paper2)
    val paper3: Color,          // 便签/强调纸面 (paper3)
    val cardBg: Color,          // 卡片亮纸面 (card)
    val rule: Color,            // 纸张边框/装订线 (rule)
    val edge: Color,            // 卡纸立体底边 (edge)
    val textPrimary: Color,     // 墨水主文字 (ink)
    val textSecondary: Color,   // 辅助文字 (muted)
    val neonBlue: Color,        // 主强调色 (accent)
    val accentDeep: Color,      // 主按钮立体底边 (accentDeep)
    val neonGreen: Color,       // 成功/掌握绿 (success)
    val neonRed: Color,         // 警示/错题红 (danger)
    val neonAmber: Color        // 提示/星星橙 (warning)
)

val ThemeList = listOf(
${ktEntries}
)

val DefaultThemeColors = ThemeList.first { it.id == 18 }

val LocalAppColors = staticCompositionLocalOf { DefaultThemeColors }
`;

fs.writeFileSync(androidThemeTarget, ktThemeContent, 'utf8');

// 4. Generate Android AppIcons.kt from design-system/icons.json
function kotlinColorOrNull(val) {
  if (!val || val === 'currentColor') return 'null';
  if (val === 'none') return 'Color.Transparent';
  return toComposeColor(val);
}

function kotlinFloatOrNull(val) {
  return val !== undefined && val !== null ? `${val}f` : 'null';
}

function parsePoints(pointsStr) {
  const nums = pointsStr
    .trim()
    .split(/[\s,]+/)
    .map(Number);
  const pairs = [];
  for (let i = 0; i + 1 < nums.length; i += 2) {
    pairs.push(`Offset(${nums[i]}f, ${nums[i + 1]}f)`);
  }
  return `listOf(${pairs.join(', ')})`;
}

const iconEntries = Object.entries(icons)
  .map(([name, def]) => {
    const shapes = (def.paths || []).map((item) => {
      const fill = kotlinColorOrNull(item.fill);
      const stroke = kotlinColorOrNull(item.stroke);
      const sw = kotlinFloatOrNull(item.strokeWidth);
      if (item.tag === 'circle') {
        return `            IconShape.Circle(${item.cx}f, ${item.cy}f, ${item.r}f, ${fill}, ${stroke}, ${sw})`;
      }
      if (item.tag === 'rect') {
        return `            IconShape.Rect(${item.x}f, ${item.y}f, ${item.width}f, ${item.height}f, ${item.rx || 0}f, ${fill}, ${stroke}, ${sw})`;
      }
      if (item.tag === 'ellipse') {
        return `            IconShape.Ellipse(${item.cx}f, ${item.cy}f, ${item.rx}f, ${item.ry}f, ${fill}, ${stroke}, ${sw})`;
      }
      if (item.tag === 'polyline') {
        return `            IconShape.Poly(${parsePoints(item.points)}, false, ${fill}, ${stroke}, ${sw})`;
      }
      if (item.tag === 'polygon') {
        return `            IconShape.Poly(${parsePoints(item.points)}, true, ${fill}, ${stroke}, ${sw})`;
      }
      return `            IconShape.Path("${item.d}", ${fill}, ${stroke}, ${sw})`;
    });
    const fb = (def.fallback || '•').replace(/"/g, '\\"');
    return `    "${name}" to AppIconDef(\n        fallback = "${fb}",\n        shapes = listOf(\n${shapes.join(',\n')}\n        )\n    )`;
  })
  .join(',\n');

const ktIconsContent = `package com.sproutmemo.android.ui.theme

// AUTO-GENERATED from design-system/icons.json via scripts/sync_design_system.mjs
// Do not edit icons manually here; update design-system/icons.json and re-run sync.

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

sealed class IconShape {
    data class Path(
        val d: String,
        val fill: Color? = null,
        val stroke: Color? = null,
        val strokeWidth: Float? = null
    ) : IconShape()

    data class Circle(
        val cx: Float,
        val cy: Float,
        val r: Float,
        val fill: Color? = null,
        val stroke: Color? = null,
        val strokeWidth: Float? = null
    ) : IconShape()

    data class Rect(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val rx: Float = 0f,
        val fill: Color? = null,
        val stroke: Color? = null,
        val strokeWidth: Float? = null
    ) : IconShape()

    data class Ellipse(
        val cx: Float,
        val cy: Float,
        val rx: Float,
        val ry: Float,
        val fill: Color? = null,
        val stroke: Color? = null,
        val strokeWidth: Float? = null
    ) : IconShape()

    data class Poly(
        val points: List<Offset>,
        val closed: Boolean,
        val fill: Color? = null,
        val stroke: Color? = null,
        val strokeWidth: Float? = null
    ) : IconShape()
}

data class AppIconDef(
    val fallback: String,
    val shapes: List<IconShape>
)

val AppIconsMap: Map<String, AppIconDef> = mapOf(
${iconEntries}
)
`;

fs.writeFileSync(androidIconsTarget, ktIconsContent, 'utf8');

// 5. Sync Typography Scale (platform-separated: Web rem vs Android sp)
const typographyPath = path.join(rootDir, 'design-system', 'typography.json');
if (fs.existsSync(typographyPath)) {
  const typography = JSON.parse(fs.readFileSync(typographyPath, 'utf8'));
  const a = typography.android || {};
  const androidScaleTarget = path.join(
    rootDir,
    'sproutmemo-android/app/src/main/java/com/sproutmemo/android/ui/theme/SproutTextScale.kt'
  );
  const ktScaleContent = `package com.sproutmemo.android.ui.theme

// AUTO-GENERATED from design-system/typography.json via scripts/sync_design_system.mjs
// Shared font family with Web; font sizes are tailored separately for Android screens.

import androidx.compose.ui.unit.sp

object SproutTextScale {
    val PageTitle = ${a.pageTitle || 22}.sp
    val SectionTitle = ${a.sectionTitle || 16}.sp
    val CardTitle = ${a.cardTitle || 16}.sp
    val Body = ${a.body || 15.5}.sp
    val Label = ${a.label || 14}.sp
    val Button = ${a.button || 14.5}.sp
    val Caption = ${a.caption || 13}.sp
    val Badge = ${a.badge || 12}.sp
}
`;
  fs.writeFileSync(androidScaleTarget, ktScaleContent, 'utf8');
}

console.log(`Synced ${themes.length} themes, ${Object.keys(icons).length} icons & typography scale to Web & Android.`);
