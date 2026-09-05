package com.accounts.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accounts.app.data.Category
import com.accounts.app.data.Transaction
import com.accounts.app.ui.AppViewModel
import com.accounts.app.ui.comps.Footnote
import com.accounts.app.ui.comps.GlassCard
import com.accounts.app.ui.theme.ExpenseRose
import com.accounts.app.ui.theme.IncomeGreen
import com.accounts.app.ui.theme.Ink
import com.accounts.app.ui.theme.Ink2
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
    var pickerOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Transaction?>(null) }

    val catMap = remember(categories) { categories.associateBy { it.id } }
    val accMap = remember(accounts) { accounts.associateBy { it.id } }
    val (start, end) = remember(month) {
        val r = Days.monthRange(month); r[0] to r[1]
    }

    val monthTx = remember(transactions, start, end) {
        transactions.filter { it.occurredAtMillis in start until end }
    }
    val visible = remember(monthTx, query) {
        if (query.isBlank()) monthTx
        else monthTx.filter { it.note.contains(query.trim(), ignoreCase = true) }
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

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("明细", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            // 月份：两侧 ‹› 快切，中间可点 -> 年/月/日选择器
            IconButton(onClick = { month = month.minusMonths(1) }) {
                Icon(Icons.Outlined.KeyboardArrowLeft, contentDescription = "上月")
            }
            Text(
                "${month.year}年${month.monthValue}月",
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        RoundedCornerShape(999.dp))
                    .clickable { pickerOpen = true }
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
                    Text("本月支出", fontSize = 10.sp, color = Ink2)
                    Text("¥${Money.format(totals.first)}", fontWeight = FontWeight.Bold,
                        fontSize = 15.sp, color = Ink)
                }
                Column {
                    Text("本月收入", fontSize = 10.sp, color = Ink2)
                    Text("¥${Money.format(totals.second)}", fontWeight = FontWeight.Bold,
                        fontSize = 15.sp, color = IncomeGreen)
                }
            }
        }

        BasicTextField(
            value = query,
            onValueChange = { query = it },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Ink),
            singleLine = true,
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    if (query.isEmpty()) Text("搜索备注… 　[筛选 ▾]", color = Ink2, fontSize = 12.5.sp)
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        LazyColumn(Modifier.weight(1f)) {
            grouped.forEach { (date, rows) ->
                item(key = "d$date") {
                    DayHeader(date = date, rows = rows)
                }
                items(rows, key = { it.id }) { t ->
                    TransactionRow(t, catMap[t.categoryId], accMap[t.accountId]) { editing = t }
                }
            }
            if (grouped.isEmpty()) {
                item { Footnote("这个月还没有记录", Modifier.padding(top = 40.dp)) }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (pickerOpen) {
        MonthYearPicker(month, onPick = { month = it; pickerOpen = false }, onDismiss = { pickerOpen = false })
    }
    editing?.let { t ->
        EditRecordDialog(t, onDismiss = { editing = null },
            onDelete = { vm.deleteRecord(t); editing = null },
            onSave = { cents, newNote ->
                vm.updateRecord(t, cents, newNote)
                editing = null
            })
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
        Text(Days.dayLabel(Days.ofDay(date)), color = Ink2, fontSize = 11.sp)
        Text(parts.joinToString(" · "), color = Ink2, fontSize = 11.sp)
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
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(cat?.name ?: "未分类", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(meta, color = Ink2, fontSize = 11.sp, maxLines = 1)
        }
        val sign = if (t.type == "income") "＋" else "−"
        val color = if (t.type == "income") IncomeGreen else ExpenseRose
        Text("$sign¥${Money.format(t.amountCents)}", color = color,
            fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun MonthYearPicker(
    initial: YearMonth,
    onPick: (YearMonth) -> Unit,
    onDismiss: () -> Unit
) {
    var year by remember { mutableStateOf(initial.year) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择月份", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { year-- }) {
                        Icon(Icons.Outlined.KeyboardArrowLeft, contentDescription = "前一年")
                    }
                    Text("$year 年", Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = FontWeight.Bold)
                    IconButton(onClick = { year++ }) {
                        Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = "后一年")
                    }
                }
                listOf(1..6, 7..12).forEach { range ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        range.forEach { m ->
                            val selected = initial.year == year && initial.monthValue == m
                            Box(
                                Modifier.weight(1f)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onPick(YearMonth.of(year, m)) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("$m 月",
                                    color = if (selected) Color.White else Ink,
                                    fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
                Text("选择日期也可在记账时间上点选（后续迭代）", style = MaterialTheme.typography.labelSmall,
                    color = Ink2, modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun EditRecordDialog(
    t: Transaction,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: (Long, String) -> Unit
) {
    var amountText by remember { mutableStateOf(Money.formatPlain(t.amountCents)) }
    var note by remember { mutableStateOf(t.note) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑记录", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                BasicTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    textStyle = MaterialTheme.typography.titleLarge.copy(color = Ink),
                    singleLine = true,
                    keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                BasicTextField(
                    value = note,
                    onValueChange = { note = it },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Ink),
                    singleLine = true,
                    decorationBox = { inner ->
                        if (note.isEmpty()) Text("备注", color = Ink2)
                        inner()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { val c = Money.parse(amountText); if (c > 0) onSave(c, note) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDelete) { Text("删除", color = MaterialTheme.colorScheme.error) }
        }
    )
}
