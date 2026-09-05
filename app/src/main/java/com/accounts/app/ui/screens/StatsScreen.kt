package com.accounts.app.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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

    var monthOffset by remember { mutableStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }
    var showAll by remember { mutableStateOf(false) }
    var selectedCatId by remember { mutableStateOf<Long?>(null) }

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
                        monthOffset = 0; showAll = false; selectedCatId = null; menuOpen = false
                    })
                    (1..11).forEach { back ->
                        val m = YearMonth.now().minusMonths(back.toLong())
                        DropdownMenuItem(
                            text = { Text("${m.year}年${m.monthValue}月") },
                            onClick = {
                                monthOffset = -back
                                showAll = false
                                selectedCatId = null
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
            GlassCard(Modifier.weight(1f)) {
                StatCell("支出", "¥${Money.format(cur.expense)}",
                    delta(cur.expense, prev.expense), expense = true)
            }
            GlassCard(Modifier.weight(1f)) {
                StatCell("收入", "¥${Money.format(cur.income)}",
                    delta(cur.income, prev.income), expense = false)
            }
            GlassCard(Modifier.weight(1f)) {
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
        Text("分类排行（支出）· 点击查看占比",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
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

        Spacer(Modifier.height(18.dp))
        Text("近 12 个月收入趋势", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        TrendBars(transactions, type = "income", color = IncomeGreen)

        Spacer(Modifier.height(28.dp))
    }
}

private fun delta(cur: Long, prev: Long): String? {
    if (prev <= 0) return null
    val pct = (cur - prev) * 100 / prev
    return if (pct >= 0) "▲ $pct%" else "▼ ${-pct}%"
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
