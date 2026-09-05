package com.accounts.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accounts.app.data.Category
import com.accounts.app.data.Transaction
import com.accounts.app.ui.AppViewModel
import com.accounts.app.ui.comps.Footnote
import com.accounts.app.ui.comps.GlassCard
import com.accounts.app.ui.comps.noRippleClickable
import com.accounts.app.ui.theme.ExpenseRose
import com.accounts.app.ui.theme.IncomeGreen
import com.accounts.app.util.Days
import com.accounts.app.util.Money
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun ListScreen(vm: AppViewModel) {
    val transactions by vm.transactions.collectAsState()
    val categories by vm.categories.collectAsState()
    val accounts by vm.accounts.collectAsState()

    var month by remember { mutableStateOf(YearMonth.now()) }
    var calOpen by remember { mutableStateOf(false) }
    var filterOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Transaction?>(null) }

    var typeFilter by remember { mutableStateOf<String?>(null) }
    var categoryFilter by remember { mutableStateOf<Long?>(null) }
    var accountFilter by remember { mutableStateOf<Long?>(null) }

    val catMap = remember(categories) { categories.associateBy { it.id } }
    val accMap = remember(accounts) { accounts.associateBy { it.id } }
    val (start, end) = remember(month) {
        val r = Days.monthRange(month); r[0] to r[1]
    }

    val monthTx = remember(transactions, start, end) {
        transactions.filter { it.occurredAtMillis in start until end }
    }
    val visible = remember(monthTx, typeFilter, categoryFilter, accountFilter) {
        monthTx.filter { t ->
            (typeFilter == null || t.type == typeFilter) &&
                (categoryFilter == null || t.categoryId == categoryFilter) &&
                (accountFilter == null || t.accountId == accountFilter)
        }
    }
    val totals = remember(visible) {
        var e = 0L; var i = 0L
        visible.forEach { if (it.type == "expense") e += it.amountCents else i += it.amountCents }
        e to i
    }
    val grouped = remember(visible) {
        visible.groupBy { Days.dayOf(it.occurredAtMillis) }
            .toSortedMap(compareByDescending { it })
            .toList()
    }
    val filterActive = typeFilter != null || categoryFilter != null || accountFilter != null

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("明细", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { month = month.minusMonths(1) }) {
                Icon(Icons.Outlined.KeyboardArrowLeft, contentDescription = "上月")
            }
            Text(
                "${month.year}年${month.monthValue}月",
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        RoundedCornerShape(999.dp))
                    .noRippleClickable { calOpen = true }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold, fontSize = 13.sp
            )
            IconButton(onClick = { month = month.plusMonths(1) }) {
                Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = "下月")
            }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("本月支出", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("¥${Money.format(totals.first)}", fontWeight = FontWeight.Bold,
                        fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Column {
                    Text("本月收入", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("¥${Money.format(totals.second)}", fontWeight = FontWeight.Bold,
                        fontSize = 15.sp, color = IncomeGreen)
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        // 搜索占位 + 筛选按钮（筛选在搜索框右侧）
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = "",
                onValueChange = {},
                textStyle = MaterialTheme.typography.bodyMedium
                    .copy(color = MaterialTheme.colorScheme.onSurface),
                singleLine = true,
                readOnly = true,
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text("搜索备注（迭代中）", color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.5.sp)
                        inner()
                    }
                }
            )
            Spacer(Modifier.width(10.dp))
            Text(
                if (filterActive) "筛选 ●" else "筛选 ▾",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        RoundedCornerShape(10.dp))
                    .noRippleClickable { filterOpen = true }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }

        LazyColumn(Modifier.weight(1f)) {
            grouped.forEach { (date, rows) ->
                item(key = "d$date") {
                    DayHeader(date = date, rows = rows)
                }
                items(rows, key = { it.id }) { t ->
                    TransactionRow(t, catMap[t.categoryId], accMap[t.accountId]?.name) { editing = t }
                }
            }
            if (grouped.isEmpty()) {
                item { Footnote("这个月还没有记录", Modifier.padding(top = 40.dp)) }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (calOpen) {
        CalendarDialog(
            initial = month,
            onPickDate = { date ->
                month = YearMonth.from(date)
                calOpen = false
            },
            onDismiss = { calOpen = false }
        )
    }
    if (filterOpen) {
        FilterDialog(
            categories = categories,
            accounts = accounts,
            typeFilter = typeFilter,
            categoryFilter = categoryFilter,
            accountFilter = accountFilter,
            onApply = { t, c, a ->
                typeFilter = t; categoryFilter = c; accountFilter = a
                filterOpen = false
            },
            onClear = {
                typeFilter = null; categoryFilter = null; accountFilter = null
                filterOpen = false
            },
            onDismiss = { filterOpen = false }
        )
    }
    editing?.let { t ->
        EditRecordDialog(
            transaction = t,
            categories = categories,
            onDismiss = { editing = null },
            onDelete = { vm.deleteRecord(t); editing = null },
            onSave = { cents, noteText, catId ->
                vm.updateRecord(t, cents, noteText, catId)
                editing = null
            }
        )
    }
}

@Composable
private fun DayHeader(date: LocalDate, rows: List<Transaction>) {
    val e = rows.filter { it.type == "expense" }.sumOf { it.amountCents }
    val i = rows.filter { it.type == "income" }.sumOf { it.amountCents }
    val parts = buildList {
        if (e > 0) add("支出 ¥${Money.format(e)}")
        if (i > 0) add("收入 ¥${Money.format(i)}")
    }
    Row(
        Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(Days.dayLabel(Days.ofDay(date)),
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        Text(parts.joinToString(" · "),
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
    }
}

@Composable
private fun TransactionRow(
    t: Transaction,
    cat: Category?,
    accountName: String?,
    onClick: () -> Unit
) {
    val meta = listOfNotNull(cat?.name, accountName, t.note.ifBlank { null }).joinToString(" · ")
    Row(
        Modifier.fillMaxWidth().noRippleClickable(onClick = onClick).padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(cat?.name ?: "未分类", color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(meta, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1)
        }
        val sign = if (t.type == "income") "＋" else "−"
        val color = if (t.type == "income") IncomeGreen else ExpenseRose
        Text("$sign¥${Money.format(t.amountCents)}", color = color,
            fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

// ===== 日历：年份/月份切换 + 具体日期选择 =====

@Composable
private fun CalendarDialog(
    initial: YearMonth,
    onPickDate: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var year by remember { mutableStateOf(initial.year) }
    var monthNo by remember { mutableStateOf(initial.monthValue) }
    val ym = YearMonth.of(year, monthNo)
    val daysInMonth = ym.lengthOfMonth()
    val firstWeekday = ym.atDay(1).dayOfWeek.value   // 周一=1

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("选择日期", fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("本月", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp,
                    modifier = Modifier.noRippleClickable {
                        val now = LocalDate.now()
                        year = now.year; monthNo = now.monthValue
                        onPickDate(now)
                    })
            }
        },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        if (monthNo == 1) { monthNo = 12; year-- } else monthNo--
                    }) {
                        Icon(Icons.Outlined.KeyboardArrowLeft, contentDescription = "上月")
                    }
                    Text("$year 年 $monthNo 月", Modifier.weight(1f),
                        textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    IconButton(onClick = {
                        if (monthNo == 12) { monthNo = 1; year++ } else monthNo++
                    }) {
                        Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = "下月")
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEach { w ->
                        Text(w, Modifier.weight(1f), textAlign = TextAlign.Center,
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(4.dp))
                val rowCount = ((firstWeekday - 1) + daysInMonth + 6) / 7
                (0 until rowCount).forEach { r ->
                    Row(Modifier.fillMaxWidth()) {
                        (1..7).forEach { col ->
                            val day = r * 7 + col - (firstWeekday - 1)
                            if (day in 1..daysInMonth) {
                                val date = ym.atDay(day)
                                val isToday = date == LocalDate.now()
                                Box(
                                    Modifier.weight(1f).padding(2.dp)
                                        .background(
                                            if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else Color.Transparent,
                                            CircleShape
                                        )
                                        .noRippleClickable { onPickDate(date) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$day", fontSize = 13.sp,
                                        color = if (isToday) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface)
                                }
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
                Text("上方 ‹ › 切换月份，点日期即跳转查看当天所在的月份流水",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp))
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

// ===== 筛选弹窗 =====

@Composable
private fun FilterDialog(
    categories: List<Category>,
    accounts: List<com.accounts.app.data.Account>,
    typeFilter: String?,
    categoryFilter: Long?,
    accountFilter: Long?,
    onApply: (String?, Long?, Long?) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var type by remember { mutableStateOf(typeFilter) }
    var category by remember { mutableStateOf(categoryFilter) }
    var account by remember { mutableStateOf(accountFilter) }

    val enabledCats = remember(categories) {
        categories.filter { it.enabled }
            .sortedWith(compareBy({ it.kind }, { it.sortOrder }))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("筛选", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("类型", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChipItem("全部", type == null) { type = null }
                    ChipItem("支出", type == "expense") { type = "expense" }
                    ChipItem("收入", type == "income") { type = "income" }
                }
                Spacer(Modifier.height(12.dp))
                Text("分类", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                CategoryChipRows(options = enabledCats, selected = category) { category = it }
                Spacer(Modifier.height(12.dp))
                Text("账户", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChipItem("全部", account == null) { account = null }
                    accounts.forEach { a ->
                        ChipItem(a.name, account == a.id) { account = a.id }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(type, category, account) }) { Text("应用") }
        },
        dismissButton = { TextButton(onClick = onClear) { Text("清空") } }
    )
}

@Composable
private fun ChipItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(label, Modifier
        .background(if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(999.dp))
        .noRippleClickable(onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 6.dp),
        fontSize = 12.sp,
        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface)
}

@Composable
private fun CategoryChipRows(options: List<Category>, selected: Long?, onSelect: (Long?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.chunked(6).forEach { rowCats ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ChipItem("全部", selected == null) { onSelect(null) }
                rowCats.forEach { cat ->
                    ChipItem(cat.name, selected == cat.id) { onSelect(cat.id) }
                }
            }
        }
    }
}

// ===== 编辑记录（含修改分类）=====

@Composable
private fun EditRecordDialog(
    transaction: Transaction,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: (Long, String, Long) -> Unit
) {
    var amountText by remember { mutableStateOf(Money.formatPlain(transaction.amountCents)) }
    var note by remember { mutableStateOf(transaction.note) }
    var catId by remember { mutableStateOf(transaction.categoryId) }
    val options = remember(transaction, categories) {
        categories.filter { it.enabled && it.kind == transaction.type }.sortedBy { it.sortOrder }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑记录", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(if (transaction.type == "expense") "支出 · 修改分类" else "收入 · 修改分类",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
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
                Spacer(Modifier.height(10.dp))
                BasicTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    textStyle = MaterialTheme.typography.titleLarge
                        .copy(color = MaterialTheme.colorScheme.onSurface),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                BasicTextField(
                    value = note,
                    onValueChange = { note = it },
                    textStyle = MaterialTheme.typography.bodyMedium
                        .copy(color = MaterialTheme.colorScheme.onSurface),
                    singleLine = true,
                    decorationBox = { inner ->
                        if (note.isEmpty()) Text("备注", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        inner()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val c = Money.parse(amountText)
                if (c > 0) onSave(c, note, catId)
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDelete) { Text("删除", color = MaterialTheme.colorScheme.error) }
        }
    )
}
