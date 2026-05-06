package eu.linkzhe.zaeditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import eu.linkzhe.zaeditor.model.VideoProject
import eu.linkzhe.zaeditor.player.VideoPlayer
import kotlinx.coroutines.delay

@Composable
fun EditorScreen(project: VideoProject, onBack: () -> Unit, onExport: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember(project.path) { VideoPlayer.create(context, project.path) }
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(1L) }

    LaunchedEffect(player) {
        while (true) {
            pos = player.currentPosition
            dur = if (player.duration > 0) player.duration else 1L
            delay(250)
        }
    }

    DisposableEffect(Unit) { onDispose { player.release() } }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0D0F14)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text(project.name, modifier = Modifier.weight(1f))
            TextButton(onClick = onExport) { Text("Export") }
        }
        Box(modifier = Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(20.dp)).background(Color.Black)) {
            AndroidView(factory = { PlayerView(it).apply { this.player = player; useController = false } })
        }
        Button(onClick = { if (player.isPlaying) player.pause() else player.play() }) { Text(if (player.isPlaying) "Pause" else "Play") }
        Text("${fmt(pos)} / ${fmt(dur)}")
        Slider(value = pos.toFloat(), onValueChange = { player.seekTo(it.toLong()) }, valueRange = 0f..dur.toFloat())
        Spacer(Modifier.height(8.dp))
        Text("Timeline", style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(12) { Box(Modifier.size(64.dp, 44.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF2A2F3D))) }
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(Color.Gray))
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf("Trim","Split","Audio","Text","Filter","Speed","Crop","Delete").forEach { Text(it) }
        }
    }
}

private fun fmt(ms: Long): String {
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    return "%02d:%02d".format(m, s)
}
