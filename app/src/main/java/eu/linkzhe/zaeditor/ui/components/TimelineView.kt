package eu.linkzhe.zaeditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import eu.linkzhe.zaeditor.theme.CardDark
import eu.linkzhe.zaeditor.theme.DividerDark
import eu.linkzhe.zaeditor.theme.PrimaryBlue
import eu.linkzhe.zaeditor.theme.SurfaceSecondary
import eu.linkzhe.zaeditor.theme.TextPrimary
import eu.linkzhe.zaeditor.theme.TextSecondary

@Composable
fun TimelineView(durationMs: Long, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Timeline", color = TextPrimary)
            Spacer(Modifier.weight(1f))
            Text(formatTime(durationMs.coerceAtLeast(0L)), color = TextSecondary)
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(116.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(CardDark)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(4) { index ->
                    Text("${index * 5}s", color = TextSecondary)
                    Spacer(Modifier.width(44.dp))
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(14) { i ->
                    Box(
                        modifier = Modifier
                            .size(width = 68.dp, height = 54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        SurfaceSecondary,
                                        if (i % 2 == 0) DividerDark else SurfaceSecondary.copy(alpha = 0.72f)
                                    )
                                )
                            )
                    )
                }
            }
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(92.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(PrimaryBlue)
                    .align(Alignment.Center)
            )
        }
    }
}
