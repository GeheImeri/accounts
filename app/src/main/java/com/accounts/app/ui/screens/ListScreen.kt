package com.accounts.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextOverflow
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
    var query by remember { mutableStateOf("") }

    var typeFilter by remember { mutableStateOf<String?>(null) }
    var catFilters by remember { mutableStateOf(emptySet<Long>()) }
    var accFilters by remember { mutableStateOf(emptySet<Long>()) }

    val catMap = remember(categories) { categories.associateBy { it.id } }
    val accMap = remember(accounts) { accounts.associateBy { it.id } }
    val (start, end) = remember(month) {
        val r = Days.monthRange(month); r[0] to r[1]
    }

    val monthTx = remember(transactions, start, end) {
        transactions.filter { it.occurredAtMillis in start until end }
    }
    val visible = remember(monthTx, query, typeFilter, catFilters, accFilters) {
        val q = query.trim()
        monthTx.filter { t ->
            (typeFilter == null || t.type == typeFilter) &&
                (catFilters.isEmpty() || t.categoryId in catFilters) &&
                (accFilters.isEmpty() || t.accountId in accFilters) &&
                (q.isEmpty() || t.note.contains(q, ignoreCase = true))
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
    val filterActive = typeFilter != null || catFilters.isNotEmpty() || accFilters.isNotEmpty()

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
                        fontSize = 15.sp, color = ExpenseRose)
                }
                Column {
                    Text("本月收入", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("¥${Money.format(totals.second)}", fontWeight = FontWeight.Bold,
                        fontSize = 15.sp, color = IncomeGreen)
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        // 搜索（按备注过滤）+ 筛选按钮
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                textStyle = MaterialTheme.typography.bodyMedium
                    .copy(color = MaterialTheme.colorScheme.onSurface),
                singleLine = true,
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        if (query.isEmpty()) {
                            Text("搜索备注…", color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.5.sp)
                        }
                        inner()
                    }
                },
                modifier = Modifier.weight(1f)
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
                item { Footnote("没有匹配的记录", Modifier.padding(top = 40.dp)) }
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
            catFilters = catFilters,
            accFilters = accFilters,
            onApply = { t, cs, as_ ->
                typeFilter = t; catFilters = cs; accFilters = as_
                filterOpen = false
            },
            onClear = {
                typeFilter = null; catFilters = emptySet(); accFilters = emptySet()
                filterOpen = false
            },
            onDismiss = { filterOpen = false }
        )
    }
    editing?.let { t ->
        EditRecordDialog(
            transaction = t,
            categories = categories,
            accounts = accounts,
            onDismiss = { editing = null },
            onDelete = { vm.deleteRecord(t); editing = null },
            onSave = { cents, noteText, catId, accId ->
                vm.updateRecord(t, cents, noteText, catId, accId)
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
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(18.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                .border(3.dp, MaterialTheme.colorScheme.background, CircleShape))
        }
        Text(Days.dayLabel(Days.ofDay(date)),
            modifier = Modifier.padding(start = 6.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        Spacer(Modifier.weight(1f))
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
    val meta = accountName ?: "未指定账户"
    Row(
        Modifier.fillMaxWidth().heightIn(min = 68.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(18.dp).height(68.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.width(2.dp).height(68.dp)
                .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(999.dp)))
        }
        Row(
            Modifier.padding(start = 6.dp, bottom = 4.dp).weight(1f)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(
                    topStart = 8.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 20.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
                    RoundedCornerShape(topStart = 8.dp, topEnd = 20.dp,
                        bottomEnd = 20.dp, bottomStart = 20.dp))
                .noRippleClickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(cat?.name ?: "未分类", color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold)
                Text(meta, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.5.sp, lineHeight = 15.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
            val sign = if (t.type == "income") "＋" else "−"
            val color = if (t.type == "income") IncomeGreen else ExpenseRose
            Text("$sign¥${Money.format(t.amountCents)}", color = color,
                fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

// ===== 日历 =====

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
    val firstWeekday = ym.atDay(1).dayOfWeek.value

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
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

// ===== 筛选（类型联动分类 + 分类/账户多选 + 横向滑动）=====

@Composable
private fun FilterDialog(
    categories: List<Category>,
    accounts: List<com.accounts.app.data.Account>,
    typeFilter: String?,
    catFilters: Set<Long>,
    accFilters: Set<Long>,
    onApply: (String?, Set<Long>, Set<Long>) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var type by remember { mutableStateOf(typeFilter) }
    var cats by remember { mutableStateOf(catFilters) }
    var accs by remember { mutableStateOf(accFilters) }

    // 类型联动：选择支出/收入后，分类列表只显示对应分类
    val enabledCats = remember(categories) {
        categories.filter { it.enabled }
            .sortedWith(compareBy({ it.kind }, { it.sortOrder }))
    }
    val shownCats = when (type) {
        "expense" -> enabledCats.filter { it.kind == "expense" }
        "income" -> enabledCats.filter { it.kind == "income" }
        else -> enabledCats
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("筛选（可多选）", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("类型", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChipItem("全部", type == null) {
                        type = null
                        cats = emptySet()
                    }
                    ChipItem("支出", type == "expense") {
                        type = "expense"; cats = emptySet()
                    }
                    ChipItem("收入", type == "income") {
                        type = "income"; cats = emptySet()
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("分类（可多选）", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    shownCats.forEach { cat ->
                        val selected = cat.id in cats
                        Text(cat.name, Modifier
                            .background(
                                if (selected) Color(cat.color)
                                else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(999.dp)
                            )
                            .noRippleClickable {
                                cats = if (selected) cats - cat.id else cats + cat.id
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 12.sp,
                            color = if (selected) Color.White
                            else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("账户（可多选）", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    accounts.forEach { a ->
                        val selected = a.id in accs
                        Text(a.name, Modifier
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(999.dp)
                            )
                            .noRippleClickable {
                                accs = if (selected) accs - a.id else accs + a.id
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 12.sp,
                            color = if (selected) Color.White
                            else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
                Text("左右滑动可查看更多分类/账户", fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(type, cats, accs) }) { Text("应用") }
        },
        dismissButton = {
            TextButton(onClick = onClear) { Text("清空") }
        }
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

// ===== 编辑记录（含修改分类）=====

@Composable
private fun EditRecordDialog(
    transaction: Transaction,
    categories: List<Category>,
    accounts: List<com.accounts.app.data.Account>,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: (Long, String, Long, Long) -> Unit
) {
    var amountText by remember { mutableStateOf(Money.formatPlain(transaction.amountCents)) }
    var note by remember { mutableStateOf(transaction.note) }
    var catId by remember { mutableStateOf(transaction.categoryId) }
    var accId by remember { mutableStateOf(transaction.accountId) }
    val options = remember(transaction, categories) {
        categories.filter { it.enabled && it.kind == transaction.type }.sortedBy { it.sortOrder }
    }
    val accOptions = remember(accounts) {
        accounts.filter { it.enabled }.sortedBy { it.sortOrder }
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
                Spacer(Modifier.height(8.dp))
                Text("钱包", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                accOptions.chunked(4).forEach { rowAccs ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        rowAccs.forEach { a ->
                            Box(
                                Modifier.weight(1f)
                                    .background(
                                        if (accId == a.id) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .noRippleClickable { accId = a.id }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(a.name, fontSize = 12.sp,
                                    color = if (accId == a.id) Color.White
                                    else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        repeat(4 - rowAccs.size) { Spacer(Modifier.weight(1f)) }
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
                if (c > 0) onSave(c, note, catId, accId)
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDelete) { Text("删除", color = MaterialTheme.colorScheme.error) }
        }
    )
}
