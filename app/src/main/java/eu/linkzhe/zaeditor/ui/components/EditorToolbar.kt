package eu.linkzhe.zaeditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.linkzhe.zaeditor.theme.CardDark
import eu.linkzhe.zaeditor.theme.DangerRed
import eu.linkzhe.zaeditor.theme.PrimaryBlue
import eu.linkzhe.zaeditor.theme.SurfaceSecondary
import eu.linkzhe.zaeditor.theme.TextSecondary

private data class EditorTool(val label: String, val icon: ImageVector, val danger: Boolean = false)

@Composable
fun EditorToolbar(modifier: Modifier = Modifier, activeTool: String = "Trim") {
    val tools = listOf(
        EditorTool("Trim", Icons.Default.ContentCut),
        EditorTool("Split", Icons.Default.Splitscreen),
        EditorTool("Audio", Icons.Default.MusicNote),
        EditorTool("Text", Icons.Default.TextFields),
        EditorTool("Filter", Icons.Default.FilterAlt),
        EditorTool("Speed", Icons.Default.GraphicEq),
        EditorTool("Crop", Icons.Default.Crop),
        EditorTool("Delete", Icons.Default.Delete, danger = true)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CardDark)
            .horizontalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        tools.forEach { tool ->
            val active = tool.label == activeTool
            val color = when {
                tool.danger -> DangerRed
                active -> PrimaryBlue
                else -> TextSecondary
            }
            Column(
                modifier = Modifier
                    .size(width = 68.dp, height = 70.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (active) SurfaceSecondary else androidx.compose.ui.graphics.Color.Transparent),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(tool.icon, contentDescription = tool.label, tint = color, modifier = Modifier.size(25.dp))
                Text(tool.label, color = color, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium)
            }
        }
    }
}
