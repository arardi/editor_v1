package eu.linkzhe.zaeditor.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.linkzhe.zaeditor.model.VideoProject
import eu.linkzhe.zaeditor.theme.CardDark
import eu.linkzhe.zaeditor.theme.DividerDark
import eu.linkzhe.zaeditor.theme.SurfaceSecondary
import eu.linkzhe.zaeditor.theme.TextPrimary
import eu.linkzhe.zaeditor.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecentProjectCard(project: VideoProject, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp)
            .clickable(enabled = project.uri != null, onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = CardDark,
        border = BorderStroke(1.dp, DividerDark)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceSecondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.VideoFile, contentDescription = null, modifier = Modifier.size(30.dp), tint = TextPrimary.copy(alpha = 0.88f))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    project.name,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.size(5.dp))
                Text(formatter.format(Date(project.createdAt)), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${formatDuration(project.durationMs)} • ${formatSize(project.fileSizeBytes)}",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Icon(Icons.Default.MoreVert, contentDescription = null, tint = TextSecondary)
        }
    }
}

@Composable
fun EmptyProjectsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(CardDark)
            .padding(vertical = 34.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceSecondary),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.VideoFile, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.size(14.dp))
        Text("No recent projects", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text("Import a video to start editing", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatDuration(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

private fun formatSize(bytes: Long): String = when {
    bytes <= 0 -> "Unknown size"
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "${bytes / (1024 * 1024)} MB"
}
