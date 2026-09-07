package com.accounts.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.accounts.app.ui.AppViewModel
import com.accounts.app.ui.comps.noRippleClickable
import com.accounts.app.ui.screens.AccountManageScreen
import com.accounts.app.ui.screens.CategoryManageScreen
import com.accounts.app.ui.screens.ListScreen
import com.accounts.app.ui.screens.RecordScreen
import com.accounts.app.ui.screens.SettingsScreen
import com.accounts.app.ui.screens.StatsScreen
import com.accounts.app.ui.screens.ThemeSelectScreen
import com.accounts.app.ui.theme.DarkPage
import com.accounts.app.ui.theme.JianjiTheme
import com.accounts.app.ui.theme.LightPage
import com.accounts.app.ui.theme.CloudSky

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: AppViewModel = viewModel()
            JianjiRoot(vm)
        }
    }
}

private const val PAGE_NONE = "NONE"
private const val PAGE_SETTINGS = "SETTINGS"
private const val PAGE_CATEGORIES = "CATEGORIES"
private const val PAGE_ACCOUNTS = "ACCOUNTS"
private const val PAGE_THEME = "THEME"

private val TAB_RECORD = "RECORD"
private val TAB_LIST = "LIST"
private val TAB_STATS = "STATS"

@Composable
private fun JianjiRoot(vm: AppViewModel) {
    // themeMode: 0=跟随系统 1=浅色 2=深色
    var themeMode by rememberSaveable { mutableStateOf(0) }
    var tab by rememberSaveable { mutableStateOf(TAB_RECORD) }
    var page by rememberSaveable { mutableStateOf(PAGE_NONE) }
    var categoriesOpenedFromHome by rememberSaveable { mutableStateOf(false) }

    val themeLabel = when (themeMode) {
        1 -> "云轨亮色"
        2 -> "夜雾蓝"
        else -> "跟随系统"
    }

    BackHandler(page != PAGE_NONE) {
        page = when (page) {
            PAGE_CATEGORIES -> if (categoriesOpenedFromHome) PAGE_NONE else PAGE_SETTINGS
            PAGE_ACCOUNTS, PAGE_THEME -> PAGE_SETTINGS
            else -> PAGE_NONE
        }
    }

    JianjiTheme(dark = when (themeMode) {
        0 -> null
        1 -> false
        else -> true
    }) {
        val resolvedDark = when (themeMode) {
            0 -> isSystemInDarkTheme()
            1 -> false
            else -> true
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(if (resolvedDark) DarkPage else LightPage)
        ) {
            when (page) {
                PAGE_NONE -> Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) {
                        when (tab) {
                            TAB_RECORD -> RecordScreen(vm, onOpenCategories = {
                                categoriesOpenedFromHome = true
                                page = PAGE_CATEGORIES
                            })
                            TAB_LIST -> ListScreen(vm)
                            else -> StatsScreen(vm, onOpenSettings = { page = PAGE_SETTINGS })
                        }
                    }
                    BottomDock(current = tab, onSelect = { tab = it })
                }

                PAGE_SETTINGS -> SettingsScreen(
                    vm = vm,
                    themeLabel = themeLabel,
                    onBack = { page = PAGE_NONE },
                    onOpenCategories = {
                        categoriesOpenedFromHome = false
                        page = PAGE_CATEGORIES
                    },
                    onOpenAccounts = { page = PAGE_ACCOUNTS },
                    onOpenTheme = { page = PAGE_THEME }
                )

                PAGE_CATEGORIES -> CategoryManageScreen(vm) {
                    page = if (categoriesOpenedFromHome) PAGE_NONE else PAGE_SETTINGS
                }
                PAGE_ACCOUNTS -> AccountManageScreen(vm) { page = PAGE_SETTINGS }
                PAGE_THEME -> ThemeSelectScreen(
                    themeMode = themeMode,
                    onSelect = { themeMode = it; page = PAGE_SETTINGS },
                    onBack = { page = PAGE_SETTINGS }
                )
            }
        }
    }
}

/** 云轨底部三站导航：选中只改变颜色，图标和圆形尺寸保持不变。 */
@Composable
private fun BottomDock(current: String, onSelect: (String) -> Unit) {
    val items = listOf(
        Triple(TAB_RECORD, Icons.Filled.Add, "记一笔"),
        Triple(TAB_LIST, Icons.Outlined.FormatListBulleted, "明细"),
        Triple(TAB_STATS, Icons.Outlined.BarChart, "统计")
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(78.dp)
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.96f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(38.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (id, icon, desc) ->
                val active = current == id
                Box(
                    Modifier
                        .size(46.dp)
                        .background(
                            if (active) CloudSky else MaterialTheme.colorScheme.surface,
                            CircleShape
                        )
                        .border(
                            1.dp,
                            if (active) CloudSky else MaterialTheme.colorScheme.outlineVariant,
                            CircleShape
                        )
                        .noRippleClickable { onSelect(id) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = desc,
                        tint = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        }
    }
}
