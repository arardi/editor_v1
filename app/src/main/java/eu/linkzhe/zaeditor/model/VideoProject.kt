package eu.linkzhe.zaeditor.model

import android.net.Uri

data class VideoProject(
    val id: Long,
    val name: String,
    val uri: Uri?,
    val path: String,
    val createdAt: Long,
    val durationMs: Long,
    val fileSizeBytes: Long
)
