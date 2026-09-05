package com.accounts.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accounts.app.data.Account
import com.accounts.app.data.Category
import com.accounts.app.data.Transaction
import com.accounts.app.ui.AppViewModel
import com.accounts.app.ui.comps.Footnote
import com.accounts.app.ui.comps.GlassCard
import com.accounts.app.ui.comps.Hairline
import com.accounts.app.ui.comps.Segment
import com.accounts.app.ui.comps.noRippleClickable
import com.accounts.app.ui.theme.Ink
import com.accounts.app.ui.theme.Ink2
import com.accounts.app.util.Money

// ================= 设置主页 =================

@Composable
fun SettingsScreen(
    vm: AppViewModel,
    themeLabel: String,
    onBack: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenTheme: () -> Unit
) {
    val accounts by vm.accounts.collectAsState()
    val defaultAccId by vm.defaultAccountId.collectAsState()
    var continuous by remember { mutableStateOf(true) }
    var accountMenu by remember { mutableStateOf(false) }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.restoreBackup(uri) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsHeader("设置", onBack)

        GlassCard(Modifier.fillMaxWidth()) {
            SetRow("分类管理", chevron = true, onClick = onOpenCategories)
            SetRow("账户管理", chevron = true, onClick = onOpenAccounts)
        }
        GlassCard(Modifier.fillMaxWidth()) {
            SetRow("主题", right = themeLabel, chevron = true, onClick = onOpenTheme)
            Box {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        .noRippleClickable { accountMenu = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("默认支出账户", fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text(accounts.firstOrNull { it.id == defaultAccId }?.name
                        ?: accounts.firstOrNull()?.name ?: "—",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("▾", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = accountMenu,
                    onDismissRequest = { accountMenu = false }) {
                    accounts.forEach { a ->
                        DropdownMenuItem(
                            text = { Text(a.name) },
                            onClick = {
                                vm.setDefaultAccount(a.id)
                                accountMenu = false
                            }
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("连续记账", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Toggle(checked = continuous, onClick = { continuous = !continuous })
            }
        }
        GlassCard(Modifier.fillMaxWidth()) {
            SetRow("导出 CSV（Excel/WPS 可开）", chevron = true, onClick = { vm.exportCsv() })
            SetRow("备份到文件（JSON）", chevron = true, onClick = { vm.exportBackup() })
            SetRow("从备份恢复", chevron = true, onClick = {
                restoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            })
        }
        GlassCard(Modifier.fillMaxWidth()) {
            SetRow("关于 · 版本 v0.1.0", right = "仅存本机 · 无网络")
        }
        Footnote("数据仅存本机 · App 未申请任何网络权限", Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(20.dp))
    }
}

// ================= 分类管理 =================

@Composable
fun CategoryManageScreen(vm: AppViewModel, onBack: () -> Unit) {
    val categories by vm.categories.collectAsState()
    var kind by remember { mutableStateOf("expense") }
    var addOpen by remember { mutableStateOf(false) }

    val list = remember(categories, kind) {
        categories.filter { it.enabled && it.kind == kind }
            .sortedWith(compareBy({ !it.pinned }, { it.sortOrder }))
    }
    val pinnedCount = list.count { it.pinned }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsHeader("分类管理", onBack, onAdd = { addOpen = true })

        Segment(options = listOf("支出", "收入"),
            selectedIndex = if (kind == "expense") 0 else 1,
            onSelect = { kind = if (it == 0) "expense" else "income" })

        GlassCard(Modifier.fillMaxWidth()) {
            list.forEachIndexed { index, cat ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(20.dp).background(chip(cat.color), RoundedCornerShape(8.dp)))
                    Text(cat.name, Modifier.padding(start = 10.dp).weight(1f),
                        fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold)
                    // 上移 / 下移（简化拖拽排序）
                    Text("▲", fontSize = 10.sp,
                        color = if (index > 0) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .noRippleClickable {
                                if (index > 0) move(list, index, index - 1, kind, vm)
                            })
                    Text("▼", fontSize = 10.sp,
                        color = if (index < list.lastIndex) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .noRippleClickable {
                                if (index < list.lastIndex) move(list, index, index + 1, kind, vm)
                            })
                    Text(
                        if (cat.pinned) "首页" else "仅统计",
                        Modifier
                            .background(
                                if (cat.pinned) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(999.dp)
                            )
                            .noRippleClickable { vm.togglePinned(cat) }
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                        fontSize = 10.sp,
                        color = if (cat.pinned) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (cat != list.last()) Hairline()
            }
        }
        Text("首页固定 8 个（当前 $pinnedCount / 8）· 点「首页/仅统计」切换 · ▲▼ 调整顺序",
            Modifier.fillMaxWidth(), fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
    }

    if (addOpen) {
        AddCategoryDialog(
            initialKind = kind,
            onDismiss = { addOpen = false },
            onAdd = { name, color, k, pinned ->
                vm.addCategory(name, color, k, pinned)
                addOpen = false
            }
        )
    }
}

// ================= 账户管理 =================

/** 上移/下移分类并持久化排序 */
private fun move(list: List<Category>, from: Int, to: Int, kind: String, vm: AppViewModel) {
    val ids = list.map { it.id }.toMutableList()
    val item = ids.removeAt(from)
    ids.add(to, item)
    vm.reorderCategories(kind, ids)
}

@Composable
fun AccountManageScreen(vm: AppViewModel, onBack: () -> Unit) {
    val accounts by vm.accounts.collectAsState()
    val transactions by vm.transactions.collectAsState()
    val transfers by vm.transfers.collectAsState()
    var addOpen by remember { mutableStateOf(false) }
    var transferOpen by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsHeader("账户管理", onBack, onAdd = { addOpen = true })

        GlassCard(Modifier.fillMaxWidth()) {
            accounts.forEach { a ->
                Row(Modifier.fillMaxWidth().padding(vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(20.dp).background(chip(a.color), RoundedCornerShape(8.dp)))
                    Text(a.name, Modifier.padding(start = 10.dp).weight(1f),
                        fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    Text("¥${Money.format(balanceOf(a, transactions, transfers))}",
                        fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                if (a != accounts.last()) Hairline()
            }
        }
        GlassCard(Modifier.fillMaxWidth().noRippleClickable { transferOpen = true }) {
            Row(Modifier.fillMaxWidth().padding(vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("账户转账", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("转账不计收支", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Footnote("余额 = 初始 + 收入 − 支出 + 转入 − 转出", Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(20.dp))
    }

    if (addOpen) {
        AddAccountDialog(onDismiss = { addOpen = false },
            onAdd = { name, color, kind -> vm.addAccount(name, color, kind); addOpen = false })
    }
    if (transferOpen) {
        TransferDialog(accounts, onDismiss = { transferOpen = false },
            onTransfer = { from, to, cents, note ->
                vm.addTransfer(from, to, cents, note); transferOpen = false
            })
    }
}

// ================= 主题选择 =================

private data class ThemeOpt(
    val mode: Int,
    val icon: ImageVector,
    val name: String,
    val desc: String
)

@Composable
fun ThemeSelectScreen(themeMode: Int, onSelect: (Int) -> Unit, onBack: () -> Unit) {
    val options = listOf(
        ThemeOpt(0, Icons.Outlined.BrightnessAuto, "跟随系统", "随手机系统自动切换亮暗（推荐）"),
        ThemeOpt(1, Icons.Outlined.LightMode, "浅色", "始终使用亮色外观"),
        ThemeOpt(2, Icons.Outlined.DarkMode, "深色", "始终使用暗色 · 夜间护眼")
    )
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsHeader("主题", onBack)
        Text("外观", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        GlassCard(Modifier.fillMaxWidth()) {
            options.forEach { (mode, icon, name, desc) ->
                Row(Modifier.fillMaxWidth().noRippleClickable { onSelect(mode) }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = name,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
                    }
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text(name, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        Text(desc, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    RadioDot(selected = themeMode == mode)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

// ================= 通用小组件 =================

@Composable
private fun SettingsHeader(title: String, onBack: () -> Unit, onAdd: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.Outlined.KeyboardArrowLeft, contentDescription = "返回")
        }
        Text(title, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        if (onAdd != null) {
            Box(
                Modifier.size(34.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .noRippleClickable(onClick = onAdd),
                contentAlignment = Alignment.Center
            ) {
                Text("＋", color = MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SetRow(
    label: String,
    right: String? = null,
    chevron: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth()
            .let { if (onClick != null) it.noRippleClickable(onClick = onClick) else it }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        if (right != null) Text(right, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (chevron) Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun Toggle(checked: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(width = 46.dp, height = 27.dp)
            .background(
                if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                CircleShape
            )
            .noRippleClickable(onClick = onClick)
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(Modifier.size(21.dp).background(Color.White, CircleShape))
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    Box(
        Modifier.size(21.dp)
            .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(Modifier.size(11.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        }
    }
}

private fun chip(color: Long): Color = androidx.compose.ui.graphics.lerp(Color(color), Color.White, 0.72f)

private fun balanceOf(
    account: Account,
    transactions: List<Transaction>,
    transfers: List<com.accounts.app.data.Transfer>
): Long {
    var bal = account.initialBalanceCents
    transactions.forEach { t ->
        if (t.accountId == account.id) {
            if (t.type == "income") bal += t.amountCents else bal -= t.amountCents
        }
    }
    transfers.forEach { tr ->
        if (tr.fromAccountId == account.id) bal -= tr.amountCents
        if (tr.toAccountId == account.id) bal += tr.amountCents
    }
    return bal
}

// ================= 对话框 =================

private val ExpensePalette = listOf(
    "#FF9F68", "#F97316", "#EF4444", "#F43F5E", "#EC4899", "#A855F7",
    "#8B7CF6", "#6366F1", "#3B82F6", "#0EA5E9", "#14B8A6", "#10B981",
    "#22C55E", "#84CC16", "#EAB308", "#F59E0B", "#F07BAF", "#62C6C0",
    "#FFC857", "#9FB4FF", "#64748B", "#78716C"
)
private val IncomePalette = listOf(
    "#2FC98A", "#22C55E", "#10B981", "#14B8A6", "#63A9FF", "#3B82F6",
    "#8B7CF6", "#A855F7", "#EC4899", "#F43F5E", "#FF8FA3", "#F59E0B",
    "#FFB15F", "#EAB308", "#84CC16", "#9A9DB3"
)

private fun colorFromHex(hex: String): Long = ("FF" + hex.removePrefix("#")).toLong(16)

@Composable
private fun AddCategoryDialog(
    initialKind: String,
    onDismiss: () -> Unit,
    onAdd: (String, Long, String, Boolean) -> Unit
) {
    var kind by remember { mutableStateOf(initialKind) }
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(colorFromHex(ExpensePalette.first())) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增分类", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Segment(options = listOf("支出", "收入"),
                    selectedIndex = if (kind == "expense") 0 else 1,
                    onSelect = {
                        kind = if (it == 0) "expense" else "income"
                        color = if (kind == "expense") colorFromHex(ExpensePalette.first())
                        else colorFromHex(IncomePalette.first())
                    })
                Spacer(Modifier.height(10.dp))
                BasicTextField(
                    value = name,
                    onValueChange = { name = it.take(6) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                            .padding(12.dp)) {
                            if (name.isEmpty()) Text("分类名称（6 字内）", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            inner()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                val palette = if (kind == "expense") ExpensePalette else IncomePalette
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    palette.chunked(6).forEach { rowColors ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowColors.forEach { hex ->
                                Box(
                                    Modifier.weight(1f).height(30.dp)
                                        .background(chip(colorFromHex(hex)), RoundedCornerShape(9.dp))
                                        .border(if (colorFromHex(hex) == color) 2.dp else 0.dp,
                                            MaterialTheme.colorScheme.primary, RoundedCornerShape(9.dp))
                                        .noRippleClickable { color = colorFromHex(hex) }
                                )
                            }
                            repeat(6 - rowColors.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                onAdd(name.trim(), color, kind, false)
            }) { Text("新增") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun AddAccountDialog(onDismiss: () -> Unit, onAdd: (String, Long, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(colorFromHex("#63A9FF")) }
    var kind by remember { mutableStateOf("ewallet") }
    val palette = listOf("#FFB15F", "#8B7CF6", "#63A9FF", "#2FC98A", "#FF8FA3", "#F07BAF")
    val kindOptions = listOf("现金" to "cash", "卡" to "card", "电子钱包" to "ewallet")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增账户", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                BasicTextField(
                    value = name,
                    onValueChange = { name = it.take(8) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                            .padding(12.dp)) {
                            if (name.isEmpty()) Text("账户名称", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            inner()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    kindOptions.forEach { (label, k) ->
                        Text(label,
                            Modifier
                                .background(
                                    if (kind == k) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(999.dp)
                                )
                                .noRippleClickable { kind = k }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            color = if (kind == k) Color.White else Ink2,
                            fontSize = 12.sp, fontWeight = if (kind == k) FontWeight.Bold else FontWeight.Normal)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    palette.forEach { hex ->
                        Box(
                            Modifier.size(32.dp)
                                .background(chip(colorFromHex(hex)), RoundedCornerShape(10.dp))
                                .border(if (colorFromHex(hex) == color) 2.dp else 0.dp,
                                    MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                                .noRippleClickable { color = colorFromHex(hex) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onAdd(name.trim(), color, kind) }) { Text("新增") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun TransferDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onTransfer: (Long, Long, Long, String) -> Unit
) {
    var fromId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 0L) }
    var toId by remember { mutableStateOf(accounts.getOrNull(1)?.id ?: 0L) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("账户转账", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                AccountChips("从", accounts, fromId) { fromId = it }
                Spacer(Modifier.height(6.dp))
                AccountChips("到", accounts, toId) { toId = it }
                Spacer(Modifier.height(10.dp))
                Text("金额（元）", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                BasicTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                            .padding(12.dp)) {
                            if (amount.isEmpty()) Text("金额", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            inner()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("转账不计收支，仅影响两个账户的余额", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp))
            }
        },
        confirmButton = {
            TextButton(enabled = fromId != toId && Money.parse(amount) > 0,
                onClick = { onTransfer(fromId, toId, Money.parse(amount), note) }) { Text("确认转账") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun AccountChips(label: String, accounts: List<Account>, selectedId: Long,
                         onSelect: (Long) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text("$label：", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            accounts.forEach { a ->
                Text(a.name,
                    Modifier
                        .background(
                            if (a.id == selectedId) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(999.dp)
                        )
                        .noRippleClickable { onSelect(a.id) }
                        .padding(horizontal = 13.dp, vertical = 6.dp),
                    color = if (a.id == selectedId) Color.White else Ink2,
                    fontSize = 12.sp,
                    fontWeight = if (a.id == selectedId) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}
