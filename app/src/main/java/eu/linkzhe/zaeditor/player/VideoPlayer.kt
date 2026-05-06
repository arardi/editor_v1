package eu.linkzhe.zaeditor.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

object VideoPlayer {
    fun create(context: Context, uri: String): ExoPlayer {
        return ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
        }
    }
}
