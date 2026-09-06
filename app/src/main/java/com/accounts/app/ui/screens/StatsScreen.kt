package com.accounts.app.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accounts.app.data.Account
import com.accounts.app.data.Transaction
import com.accounts.app.ui.AppViewModel
import com.accounts.app.ui.comps.GlassCard
import com.accounts.app.ui.comps.noRippleClickable
import com.accounts.app.ui.theme.ExpenseRose
import com.accounts.app.ui.theme.IncomeGreen
import com.accounts.app.util.Days
import com.accounts.app.util.Money
import java.time.YearMonth

private data class Rank(
    val categoryId: Long,
    val name: String,
    val color: Color,
    val amount: Long,
    val pct: Float
)

private data class Totals(val expense: Long, val income: Long)

private fun totalsOf(list: List<Transaction>): Totals {
    var e = 0L; var i = 0L
    list.forEach { if (it.type == "expense") e += it.amountCents else i += it.amountCents }
    return Totals(e, i)
}

@Composable
fun StatsScreen(vm: AppViewModel, onOpenSettings: () -> Unit) {
    val transactions by vm.transactions.collectAsState()
    val categories by vm.categories.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val transfers by vm.transfers.collectAsState()

    var monthOffset by remember { mutableStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }
    var showAll by remember { mutableStateOf(false) }
    var selectedCatId by remember { mutableStateOf<Long?>(null) }
    var accountSelectedId by remember { mutableStateOf<Long?>(null) }
    var showAllAccounts by remember { mutableStateOf(false) }
    var balanceDialog by remember { mutableStateOf(false) }
    var dailyDialogType by remember { mutableStateOf<String?>(null) }

    val month = remember(monthOffset) { YearMonth.now().plusMonths(monthOffset.toLong()) }
    val catMap = remember(categories) { categories.associateBy { it.id } }

    val monthTx = remember(transactions, month) {
        val r = Days.monthRange(month)
        transactions.filter { it.occurredAtMillis in r[0] until r[1] }
    }
    val prevTx = remember(transactions, month) {
        val r = Days.monthRange(month.minusMonths(1))
        transactions.filter { it.occurredAtMillis in r[0] until r[1] }
    }
    val cur = totalsOf(monthTx)
    val prev = totalsOf(prevTx)
    val balance = cur.income - cur.expense

    val totalExpense = cur.expense
    val fallbackColor = MaterialTheme.colorScheme.onSurfaceVariant
    val ranks = remember(monthTx, catMap) {
        monthTx.filter { it.type == "expense" }
            .groupBy { it.categoryId }
            .map { (cid, list) ->
                val cat = catMap[cid]
                Rank(
                    categoryId = cid,
                    name = cat?.name ?: "未分类",
                    color = if (cat != null) Color(cat.color) else fallbackColor,
                    amount = list.sumOf { it.amountCents },
                    pct = 0f
                )
            }
            .sortedByDescending { it.amount }
            .map { r ->
                r.copy(pct = if (totalExpense > 0) r.amount * 100f / totalExpense else 0f)
            }
    }
    val shown = if (showAll) ranks else ranks.take(4)

    // ===== 当月各账户支出构成 / 排行 =====
    val accMap = remember(accounts) { accounts.associateBy { it.id } }
    val accountRanks = remember(monthTx, accMap) {
        monthTx.filter { it.type == "expense" }
            .groupBy { it.accountId }
            .map { (aid, list) ->
                val acc = accMap[aid]
                Rank(
                    categoryId = aid,
                    name = acc?.name ?: "未分类",
                    color = if (acc != null) Color(acc.color) else fallbackColor,
                    amount = list.sumOf { it.amountCents },
                    pct = 0f
                )
            }
            .sortedByDescending { it.amount }
            .map { r ->
                r.copy(pct = if (totalExpense > 0) r.amount * 100f / totalExpense else 0f)
            }
    }
    val shownAccounts = if (showAllAccounts) accountRanks else accountRanks.take(4)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        // 顶栏：标题 …… [设置(滑杆)] [本月▾ 菜单]
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("统计", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Tune, contentDescription = "设置",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                val label = if (monthOffset == 0) "本月" else "${month.year}年${month.monthValue}月"
                Text(
                    label,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            RoundedCornerShape(999.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(999.dp))
                        .noRippleClickable { menuOpen = true }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold, fontSize = 12.sp
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("本月") }, onClick = {
                        monthOffset = 0; showAll = false; selectedCatId = null
                        showAllAccounts = false; accountSelectedId = null
                        menuOpen = false
                    })
                    (1..11).forEach { back ->
                        val m = YearMonth.now().minusMonths(back.toLong())
                        DropdownMenuItem(
                            text = { Text("${m.year}年${m.monthValue}月") },
                            onClick = {
                                monthOffset = -back
                                showAll = false
                                selectedCatId = null
                                showAllAccounts = false
                                accountSelectedId = null
                                menuOpen = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        // 总览卡
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassCard(
                Modifier.weight(1f).noRippleClickable { dailyDialogType = "expense" }
            ) {
                StatCell("支出", "¥${Money.format(cur.expense)}",
                    delta(cur.expense, prev.expense), expense = true)
            }
            GlassCard(
                Modifier.weight(1f).noRippleClickable { dailyDialogType = "income" }
            ) {
                StatCell("收入", "¥${Money.format(cur.income)}",
                    delta(cur.income, prev.income), expense = false)
            }
            // 结余：点击查看各钱包余额
            GlassCard(
                Modifier.weight(1f).noRippleClickable { balanceDialog = true }
            ) {
                StatCell("结余", "¥${Money.format(balance)}", null, expense = false)
            }
        }

        Spacer(Modifier.height(14.dp))
        // 环形图（独立卡片、下移，避免与上方总览卡重叠）
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp)) {
                DonutChart(ranks, selectedCatId)
                Column(Modifier.padding(start = 10.dp)) {
                    Text("支出构成", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    ranks.forEach { r ->
                        val sel = selectedCatId == r.categoryId
                        Row(verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 3.dp)) {
                            Box(Modifier.size(9.dp).background(r.color, CircleShape))
                            Text(r.name, Modifier.padding(start = 6.dp), fontSize = 12.sp,
                                color = if (sel) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                            Text("${Math.round(r.pct)}%", Modifier.weight(1f),
                                textAlign = TextAlign.End, fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            shown.forEach { r ->
                RankRow(r, selected = selectedCatId == r.categoryId, onClick = {
                    selectedCatId = if (selectedCatId == r.categoryId) null else r.categoryId
                })
            }
            if (ranks.size > 4) {
                Text(
                    if (showAll) "收起 ▴" else "展开全部 ${ranks.size} 个分类 ▾",
                    Modifier.fillMaxWidth().noRippleClickable { showAll = !showAll }
                        .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold, fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("近 12 个月支出趋势", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        TrendBars(transactions, type = "expense",
            color = MaterialTheme.colorScheme.primary)

        // ===== 当月各账户支出构成（环形）=====
        Spacer(Modifier.height(18.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp)) {
                DonutChart(accountRanks, accountSelectedId)
                Column(Modifier.padding(start = 10.dp)) {
                    Text("本月各账户支出", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    accountRanks.forEach { r ->
                        val sel = accountSelectedId == r.categoryId
                        Row(verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 3.dp)) {
                            Box(Modifier.size(9.dp).background(r.color, CircleShape))
                            Text(r.name, Modifier.padding(start = 6.dp), fontSize = 12.sp,
                                color = if (sel) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                            Text("${Math.round(r.pct)}%", Modifier.weight(1f),
                                textAlign = TextAlign.End, fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // ===== 各账户支出排行 · 点击可突出环形 =====
        Spacer(Modifier.height(14.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            shownAccounts.forEach { r ->
                RankRow(r, selected = accountSelectedId == r.categoryId, onClick = {
                    accountSelectedId =
                        if (accountSelectedId == r.categoryId) null else r.categoryId
                })
            }
            if (accountRanks.size > 4) {
                Text(
                    if (showAllAccounts) "收起 ▴" else "展开全部 ${accountRanks.size} 个账户 ▾",
                    Modifier.fillMaxWidth().noRippleClickable { showAllAccounts = !showAllAccounts }
                        .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold, fontSize = 12.sp
                )
            }
        }

        // ===== 近 12 个月各账户余额变化（折线）=====
        Spacer(Modifier.height(14.dp))
        AccountBalanceSection(accounts, transactions, transfers)

        Spacer(Modifier.height(18.dp))
        Text("近 12 个月收入趋势", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        TrendBars(transactions, type = "income", color = IncomeGreen)

        Spacer(Modifier.height(28.dp))
    }

    if (dailyDialogType != null) {
        MonthAmountsDialog(
            month = month,
            type = dailyDialogType!!,
            transactions = transactions,
            onDismiss = { dailyDialogType = null }
        )
    }
    if (balanceDialog) {
        BalanceDialog(
            accounts = accounts,
            transactions = transactions,
            transfers = transfers,
            monthBalance = balance
        ) { balanceDialog = false }
    }
}

private fun delta(cur: Long, prev: Long): String? {
    if (prev <= 0) return null
    val pct = (cur - prev) * 100 / prev
    return if (pct >= 0) "▲ $pct%" else "▼ ${-pct}%"
}

/** 账户余额 = 初始 + 收入 − 支出 + 转入 − 转出 */
private fun accountBalance(
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

private fun balanceText(cents: Long): String =
    if (cents < 0) "-¥${Money.format(-cents)}" else "¥${Money.format(cents)}"

/** 账户在某时间点(不包含)前的余额 = 期初 + 之前所有收支/转账 */
private fun accountBalanceUpTo(
    account: Account,
    transactions: List<Transaction>,
    transfers: List<com.accounts.app.data.Transfer>,
    endExclusive: Long
): Long {
    var bal = account.initialBalanceCents
    transactions.forEach { t ->
        if (t.accountId == account.id && t.occurredAtMillis < endExclusive) {
            if (t.type == "income") bal += t.amountCents else bal -= t.amountCents
        }
    }
    transfers.forEach { tr ->
        if (tr.occurredAtMillis < endExclusive) {
            if (tr.fromAccountId == account.id) bal -= tr.amountCents
            if (tr.toAccountId == account.id) bal += tr.amountCents
        }
    }
    return bal
}

/** 近 12 个月各账户余额变化 · 折线图（图例可隐藏/显示） */
@Composable
private fun AccountBalanceSection(
    accounts: List<Account>,
    transactions: List<Transaction>,
    transfers: List<com.accounts.app.data.Transfer>
) {
    val enabled = remember(accounts) {
        accounts.filter { it.enabled }.sortedBy { it.sortOrder }
    }
    val months = remember { (11 downTo 0).map { YearMonth.now().minusMonths(it.toLong()) } }
    val boundaries = remember(months) {
        months.map { m -> Days.monthRange(m.plusMonths(1))[0] }
    }
    val hidden = remember { mutableStateListOf<Long>() }
    val allSeries = remember(enabled, transactions, transfers, boundaries) {
        enabled.map { acc ->
            acc to boundaries.map { end -> accountBalanceUpTo(acc, transactions, transfers, end) }
        }
    }
    val maxV = allSeries.flatMap { it.second }.maxOrNull() ?: 1L
    val minV = allSeries.flatMap { it.second }.minOrNull() ?: 0L
    val rangeV = (maxV - minV).coerceAtLeast(1L)
    val visible = allSeries.filter { it.first.id !in hidden }
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val dimColor = MaterialTheme.colorScheme.onSurfaceVariant

    GlassCard(Modifier.fillMaxWidth()) {
        // 图例（点选隐藏/显示）
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("近 12 个月各账户余额", fontSize = 12.sp, color = dimColor)
            enabled.forEach { acc ->
                val on = acc.id !in hidden
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.noRippleClickable {
                        if (on) hidden.add(acc.id) else hidden.remove(acc.id)
                    }) {
                    Box(Modifier.size(9.dp).background(
                        if (on) Color(acc.color)
                        else dimColor.copy(alpha = 0.3f), CircleShape))
                    Text(acc.name, Modifier.padding(start = 4.dp), fontSize = 11.sp,
                        color = if (on) MaterialTheme.colorScheme.onSurface else dimColor,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
        if (visible.isEmpty()) {
            Text("点击上方图例可显示账户曲线", fontSize = 11.sp, color = dimColor,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                textAlign = TextAlign.Center)
        } else {
            Canvas(Modifier.fillMaxWidth().height(150.dp)) {
                val top = 10.dp.toPx()
                val bottom = 12.dp.toPx()
                val padX = 8.dp.toPx()
                val n = months.size
                val stepX = if (n > 1) (size.width - 2 * padX) / (n - 1) else 0f
                fun xOf(i: Int): Float = padX + i * stepX
                fun yOf(v: Long): Float =
                    top + (1f - (v - minV).toFloat() / rangeV.toFloat()) * (size.height - top - bottom)

                listOf(0.2f, 0.5f, 0.8f).forEach { f ->
                    val yy = top + f * (size.height - top - bottom)
                    drawLine(color = gridColor,
                        start = Offset(padX, yy), end = Offset(size.width - padX, yy),
                        strokeWidth = 1.dp.toPx())
                }
                visible.forEach { (acc, series) ->
                    val color = Color(acc.color)
                    for (i in 1 until n) {
                        drawLine(color = color,
                            start = Offset(xOf(i - 1), yOf(series[i - 1])),
                            end = Offset(xOf(i), yOf(series[i])),
                            strokeWidth = 2.dp.toPx())
                    }
                    series.forEachIndexed { i, v ->
                        drawCircle(color = color, radius = 3.dp.toPx(),
                            center = Offset(xOf(i), yOf(v)))
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                months.forEach { m ->
                    Text("${m.monthValue}月", Modifier.weight(1f),
                        fontSize = 8.sp, textAlign = TextAlign.Center, color = dimColor)
                }
            }
            Text("余额 = 期初 + 各月累计收支/转账 · 点图例可隐藏", fontSize = 9.sp,
                color = dimColor,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                textAlign = TextAlign.Center)
        }
    }
}

/** 每日金额日历（支出/收入，可切换月份；支出金额带 −） */
@Composable
private fun MonthAmountsDialog(
    month: YearMonth,
    type: String,
    transactions: List<Transaction>,
    onDismiss: () -> Unit
) {
    val typeLabel = if (type == "income") "收入" else "支出"
    var m by remember(month) { mutableStateOf(month) }
    val dayAmounts = remember(m, type, transactions) {
        val r = Days.monthRange(m)
        transactions.filter {
            it.type == type && it.occurredAtMillis in r[0] until r[1]
        }.groupBy { Days.dayOf(it.occurredAtMillis).dayOfMonth }
            .mapValues { (_, list) -> list.sumOf { it.amountCents } }
    }
    val total = remember(dayAmounts) { dayAmounts.values.sum() }
    val daysInMonth = m.lengthOfMonth()
    val firstWeekday = m.atDay(1).dayOfWeek.value

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$typeLabel日历", fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("‹", fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.noRippleClickable { m = m.minusMonths(1) })
                Text("${m.year}年${m.monthValue}月", fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp))
                Text("›", fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.noRippleClickable { m = m.plusMonths(1) })
            }
        },
        text = {
            Column {
                Row(Modifier.fillMaxWidth()) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEach { w ->
                        Text(w, Modifier.weight(1f), textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(4.dp))
                val rowCount = ((firstWeekday - 1) + daysInMonth + 6) / 7
                (0 until rowCount).forEach { r ->
                    Row(Modifier.fillMaxWidth()) {
                        (1..7).forEach { col ->
                            val day = r * 7 + col - (firstWeekday - 1)
                            if (day in 1..daysInMonth) {
                                val cents = dayAmounts[day] ?: 0L
                                Column(
                                    Modifier.weight(1f).padding(vertical = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("$day", fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        if (cents > 0) {
                                            (if (type == "income") "" else "-") +
                                                Money.formatPlain(cents)
                                        } else {
                                            "\u00A0"
                                        },
                                        fontSize = 8.sp, maxLines = 1,
                                        color = if (cents > 0) {
                                            if (type == "income") IncomeGreen else ExpenseRose
                                        } else {
                                            Color.Transparent
                                        }
                                    )
                                }
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
                Text("该月$typeLabel总额：¥${Money.format(total)}",
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    fontSize = 12.sp, textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface)
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

/** 点击「结余」后的弹窗：各钱包当前余额 */
@Composable
private fun BalanceDialog(
    accounts: List<Account>,
    transactions: List<Transaction>,
    transfers: List<com.accounts.app.data.Transfer>,
    monthBalance: Long,
    onDismiss: () -> Unit
) {
    val rows = remember(accounts, transactions, transfers) {
        accounts.filter { it.enabled }
            .sortedBy { it.sortOrder }
            .map { a -> a to accountBalance(a, transactions, transfers) }
    }
    val total = remember(rows) { rows.sumOf { it.second } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("钱包结余", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                // 顶部：本月结余摘要（与统计页卡片一致）
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("本月结余", fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.weight(1f))
                    Text(balanceText(monthBalance), fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (monthBalance < 0) ExpenseRose else IncomeGreen)
                }
                rows.forEach { (acc, bal) ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(12.dp).background(Color(acc.color), CircleShape))
                        Text(acc.name, Modifier.padding(start = 10.dp).weight(1f),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold)
                        Text(balanceText(bal), fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (bal < 0) ExpenseRose
                            else MaterialTheme.colorScheme.onSurface)
                    }
                }
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("累计合计", Modifier.weight(1f), fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text(balanceText(total), fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        color = if (total < 0) ExpenseRose else IncomeGreen)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("知道了") }
        }
    )
}

@Composable
private fun StatCell(label: String, value: String, deltaText: String?, expense: Boolean) {
    Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp,
        color = MaterialTheme.colorScheme.onSurface)
    if (deltaText != null) {
        Text(deltaText, fontSize = 10.sp,
            color = if (expense) ExpenseRose else IncomeGreen)
    }
}

@Composable
private fun DonutChart(ranks: List<Rank>, selectedCatId: Long?) {
    val size = 132.dp
    val stroke = 22f
    Box(Modifier.size(size)) {
        Canvas(Modifier.fillMaxSize()) {
            val total = ranks.sumOf { it.amount }.toFloat()
            if (total <= 0f) return@Canvas
            if (selectedCatId == null) {
                var start = -90f
                ranks.take(6).forEach { r ->
                    val sweep = r.amount * 360f / total
                    drawArc(color = r.color, startAngle = start, sweepAngle = sweep - 1.5f,
                        useCenter = false, style = Stroke(width = stroke))
                    start += sweep
                }
            } else {
                var start = -90f
                ranks.take(6).forEach { r ->
                    val sweep = r.amount * 360f / total
                    drawArc(
                        color = if (r.categoryId == selectedCatId) r.color
                        else r.color.copy(alpha = 0.12f),
                        startAngle = start, sweepAngle = sweep - 1.5f,
                        useCenter = false, style = Stroke(width = stroke)
                    )
                    start += sweep
                }
            }
        }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            if (selectedCatId == null) {
                val top = ranks.firstOrNull()
                Text(if (top != null) "${Math.round(top.pct)}%" else "0%",
                    fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(top?.name ?: "暂无", fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val sel = ranks.firstOrNull { it.categoryId == selectedCatId }
                if (sel != null) {
                    Text("${Math.round(sel.pct)}%", fontWeight = FontWeight.Bold, fontSize = 20.sp,
                        color = sel.color)
                    Text(sel.name, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun RankRow(r: Rank, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp)
            .noRippleClickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(if (selected) 16.dp else 12.dp)
            .background(r.color, CircleShape))
        Text(r.name, Modifier.width(52.dp).padding(start = 8.dp), fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface)
        Box(
            Modifier.weight(1f).height(10.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(999.dp))
        ) {
            Box(
                Modifier.fillMaxWidth(r.pct.coerceIn(0f, 100f) / 100f)
                    .height(10.dp).background(r.color, RoundedCornerShape(999.dp))
            )
        }
        Text("¥${Money.format(r.amount)}", Modifier.width(78.dp), textAlign = TextAlign.End,
            fontSize = 12.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface)
        Text("${Math.round(r.pct)}%", Modifier.width(36.dp), textAlign = TextAlign.End,
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** 近 12 个月趋势（按类型） */
@Composable
private fun TrendBars(all: List<Transaction>, type: String, color: Color) {
    val now = YearMonth.now()
    val data = (11 downTo 0).map { idx ->
        val m = now.minusMonths(idx.toLong())
        val r = Days.monthRange(m)
        val sum = all.filter { it.type == type && it.occurredAtMillis in r[0] until r[1] }
            .sumOf { it.amountCents }
        m to sum
    }
    val max = data.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L
    Row(
        Modifier.fillMaxWidth().height(100.dp).padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { (m, sum) ->
            val frac = sum.toFloat() / max.toFloat()
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)) {
                Box(
                    Modifier.fillMaxWidth()
                        .height((72 * frac).dp.coerceAtLeast(2.dp))
                        .background(
                            if (m == now) color else color.copy(alpha = 0.35f),
                            RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                        )
                )
                Text("${m.monthValue}月", fontSize = 8.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
