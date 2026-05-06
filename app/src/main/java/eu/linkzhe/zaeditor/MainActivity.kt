package eu.linkzhe.zaeditor

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import eu.linkzhe.zaeditor.data.ProjectStore
import eu.linkzhe.zaeditor.model.EditorState
import eu.linkzhe.zaeditor.model.VideoProject
import eu.linkzhe.zaeditor.player.VideoPlayer
import java.text.DateFormat
import java.util.Date
import kotlin.math.max

class MainActivity : ComponentActivity() {
    private lateinit var store: ProjectStore
    private lateinit var container: FrameLayout
    private val projects = mutableListOf<VideoProject>()
    private var currentProject: VideoProject? = null
    private var player: ExoPlayer? = null
    private val uiHandler = Handler(Looper.getMainLooper())
    private var currentScreen = ScreenState.HOME
    private var lastBackPressedTime = 0L
    private var currentTool: EditorTool? = null
    private var editorState = EditorState()

    private val editorTools = listOf(
        EditorTool("trim", "Trim", R.drawable.ic_trim),
        EditorTool("split", "Split", R.drawable.ic_split),
        EditorTool("mirror", "Mirror", R.drawable.ic_mirror),
        EditorTool("background", "BG", R.drawable.ic_background),
        EditorTool("overlay", "Overlay", R.drawable.ic_overlay),
        EditorTool("text", "Text", R.drawable.ic_text),
        EditorTool("filter", "Filter", R.drawable.ic_filter),
        EditorTool("speed", "Speed", R.drawable.ic_speed),
        EditorTool("crop", "Crop", R.drawable.ic_crop)
    )

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
        setupBackNavigation()
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

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentScreen == ScreenState.EDITOR) {
                    showHome()
                    return
                }

                val now = System.currentTimeMillis()
                if (now - lastBackPressedTime < 2_000L) {
                    finish()
                } else {
                    lastBackPressedTime = now
                    Toast.makeText(this@MainActivity, R.string.back_to_exit, Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun showHome() {
        currentScreen = ScreenState.HOME
        currentTool = null
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
        currentScreen = ScreenState.EDITOR
        currentProject = project
        currentTool = null
        editorState = EditorState()
        val view = layoutInflater.inflate(R.layout.screen_editor, container, false)
        container.removeAllViews()
        container.addView(view)

        view.findViewById<TextView>(R.id.editorProjectTitle).text = project.name
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { showHome() }
        view.findViewById<ImageButton>(R.id.exportButton).setOnClickListener { showExportDialog() }
        view.findViewById<TextView>(R.id.timelineToggle).setOnClickListener { showTimelinePanel() }

        val playerView = view.findViewById<PlayerView>(R.id.playerView)
        val playPause = view.findViewById<ImageButton>(R.id.playPauseButton)
        val timeline = view.findViewById<TimelineView>(R.id.timelineView)
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

        setupToolbarSelection(view.findViewById(R.id.toolContainer))
        setupTransformPanel(view)
        setupBackgroundPanel(view)
        setupOverlayPanel(view)
        setupOverlayDrag(view)
        hideAllPanels(view)
        applyEditorStateToPreview()
        uiHandler.removeCallbacks(progressUpdater)
        uiHandler.post(progressUpdater)
    }

    private fun setupToolbarSelection(toolbar: LinearLayout) {
        toolbar.removeAllViews()

        editorTools.forEach { tool ->
            val item = LayoutInflater.from(this).inflate(R.layout.item_editor_tool, toolbar, false)
            item.layoutParams = LinearLayout.LayoutParams(dp(64), ViewGroup.LayoutParams.MATCH_PARENT)

            item.findViewById<ImageView>(R.id.tool_icon).setImageResource(tool.iconRes)
            item.findViewById<TextView>(R.id.tool_label).text = tool.label

            item.setBackgroundColor(Color.TRANSPARENT)
            item.setOnClickListener { onToolSelected(tool) }

            toolbar.addView(item)
        }

        updateToolSelection(toolbar)
    }

    private fun onToolSelected(tool: EditorTool) {
        currentTool = tool

        container.findViewById<LinearLayout>(R.id.toolContainer)?.let { toolbar ->
            updateToolSelection(toolbar)
        }

        when (tool.id) {
            "trim", "split" -> showTimelinePanel()
            "mirror" -> showTransformPanel()
            "background" -> showBackgroundPanel()
            "overlay" -> showOverlayPanel()
            else -> hideAllPanels(container.getChildAt(0))
        }
    }

    private fun updateToolSelection(toolbar: LinearLayout) {
        val selectedColor = ContextCompat.getColor(this, R.color.za_primary)
        val normalColor = ContextCompat.getColor(this, R.color.za_text_secondary)

        for (index in 0 until toolbar.childCount) {
            val item = toolbar.getChildAt(index)
            val tool = editorTools.getOrNull(index)
            val color = if (tool?.id == currentTool?.id) selectedColor else normalColor

            item.setBackgroundColor(Color.TRANSPARENT)
            item.findViewById<ImageView>(R.id.tool_icon).setColorFilter(color)
            item.findViewById<TextView>(R.id.tool_label).setTextColor(color)
        }
    }

    private fun showTimelinePanel() {
        val root = container.getChildAt(0) ?: return
        showOnlyPanel(root, R.id.timelinePanel)
    }

    private fun showTransformPanel() {
        val root = container.getChildAt(0) ?: return
        showOnlyPanel(root, R.id.transformPanel)
    }

    private fun showBackgroundPanel() {
        val root = container.getChildAt(0) ?: return
        showOnlyPanel(root, R.id.backgroundPanel)
    }

    private fun showOverlayPanel() {
        val root = container.getChildAt(0) ?: return
        showOnlyPanel(root, R.id.overlayPanel)
    }

    private fun hideAllPanels(root: View?) {
        root ?: return
        listOf(R.id.timelinePanel, R.id.transformPanel, R.id.backgroundPanel, R.id.overlayPanel).forEach { id ->
            root.findViewById<View>(id)?.visibility = View.GONE
        }
    }

    private fun showOnlyPanel(root: View, panelId: Int) {
        hideAllPanels(root)
        root.findViewById<View>(panelId)?.apply {
            alpha = 0f
            translationY = dp(8).toFloat()
            visibility = View.VISIBLE
            animate().alpha(1f).translationY(0f).setDuration(140L).start()
        }
    }

    private fun setupTransformPanel(root: View) {
        root.findViewById<TextView>(R.id.mirrorButton).setOnClickListener {
            editorState.mirrorHorizontal = !editorState.mirrorHorizontal
            applyEditorStateToPreview()
        }
        root.findViewById<TextView>(R.id.flipButton).setOnClickListener {
            editorState.flipVertical = !editorState.flipVertical
            applyEditorStateToPreview()
        }
    }

    private fun setupBackgroundPanel(root: View) {
        val colors = listOf(
            ColorOption("Black", Color.BLACK),
            ColorOption("Dark", Color.rgb(18, 22, 34)),
            ColorOption("Blue", Color.rgb(20, 64, 150)),
            ColorOption("White", Color.WHITE),
            ColorOption("Red", Color.rgb(170, 36, 48)),
            ColorOption("Green", Color.rgb(28, 135, 82))
        )
        val container = root.findViewById<LinearLayout>(R.id.backgroundColorContainer)
        container.removeAllViews()
        colors.forEach { option ->
            container.addView(createColorChip(option.color, option.label) {
                editorState.backgroundColor = option.color
                applyEditorStateToPreview()
            })
        }
    }

    private fun setupOverlayPanel(root: View) {
        root.findViewById<TextView>(R.id.overlayToggle).setOnClickListener {
            editorState.overlayEnabled = !editorState.overlayEnabled
            applyEditorStateToPreview()
        }

        val overlayColors = listOf(
            ColorOption("Blue", Color.rgb(10, 132, 255)),
            ColorOption("Red", Color.rgb(255, 69, 58)),
            ColorOption("Green", Color.rgb(48, 209, 88)),
            ColorOption("Yellow", Color.rgb(255, 214, 10)),
            ColorOption("Purple", Color.rgb(191, 90, 242)),
            ColorOption("White", Color.WHITE)
        )
        root.findViewById<LinearLayout>(R.id.overlayColorContainer).apply {
            removeAllViews()
            overlayColors.forEach { option ->
                addView(createColorChip(option.color, option.label) {
                    editorState.overlayColor = option.color
                    editorState.overlayEnabled = true
                    applyEditorStateToPreview()
                })
            }
        }

        root.findViewById<LinearLayout>(R.id.overlayAlphaContainer).apply {
            removeAllViews()
            listOf(0.25f to "25%", 0.50f to "50%", 0.75f to "75%").forEach { (alpha, label) ->
                addView(createPanelButton(label) {
                    editorState.overlayAlpha = alpha
                    editorState.overlayEnabled = true
                    applyEditorStateToPreview()
                })
            }
        }

        root.findViewById<TextView>(R.id.overlayTopButton).setOnClickListener { positionOverlay(OverlayPosition.TOP) }
        root.findViewById<TextView>(R.id.overlayBottomButton).setOnClickListener { positionOverlay(OverlayPosition.BOTTOM) }
        root.findViewById<TextView>(R.id.overlayLeftButton).setOnClickListener { positionOverlay(OverlayPosition.LEFT) }
        root.findViewById<TextView>(R.id.overlayRightButton).setOnClickListener { positionOverlay(OverlayPosition.RIGHT) }
        root.findViewById<TextView>(R.id.overlayCenterButton).setOnClickListener { positionOverlay(OverlayPosition.CENTER) }
    }

    private fun positionOverlay(position: OverlayPosition) {
        editorState.overlayEnabled = true
        when (position) {
            OverlayPosition.TOP -> {
                editorState.overlayWidth = 1f
                editorState.overlayHeight = 0.25f
                editorState.overlayX = 0f
                editorState.overlayY = 0f
            }
            OverlayPosition.BOTTOM -> {
                editorState.overlayWidth = 1f
                editorState.overlayHeight = 0.25f
                editorState.overlayX = 0f
                editorState.overlayY = 0.75f
            }
            OverlayPosition.LEFT -> {
                editorState.overlayWidth = 0.25f
                editorState.overlayHeight = 1f
                editorState.overlayX = 0f
                editorState.overlayY = 0f
            }
            OverlayPosition.RIGHT -> {
                editorState.overlayWidth = 0.25f
                editorState.overlayHeight = 1f
                editorState.overlayX = 0.75f
                editorState.overlayY = 0f
            }
            OverlayPosition.CENTER -> {
                editorState.overlayWidth = 0.70f
                editorState.overlayHeight = 0.25f
                editorState.overlayX = 0.15f
                editorState.overlayY = 0.375f
            }
        }
        applyEditorStateToPreview()
    }

    private fun setupOverlayDrag(root: View) {
        val overlay = root.findViewById<View?>(R.id.overlayColorView) ?: return
        val preview = root.findViewById<FrameLayout?>(R.id.previewContainer) ?: return

        var downRawX = 0f
        var downRawY = 0f
        var startX = 0f
        var startY = 0f

        overlay.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = view.x
                    startY = view.y
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val newX = (startX + event.rawX - downRawX)
                        .coerceIn(0f, (preview.width - view.width).coerceAtLeast(0).toFloat())
                    val newY = (startY + event.rawY - downRawY)
                        .coerceIn(0f, (preview.height - view.height).coerceAtLeast(0).toFloat())

                    view.x = newX
                    view.y = newY

                    if (preview.width > 0 && preview.height > 0) {
                        editorState.overlayX = newX / preview.width.toFloat()
                        editorState.overlayY = newY / preview.height.toFloat()
                    }

                    true
                }

                else -> true
            }
        }
    }

    private fun applyEditorStateToPreview() {
        val root = container.getChildAt(0) ?: return
        val previewContainer = root.findViewById<FrameLayout>(R.id.previewContainer) ?: return
        val playerView = root.findViewById<PlayerView>(R.id.playerView) ?: return
        val overlay = root.findViewById<View>(R.id.overlayColorView) ?: return
        val handle = root.findViewById<View>(R.id.overlayDragHandle) ?: return

        playerView.scaleX = if (editorState.mirrorHorizontal) -1f else 1f
        playerView.scaleY = if (editorState.flipVertical) -1f else 1f
        previewContainer.setBackgroundColor(editorState.backgroundColor)
        overlay.visibility = if (editorState.overlayEnabled) View.VISIBLE else View.GONE
        handle.visibility = if (editorState.overlayEnabled) View.VISIBLE else View.GONE
        overlay.setBackgroundColor(editorState.overlayColorWithAlpha)
        updateTransformButtons(root)
        updateOverlayToggle(root)
        updateOverlayLayout(previewContainer, overlay, handle)
    }

    private fun updateTransformButtons(root: View) {
        val active = ContextCompat.getColor(this, R.color.za_primary)
        val inactive = ContextCompat.getColor(this, R.color.za_text_secondary)
        root.findViewById<TextView>(R.id.mirrorButton)?.setTextColor(if (editorState.mirrorHorizontal) active else inactive)
        root.findViewById<TextView>(R.id.flipButton)?.setTextColor(if (editorState.flipVertical) active else inactive)
    }

    private fun updateOverlayToggle(root: View) {
        root.findViewById<TextView>(R.id.overlayToggle)?.apply {
            text = if (editorState.overlayEnabled) "Overlay on" else "Enable overlay"
            setTextColor(ContextCompat.getColor(this@MainActivity, if (editorState.overlayEnabled) R.color.za_primary else R.color.za_text_secondary))
        }
    }

    private fun updateOverlayLayout(previewContainer: FrameLayout, overlay: View, handle: View) {
        previewContainer.post {
            val width = max(1, (previewContainer.width * editorState.overlayWidth).toInt())
            val height = max(1, (previewContainer.height * editorState.overlayHeight).toInt())
            val maxLeftRatio = (1f - editorState.overlayWidth).coerceAtLeast(0f)
            val maxTopRatio = (1f - editorState.overlayHeight).coerceAtLeast(0f)
            editorState.overlayX = editorState.overlayX.coerceIn(0f, maxLeftRatio)
            editorState.overlayY = editorState.overlayY.coerceIn(0f, maxTopRatio)
            overlay.layoutParams = FrameLayout.LayoutParams(width, height)
            overlay.x = previewContainer.width * editorState.overlayX
            overlay.y = previewContainer.height * editorState.overlayY
            handle.x = overlay.x + width / 2f - handle.width / 2f
            handle.y = overlay.y + height / 2f - handle.height / 2f
        }
    }

    private fun createColorChip(color: Int, label: String, onClick: () -> Unit): View {
        return TextView(this).apply {
            contentDescription = label
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
                setStroke(dp(1), ContextCompat.getColor(this@MainActivity, R.color.za_divider))
            }
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(dp(30), dp(30)).apply { marginEnd = dp(12) }
        }
    }

    private fun createPanelButton(label: String, onClick: () -> Unit): View {
        return TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.za_text_secondary))
            textSize = 11f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_panel_button)
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(0, dp(30), 1f).apply { marginEnd = dp(8) }
        }
    }

    private fun updatePlaybackState() {
        val root = container.getChildAt(0) ?: return
        val exoPlayer = player ?: return
        val duration = if (exoPlayer.duration > 0) exoPlayer.duration else 1L
        val position = exoPlayer.currentPosition.coerceAtLeast(0L).coerceAtMost(duration)

        root.findViewById<TextView?>(R.id.currentTimeText)?.text =
            "${formatTime(position)} / ${formatTime(duration)}"

        root.findViewById<TextView?>(R.id.durationText)?.text =
            formatTime(duration)

        root.findViewById<ImageButton?>(R.id.playPauseButton)?.setImageResource(
            if (exoPlayer.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )

        root.findViewById<TimelineView?>(R.id.timelineView)?.apply {
            durationMs = duration
            currentPositionMs = position
        }
    }

    private fun showImportSheet() {
        val dialog = Dialog(this)
        val sheet = layoutInflater.inflate(R.layout.view_import_video_sheet, null, false)
        sheet.findViewById<View>(R.id.importVideoAction).setOnClickListener {
            dialog.dismiss()
            pickVideo()
        }
        sheet.findViewById<View?>(R.id.cancelAction)?.setOnClickListener {
            dialog.dismiss()
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
            .setTitle("Export")
            .setMessage("Export pipeline prepared. Final rendering coming soon.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun formatTime(ms: Long): String = TimelineView.formatTime(ms)

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

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private enum class ScreenState { HOME, EDITOR }
    private enum class OverlayPosition { TOP, BOTTOM, LEFT, RIGHT, CENTER }
    private data class EditorTool(val id: String, val label: String, val iconRes: Int)
    private data class ColorOption(val label: String, val color: Int)
}
