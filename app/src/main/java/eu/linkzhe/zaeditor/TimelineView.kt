package eu.linkzhe.zaeditor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.max

class TimelineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val density = resources.displayMetrics.density
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val playheadPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trackRect = RectF()

    private var durationMs: Long = 1L
    private var positionMs: Long = 0L
    private var seekListener: ((Long) -> Unit)? = null

    init {
        isClickable = true
        trackPaint.color = ContextCompat.getColor(context, R.color.za_surface_soft)
        progressPaint.color = ContextCompat.getColor(context, R.color.za_primary_soft)
        tickPaint.color = ContextCompat.getColor(context, R.color.za_divider)
        textPaint.color = ContextCompat.getColor(context, R.color.za_text_secondary)
        textPaint.textSize = 10f * resources.displayMetrics.scaledDensity
        textPaint.textAlign = Paint.Align.CENTER
        playheadPaint.color = ContextCompat.getColor(context, R.color.za_primary)
        labelPaint.color = ContextCompat.getColor(context, R.color.za_text_primary)
        labelPaint.textSize = 13f * resources.displayMetrics.scaledDensity
        labelPaint.isFakeBoldText = true
        setPadding(dp(14), dp(12), dp(14), dp(12))
    }

    fun setTimeline(durationMs: Long, positionMs: Long) {
        this.durationMs = max(1L, durationMs)
        this.positionMs = positionMs.coerceIn(0L, this.durationMs)
        invalidate()
    }

    fun setOnSeekRequested(listener: (Long) -> Unit) {
        seekListener = listener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val left = paddingLeft.toFloat()
        val right = (width - paddingRight).toFloat()
        val top = paddingTop.toFloat()
        val bottom = (height - paddingBottom).toFloat()
        val trackTop = top + dp(34)
        val trackBottom = bottom - dp(18)
        val radius = dp(14).toFloat()
        val ratio = positionMs / durationMs.toFloat()
        val playheadX = left + (right - left) * ratio

        canvas.drawText(context.getString(R.string.video_timeline), left, top + dp(13), labelPaint)
        val timeText = "${formatTime(positionMs)} / ${formatTime(durationMs)}"
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(timeText, right, top + dp(13), textPaint)

        trackRect.set(left, trackTop, right, trackBottom)
        canvas.drawRoundRect(trackRect, radius, radius, trackPaint)

        val progressRect = RectF(left, trackTop, playheadX, trackBottom)
        canvas.drawRoundRect(progressRect, radius, radius, progressPaint)

        val tickCount = 7
        for (index in 0 until tickCount) {
            val tickRatio = index / (tickCount - 1).toFloat()
            val x = left + (right - left) * tickRatio
            val tickHeight = if (index % 2 == 0) dp(13) else dp(8)
            canvas.drawRect(x - 0.5f * density, trackTop + dp(10), x + 0.5f * density, trackTop + dp(10) + tickHeight, tickPaint)
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(formatRulerTime((durationMs * tickRatio).toLong()), x, bottom, textPaint)
        }

        canvas.drawRect(playheadX - density, trackTop - dp(8), playheadX + density, trackBottom + dp(8), playheadPaint)
        canvas.drawCircle(playheadX, trackTop - dp(8), dp(6).toFloat(), playheadPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                seekToX(event.x)
                if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun seekToX(x: Float) {
        val left = paddingLeft.toFloat()
        val right = (width - paddingRight).toFloat()
        val ratio = ((x - left) / (right - left)).coerceIn(0f, 1f)
        val target = (durationMs * ratio).toLong()
        positionMs = target
        invalidate()
        seekListener?.invoke(target)
    }

    private fun dp(value: Int): Int = (value * density).toInt()

    companion object {
        fun formatTime(ms: Long): String {
            val totalSeconds = (ms / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }

        private fun formatRulerTime(ms: Long): String = "${ms / 1000}s"
    }
}
