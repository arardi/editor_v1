package eu.linkzhe.zaeditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.linkzhe.zaeditor.theme.CardDark
import eu.linkzhe.zaeditor.theme.DividerDark
import eu.linkzhe.zaeditor.theme.PrimaryBlue
import eu.linkzhe.zaeditor.theme.SurfaceSecondary
import eu.linkzhe.zaeditor.theme.TextPrimary
import eu.linkzhe.zaeditor.theme.TextSecondary
import eu.linkzhe.zaeditor.theme.TimelineTrack
import kotlin.math.ceil
import kotlin.math.max

@Composable
fun TimelineView(
    durationMs: Long,
    currentPositionMs: Long,
    modifier: Modifier = Modifier,
    onSeekRequested: (Long) -> Unit = {}
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    val frameCount = remember(safeDuration) {
        max(8, ceil(safeDuration / 500.0).toInt()).coerceAtMost(36)
    }
    val tickCount = remember(safeDuration) {
        max(4, ceil(safeDuration / 1000.0).toInt() + 1).coerceAtMost(8)
    }
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(CardDark)
            .pointerInput(safeDuration) {
                detectTapGestures { offset ->
                    val ratio = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onSeekRequested((safeDuration * ratio).toLong())
                }
            }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VideoFile, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Video", color = TextPrimary, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Text(formatTime(currentPositionMs), color = PrimaryBlue, fontSize = 12.sp)
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                repeat(tickCount) { tick ->
                    val position = (safeDuration * tick / (tickCount - 1)).coerceAtMost(safeDuration)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(if (tick % 2 == 0) 10.dp else 6.dp)
                                .background(DividerDark)
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(formatRulerTime(position), color = TextSecondary, fontSize = 10.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(TimelineTrack.copy(alpha = 0.35f))
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(frameCount) { index ->
                    FrameBlock(index = index)
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 28.dp)
                .size(14.dp)
                .clip(CircleShape)
                .background(PrimaryBlue)
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 10.dp)
                .width(2.dp)
                .height(102.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(PrimaryBlue)
        )
    }
}

@Composable
private fun FrameBlock(index: Int) {
    Box(
        modifier = Modifier
            .size(width = 42.dp, height = 54.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SurfaceSecondary.copy(alpha = if (index % 3 == 0) 0.92f else 0.78f),
                        TimelineTrack.copy(alpha = if (index % 2 == 0) 0.92f else 0.72f)
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.Center)
                .background(DividerDark.copy(alpha = 0.55f))
        )
    }
}

private fun formatRulerTime(ms: Long): String {
    val seconds = (ms / 1000).coerceAtLeast(0)
    return "${seconds}s"
}
