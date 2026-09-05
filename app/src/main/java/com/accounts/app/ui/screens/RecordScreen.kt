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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accounts.app.data.Category
import com.accounts.app.ui.AppViewModel
import com.accounts.app.ui.comps.CtaButton
import com.accounts.app.ui.comps.GlassCard
import com.accounts.app.ui.comps.Segment
import com.accounts.app.ui.theme.Ink
import com.accounts.app.ui.theme.Ink2
import com.accounts.app.ui.theme.chipColor
import com.accounts.app.util.Days
import com.accounts.app.util.Money

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

    var kind by remember { mutableStateOf("expense") }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedCatId by remember { mutableStateOf<Long?>(null) }
    var accountId by remember { mutableStateOf(0L) }
    var occurredAt by remember { mutableStateOf(Days.nowMillis()) }
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(accounts) {
        if (accountId == 0L && accounts.isNotEmpty()) accountId = accounts.first().id
    }

    val cats = homeCategories(categories, kind)
    val recent = remember(transactions) {
        transactions.map { it.amountCents }.distinct().take(4)
    }
    // 示例模板：早餐 / 地铁（后续迭代支持用户自定义与长按编辑）
    val templates = remember(categories) {
        listOf(
            Triple("早餐", 800L, "餐饮"),
            Triple("地铁", 600L, "交通")
        ).mapNotNull { (label, cents, catName) ->
            categories.firstOrNull { it.kind == "expense" && it.name == catName }
                ?.let { Template(label, cents, it) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(6.dp))
        Segment(options = listOf("支出", "收入"),
            selectedIndex = if (kind == "expense") 0 else 1,
            onSelect = { kind = if (it == 0) "expense" else "income" })

        // 金额
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("¥", fontSize = 24.sp, color = Ink2, fontWeight = FontWeight.Medium)
            BasicTextField(
                value = amountText,
                onValueChange = { s ->
                    val f = s.filter { it.isDigit() || it == '.' }
                    if (f.count { it == '.' } <= 1 && f.length <= 12) amountText = f
                },
                textStyle = TextStyle(
                    color = Ink, fontSize = 44.sp, fontWeight = FontWeight.Bold
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.padding(start = 6.dp).width(200.dp)
            )
        }

        // 备注
        BasicTextField(
            value = note,
            onValueChange = { note = it },
            textStyle = TextStyle(color = Ink2, fontSize = 13.sp),
            singleLine = true,
            decorationBox = { inner ->
                if (note.isEmpty()) Text("备注（可选）", color = Ink2.copy(alpha = 0.7f), fontSize = 13.sp)
                inner()
            },
            modifier = Modifier.fillMaxWidth()
        )

        // 分类色片（无图标，见晨雾规范）
        Text("", Modifier.height(2.dp))
        cats.chunked(4).forEach { rowCats ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowCats.forEach { cat ->
                    CategoryTile(
                        cat = cat,
                        selected = selectedCatId == cat.id,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedCatId = cat.id
                            val cents = Money.parse(amountText)
                            if (cents > 0) {
                                vm.addRecord(kind, cents, cat.id, accountId, occurredAt, note)
                                amountText = ""
                            }
                        }
                    )
                }
                repeat(4 - rowCats.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        // 钱包 / 时间（无图标）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box {
                Text("钱包：${accountName(accounts, accountId)} ▾",
                    fontSize = 13.sp, color = Ink)
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    accounts.forEach { a ->
                        DropdownMenuItem(
                            text = { Text(a.name) },
                            onClick = { accountId = a.id; menuOpen = false }
                        )
                    }
                }
                Box(Modifier.matchParentSize().clickable { menuOpen = true })
            }
            Text(Days.todayTimeLabel(occurredAt), fontSize = 13.sp, color = Ink2)
        }

        SectionLabel2("最近金额")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            recent.forEach { cents ->
                Text(Money.formatPlain(cents),
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            RoundedCornerShape(999.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(999.dp))
                        .clickable { amountText = Money.formatPlain(cents) }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    fontSize = 12.sp, color = Ink)
            }
        }

        SectionLabel2("我的模板")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            templates.forEach { t ->
                Text("⭐ ${t.label} ¥${Money.formatPlain(t.cents)}",
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            RoundedCornerShape(999.dp))
                        .clickable {
                            vm.addRecord(kind, t.cents, t.category.id, accountId, occurredAt, "")
                            amountText = ""
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    fontSize = 12.sp, color = Ink)
            }
        }

        val cents = Money.parse(amountText)
        CtaButton(text = "保 存", enabled = cents > 0 && selectedCatId != null) {
            val cid = selectedCatId
            if (cid != null && cents > 0) {
                vm.addRecord(kind, cents, cid, accountId, occurredAt, note)
                amountText = ""
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

private data class Template(val label: String, val cents: Long, val category: Category)

private fun accountName(accounts: List<com.accounts.app.data.Account>, id: Long): String =
    accounts.firstOrNull { it.id == id }?.name ?: "—"

@Composable
private fun CategoryTile(cat: Category, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Column(
        modifier = modifier
            .background(chipColor(cat.color), RoundedCornerShape(22.dp))
            .border(if (selected) 2.5.dp else 0.dp, borderColor, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(cat.name, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun SectionLabel2(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp))
}
