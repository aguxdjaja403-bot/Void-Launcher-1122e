package com.purplevoid.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import java.util.Calendar
import kotlin.math.min

/** Reloj analógico simple para el widget del home. */
class ClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val d = resources.displayMetrics.density

    private val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xB3FFFFFF.toInt()
        strokeCap = Paint.Cap.ROUND
    }
    private val hand = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        strokeCap = Paint.Cap.ROUND
    }
    private val second = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFB74D.toInt()
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 1.5f * d
    }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFB74D.toInt() }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) / 2f - 2 * d

        for (i in 0 until 60) {
            val big = i % 5 == 0
            tick.strokeWidth = (if (big) 2f else 1f) * d
            val len = r * (if (big) 0.13f else 0.06f)
            canvas.save()
            canvas.rotate(i * 6f, cx, cy)
            canvas.drawLine(cx, cy - r, cx, cy - r + len, tick)
            canvas.restore()
        }

        val c = Calendar.getInstance()
        val h = c.get(Calendar.HOUR)
        val m = c.get(Calendar.MINUTE)
        val s = c.get(Calendar.SECOND)
        val ms = c.get(Calendar.MILLISECOND)

        drawHand(canvas, cx, cy, h * 30f + m * 0.5f, r * 0.50f, 4f * d, hand)
        drawHand(canvas, cx, cy, m * 6f + s * 0.1f, r * 0.75f, 3f * d, hand)
        drawHand(canvas, cx, cy, s * 6f, r * 0.85f, 1.5f * d, second)
        canvas.drawCircle(cx, cy, 4f * d, dot)

        postInvalidateDelayed((1000 - ms).toLong())
    }

    private fun drawHand(
        canvas: Canvas, cx: Float, cy: Float,
        angle: Float, length: Float, width: Float, paint: Paint
    ) {
        paint.strokeWidth = width
        canvas.save()
        canvas.rotate(angle, cx, cy)
        canvas.drawLine(cx, cy, cx, cy - length, paint)
        canvas.restore()
    }
}
