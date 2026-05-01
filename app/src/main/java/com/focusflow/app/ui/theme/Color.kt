package com.focusflow.app.ui.theme

import androidx.compose.ui.graphics.Color

// Light theme
val Primary = Color(0xFF4F6EF7)
val OnPrimary = Color(0xFFFFFFFF)
val Background = Color(0xFFF8F9FC)
val Surface = Color(0xFFFFFFFF)
val SurfaceVariant = Color(0xFFE8E9F0)
val OnSurface = Color(0xFF1C1E24)
val OnSurfaceVariant = Color(0xFF8E919A)
val Accent = Color(0xFFFF6B6B)
val Outline = Color(0xFFD0D0D8)

// Dark theme
val DarkPrimary = Color(0xFF8B9EFF)
val DarkOnPrimary = Color(0xFF1C1E24)
val DarkBackground = Color(0xFF121316)
val DarkSurface = Color(0xFF1E1F24)
val DarkSurfaceVariant = Color(0xFF2A2C33)
val DarkOnSurface = Color(0xFFE4E5E9)
val DarkOnSurfaceVariant = Color(0xFF9CA0AA)
val DarkAccent = Color(0xFFFF8A80)
val DarkOutline = Color(0xFF3E4048)

fun colorToHex(color: Color): String {
    val red = (color.red * 255).toInt()
    val green = (color.green * 255).toInt()
    val blue = (color.blue * 255).toInt()
    return String.format("#%02X%02X%02X", red, green, blue)
}

fun hexToColor(hex: String): Color {
    val color = hex.removePrefix("#")
    val rgb = color.toLong(16)
    return Color(
        red = ((rgb shr 16) and 0xFF) / 255f,
        green = ((rgb shr 8) and 0xFF) / 255f,
        blue = (rgb and 0xFF) / 255f
    )
}

val ChartColors = listOf(
    Color(0xFF4F6EF7),
    Color(0xFFFF6B6B),
    Color(0xFF51CF66),
    Color(0xFFFFD43B),
    Color(0xFF845EF7),
    Color(0xFFFF922B),
    Color(0xFF20C997),
    Color(0xFFF06595),
)
