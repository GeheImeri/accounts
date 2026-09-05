package com.accounts.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ===== 晨雾 · 颜色令牌（见 docs/03-晨雾视觉规范与设计定稿.md） =====
val MistViolet = Color(0xFF8B7CF6)
val MistVioletSoft = Color(0xFFEDE7FB)
val MistBlue = Color(0xFF5FB4FF)
val MistBlueSoft = Color(0xFFE4F0FE)
val IncomeGreen = Color(0xFF2FC98A)
val ExpenseRose = Color(0xFFFF7D9C)
val Ink = Color(0xFF3F4257)
val Ink2 = Color(0xFF9A9DB3)
val LineMist = Color(0xFFEEEBF6)
val WhiteGlass = Color(0xFFEFF2FF)

val LightPage = Brush.verticalGradient(
    listOf(Color(0xFFF3EFFB), Color(0xFFE9F1FE), Color(0xFFFCEFF4))
)
val DarkPage = Brush.verticalGradient(
    listOf(Color(0xFF12111A), Color(0xFF151320), Color(0xFF1B1022))
)

/** CTA 渐变：紫 → 蓝 */
val MistCta = Brush.linearGradient(listOf(MistViolet, MistBlue))

/** 分类色片配色：色值与白色按比例混合出柔和浅色 */
fun chipColor(color: Long): Color =
    androidx.compose.ui.graphics.lerp(Color(color), Color.White, 0.72f)

private val LightColors = lightColorScheme(
    primary = MistViolet,
    onPrimary = Color.White,
    primaryContainer = MistVioletSoft,
    onPrimaryContainer = Color(0xFF4C3F8F),
    secondary = MistBlue,
    onSecondary = Color.White,
    secondaryContainer = MistBlueSoft,
    background = Color.White,
    surface = Color.White,
    surfaceVariant = Color(0xFFF4F1FA),
    onSurface = Ink,
    onSurfaceVariant = Ink2,
    error = Color(0xFFE5484D)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9D8CFF),
    onPrimary = Color(0xFF171226),
    primaryContainer = Color(0xFF332A55),
    onPrimaryContainer = Color(0xFFDDD4FF),
    secondary = Color(0xFF6FC3FF),
    background = Color(0xFF12111A),
    surface = Color(0xFF1B1A26),
    surfaceVariant = Color(0xFF262436),
    onSurface = Color(0xFFECEAF6),
    onSurfaceVariant = Color(0xFF9A97AC),
    error = Color(0xFFFF8A95)
)

/**
 * 主题入口。
 * @param dark 由外部持有（设置页可选 跟随系统/浅色/深色）；null 表示跟随系统
 */
@Composable
fun JianjiTheme(dark: Boolean?, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val useDark = dark ?: systemDark
    MaterialTheme(
        colorScheme = if (useDark) DarkColors else LightColors,
        content = content
    )
}
