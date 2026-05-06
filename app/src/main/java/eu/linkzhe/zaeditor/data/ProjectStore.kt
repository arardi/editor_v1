package eu.linkzhe.zaeditor.data

import android.content.Context
import android.net.Uri
import eu.linkzhe.zaeditor.model.VideoProject

class ProjectStore(context: Context) {
    private val prefs = context.getSharedPreferences("za_editor_projects", Context.MODE_PRIVATE)

    fun getProjects(): List<VideoProject> {
        val raw = prefs.getString("projects", null)
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split("||").mapNotNull { decodeProject(it) }
    }

    fun addProject(project: VideoProject) {
        val projects = getProjects().filterNot { it.id == project.id && it.uri == null }.toMutableList()
        projects.add(0, project)
        val cleaned = projects.take(20)
        prefs.edit().putString("projects", cleaned.joinToString("||") { encodeProject(it) }).apply()
    }


    private fun encodeProject(project: VideoProject): String {
        return listOf(
            project.id,
            project.name.replace("|", " "),
            project.uri?.toString() ?: "",
            project.path.replace("|", " "),
            project.createdAt,
            project.durationMs,
            project.fileSizeBytes
        ).joinToString("|")
    }

    private fun decodeProject(value: String): VideoProject? {
        val p = value.split("|")
        if (p.size < 7) return null
        return VideoProject(
            id = p[0].toLongOrNull() ?: return null,
            name = p[1],
            uri = p[2].takeIf { it.isNotBlank() }?.let(Uri::parse),
            path = p[3],
            createdAt = p[4].toLongOrNull() ?: 0L,
            durationMs = p[5].toLongOrNull() ?: 0L,
            fileSizeBytes = p[6].toLongOrNull() ?: 0L
        )
    }
}
