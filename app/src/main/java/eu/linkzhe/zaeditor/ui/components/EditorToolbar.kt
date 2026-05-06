package eu.linkzhe.zaeditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.linkzhe.zaeditor.theme.CardDark
import eu.linkzhe.zaeditor.theme.DangerRed
import eu.linkzhe.zaeditor.theme.PrimaryBlue
import eu.linkzhe.zaeditor.theme.TextSecondary

data class EditorTool(val label: String, val icon: ImageVector, val danger: Boolean = false)

val EditorTools = listOf(
    EditorTool("Trim", Icons.Default.ContentCut),
    EditorTool("Split", Icons.Default.Splitscreen),
    EditorTool("Audio", Icons.Default.MusicNote),
    EditorTool("Text", Icons.Default.TextFields),
    EditorTool("Filter", Icons.Default.FilterAlt),
    EditorTool("Speed", Icons.Default.GraphicEq),
    EditorTool("Crop", Icons.Default.Crop),
    EditorTool("Delete", Icons.Default.Delete, danger = true)
)

@Composable
fun EditorToolbar(
    selectedTool: EditorTool,
    onToolSelected: (EditorTool) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 82.dp, max = 96.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(CardDark.copy(alpha = 0.96f))
            .horizontalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EditorTools.forEach { tool ->
            val selected = tool == selectedTool
            val color = when {
                tool.danger -> DangerRed
                selected -> PrimaryBlue
                else -> TextSecondary
            }
            Column(
                modifier = Modifier
                    .size(width = 66.dp, height = 60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) PrimaryBlue.copy(alpha = 0.12f) else Color.Transparent)
                    .clickable { onToolSelected(tool) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(tool.icon, contentDescription = tool.label, tint = color, modifier = Modifier.size(23.dp))
                Text(
                    text = tool.label,
                    color = color,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}
