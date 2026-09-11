@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.accounts.app.ui.screens

import android.app.DatePickerDialog as AndroidDatePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accounts.app.data.Budget
import com.accounts.app.data.Account
import com.accounts.app.data.Category
import com.accounts.app.data.Transaction
import com.accounts.app.ui.AppViewModel
import com.accounts.app.ui.comps.CtaButton
import com.accounts.app.ui.comps.Segment
import com.accounts.app.ui.comps.noRippleClickable
import com.accounts.app.ui.theme.ExpenseRose
import com.accounts.app.ui.theme.IncomeGreen
import com.accounts.app.ui.theme.chipColor
import com.accounts.app.util.Days
import com.accounts.app.util.Money
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneOffset

/** 首页可见分类：启用 + 该类型，pinned 优先；云轨支持横向滑动，不再限制 8 格。 */
fun homeCategories(all: List<Category>, kind: String): List<Category> =
    all.filter { it.enabled && it.kind == kind }
        .sortedWith(compareBy({ !it.pinned }, { it.sortOrder }))

@Composable
fun RecordScreen(
    vm: AppViewModel,
    onOpenCategories: () -> Unit,
    onOpenAccounts: () -> Unit
) {
    val categories by vm.categories.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val activeAccounts = remember(accounts) { accounts.filter { it.enabled }.sortedBy { it.sortOrder } }
    val transactions by vm.transactions.collectAsState()

    var kind by remember { mutableStateOf("expense") }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedCatId by remember { mutableStateOf<Long?>(null) }
    var accountId by remember { mutableStateOf(0L) }
    var occurredAt by remember { mutableStateOf(Days.nowMillis()) }
    var timePickerOpen by remember { mutableStateOf(false) }

    val defaultAccountId by vm.defaultAccountId.collectAsState()
    LaunchedEffect(activeAccounts, defaultAccountId) {
        if (activeAccounts.none { it.id == defaultAccountId } && activeAccounts.isNotEmpty()) {
            vm.setDefaultAccount(activeAccounts.first().id)
        }
        if (accountId == 0L) {
            accountId = activeAccounts.firstOrNull { it.id == defaultAccountId }?.id
                ?: activeAccounts.firstOrNull()?.id
                ?: 0L
        }
    }
    // 记完一笔后：钱包回到默认账户、时间回到"现在"
    val resetToDefaults = {
        accountId = activeAccounts.firstOrNull { it.id == defaultAccountId }?.id
            ?: activeAccounts.firstOrNull()?.id
            ?: 0L
        occurredAt = Days.nowMillis()
    }

    val cats = homeCategories(categories, kind)
    val budgets by vm.budgets.collectAsState()
    var editingBudget by remember { mutableStateOf<Budget?>(null) }
    var addBudgetOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        Segment(options = listOf("支出", "收入"),
            selectedIndex = if (kind == "expense") 0 else 1,
            onSelect = { kind = if (it == 0) "expense" else "income" })

        // ===== 云轨票价：轻量大数字 =====
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("¥", fontSize = 26.sp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                fontWeight = FontWeight.Medium)
            BasicTextField(
                value = amountText,
                onValueChange = { s ->
                    val f = s.filter { it.isDigit() || it == '.' }
                    if (f.count { it == '.' } <= 1 && f.length <= 10) amountText = f
                },
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.padding(start = 6.dp).width(210.dp),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.Center) {
                        if (amountText.isEmpty()) {
                            Text("0.00", fontSize = 42.sp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                                fontWeight = FontWeight.Bold)
                        }
                        inner()
                    }
                }
            )
        }

        // ===== 备注胶囊 + 补记时间 =====
        Row(
            Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(999.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(999.dp))
                .padding(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = note,
                onValueChange = { note = it },
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Start
                ),
                singleLine = true,
                decorationBox = { inner ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        if (note.isEmpty()) {
                            Text("备注", color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp)
                        }
                        inner()
                    }
                },
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
            )
            Text(
                Days.todayTimeLabel(occurredAt),
                fontSize = 10.sp,
                color = IncomeGreen,
                modifier = Modifier
                    .background(IncomeGreen.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                    .noRippleClickable { timePickerOpen = true }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }

        // ===== 分类航线：所有类别都可横向滑动，末尾直达分类管理 =====
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(cats, key = { it.id }) { cat ->
                CategoryTile(
                    cat = cat,
                    selected = selectedCatId == cat.id,
                    modifier = Modifier.width(70.dp),
                    onClick = { selectedCatId = cat.id }
                )
            }
            item {
                ManageCategoryTile(Modifier.width(70.dp), onOpenCategories)
            }
        }

        // ===== 账户航线：支出为出发账户，收入为入账账户；末尾直达账户管理 =====
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp).padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (kind == "income") "入账账户" else "出发账户",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
            Text(
                "默认账户 · ${accountName(activeAccounts, defaultAccountId)}",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(activeAccounts, key = { it.id }) { account ->
                AccountTile(
                    account = account,
                    selected = accountId == account.id,
                    isDefault = account.id == defaultAccountId,
                    modifier = Modifier.width(70.dp),
                    onClick = { accountId = account.id }
                )
            }
            item {
                ManageAccountTile(Modifier.width(70.dp), onOpenAccounts)
            }
        }

        val cents = Money.parse(amountText)
        CtaButton(text = "保 存", enabled = cents > 0 && selectedCatId != null) {
            val cid = selectedCatId
            if (cid != null && cents > 0) {
                vm.addRecord(kind, cents, cid, accountId, occurredAt, note)
                amountText = ""
                note = ""   // 备注随金额一起重置，便于连续录入
                resetToDefaults()  // 钱包与时间回到默认
            }
        }

        // ===== 预算卡（保存按钮下方 · 支持多条，各自月/年）=====
        budgets.forEach { b ->
            BudgetCard(
                budget = b,
                transactions = transactions,
                accounts = accounts,
                onEdit = { editingBudget = b }
            )
        }
        Text("＋ 新增预算", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().noRippleClickable { addBudgetOpen = true }
                .border(1.5.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                    RoundedCornerShape(20.dp))
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                    RoundedCornerShape(20.dp)
                )
                .padding(vertical = 12.dp),
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
    }

    if (timePickerOpen) {
        DateTimePickerDialog(
            initial = occurredAt,
            onPick = { occurredAt = it; timePickerOpen = false },
            onDismiss = { timePickerOpen = false }
        )
    }
    editingBudget?.let { b ->
        BudgetEditDialog(
            name = b.name,
            amountCents = b.amountCents,
            period = b.period,
            startAtMillis = b.startAtMillis,
            endAtMillis = b.endAtMillis,
            accountId = b.accountId,
            accounts = accounts.filter { it.enabled || it.id == b.accountId }
                .sortedBy { it.sortOrder },
            onDismiss = { editingBudget = null },
            onDelete = { vm.deleteBudget(b); editingBudget = null },
            onSave = { nm, cents, p, start, end, budgetAccountId ->
                vm.updateBudget(b.copy(name = nm, amountCents = cents, period = p,
                    startAtMillis = start, endAtMillis = end,
                    accountId = budgetAccountId))
                editingBudget = null
            }
        )
    }
    if (addBudgetOpen) {
        BudgetEditDialog(
            name = "",
            amountCents = 0L,
            period = "month",
            startAtMillis = null,
            endAtMillis = null,
            accountId = null,
            accounts = activeAccounts,
            onDismiss = { addBudgetOpen = false },
            onDelete = null,
            onSave = { nm, cents, p, start, end, budgetAccountId ->
                val defaultName = when (p) {
                    "year" -> "今年预算"
                    "custom" -> "区间预算"
                    else -> "本月预算"
                }
                vm.addBudget(nm.ifBlank { defaultName }, cents, p, start, end, budgetAccountId)
                addBudgetOpen = false
            }
        )
    }
}

private fun accountName(accounts: List<com.accounts.app.data.Account>, id: Long): String =
    accounts.firstOrNull { it.id == id }?.name ?: "—"

private fun accountIcon(account: Account): String = account.icon.ifBlank {
    when (account.kind) {
        "cash" -> "💵"
        "card" -> "💳"
        "ewallet" -> "◉"
        else -> "◈"
    }
}

@Composable
private fun AccountTile(
    account: Account,
    selected: Boolean,
    isDefault: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier.noRippleClickable(onClick = onClick).padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Box(
                Modifier.size(46.dp)
                    .background(
                        if (selected) chipColor(account.color) else MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 7.dp)
                    )
                    .border(
                        if (selected) 1.5.dp else 1.dp,
                        if (selected) Color(account.color) else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 7.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(accountIcon(account), fontSize = 19.sp)
            }
            if (isDefault) {
                Text(
                    "默认",
                    fontSize = 7.sp,
                    color = IncomeGreen,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopEnd)
                        .background(IncomeGreen.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(account.name, fontSize = 10.sp, maxLines = 1,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold)
    }
}

@Composable
private fun ManageAccountTile(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier.noRippleClickable(onClick = onClick).padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(46.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("＋", color = MaterialTheme.colorScheme.primary, fontSize = 21.sp,
                fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(5.dp))
        Text("管理", color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold, fontSize = 10.sp)
    }
}

@Composable
private fun RouteInfo(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
            .noRippleClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}

@Composable
private fun CategoryTile(cat: Category, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .noRippleClickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .shadow(if (selected) 8.dp else 2.dp, CircleShape)
                .size(50.dp)
                .background(if (selected) Color(cat.color).copy(alpha = 0.78f) else chipColor(cat.color), CircleShape)
                .border(1.dp, Color(cat.color).copy(alpha = if (selected) 0.9f else 0.28f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(cat.icon.ifBlank { "•" }, fontSize = 21.sp, lineHeight = 24.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(cat.name, color = MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun ManageCategoryTile(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier.noRippleClickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(50.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("＋", color = MaterialTheme.colorScheme.primary, fontSize = 22.sp,
                fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Text("管理", color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
private fun SectionLabel2(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 2.dp))
}

/** 红底白色 ×（Canvas 画线，保证严格居中） */
@Composable
private fun CrossBadge(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(16.dp)
            .background(MaterialTheme.colorScheme.error, CircleShape)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val s = size.width * 0.3f
            val e = size.width * 0.7f
            val w = size.width * 0.16f
            drawLine(Color.White, Offset(s, s), Offset(e, e), strokeWidth = w)
            drawLine(Color.White, Offset(s, e), Offset(e, s), strokeWidth = w)
        }
    }
}

// ===== 时间选择：先选日期，再选时间（补记用）=====

@Composable
private fun DateTimePickerDialog(initial: Long, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val zone = java.time.ZoneId.systemDefault()
    val initialDate = Instant.ofEpochMilli(initial).atZone(zone).toLocalDate()
    val initialTime = Instant.ofEpochMilli(initial).atZone(zone).toLocalTime()

    var step by remember { mutableStateOf(0) }   // 0=日期 1=时间
    var pickedDate by remember { mutableStateOf(initialDate) }

    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )

    if (step == 0) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    val sel = dateState.selectedDateMillis
                    if (sel != null) {
                        pickedDate = Instant.ofEpochMilli(sel).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    step = 1
                }) { Text("下一步") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
        ) {
            DatePicker(state = dateState)
        }
    } else {
        val timeState = rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { step = 0 },
            title = { Text("选择时间") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    val ldt = LocalDateTime.of(
                        pickedDate,
                        LocalTime.of(timeState.hour, timeState.minute)
                    )
                    onPick(ldt.atZone(zone).toInstant().toEpochMilli())
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { step = 0 }) { Text("上一步") } }
        )
    }
}

// ===== 新增模板 =====

@Composable
private fun AddTemplateDialog(
    categories: List<Category>,
    kind: String,
    onDismiss: () -> Unit,
    onAdd: (String, Long, Long, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var catId by remember { mutableStateOf<Long?>(null) }
    val options = categories.filter { it.enabled && it.kind == kind }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增模板", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                BasicTextField(
                    value = name,
                    onValueChange = { name = it.take(8) },
                    textStyle = MaterialTheme.typography.bodyLarge
                        .copy(color = MaterialTheme.colorScheme.onSurface),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(10.dp))
                            .padding(12.dp)) {
                            if (name.isEmpty()) Text("模板名称，如：早餐",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            inner()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                BasicTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    textStyle = MaterialTheme.typography.bodyLarge
                        .copy(color = MaterialTheme.colorScheme.onSurface),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(10.dp))
                            .padding(12.dp)) {
                            if (amount.isEmpty()) Text("金额", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            inner()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text("分类", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                options.chunked(4).forEach { rowCats ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        rowCats.forEach { cat ->
                            Box(
                                Modifier.weight(1f)
                                    .background(
                                        if (catId == cat.id) Color(cat.color)
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .noRippleClickable { catId = cat.id }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(cat.name, fontSize = 12.sp,
                                    color = if (catId == cat.id) Color.White
                                    else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        repeat(4 - rowCats.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && Money.parse(amount) > 0 && catId != null,
                onClick = { onAdd(name.trim(), Money.parse(amount), catId!!, kind) }
            ) { Text("新增") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

// ===== 预算卡（保存按钮下方 · 多条各自月/年）=====

private fun budgetMoney(cents: Long): String =
    if (cents < 0) "-¥${Money.format(-cents)}" else "¥${Money.format(cents)}"

private fun localDateOf(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()

private fun startOfDate(date: LocalDate): Long =
    date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun shortDate(date: LocalDate): String =
    "%02d.%02d".format(date.monthValue, date.dayOfMonth)

@Composable
private fun BudgetCard(
    budget: Budget,
    transactions: List<Transaction>,
    accounts: List<Account>,
    onEdit: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val today = LocalDate.now()
    val startEnd: Pair<Long, Long>
    val periodBadge: String
    val spentLabel: String
    val days: Int
    when (budget.period) {
        "year" -> {
            startEnd = Days.monthRange(YearMonth.of(today.year, 1))[0] to
                Days.monthRange(YearMonth.of(today.year + 1, 1))[0]
            periodBadge = "按年"
            spentLabel = "今年已支出"
            days = today.dayOfYear
        }
        "custom" -> {
            val start = budget.startAtMillis ?: startOfDate(today)
            val end = budget.endAtMillis?.takeIf { it > start } ?: startOfDate(today.plusDays(1))
            startEnd = start to end
            val startDate = localDateOf(start)
            val endExclusiveDate = localDateOf(end)
            val elapsedEnd = minOf(today.plusDays(1), endExclusiveDate)
            days = if (elapsedEnd > startDate) {
                java.time.temporal.ChronoUnit.DAYS.between(startDate, elapsedEnd).toInt()
            } else 0
            periodBadge = "自定义"
            spentLabel = "${shortDate(startDate)}–${shortDate(endExclusiveDate.minusDays(1))} 已支出"
        }
        else -> {
            val r = Days.monthRange(YearMonth.from(today))
            startEnd = r[0] to r[1]
            periodBadge = "按月"
            spentLabel = "本月已支出"
            days = today.dayOfMonth
        }
    }
    val amount = budget.amountCents
    val accountLabel = budget.accountId?.let { accountName(accounts, it) } ?: "全部钱包"
    val spent = transactions.filter {
        it.type == "expense" &&
            it.occurredAtMillis in startEnd.first until startEnd.second &&
            (budget.accountId == null || it.accountId == budget.accountId)
    }.sumOf { it.amountCents }
    val remaining = amount - spent
    val pctUsed = if (amount > 0) spent * 100 / amount else 0L
    val frac = if (amount > 0) spent.toFloat() / amount.toFloat() else 0f
    val over = amount > 0 && spent > amount

    Column(
        Modifier.fillMaxWidth()
            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(topStart = 24.dp,
                topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 8.dp))
            .background(scheme.surface, RoundedCornerShape(topStart = 24.dp,
                topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 8.dp))
            .padding(13.dp)
    ) {
        if (amount <= 0) {
            Text("${budget.name} · ＋ 设置金额（$periodBadge）",
                fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold,
                color = scheme.primary,
                modifier = Modifier.fillMaxWidth().noRippleClickable(onClick = onEdit)
                    .padding(vertical = 10.dp),
                textAlign = TextAlign.Center)
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(budget.name, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                maxLines = 1)
            Text(periodBadge, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 7.dp)
                    .background(
                        if (budget.period == "year") IncomeGreen.copy(alpha = 0.16f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        RoundedCornerShape(999.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                color = if (budget.period == "year") IncomeGreen
                else MaterialTheme.colorScheme.primary)
            Spacer(Modifier.weight(1f))
            Text("✎ 修改", fontSize = 13.sp,
                fontWeight = FontWeight.Bold, color = scheme.primary,
                modifier = Modifier.noRippleClickable(onClick = onEdit))
        }
        Text("钱包 · $accountLabel", fontSize = 10.sp,
            color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text(spentLabel, fontSize = 12.sp, color = scheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text("¥${Money.format(spent)}", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = ExpenseRose)
        }
        Box(
            Modifier.fillMaxWidth().padding(top = 8.dp).height(10.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(999.dp))
        ) {
            Box(
                Modifier.fillMaxWidth(frac.coerceIn(0f, 1f)).height(10.dp)
                    .background(if (over) ExpenseRose else IncomeGreen,
                        RoundedCornerShape(999.dp))
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text("已用 ${pctUsed}%", fontSize = 9.5.sp, color = scheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text(
                if (remaining >= 0) "剩余 ${100 - pctUsed.coerceAtMost(100)}%"
                else "超支 ${budgetMoney(-remaining)}",
                fontSize = 9.5.sp,
                color = if (remaining >= 0) scheme.onSurfaceVariant else ExpenseRose
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
            Column(
                Modifier.weight(1f)
                    .border(1.dp, scheme.outlineVariant, RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text("预算余额", fontSize = 10.sp, color = scheme.onSurfaceVariant)
                Text(budgetMoney(remaining), fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    color = if (remaining < 0) ExpenseRose else IncomeGreen)
            }
            Spacer(Modifier.width(10.dp))
            Column(
                Modifier.weight(1f)
                    .border(1.dp, scheme.outlineVariant, RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text("每日平均", fontSize = 10.sp, color = scheme.onSurfaceVariant)
                val avgCents = if (days > 0) spent / days else 0L
                Text("¥${Money.format(avgCents)}", fontSize = 15.sp,
                    fontWeight = FontWeight.Bold, color = scheme.primary)
            }
        }
    }
}

@Composable
private fun BudgetEditDialog(
    name: String,
    amountCents: Long,
    period: String,
    startAtMillis: Long?,
    endAtMillis: Long?,
    accountId: Long?,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?,
    onSave: (String, Long, String, Long?, Long?, Long?) -> Unit
) {
    var nm by remember { mutableStateOf(name) }
    var amount by remember { mutableStateOf(if (amountCents > 0) Money.formatPlain(amountCents) else "") }
    var p by remember { mutableStateOf(period) }
    val today = LocalDate.now()
    var customStart by remember {
        mutableStateOf(startAtMillis ?: startOfDate(today))
    }
    var customEnd by remember {
        mutableStateOf(endAtMillis ?: startOfDate(today.plusMonths(1)))
    }
    var selectedAccountId by remember { mutableStateOf(accountId) }
    var dateTarget by remember { mutableStateOf<String?>(null) }
    // 日期选择器不与预算编辑弹窗同时挂载；否则底层 Dialog 可能拦截日期格的触控。
    if (dateTarget == null) {
        AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (name.isBlank()) "新增预算" else "编辑预算",
            fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                BasicTextField(
                    value = nm,
                    onValueChange = { nm = it.take(10) },
                    textStyle = MaterialTheme.typography.bodyLarge
                        .copy(color = MaterialTheme.colorScheme.onSurface),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(10.dp))
                            .padding(12.dp)) {
                            if (nm.isEmpty()) Text("预算名称（可自定义）",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            inner()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text("计算方式", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("按月" to "month", "按年" to "year", "自定义" to "custom").forEach { (lbl, v) ->
                        val sel = p == v
                        Text(lbl, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                                .background(if (sel) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(999.dp))
                                .noRippleClickable { p = v }
                                .padding(vertical = 7.dp),
                            color = if (sel) Color.White
                            else MaterialTheme.colorScheme.onSurface)
                    }
                }
                if (p == "custom") {
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(
                            Modifier.weight(1f)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                .noRippleClickable { dateTarget = "start" }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text("开始日期", fontSize = 9.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(localDateOf(customStart).toString(), fontSize = 12.sp,
                                fontWeight = FontWeight.Bold)
                        }
                        Column(
                            Modifier.weight(1f)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                .noRippleClickable { dateTarget = "end" }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text("结束日期（含当日）", fontSize = 9.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(localDateOf(customEnd).minusDays(1).toString(), fontSize = 12.sp,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("预算钱包", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val allSelected = selectedAccountId == null
                    Text("全部钱包", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(
                                if (allSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(999.dp)
                            )
                            .noRippleClickable { selectedAccountId = null }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        color = if (allSelected) Color.White
                        else MaterialTheme.colorScheme.onSurface)
                    accounts.forEach { account ->
                        val selected = selectedAccountId == account.id
                        Text("${accountIcon(account)} ${account.name}", fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(999.dp)
                                )
                                .noRippleClickable { selectedAccountId = account.id }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            color = if (selected) Color.White
                            else MaterialTheme.colorScheme.onSurface)
                    }
                }
                Spacer(Modifier.height(8.dp))
                BasicTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    textStyle = MaterialTheme.typography.bodyLarge
                        .copy(color = MaterialTheme.colorScheme.onSurface),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(10.dp))
                            .padding(12.dp)) {
                            if (amount.isEmpty()) Text("预算金额",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            inner()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("按所选周期和钱包统计；结束日期当天计入预算。",
                    fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp))
            }
        },
        confirmButton = {
            TextButton(
                enabled = p != "custom" || customEnd > customStart,
                onClick = {
                    onSave(
                        nm.trim(), Money.parse(amount), p,
                        if (p == "custom") customStart else null,
                        if (p == "custom") customEnd else null,
                        selectedAccountId
                    )
                }
            ) { Text("保存") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
        )
    } else {
        val target = requireNotNull(dateTarget)

        val initialDate = if (target == "start") {
            localDateOf(customStart)
        } else {
            localDateOf(customEnd).minusDays(1)
        }
        BudgetDatePickerDialog(
            title = if (target == "start") "选择开始日期" else "选择结束日期",
            initialDate = initialDate,
            onDismiss = { dateTarget = null },
            onPick = { picked ->
                if (target == "start") {
                    customStart = startOfDate(picked)
                    if (customEnd <= customStart) customEnd = startOfDate(picked.plusDays(1))
                } else {
                    val inclusiveEnd = startOfDate(picked.plusDays(1))
                    customEnd = if (inclusiveEnd > customStart) inclusiveEnd
                    else startOfDate(localDateOf(customStart).plusDays(1))
                }
                dateTarget = null
            }
        )
    }
}

@Composable
private fun BudgetDatePickerDialog(
    title: String,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onPick: (LocalDate) -> Unit
) {
    val context = LocalContext.current
    // 使用系统日期选择器，避开 Compose Dialog 内嵌日期控件在部分手机上的触控问题。
    LaunchedEffect(initialDate, title) {
        AndroidDatePickerDialog(
            context,
            { _, year, month, day -> onPick(LocalDate.of(year, month + 1, day)) },
            initialDate.year,
            initialDate.monthValue - 1,
            initialDate.dayOfMonth
        ).apply {
            setTitle(title)
            setOnCancelListener { onDismiss() }
            show()
        }
    }
}
