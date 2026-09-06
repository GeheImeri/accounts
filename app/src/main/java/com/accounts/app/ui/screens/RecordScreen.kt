@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.accounts.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accounts.app.data.Budget
import com.accounts.app.data.Category
import com.accounts.app.data.Transaction
import com.accounts.app.ui.AppViewModel
import com.accounts.app.ui.comps.CtaButton
import com.accounts.app.ui.comps.Segment
import com.accounts.app.ui.comps.noRippleClickable
import com.accounts.app.ui.theme.ExpenseRose
import com.accounts.app.ui.theme.IncomeGreen
import com.accounts.app.ui.theme.Ink
import com.accounts.app.ui.theme.chipColor
import com.accounts.app.util.Days
import com.accounts.app.util.Money
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneOffset

/** 首页可见分类：启用 + 该类型，pinned 优先，最多 8 个（首页固定 8 格） */
fun homeCategories(all: List<Category>, kind: String): List<Category> =
    all.filter { it.enabled && it.kind == kind }
        .sortedWith(compareBy({ !it.pinned }, { it.sortOrder }))
        .take(8)

@Composable
fun RecordScreen(vm: AppViewModel) {
    val categories by vm.categories.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val transactions by vm.transactions.collectAsState()
    val templates by vm.templates.collectAsState()

    var kind by remember { mutableStateOf("expense") }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedCatId by remember { mutableStateOf<Long?>(null) }
    var accountId by remember { mutableStateOf(0L) }
    var occurredAt by remember { mutableStateOf(Days.nowMillis()) }
    var accountMenu by remember { mutableStateOf(false) }
    var timePickerOpen by remember { mutableStateOf(false) }
    var recentDeleteMode by remember { mutableStateOf(false) }
    var templateDeleteMode by remember { mutableStateOf(false) }
    var addTemplateOpen by remember { mutableStateOf(false) }
    val hiddenRecents = remember { mutableStateListOf<Long>() }

    val defaultAccountId by vm.defaultAccountId.collectAsState()
    LaunchedEffect(accounts, defaultAccountId) {
        if (accountId == 0L) {
            accountId = accounts.firstOrNull { it.id == defaultAccountId }?.id
                ?: accounts.firstOrNull()?.id
                ?: 0L
        }
    }
    // 记完一笔后：钱包回到默认账户、时间回到"现在"
    val resetToDefaults = {
        accountId = accounts.firstOrNull { it.id == defaultAccountId }?.id
            ?: accounts.firstOrNull()?.id
            ?: 0L
        occurredAt = Days.nowMillis()
    }

    val cats = homeCategories(categories, kind)
    val baseRecents = remember(transactions) { transactions.map { it.amountCents }.distinct() }
    val recents = baseRecents.filterNot { it in hiddenRecents }.take(4)
    val enabledCategoryIds = categories.asSequence().filter { it.enabled }.map { it.id }.toSet()
    val kindTemplates = templates.filter {
        it.kind == kind && it.categoryId in enabledCategoryIds
    }

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

        // ===== 金额（空时显示与 ¥ 同色的 0.00）=====
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("¥", fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium)
            BasicTextField(
                value = amountText,
                onValueChange = { s ->
                    val f = s.filter { it.isDigit() || it == '.' }
                    if (f.count { it == '.' } <= 1 && f.length <= 10) amountText = f
                },
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold)
                        }
                        inner()
                    }
                }
            )
        }

        // ===== 备注（居中）=====
        BasicTextField(
            value = note,
            onValueChange = { note = it },
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            ),
            singleLine = true,
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth().height(30.dp), contentAlignment = Alignment.Center) {
                    if (note.isEmpty()) {
                        Text("备注（可选）", color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp)
                    }
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        // ===== 分类色片（仅选中；保存走下方按钮，不再点分类即存）=====
        cats.chunked(4).forEach { rowCats ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowCats.forEach { cat ->
                    CategoryTile(
                        cat = cat,
                        selected = selectedCatId == cat.id,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedCatId = cat.id }
                    )
                }
                repeat(4 - rowCats.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        // ===== 钱包（可切换）/ 时间（可点击选择，补记用）=====
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("钱包：${accountName(accounts, accountId)} ▾", fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.noRippleClickable { accountMenu = true })
            Text(
                Days.todayTimeLabel(occurredAt),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.noRippleClickable { timePickerOpen = true }
            )
        }
        DropdownMenu(expanded = accountMenu, onDismissRequest = { accountMenu = false }) {
            accounts.forEach { a ->
                DropdownMenuItem(
                    text = { Text(a.name) },
                    onClick = { accountId = a.id; accountMenu = false }
                )
            }
        }

        // ===== 最近金额（长按进入删除）=====
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel2("最近金额")
            Spacer(Modifier.weight(1f))
            if (recentDeleteMode) {
                Text("✕ 完成", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.noRippleClickable { recentDeleteMode = false })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            recents.forEach { cents ->
                val src = remember(cents) { MutableInteractionSource() }
                Box {
                    Text(Money.formatPlain(cents),
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                RoundedCornerShape(999.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(999.dp))
                            .combinedClickable(
                                interactionSource = src,
                                indication = null,
                                onClick = {
                                    if (recentDeleteMode) hiddenRecents.add(cents)
                                    else amountText = Money.formatPlain(cents)
                                },
                                onLongClick = { recentDeleteMode = true }
                            )
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    if (recentDeleteMode) {
                        CrossBadge(Modifier.align(Alignment.TopEnd))
                    }
                }
            }
        }

        // ===== 我的模板（长按删除 / ＋新增）=====
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel2("我的模板")
            Spacer(Modifier.weight(1f))
            if (templateDeleteMode) {
                Text("✕ 完成", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.noRippleClickable { templateDeleteMode = false })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            kindTemplates.forEach { t ->
                val tsrc = remember(t.id) { MutableInteractionSource() }
                val cat = categories.firstOrNull { it.id == t.categoryId }
                Box {
                    Text("${t.name} ¥${Money.formatPlain(t.amountCents)}",
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                RoundedCornerShape(999.dp))
                            .combinedClickable(
                                interactionSource = tsrc,
                                indication = null,
                                onClick = {
                                    if (templateDeleteMode) vm.deleteTemplate(t)
                                    else {
                                        vm.addRecord(kind, t.amountCents, t.categoryId,
                                            accountId, occurredAt, "")
                                        amountText = ""
                                        resetToDefaults()
                                    }
                                },
                                onLongClick = { templateDeleteMode = true }
                            )
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        fontSize = 12.sp,
                        color = if (cat != null) Color(cat.color) else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold)
                    if (templateDeleteMode) {
                        CrossBadge(Modifier.align(Alignment.TopEnd))
                    }
                }
            }
            Box(
                Modifier
                    .size(32.dp)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape)
                    .noRippleClickable { addTemplateOpen = true },
                contentAlignment = Alignment.Center
            ) {
                Text("＋", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 1.dp))
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
            onDismiss = { editingBudget = null },
            onDelete = { vm.deleteBudget(b); editingBudget = null },
            onSave = { nm, cents, p ->
                vm.updateBudget(b.copy(name = nm, amountCents = cents, period = p))
                editingBudget = null
            }
        )
    }
    if (addBudgetOpen) {
        BudgetEditDialog(
            name = "",
            amountCents = 0L,
            period = "month",
            onDismiss = { addBudgetOpen = false },
            onDelete = null,
            onSave = { nm, cents, p ->
                vm.addBudget(nm.ifBlank { if (p == "year") "今年预算" else "本月预算" }, cents, p)
                addBudgetOpen = false
            }
        )
    }
    if (addTemplateOpen) {
        AddTemplateDialog(
            categories = categories,
            kind = kind,
            onDismiss = { addTemplateOpen = false },
            onAdd = { name, cents, catId, k ->
                vm.addTemplate(name, cents, catId, k)
                addTemplateOpen = false
            }
        )
    }
}

private fun accountName(accounts: List<com.accounts.app.data.Account>, id: Long): String =
    accounts.firstOrNull { it.id == id }?.name ?: "—"

@Composable
private fun CategoryTile(cat: Category, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .shadow(if (selected) 6.dp else 2.dp, shape)
            .background(chipColor(cat.color), shape)
            .border(if (selected) 2.5.dp else 0.dp, MaterialTheme.colorScheme.primary, shape)
            .noRippleClickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (cat.icon.isNotEmpty()) {
            Text(cat.icon, fontSize = 21.sp, lineHeight = 26.sp)
            Spacer(Modifier.height(3.dp))
        }
        Text(cat.name, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            maxLines = 1)
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

@Composable
private fun BudgetCard(
    budget: Budget,
    transactions: List<Transaction>,
    onEdit: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val today = LocalDate.now()
    val startEnd: Pair<Long, Long>
    var days = today.dayOfMonth
    if (budget.period == "year") {
        startEnd = Days.monthRange(YearMonth.of(today.year, 1))[0] to
            Days.monthRange(YearMonth.of(today.year + 1, 1))[0]
        days = today.dayOfYear
    } else {
        val r = Days.monthRange(YearMonth.from(today))
        startEnd = r[0] to r[1]
    }
    val amount = budget.amountCents
    val spent = transactions.filter {
        it.type == "expense" && it.occurredAtMillis in startEnd.first until startEnd.second
    }.sumOf { it.amountCents }
    val remaining = amount - spent
    val pctUsed = if (amount > 0) spent * 100 / amount else 0L
    val frac = if (amount > 0) spent.toFloat() / amount.toFloat() else 0f
    val over = amount > 0 && spent > amount
    val periodLabel = if (budget.period == "year") "今年" else "本月"
    val periodBadge = if (budget.period == "year") "按年" else "按月"

    Column(
        Modifier.fillMaxWidth()
            .border(1.dp, Color(0xFFE8E2F4), RoundedCornerShape(22.dp))
            .background(scheme.surface, RoundedCornerShape(22.dp))
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
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("${periodLabel}已支出", fontSize = 12.sp, color = scheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text("¥${Money.format(spent)}", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = ExpenseRose)
        }
        Box(
            Modifier.fillMaxWidth().padding(top = 8.dp).height(10.dp)
                .background(Color(0xFFF1EEF8), RoundedCornerShape(999.dp))
        ) {
            Box(
                Modifier.fillMaxWidth(frac.coerceIn(0f, 1f)).height(10.dp)
                    .background(if (over) ExpenseRose else scheme.primary,
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
                    .border(1.dp, Color(0xFFE8E2F4), RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text("预算余额", fontSize = 10.sp, color = scheme.onSurfaceVariant)
                Text(budgetMoney(remaining), fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    color = if (remaining < 0) ExpenseRose else IncomeGreen)
            }
            Spacer(Modifier.width(10.dp))
            Column(
                Modifier.weight(1f)
                    .border(1.dp, Color(0xFFE8E2F4), RoundedCornerShape(14.dp))
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
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?,
    onSave: (String, Long, String) -> Unit
) {
    var nm by remember { mutableStateOf(name) }
    var amount by remember { mutableStateOf(if (amountCents > 0) Money.formatPlain(amountCents) else "") }
    var p by remember { mutableStateOf(period) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (name.isBlank()) "新增预算" else "编辑预算",
            fontWeight = FontWeight.Bold) },
        text = {
            Column {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("计算方式", fontSize = 13.sp)
                    Spacer(Modifier.weight(1f))
                    listOf("按月" to "month", "按年" to "year").forEach { (lbl, v) ->
                        val sel = p == v
                        Text(lbl, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 6.dp)
                                .background(if (sel) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(999.dp))
                                .noRippleClickable { p = v }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            color = if (sel) Color.White
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
                Text("月与年各自独立统计（已支出/余额/每日平均按所选方式计算）",
                    fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(nm.trim(), Money.parse(amount), p) }) { Text("保存") }
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
}
