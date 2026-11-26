package com.purplevoid.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** Fondo morado con manchas de color que se mueven despacio (efecto líquido). */
class LiquidBackground @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private class Blob(
        val color: Int,
        val ax: Float, val ay: Float,
        val radius: Float,
        val sx: Float, val sy: Float,
        val phase: Float
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    }

    private val blobs = listOf(
        Blob(0xE67C3AED.toInt(), 0.20f, 0.15f, 0.75f, 0.30f, 0.23f, 0.0f),
        Blob(0xDDC026D3.toInt(), 0.85f, 0.40f, 0.65f, 0.21f, 0.34f, 1.7f),
        Blob(0xEE4C1D95.toInt(), 0.50f, 0.70f, 0.85f, 0.17f, 0.26f, 3.1f),
        Blob(0xAAF472B6.toInt(), 0.15f, 0.85f, 0.50f, 0.28f, 0.19f, 4.4f),
        Blob(0x88FB923C.toInt(), 0.90f, 0.95f, 0.40f, 0.24f, 0.31f, 5.6f)
    )

    private var running = false

    fun start() {
        running = true
        postInvalidateOnAnimation()
    }

    fun stop() {
        running = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val base = max(w, h).toFloat()
        for (b in blobs) {
            val r = base * b.radius * 0.6f
            b.paint.shader = RadialGradient(
                0f, 0f, r,
                b.color, b.color and 0x00FFFFFF,
                Shader.TileMode.CLAMP
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(0xFF12002B.toInt())
        val t = SystemClock.uptimeMillis() / 1000f
        val w = width.toFloat()
        val h = height.toFloat()
        val base = max(w, h)
        for (b in blobs) {
            val x = w * (b.ax + 0.12f * sin(t * b.sx + b.phase))
            val y = h * (b.ay + 0.10f * cos(t * b.sy + b.phase))
            canvas.save()
            canvas.translate(x, y)
            canvas.drawCircle(0f, 0f, base * b.radius * 0.6f, b.paint)
            canvas.restore()
        }
        if (running) postInvalidateOnAnimation()
    }
}
