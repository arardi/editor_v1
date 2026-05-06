package eu.linkzhe.zaeditor.export

import android.content.Context
import android.net.Uri
import eu.linkzhe.zaeditor.model.EditorState
import eu.linkzhe.zaeditor.model.VideoProject
import java.io.File

class VideoExportManager(private val context: Context) {
    fun exportProject(project: VideoProject, editorState: EditorState, outputFile: File) {
        project.uri?.let { export(it, editorState, outputFile) }
    }

    fun export(inputUri: Uri, editorState: EditorState, outputFile: File) {
        // TODO: Build the final MediaCodec/FFmpeg rendering pipeline.
        // TODO: Apply mirrorHorizontal transform.
        // TODO: Render backgroundColor as the video canvas behind the source video.
        // TODO: Composite overlayColorWithAlpha at overlayX/overlayY with overlayWidth/overlayHeight.
        // TODO: Apply trim and split ranges once timeline editing data is persisted.
        // The parameters are intentionally retained for the upcoming export implementation.
        inputUri.toString()
        editorState.overlayColorWithAlpha
        outputFile.absolutePath
    }
}
