package com.accounts.app.ui.comps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.accounts.app.ui.theme.MistCta

/** 磨砂玻璃卡片 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val c = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .background(c.surface.copy(alpha = 0.66f), shape)
            .border(1.dp, c.surface.copy(alpha = 0.9f), shape)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        content = content
    )
}

/** 无涟漪点击：去掉按下时的灰色方块/矩形高亮 */
@Composable
fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    return this.clickable(interactionSource = interaction, indication = null, onClick = onClick)
}

/** 渐变大按钮（胶囊；无灰色方块反馈） */
@Composable
fun CtaButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MistCta, RoundedCornerShape(999.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = MaterialTheme.typography.titleMedium.fontSize)
    }
}

/** 支出/收入 段选择（更方正、贴近预览：容器圆角 14、选中块圆角 8） */
@Composable
fun Segment(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(c.primaryContainer.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(3.dp)
    ) {
        options.forEachIndexed { i, label ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (i == selectedIndex) {
                            Modifier
                                .background(MistCta, RoundedCornerShape(8.dp))
                                .border(0.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        } else {
                            Modifier
                        }
                    )
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = { onSelect(i) }
                    )
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label,
                    color = if (i == selectedIndex) Color.White else c.onSurfaceVariant,
                    fontWeight = if (i == selectedIndex) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

/** 区块小标题 */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier.padding(vertical = 6.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** 细分割线（玻璃卡片内部行分隔） */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}

/** 页脚说明小字 */
@Composable
fun Footnote(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}
