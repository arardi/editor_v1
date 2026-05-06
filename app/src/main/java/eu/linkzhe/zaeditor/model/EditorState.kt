package eu.linkzhe.zaeditor.model

import android.graphics.Color

/**
 * Preview/editor adjustments. The same state is passed to export so the
 * rendering pipeline can apply these visual decisions later.
 */
data class EditorState(
    var mirrorHorizontal: Boolean = false,
    var flipVertical: Boolean = false,
    var backgroundColor: Int = Color.BLACK,
    var overlayEnabled: Boolean = false,
    var overlayColor: Int = 0xFF0A84FF.toInt(),
    var overlayX: Float = 0.5f,
    var overlayY: Float = 0.5f,
    var overlayWidth: Float = 1.0f,
    var overlayHeight: Float = 0.25f,
    var overlayAlpha: Float = 0.35f
) {
    val overlayColorWithAlpha: Int
        get() {
            val alpha = (overlayAlpha.coerceIn(0f, 1f) * 255).toInt()
            return Color.argb(alpha, Color.red(overlayColor), Color.green(overlayColor), Color.blue(overlayColor))
        }
}
