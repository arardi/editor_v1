package eu.linkzhe.zaeditor

import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eu.linkzhe.zaeditor.data.ProjectStore
import eu.linkzhe.zaeditor.model.VideoProject
import eu.linkzhe.zaeditor.theme.ZaEditorTheme
import eu.linkzhe.zaeditor.ui.EditorScreen
import eu.linkzhe.zaeditor.ui.HomeScreen

class MainActivity : ComponentActivity() {
    private lateinit var store: ProjectStore
    private val projects = mutableStateListOf<VideoProject>()
    private var currentProject by mutableStateOf<VideoProject?>(null)
    private var showExportDialog by mutableStateOf(false)

    private val photoPicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) addProjectFromUri(uri)
    }
    private val docPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) addProjectFromUri(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        store = ProjectStore(this)
        projects.addAll(store.getProjects())

        setContent {
            ZaEditorTheme {
                val project = currentProject
                if (project == null) {
                    HomeScreen(projects = projects, onImportVideo = { pickVideo() }, onOpenProject = { currentProject = it })
                } else {
                    EditorScreen(project = project, onBack = { currentProject = null }, onExport = { showExportDialog = true })
                }
                if (showExportDialog) {
                    AlertDialog(
                        onDismissRequest = { showExportDialog = false },
                        confirmButton = { TextButton(onClick = { showExportDialog = false }) { Text("OK") } },
                        title = { Text("Export") },
                        text = { Text("Export feature coming soon") }
                    )
                }
            }
        }
    }

    private fun pickVideo() {
        if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(this)) {
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
        } else {
            docPicker.launch(arrayOf("video/*"))
        }
    }

    private fun addProjectFromUri(uri: Uri) {
        contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val name = queryDisplayName(uri) ?: "Za"
        val size = querySize(uri)
        val project = VideoProject(
            id = System.currentTimeMillis(),
            name = name.substringBeforeLast('.'),
            uri = uri,
            path = uri.toString(),
            createdAt = System.currentTimeMillis(),
            durationMs = 0,
            fileSizeBytes = size
        )
        store.addProject(project)
        projects.clear(); projects.addAll(store.getProjects())
        currentProject = project
    }

    private fun queryDisplayName(uri: Uri): String? = queryMetadata(uri) { c ->
        val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0) c.getString(idx) else null
    }

    private fun querySize(uri: Uri): Long = queryMetadata(uri) { c ->
        val idx = c.getColumnIndex(OpenableColumns.SIZE)
        if (idx >= 0) c.getLong(idx) else 0L
    } ?: 0L

    private fun <T> queryMetadata(uri: Uri, mapper: (Cursor) -> T?): T? {
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) return mapper(c)
        }
        return null
    }
}
