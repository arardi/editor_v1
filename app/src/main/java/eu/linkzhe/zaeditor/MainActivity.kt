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
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import eu.linkzhe.zaeditor.data.ProjectStore
import eu.linkzhe.zaeditor.model.VideoProject
import eu.linkzhe.zaeditor.player.VideoPlayer
import eu.linkzhe.zaeditor.ui.TimelineView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var store: ProjectStore
    private lateinit var screenContainer: FrameLayout
    private val projects = mutableListOf<VideoProject>()
    private var currentProject: VideoProject? = null
    private var player: ExoPlayer? = null
    private val ticker = Handler(Looper.getMainLooper())
    private var selectedTool = "Trim"
    private val toolViews = mutableMapOf<String, View>()

    private val photoPicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) addProjectFromUri(uri)
    }
    private val docPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) addProjectFromUri(uri)
    }

    private val progressTicker = object : Runnable {
        override fun run() {
            updatePlaybackUi()
            ticker.postDelayed(this, 250L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        store = ProjectStore(this)
        projects.addAll(store.getProjects())
        setContentView(R.layout.activity_main)
        screenContainer = findViewById(R.id.screenContainer)
        showHomeScreen()
    }

    override fun onDestroy() {
        ticker.removeCallbacks(progressTicker)
        player?.release()
        player = null
        super.onDestroy()
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = ContextCompat.getColor(this, R.color.app_background)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.app_background)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

    private fun showHomeScreen() {
        currentProject = null
        releasePlayer()
        screenContainer.removeAllViews()
        val view = layoutInflater.inflate(R.layout.screen_home, screenContainer, false)
        screenContainer.addView(view)
        view.findViewById<View>(R.id.startCreatingCard).setOnClickListener { showImportSheet() }
        renderRecentProjects(view)
    }

    private fun renderRecentProjects(root: View) {
        val recentContainer = root.findViewById<LinearLayout>(R.id.recentProjectsContainer)
        val emptyState = root.findViewById<View>(R.id.emptyProjectsState)
        recentContainer.removeAllViews()
        val realProjects = projects.filter { it.uri != null }
        emptyState.visibility = if (realProjects.isEmpty()) View.VISIBLE else View.GONE
        realProjects.forEach { project ->
            val item = layoutInflater.inflate(R.layout.item_recent_project, recentContainer, false)
            item.findViewById<TextView>(R.id.projectName).text = project.name
            item.findViewById<TextView>(R.id.projectDate).text = dateFormatter.format(Date(project.createdAt))
            item.findViewById<TextView>(R.id.projectMeta).text = "${formatTime(project.durationMs)} • ${formatSize(project.fileSizeBytes)}"
            item.setOnClickListener { showEditorScreen(project) }
            recentContainer.addView(item)
        }
    }

    private fun showImportSheet() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val sheet = LayoutInflater.from(this).inflate(R.layout.view_import_video_sheet, null)
        dialog.setContentView(sheet)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.BOTTOM)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        sheet.findViewById<View>(R.id.importVideoAction).setOnClickListener {
            dialog.dismiss()
            pickVideo()
        }
        sheet.findViewById<View>(R.id.cancelAction).setOnClickListener { dialog.dismiss() }
        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun showEditorScreen(project: VideoProject) {
        currentProject = project
        screenContainer.removeAllViews()
        val view = layoutInflater.inflate(R.layout.screen_editor, screenContainer, false)
        screenContainer.addView(view)

        val title = view.findViewById<TextView>(R.id.editorProjectTitle)
        val playButton = view.findViewById<ImageButton>(R.id.playPauseButton)
        val playerView = view.findViewById<PlayerView>(R.id.playerView)
        val timeline = view.findViewById<TimelineView>(R.id.timelineView)
        val toolContainer = view.findViewById<LinearLayout>(R.id.toolContainer)

        title.text = project.name
        view.findViewById<View>(R.id.backButton).setOnClickListener { showHomeScreen() }
        view.findViewById<View>(R.id.exportButton).setOnClickListener { showExportDialog() }
        playButton.setOnClickListener { togglePlayback() }

        setupTools(toolContainer)
        setupPlayer(project, playerView, timeline, view.findViewById(R.id.previewContainer))
        ticker.removeCallbacks(progressTicker)
        ticker.post(progressTicker)
    }

    private fun setupPlayer(project: VideoProject, playerView: PlayerView, timeline: TimelineView, previewContainer: FrameLayout) {
        releasePlayer()
        val exoPlayer = VideoPlayer.create(this, project.path)
        player = exoPlayer
        playerView.player = exoPlayer
        playerView.useController = false
        timeline.durationMs = project.durationMs.takeIf { it > 0 } ?: 1L
        timeline.onSeek = { position -> exoPlayer.seekTo(position) }
        exoPlayer.addListener(object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                adjustPlayerViewSize(playerView, previewContainer, videoSize)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlaybackUi()
            }
        })
    }

    private fun adjustPlayerViewSize(playerView: PlayerView, previewContainer: FrameLayout, videoSize: VideoSize) {
        if (videoSize.height <= 0) return
        previewContainer.post {
            val ratio = videoSize.width.toFloat() / videoSize.height.toFloat()
            val params = playerView.layoutParams as FrameLayout.LayoutParams
            params.gravity = Gravity.CENTER
            if (ratio < 0.85f) {
                params.height = FrameLayout.LayoutParams.MATCH_PARENT
                params.width = ((previewContainer.height * ratio).toInt()).coerceAtMost((previewContainer.width * 0.65f).toInt())
            } else {
                params.width = FrameLayout.LayoutParams.MATCH_PARENT
                params.height = (previewContainer.width / (16f / 9f)).toInt().coerceAtMost(previewContainer.height)
            }
            playerView.layoutParams = params
        }
    }

    private fun setupTools(container: LinearLayout) {
        container.removeAllViews()
        toolViews.clear()
        editorTools.forEach { tool ->
            val item = layoutInflater.inflate(R.layout.item_editor_tool, container, false)
            item.findViewById<ImageView>(R.id.toolIcon).setImageResource(tool.iconRes)
            item.findViewById<TextView>(R.id.toolLabel).text = tool.label
            item.setOnClickListener {
                selectedTool = tool.label
                updateToolSelection()
            }
            toolViews[tool.label] = item
            container.addView(item)
        }
        updateToolSelection()
    }

    private fun updateToolSelection() {
        editorTools.forEach { tool ->
            val item = toolViews[tool.label] ?: return@forEach
            val selected = tool.label == selectedTool
            val color = when {
                selected && tool.danger -> ContextCompat.getColor(this, R.color.danger_red)
                selected -> ContextCompat.getColor(this, R.color.primary_blue)
                else -> ContextCompat.getColor(this, R.color.text_secondary)
            }
            item.setBackgroundResource(R.drawable.bg_tool_transparent)
            item.findViewById<ImageView>(R.id.toolIcon).setColorFilter(color)
            item.findViewById<TextView>(R.id.toolLabel).setTextColor(color)
        }
    }

    private fun togglePlayback() {
        player?.let { if (it.isPlaying) it.pause() else it.play() }
        updatePlaybackUi()
    }

    private fun updatePlaybackUi() {
        val root = screenContainer.getChildAt(0) ?: return
        val exoPlayer = player ?: return
        val duration = if (exoPlayer.duration > 0) exoPlayer.duration else 1L
        val position = exoPlayer.currentPosition.coerceAtLeast(0L).coerceAtMost(duration)
        root.findViewById<TextView?>(R.id.currentTimeText)?.text = formatTime(position)
        root.findViewById<TextView?>(R.id.durationText)?.text = formatTime(duration)
        root.findViewById<ImageButton?>(R.id.playPauseButton)?.setImageResource(if (exoPlayer.isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
        root.findViewById<TimelineView?>(R.id.timelineView)?.apply {
            durationMs = duration
            currentPositionMs = position
        }
    }

    private fun releasePlayer() {
        ticker.removeCallbacks(progressTicker)
        player?.release()
        player = null
    }

    private fun showExportDialog() {
        AlertDialog.Builder(this)
            .setTitle("Export")
            .setMessage("Export feature coming soon")
            .setPositiveButton("OK", null)
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
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // Photo Picker grants access without persistable permissions.
        } catch (_: IllegalArgumentException) {
            // Some providers do not expose persistable permissions.
        }
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
        projects.clear()
        projects.addAll(store.getProjects())
        showEditorScreen(project)
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

    companion object {
        private val dateFormatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

        private val editorTools = listOf(
            EditorTool("Trim", R.drawable.ic_trim),
            EditorTool("Split", R.drawable.ic_split),
            EditorTool("Audio", R.drawable.ic_audio),
            EditorTool("Text", R.drawable.ic_text),
            EditorTool("Filter", R.drawable.ic_filter),
            EditorTool("Speed", R.drawable.ic_speed),
            EditorTool("Crop", R.drawable.ic_crop),
            EditorTool("Delete", R.drawable.ic_delete, danger = true)
        )

        private fun formatTime(ms: Long): String {
            val total = (ms / 1000).coerceAtLeast(0)
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
    }
}

private data class EditorTool(
    val label: String,
    val iconRes: Int,
    val danger: Boolean = false
)
