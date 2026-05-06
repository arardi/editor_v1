package eu.linkzhe.zaeditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import eu.linkzhe.zaeditor.model.VideoProject
import eu.linkzhe.zaeditor.player.VideoPlayer
import eu.linkzhe.zaeditor.theme.AppBackground
import eu.linkzhe.zaeditor.ui.components.EditorPreview
import eu.linkzhe.zaeditor.ui.components.EditorToolbar
import eu.linkzhe.zaeditor.ui.components.PlaybackControls
import eu.linkzhe.zaeditor.ui.components.TimelineView
import eu.linkzhe.zaeditor.ui.components.ZaTopBar
import kotlinx.coroutines.delay

@Composable
fun EditorScreen(project: VideoProject, onBack: () -> Unit, onExport: () -> Unit) {
    val context = LocalContext.current
    val player = remember(project.path) { VideoPlayer.create(context, project.path) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(project.durationMs.takeIf { it > 0 } ?: 1L) }
    var isPlaying by remember { mutableStateOf(false) }
    var aspectRatio by remember { mutableFloatStateOf(9f / 16f) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(value: Boolean) {
                isPlaying = value
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.height > 0) {
                    aspectRatio = videoSize.width.toFloat() / videoSize.height.toFloat()
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player) {
        while (true) {
            position = player.currentPosition.coerceAtLeast(0L)
            duration = if (player.duration > 0) player.duration else duration.coerceAtLeast(1L)
            isPlaying = player.isPlaying
            delay(250)
        }
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = { ZaTopBar(projectName = project.name, onBack = onBack, onExport = onExport) },
        bottomBar = { EditorToolbar(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EditorPreview(player = player, aspectRatio = aspectRatio)
            PlaybackControls(
                isPlaying = isPlaying,
                positionMs = position,
                durationMs = duration,
                onTogglePlay = { if (player.isPlaying) player.pause() else player.play() },
                onSeek = { player.seekTo(it) }
            )
            TimelineView(durationMs = duration)
            Spacer(Modifier.height(6.dp))
        }
    }
}
