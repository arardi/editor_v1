package eu.linkzhe.zaeditor

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import eu.linkzhe.zaeditor.data.ProjectStore
import eu.linkzhe.zaeditor.model.VideoProject
import eu.linkzhe.zaeditor.player.VideoPlayer
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    private lateinit var store: ProjectStore
    private lateinit var container: FrameLayout
    private val projects = mutableListOf<VideoProject>()
    private var currentProject: VideoProject? = null
    private var player: ExoPlayer? = null
    private val uiHandler = Handler(Looper.getMainLooper())
    private var selectedToolIndex = 0

    private val progressUpdater = object : Runnable {
        override fun run() {
            updatePlaybackState()
            uiHandler.postDelayed(this, 250)
        }
    }

    private val photoPicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) addProjectFromUri(uri)
    }

    private val docPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) addProjectFromUri(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = ContextCompat.getColor(this, R.color.za_background)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.za_background)
        setContentView(R.layout.activity_main)
        container = findViewById(R.id.main_container)
        store = ProjectStore(this)
        projects.addAll(store.getProjects())
        showHome()
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        uiHandler.removeCallbacks(progressUpdater)
        releasePlayer()
        super.onDestroy()
    }

    private fun showHome() {
        releasePlayer()
        currentProject = null
        val view = layoutInflater.inflate(R.layout.screen_home, container, false)
        container.removeAllViews()
        container.addView(view)
        view.findViewById<View>(R.id.import_card).setOnClickListener { showImportSheet() }
        bindRecentProjects(view)
    }

    private fun bindRecentProjects(root: View) {
        val list = root.findViewById<LinearLayout>(R.id.recent_projects_container)
        val empty = root.findViewById<TextView>(R.id.empty_projects)
        val realProjects = projects.filter { it.uri != null }
        list.removeAllViews()
        empty.visibility = if (realProjects.isEmpty()) View.VISIBLE else View.GONE
        realProjects.forEach { project ->
            val item = layoutInflater.inflate(R.layout.item_recent_project, list, false)
            item.findViewById<TextView>(R.id.project_name).text = project.name
            item.findViewById<TextView>(R.id.project_meta).text = projectMeta(project)
            item.setOnClickListener { showEditor(project) }
            list.addView(item)
        }
    }

    private fun showEditor(project: VideoProject) {
        currentProject = project
        val view = layoutInflater.inflate(R.layout.screen_editor, container, false)
        container.removeAllViews()
        container.addView(view)

        view.findViewById<TextView>(R.id.editor_title).text = project.name
        view.findViewById<ImageButton>(R.id.back_button).setOnClickListener { showHome() }
        view.findViewById<ImageButton>(R.id.export_button).setOnClickListener { showExportDialog() }

        val playerView = view.findViewById<PlayerView>(R.id.player_view)
        val playPause = view.findViewById<ImageButton>(R.id.play_pause_button)
        val timeline = view.findViewById<TimelineView>(R.id.timeline_view)
        val createdPlayer = VideoPlayer.create(this, project.path)
        player = createdPlayer
        playerView.player = createdPlayer
        playPause.setOnClickListener {
            if (createdPlayer.isPlaying) createdPlayer.pause() else createdPlayer.play()
            updatePlaybackState()
        }
        timeline.setOnSeekRequested { createdPlayer.seekTo(it) }
        createdPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) = updatePlaybackState()
            override fun onIsPlayingChanged(isPlaying: Boolean) = updatePlaybackState()
        })
        bindToolbar(view.findViewById(R.id.editor_toolbar))
        uiHandler.removeCallbacks(progressUpdater)
        uiHandler.post(progressUpdater)
    }

    private fun bindToolbar(toolbar: LinearLayout) {
        val tools = listOf(
            EditorTool("Trim", R.drawable.ic_cut),
            EditorTool("Speed", R.drawable.ic_speed),
            EditorTool("Text", R.drawable.ic_text),
            EditorTool("Filter", R.drawable.ic_filter)
        )
        toolbar.removeAllViews()
        tools.forEachIndexed { index, tool ->
            val item = LayoutInflater.from(this).inflate(R.layout.item_editor_tool, toolbar, false)
            item.findViewById<ImageView>(R.id.tool_icon).setImageResource(tool.iconRes)
            item.findViewById<TextView>(R.id.tool_label).text = tool.label
            item.setOnClickListener {
                selectedToolIndex = index
                updateToolSelection(toolbar)
            }
            toolbar.addView(item)
        }
        updateToolSelection(toolbar)
    }

    private fun updateToolSelection(toolbar: LinearLayout) {
        val selectedColor = ContextCompat.getColor(this, R.color.za_primary)
        val normalColor = ContextCompat.getColor(this, R.color.za_text_secondary)
        for (index in 0 until toolbar.childCount) {
            val item = toolbar.getChildAt(index)
            val color = if (index == selectedToolIndex) selectedColor else normalColor
            item.findViewById<ImageView>(R.id.tool_icon).setColorFilter(color)
            item.findViewById<TextView>(R.id.tool_label).setTextColor(color)
            item.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    private fun updatePlaybackState() {
        val root = container.getChildAt(0) ?: return
        val activePlayer = player ?: return
        val duration = if (activePlayer.duration > 0) activePlayer.duration else currentProject?.durationMs ?: 1L
        val position = activePlayer.currentPosition.coerceAtLeast(0L)
        root.findViewById<TimelineView>(R.id.timeline_view)?.setTimeline(duration, position)
        root.findViewById<TextView>(R.id.time_label)?.text = "${TimelineView.formatTime(position)} / ${TimelineView.formatTime(duration)}"
        root.findViewById<ImageButton>(R.id.play_pause_button)?.setImageResource(
            if (activePlayer.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    private fun showImportSheet() {
        val dialog = Dialog(this)
        val sheet = layoutInflater.inflate(R.layout.view_import_video_sheet, null, false)
        sheet.findViewById<View>(R.id.import_video_action).setOnClickListener {
            dialog.dismiss()
            pickVideo()
        }
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(sheet)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.show()
        dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    private fun showExportDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.export)
            .setMessage(R.string.export_coming_soon)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun pickVideo() {
        if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(this)) {
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
        } else {
            docPicker.launch(arrayOf("video/*"))
        }
    }

    private fun addProjectFromUri(uri: Uri) {
        takeReadPermission(uri)
        val name = queryDisplayName(uri) ?: "Za video"
        val size = querySize(uri)
        val project = VideoProject(
            id = System.currentTimeMillis(),
            name = name.substringBeforeLast('.'),
            uri = uri,
            path = uri.toString(),
            createdAt = System.currentTimeMillis(),
            durationMs = 1L,
            fileSizeBytes = size
        )
        store.addProject(project)
        projects.clear()
        projects.addAll(store.getProjects())
        showEditor(project)
    }

    private fun takeReadPermission(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // Photo Picker grants temporary read access; OpenDocument grants persistable access.
        }
    }

    private fun releasePlayer() {
        uiHandler.removeCallbacks(progressUpdater)
        player?.release()
        player = null
    }

    private fun queryDisplayName(uri: Uri): String? = queryMetadata(uri) { cursor ->
        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0) cursor.getString(idx) else null
    }

    private fun querySize(uri: Uri): Long = queryMetadata(uri) { cursor ->
        val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (idx >= 0) cursor.getLong(idx) else 0L
    } ?: 0L

    private fun <T> queryMetadata(uri: Uri, mapper: (Cursor) -> T?): T? {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) return mapper(cursor)
        }
        return null
    }

    private fun projectMeta(project: VideoProject): String {
        val date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(project.createdAt))
        val size = if (project.fileSizeBytes > 0) " • ${formatBytes(project.fileSizeBytes)}" else ""
        return "$date$size"
    }

    private fun formatBytes(bytes: Long): String {
        val mb = bytes / (1024f * 1024f)
        return if (mb >= 1f) "%.1f MB".format(mb) else "${bytes / 1024} KB"
    }

    private data class EditorTool(val label: String, val iconRes: Int)
}
