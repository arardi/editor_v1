package eu.linkzhe.zaeditor.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import eu.linkzhe.zaeditor.R
import kotlin.math.max

class TimelineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    var durationMs: Long = 1L
        set(value) {
            field = value.coerceAtLeast(1L)
            invalidate()
        }

    var currentPositionMs: Long = 0L
        set(value) {
            field = value.coerceIn(0L, durationMs)
            invalidate()
        }

    var onSeek: ((Long) -> Unit)? = null

    private val density = resources.displayMetrics.density
    private val textPrimary = color(R.color.text_primary)
    private val textSecondary = color(R.color.text_secondary)
    private val primaryBlue = color(R.color.primary_blue)
    private val trackColor = color(R.color.timeline_track)
    private val surfaceSecondary = color(R.color.surface_secondary)

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textSecondary
        textSize = 10f * scaledDensity
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textPrimary
        textSize = 12f * scaledDensity
        isFakeBoldText = true
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textSecondary
        strokeWidth = dp(1f)
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = trackColor
        style = Paint.Style.FILL
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = surfaceSecondary
        style = Paint.Style.FILL
    }
    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryBlue
        style = Paint.Style.FILL
    }

    private val scaledDensity: Float get() = resources.displayMetrics.scaledDensity

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val contentLeft = paddingLeft + dp(16f)
        val contentRight = width - paddingRight - dp(16f)
        val contentWidth = max(1f, contentRight - contentLeft)
        val progress = (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        val playheadX = contentLeft + contentWidth * progress

        canvas.drawText("Timeline", contentLeft, dp(24f), labelPaint)
        canvas.drawText(formatTime(currentPositionMs), contentRight - textPaint.measureText(formatTime(currentPositionMs)), dp(24f), textPaint)

        val rulerTop = dp(38f)
        val tickCount = 6
        repeat(tickCount) { index ->
            val ratio = index / (tickCount - 1).toFloat()
            val x = contentLeft + contentWidth * ratio
            val major = index % 2 == 0
            canvas.drawLine(x, rulerTop, x, rulerTop + if (major) dp(10f) else dp(6f), tickPaint)
            val label = formatRulerTime((durationMs * ratio).toLong())
            canvas.drawText(label, x - textPaint.measureText(label) / 2f, rulerTop + dp(24f), textPaint)
        }

        val trackTop = dp(72f)
        val trackBottom = height - dp(18f)
        val trackRect = RectF(contentLeft, trackTop, contentRight, trackBottom)
        canvas.drawRoundRect(trackRect, dp(12f), dp(12f), trackPaint)

        val progressRect = RectF(contentLeft, trackTop, playheadX, trackBottom)
        canvas.drawRoundRect(progressRect, dp(12f), dp(12f), progressPaint)

        repeat(12) { index ->
            val x = contentLeft + contentWidth * (index / 11f)
            canvas.drawLine(x, trackTop + dp(8f), x, trackBottom - dp(8f), tickPaint)
        }

        canvas.drawCircle(playheadX, rulerTop - dp(4f), dp(6f), activePaint)
        canvas.drawRoundRect(RectF(playheadX - dp(1f), rulerTop - dp(4f), playheadX + dp(1f), trackBottom + dp(6f)), dp(2f), dp(2f), activePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val contentLeft = paddingLeft + dp(16f)
                val contentRight = width - paddingRight - dp(16f)
                val ratio = ((event.x - contentLeft) / max(1f, contentRight - contentLeft)).coerceIn(0f, 1f)
                val position = (durationMs * ratio).toLong().coerceIn(0L, durationMs)
                currentPositionMs = position
                onSeek?.invoke(position)
                return true
            }
        }
        return true
    }

    private fun color(id: Int): Int = ContextCompat.getColor(context, id)

    private fun dp(value: Float): Float = value * density

    private fun formatRulerTime(ms: Long): String = "${(ms / 1000).coerceAtLeast(0)}s"

    private fun formatTime(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }
}
