package com.sproutmemo.android.ui.theme

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
    "users" to AppIconDef(
        fallback = "👥",
        shapes = listOf(
            IconShape.Path("M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2", null, null, null),
            IconShape.Circle(9f, 7f, 4f, null, null, null),
            IconShape.Path("M22 21v-2a4 4 0 0 0-3-3.87", null, null, null),
            IconShape.Path("M16 3.13a4 4 0 0 1 0 7.75", null, null, null)
        )
    ),
    "tag" to AppIconDef(
        fallback = "🏷️",
        shapes = listOf(
            IconShape.Path("M12 2H2v10l9.29 9.29c.94.94 2.48.94 3.42 0l6.58-6.58c.94-.94.94-2.48 0-3.42L12 2Z", null, null, null),
            IconShape.Path("M7 7h.01", null, null, null)
        )
    ),
    "calendar" to AppIconDef(
        fallback = "📅",
        shapes = listOf(
            IconShape.Rect(3f, 4f, 18f, 18f, 3f, null, null, null),
            IconShape.Path("M16 2v4M8 2v4M3 10h18", null, null, null)
        )
    ),
    "link" to AppIconDef(
        fallback = "🔗",
        shapes = listOf(
            IconShape.Path("M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71", null, null, null),
            IconShape.Path("M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71", null, null, null)
        )
    ),
    "database" to AppIconDef(
        fallback = "💾",
        shapes = listOf(
            IconShape.Ellipse(12f, 5f, 9f, 3f, null, null, null),
            IconShape.Path("M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5", null, null, null),
            IconShape.Path("M3 12c0 1.66 4 3 9 3s9-1.34 9-3", null, null, null)
        )
    ),
    "child" to AppIconDef(
        fallback = "👶",
        shapes = listOf(
            IconShape.Circle(12f, 8f, 5f, null, null, null),
            IconShape.Path("M20 21a8 8 0 0 0-16 0", null, null, null)
        )
    ),
    "book" to AppIconDef(
        fallback = "📖",
        shapes = listOf(
            IconShape.Path("M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1 0-5H20", null, null, null)
        )
    ),
    "file" to AppIconDef(
        fallback = "📄",
        shapes = listOf(
            IconShape.Path("M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z", null, null, null),
            IconShape.Poly(listOf(Offset(14f, 2f), Offset(14f, 8f), Offset(20f, 8f)), false, null, null, null),
            IconShape.Path("M8 13h8M8 17h5", null, null, null)
        )
    ),
    "list" to AppIconDef(
        fallback = "📋",
        shapes = listOf(
            IconShape.Rect(4f, 3f, 16f, 18f, 3f, null, null, null),
            IconShape.Path("M8 8h8M8 12h8M8 16h5", null, null, null)
        )
    ),
    "target" to AppIconDef(
        fallback = "🎯",
        shapes = listOf(
            IconShape.Circle(12f, 12f, 9f, null, null, null),
            IconShape.Circle(12f, 12f, 5f, null, null, null),
            IconShape.Circle(12f, 12f, 1.5f, null, null, null)
        )
    ),
    "pen" to AppIconDef(
        fallback = "✏️",
        shapes = listOf(
            IconShape.Path("M12 20h9", null, null, null),
            IconShape.Path("M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z", null, null, null)
        )
    ),
    "settings" to AppIconDef(
        fallback = "⚙️",
        shapes = listOf(
            IconShape.Circle(12f, 12f, 3f, null, null, null),
            IconShape.Path("M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1Z", null, null, null)
        )
    ),
    "user" to AppIconDef(
        fallback = "👤",
        shapes = listOf(
            IconShape.Circle(12f, 8f, 4f, null, null, null),
            IconShape.Path("M5 20a7 7 0 0 1 14 0", null, null, null)
        )
    ),
    "key" to AppIconDef(
        fallback = "🔑",
        shapes = listOf(
            IconShape.Circle(7.5f, 15.5f, 4.5f, null, null, null),
            IconShape.Path("m21 2-9.6 9.6M15.5 7.5l3 3L22 7l-3-3", null, null, null)
        )
    ),
    "logout" to AppIconDef(
        fallback = "🚪",
        shapes = listOf(
            IconShape.Path("M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4", null, null, null),
            IconShape.Poly(listOf(Offset(16f, 17f), Offset(21f, 12f), Offset(16f, 7f)), false, null, null, null),
            IconShape.Path("M9 12h12", null, null, null)
        )
    ),
    "chevron-left" to AppIconDef(
        fallback = "◀",
        shapes = listOf(
            IconShape.Poly(listOf(Offset(15f, 18f), Offset(9f, 12f), Offset(15f, 6f)), false, null, null, null)
        )
    ),
    "chevron-right" to AppIconDef(
        fallback = "▶",
        shapes = listOf(
            IconShape.Poly(listOf(Offset(9f, 18f), Offset(15f, 12f), Offset(9f, 6f)), false, null, null, null)
        )
    ),
    "chevron-down" to AppIconDef(
        fallback = "▼",
        shapes = listOf(
            IconShape.Poly(listOf(Offset(6f, 9f), Offset(12f, 15f), Offset(18f, 9f)), false, null, null, null)
        )
    ),
    "sprout" to AppIconDef(
        fallback = "🌱",
        shapes = listOf(
            IconShape.Path("M7 20h10M12 20v-9", null, null, null),
            IconShape.Path("M12 11c0-4.5 3.5-7 7-7 0 3.5-2.5 7-7 7Z", null, null, null),
            IconShape.Path("M12 13c0-3.5-2.5-6-6-6 0 3 2 6 6 6Z", null, null, null)
        )
    ),
    "chart" to AppIconDef(
        fallback = "📊",
        shapes = listOf(
            IconShape.Path("M3 3v18h18", null, null, null),
            IconShape.Path("m7 14 4-4 4 3 5-6", null, null, null)
        )
    ),
    "palette" to AppIconDef(
        fallback = "🎨",
        shapes = listOf(
            IconShape.Circle(13.5f, 6.5f, 0.5f, null, null, null),
            IconShape.Circle(17.5f, 10.5f, 0.5f, null, null, null),
            IconShape.Circle(8.5f, 7.5f, 0.5f, null, null, null),
            IconShape.Circle(6.5f, 12.5f, 0.5f, null, null, null),
            IconShape.Path("M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10c.926 0 1.648-.746 1.648-1.688 0-.437-.18-.835-.437-1.125-.29-.289-.438-.652-.438-1.125a1.64 1.64 0 0 1 1.668-1.668h1.996c3.051 0 5.555-2.503 5.555-5.554C21.965 6.012 17.461 2 12 2Z", null, null, null)
        )
    ),
    "check" to AppIconDef(
        fallback = "✓",
        shapes = listOf(
            IconShape.Poly(listOf(Offset(20f, 6f), Offset(9f, 17f), Offset(4f, 12f)), false, null, null, null)
        )
    ),
    "close" to AppIconDef(
        fallback = "✕",
        shapes = listOf(
            IconShape.Path("M18 6 6 18M6 6l12 12", null, null, null)
        )
    ),
    "plus" to AppIconDef(
        fallback = "＋",
        shapes = listOf(
            IconShape.Path("M12 5v14M5 12h14", null, null, null)
        )
    ),
    "minus" to AppIconDef(
        fallback = "－",
        shapes = listOf(
            IconShape.Path("M5 12h14", null, null, null)
        )
    ),
    "search" to AppIconDef(
        fallback = "🔍",
        shapes = listOf(
            IconShape.Circle(11f, 11f, 7f, null, null, null),
            IconShape.Path("m21 21-4.3-4.3", null, null, null)
        )
    ),
    "camera" to AppIconDef(
        fallback = "📷",
        shapes = listOf(
            IconShape.Path("M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z", null, null, null),
            IconShape.Circle(12f, 13f, 3f, null, null, null)
        )
    ),
    "image" to AppIconDef(
        fallback = "🖼️",
        shapes = listOf(
            IconShape.Rect(3f, 5f, 18f, 14f, 3f, null, null, null),
            IconShape.Circle(8.5f, 10f, 1.5f, null, null, null),
            IconShape.Path("m21 16-5-5L5 19", null, null, null)
        )
    ),
    "music" to AppIconDef(
        fallback = "🎵",
        shapes = listOf(
            IconShape.Path("M9 18V5l12-2v13", null, null, null),
            IconShape.Circle(6f, 18f, 3f, null, null, null),
            IconShape.Circle(18f, 16f, 3f, null, null, null)
        )
    ),
    "volume" to AppIconDef(
        fallback = "🔊",
        shapes = listOf(
            IconShape.Poly(listOf(Offset(11f, 5f), Offset(6f, 9f), Offset(2f, 9f), Offset(2f, 15f), Offset(6f, 15f), Offset(11f, 19f), Offset(11f, 5f)), true, null, null, null),
            IconShape.Path("M15.54 8.46a5 5 0 0 1 0 7.07", null, null, null)
        )
    ),
    "video" to AppIconDef(
        fallback = "🎥",
        shapes = listOf(
            IconShape.Rect(2f, 6f, 14f, 12f, 2.5f, null, null, null),
            IconShape.Path("m22 8-6 4 6 4V8Z", null, null, null)
        )
    ),
    "paperclip" to AppIconDef(
        fallback = "📎",
        shapes = listOf(
            IconShape.Path("m21.44 11.05-9.19 9.19a6 6 0 0 1-8.49-8.49l8.57-8.57A4 4 0 1 1 18 8.84l-8.59 8.57a2 2 0 0 1-2.83-2.83l8.49-8.48", null, null, null)
        )
    ),
    "trash" to AppIconDef(
        fallback = "🗑️",
        shapes = listOf(
            IconShape.Path("M3 6h18", null, null, null),
            IconShape.Path("M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2", null, null, null),
            IconShape.Path("M19 6v13a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6", null, null, null)
        )
    ),
    "folder" to AppIconDef(
        fallback = "📂",
        shapes = listOf(
            IconShape.Path("M4 20h16a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.93a2 2 0 0 1-1.66-.9l-.82-1.2A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13c0 1.1.9 2 2 2Z", null, null, null)
        )
    ),
    "mastery-clueless" to AppIconDef(
        fallback = "🔴",
        shapes = listOf(
            IconShape.Circle(12f, 12f, 9.5f, Color(0xFFFEE2E2), Color(0xFFEF4444), 2f),
            IconShape.Path("M9.6 9.5a2.5 2.5 0 0 1 4.9.7c0 1.7-2.5 2.2-2.5 3.6", null, Color(0xFFDC2626), 2.2f),
            IconShape.Circle(12f, 17f, 1f, Color(0xFFDC2626), Color.Transparent, null)
        )
    ),
    "mastery-partial" to AppIconDef(
        fallback = "🟡",
        shapes = listOf(
            IconShape.Path("M9 18h6", null, Color(0xFFD97706), 2.2f),
            IconShape.Path("M10 21h4", null, Color(0xFFD97706), 2.2f),
            IconShape.Path("M12 3a6 6 0 0 0-4 10.47c.75.67 1 1.53 1 2.53h6c0-1 .25-1.86 1-2.53A6 6 0 0 0 12 3Z", Color(0xFFFEF3C7), Color(0xFFF59E0B), 2f)
        )
    ),
    "mastery-mastered" to AppIconDef(
        fallback = "🟢",
        shapes = listOf(
            IconShape.Circle(12f, 12f, 9.5f, Color(0xFFD1FAE5), Color(0xFF10B981), 2f),
            IconShape.Path("m8.5 12.5 2.5 2.5 4.8-5.2", null, Color(0xFF047857), 2.4f)
        )
    ),
    "star-filled" to AppIconDef(
        fallback = "⭐",
        shapes = listOf(
            IconShape.Path("M12 2.8 14.8 8.5l6.2.9-4.5 4.4 1.1 6.2L12 17l-5.6 3 1.1-6.2L3 9.4l6.2-.9L12 2.8Z", Color(0xFFFBBF24), Color(0xFFD97706), 1.8f)
        )
    ),
    "star-outline" to AppIconDef(
        fallback = "☆",
        shapes = listOf(
            IconShape.Path("M12 2.8 14.8 8.5l6.2.9-4.5 4.4 1.1 6.2L12 17l-5.6 3 1.1-6.2L3 9.4l6.2-.9L12 2.8Z", Color(0xFFFEFDF9), Color(0xFFCFC4AC), 1.8f)
        )
    )
)
