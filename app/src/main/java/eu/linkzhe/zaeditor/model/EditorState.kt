package eu.linkzhe.zaeditor.model

import android.graphics.Color
import kotlin.math.roundToInt

/**
 * Preview/editor adjustments. The same state is passed to export so the
 * rendering pipeline can apply these visual decisions later.
 */
data class TextOverlayState(
    var enabled: Boolean = false,
    var text: String = "Your Text",
    var x: Float = 0.5f,
    var y: Float = 0.5f,
    var textSizeSp: Float = 28f,
    var color: Int = Color.WHITE,
    var fontStyle: String = "sans"
)

data class EditorState(
    var mirrorHorizontal: Boolean = false,
    var backgroundColor: Int = Color.BLACK,
    var overlayEnabled: Boolean = false,
    var overlayColor: Int = 0xFF0A84FF.toInt(),
    var overlayX: Float = 0.5f,
    var overlayY: Float = 0.5f,
    var overlayWidth: Float = 1.0f,
    var overlayHeight: Float = 0.25f,
    var overlayAlpha: Float = 0.35f,
    var textOverlay: TextOverlayState = TextOverlayState()
) {
    val overlayColorWithAlpha: Int
        get() {
            val alpha = (overlayAlpha.coerceIn(0f, 1f) * 255).roundToInt()
            return Color.argb(alpha, Color.red(overlayColor), Color.green(overlayColor), Color.blue(overlayColor))
        }
}
