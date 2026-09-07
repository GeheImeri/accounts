package com.accounts.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ===== 云轨 · 漫游颜色令牌 =====
val MistViolet = Color(0xFF9C91E8)
val MistVioletSoft = Color(0xFFF0EDFF)
val MistBlue = Color(0xFF5BBEDD)
val MistBlueSoft = Color(0xFFDFF5FF)
val CloudMint = Color(0xFF70D1B8)
val CloudSky = Color(0xFF5BBEDD)
val CloudSurface = Color(0xFFF8FDFF)
val IncomeGreen = Color(0xFF24A47D)
val ExpenseRose = Color(0xFFEF718A)
val Ink = Color(0xFF28445B)
val Ink2 = Color(0xFF7892A2)
val LineMist = Color(0xFFD5EBF1)
val WhiteGlass = Color(0xFFF8FDFF)

val LightPage = Brush.verticalGradient(
    listOf(Color(0xFFEDF9FF), Color(0xFFF7FCFF), Color(0xFFEAFBF5))
)
val DarkPage = Brush.verticalGradient(
    listOf(Color(0xFF173144), Color(0xFF19394A), Color(0xFF173E3B))
)

/** CTA 渐变：薄荷 → 云蓝 */
val MistCta = Brush.linearGradient(listOf(CloudMint, CloudSky))

/** 分类色片配色：色值与白色按比例混合出柔和浅色 */
fun chipColor(color: Long): Color =
    androidx.compose.ui.graphics.lerp(Color(color), Color.White, 0.82f)

private val LightColors = lightColorScheme(
    primary = CloudSky,
    onPrimary = Color.White,
    primaryContainer = MistBlueSoft,
    onPrimaryContainer = Color(0xFF245D73),
    secondary = CloudMint,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDFF7F0),
    background = CloudSurface,
    surface = Color.White,
    surfaceVariant = Color(0xFFEAF5F8),
    onSurface = Ink,
    onSurfaceVariant = Ink2,
    outline = Color(0xFFB8DCE8),
    outlineVariant = LineMist,
    error = ExpenseRose
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF79D5EF),
    onPrimary = Color(0xFF123B4B),
    primaryContainer = Color(0xFF28566A),
    onPrimaryContainer = Color(0xFFD9F5FF),
    secondary = Color(0xFF82DEC4),
    background = Color(0xFF173144),
    surface = Color(0xFF1E4253),
    surfaceVariant = Color(0xFF285263),
    onSurface = Color(0xFFE8F7FC),
    onSurfaceVariant = Color(0xFFA8C4CF),
    outline = Color(0xFF467487),
    error = Color(0xFFFF91A7)
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
