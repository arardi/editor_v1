package eu.linkzhe.zaeditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.linkzhe.zaeditor.model.VideoProject
import eu.linkzhe.zaeditor.theme.AppBackground
import eu.linkzhe.zaeditor.theme.TextPrimary
import eu.linkzhe.zaeditor.theme.TextSecondary
import eu.linkzhe.zaeditor.ui.components.EmptyProjectsState
import eu.linkzhe.zaeditor.ui.components.ImportVideoBottomSheet
import eu.linkzhe.zaeditor.ui.components.RecentProjectCard
import eu.linkzhe.zaeditor.ui.components.StartCreatingCard

@Composable
fun HomeScreen(
    projects: List<VideoProject>,
    onImportVideo: () -> Unit,
    onOpenProject: (VideoProject) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    val realProjects = projects.filter { it.uri != null }

    Scaffold(containerColor = AppBackground) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .statusBarsPadding()
                .padding(innerPadding)
                .padding(horizontal = 22.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Spacer(Modifier.height(6.dp))
            StartCreatingCard(onClick = { showSheet = true })
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Recent Projects",
                    modifier = Modifier.weight(1f),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Icon(Icons.Default.Sort, contentDescription = null, tint = TextSecondary)
            }
            if (realProjects.isEmpty()) {
                EmptyProjectsState()
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    realProjects.forEach { project ->
                        RecentProjectCard(project = project, onClick = { onOpenProject(project) })
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }

    if (showSheet) {
        ImportVideoBottomSheet(
            onDismiss = { showSheet = false },
            onImportVideo = {
                showSheet = false
                onImportVideo()
            }
        )
    }
}
