package com.accounts.app.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accounts.app.data.Transaction
import com.accounts.app.ui.AppViewModel
import com.accounts.app.ui.comps.GlassCard
import com.accounts.app.ui.theme.ExpenseRose
import com.accounts.app.ui.theme.IncomeGreen
import com.accounts.app.ui.theme.Ink
import com.accounts.app.ui.theme.Ink2
import com.accounts.app.util.Days
import com.accounts.app.util.Money
import java.time.YearMonth

private data class Rank(
    val name: String,
    val color: Color,
    val amount: Long,
    val pct: Float
)

@Composable
fun StatsScreen(vm: AppViewModel, onOpenSettings: () -> Unit) {
    val transactions by vm.transactions.collectAsState()
    val categories by vm.categories.collectAsState()

    var monthOffset by remember { mutableStateOf(0) }   // 0=本月 -1=上月 …
    var showAll by remember { mutableStateOf(false) }
    val month = remember(monthOffset) { YearMonth.now().plusMonths(monthOffset.toLong()) }

    val catMap = remember(categories) { categories.associateBy { it.id } }
    val (start, end) = remember(month) {
        val r = Days.monthRange(month); r[0] to r[1]
    }
    val monthTx = remember(transactions, start, end) {
        transactions.filter { it.occurredAtMillis in start until end }
    }
    val prevTx = remember(transactions, month) {
        val p = month.minusMonths(1)
        val r = Days.monthRange(p)
        transactions.filter { it.occurredAtMillis in r[0] until r[1] }
    }

    data class Totals(val expense: Long, val income: Long)
    fun totalsOf(list: List<Transaction>): Totals {
        var e = 0L; var i = 0L
        list.forEach { if (it.type == "expense") e += it.amountCents else i += it.amountCents }
        return Totals(e, i)
    }
    val cur = totalsOf(monthTx)
    val prev = totalsOf(prevTx)
    val balance = cur.income - cur.expense

    // 支出分类排行
    val totalExpense = cur.expense
    val ranks = remember(monthTx, catMap) {
        monthTx.filter { it.type == "expense" }
            .groupBy { it.categoryId }
            .map { (cid, list) ->
                val cat = catMap[cid]
                Rank(
                    name = cat?.name ?: "未分类",
                    color = if (cat != null) Color(cat.color) else Ink2,
                    amount = list.sumOf { it.amountCents },
                    pct = 0f
                )
            }
            .sortedByDescending { it.amount }
            .map { r -> r.copy(pct = if (totalExpense > 0) r.amount * 100f / totalExpense else 0f) }
    }
    val shown = if (showAll) ranks else ranks.take(4)
    val ringData = ranks.take(6)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        // 顶栏：标题 …… [设置(滑杆)] [月份]
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("统计", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Tune, contentDescription = "设置", tint = Ink2)
            }
            val label = if (monthOffset == 0) "本月" else
                "${month.monthValue}月"
            Text(
                label,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), RoundedCornerShape(999.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(999.dp))
                    .clickable {
                        monthOffset = if (monthOffset == -11) 0 else monthOffset - 1
                        showAll = false
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold, fontSize = 12.sp
            )
        }

        // 总览卡
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassCard(Modifier.weight(1f)) { StatCell("支出", "¥${Money.format(cur.expense)}", delta(cur.expense, prev.expense), expense = true) }
            GlassCard(Modifier.weight(1f)) { StatCell("收入", "¥${Money.format(cur.income)}", delta(cur.income, prev.income), expense = false) }
            GlassCard(Modifier.weight(1f)) { StatCell("结余", "¥${Money.format(balance)}", null, expense = false) }
        }

        // 环形图 + 图例
        Row(verticalAlignment = Alignment.CenterVertically) {
            DonutChart(ringData)
            Column(Modifier.padding(start = 8.dp)) {
                ringData.forEach { r ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Box(Modifier.size(9.dp).background(r.color, CircleShape))
                        Text(r.name, Modifier.padding(start = 6.dp), fontSize = 12.sp, color = Ink)
                        Text("${Math.round(r.pct)}%", Modifier.weight(1f), textAlign = TextAlign.End,
                            fontSize = 11.sp, color = Ink2)
                    }
                }
            }
        }

        Text("分类排行（支出）", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))

        GlassCard(Modifier.fillMaxWidth()) {
            shown.forEach { r ->
                RankRow(r)
            }
            if (ranks.size > 4) {
                Text(
                    if (showAll) "收起 ▴" else "展开全部 ${ranks.size} 个分类 ▾",
                    Modifier.fillMaxWidth().clickable { showAll = !showAll }
                        .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold, fontSize = 12.sp
                )
            }
        }

        Text("近 6 个月支出趋势", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
        TrendBars(transactions)

        Spacer(Modifier.height(24.dp))
    }
}

private fun delta(cur: Long, prev: Long): String? {
    if (prev <= 0) return null
    val pct = (cur - prev) * 100 / prev
    return if (pct >= 0) "▲ $pct%" else "▼ ${-pct}%"
}

@Composable
private fun StatCell(label: String, value: String, deltaText: String?, expense: Boolean) {
    Text(label, fontSize = 10.sp, color = Ink2)
    Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink)
    if (deltaText != null) {
        Text(deltaText, fontSize = 10.sp,
            color = if (expense) ExpenseRose else IncomeGreen)
    }
}

@Composable
private fun DonutChart(data: List<Rank>) {
    val total = data.sumOf { it.amount }.toFloat()
    val size = 118.dp
    Box(Modifier.size(size)) {
        Canvas(Modifier.fillMaxSize()) {
            if (total <= 0f) return@Canvas
            var start = -90f
            data.forEach { r ->
                val sweep = r.amount * 360f / total
                drawArc(color = r.color, startAngle = start, sweepAngle = sweep - 1f, useCenter = false,
                    style = Stroke(width = 18f))
                start += sweep
            }
        }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            val top = data.firstOrNull()
            Text(if (top != null) "${Math.round(top.pct)}%" else "0%",
                fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink)
            Text(top?.name ?: "暂无", fontSize = 10.sp, color = Ink2)
        }
    }
}

@Composable
private fun RankRow(r: Rank) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(12.dp).background(r.color, CircleShape))
        Text(r.name, Modifier.width(52.dp).padding(start = 8.dp), fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold, color = Ink)
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
            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text("${Math.round(r.pct)}%", Modifier.width(36.dp), textAlign = TextAlign.End,
            fontSize = 11.sp, color = Ink2)
    }
}

@Composable
private fun TrendBars(all: List<Transaction>) {
    val now = YearMonth.now()
    val data = (5 downTo 0).map { idx ->
        val m = now.minusMonths(idx.toLong())
        val r = Days.monthRange(m)
        val sum = all.filter { it.type == "expense" && it.occurredAtMillis in r[0] until r[1] }
            .sumOf { it.amountCents }
        m to sum
    }
    val max = data.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L
    Row(
        Modifier.fillMaxWidth().height(110.dp).padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { (m, sum) ->
            val frac = sum.toFloat() / max.toFloat()
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)) {
                Box(
                    Modifier.fillMaxWidth()
                        .height((74 * frac).dp.coerceAtLeast(2.dp))
                        .background(
                            if (m == now) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                        )
                )
                Text("${m.monthValue}月", fontSize = 10.sp, color = Ink2)
            }
        }
    }
}
