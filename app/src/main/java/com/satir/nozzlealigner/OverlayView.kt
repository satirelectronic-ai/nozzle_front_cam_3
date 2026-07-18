package com.satir.nozzlealigner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/** Siyah-beyaz görüntü + sabit merkez nişanı + kırmızı ışın + yön okunu çizer. */
class OverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var result: NozzleProcessor.Result? = null
    private var displayBmp: Bitmap? = null
    private val bmpPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val srcRect = Rect()
    private val dstRect = RectF()

    /** Ön kamera aynalı gösterildiğinden çizimi de yatay aynala. */
    var mirror: Boolean = false

    /** Sabit merkez referansı (kare oranı 0..1) — MainActivity ile senkron. */
    var refFracX: Float = 0.5f
    var refFracY: Float = 0.5f

    private var lastFrameW = 0
    private var lastFrameH = 0

    private val nozzlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 4f; color = Color.WHITE
    }
    private val beamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 3f; color = Color.YELLOW
    }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 6f
    }
    // Sabit merkez nişanı (camgöbeği)
    private val refPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 3f; color = Color.rgb(0, 230, 230)
    }

    fun update(r: NozzleProcessor.Result?) {
        result = r
        displayBmp = r?.display
        if (r != null && r.frameW > 0) { lastFrameW = r.frameW; lastFrameH = r.frameH }
        postInvalidate()
    }

    // --- ölçekleme yardımcıları (kare -> görünüm) ---
    private fun scale(): Float {
        if (lastFrameW == 0 || lastFrameH == 0) return 1f
        return min(width.toFloat() / lastFrameW, height.toFloat() / lastFrameH)
    }
    private fun offX() = (width - lastFrameW * scale()) / 2f
    private fun offY() = (height - lastFrameH * scale()) / 2f
    private fun mapX(fx: Double) = offX() + fx.toFloat() * scale()
    private fun mapY(fy: Double) = offY() + fy.toFloat() * scale()

    /** Ekran dokunuşunu kare oranına (0..1) çevirir; ayna dikkate alınır. */
    fun screenToFrac(sx: Float, sy: Float): Pair<Float, Float>? {
        if (lastFrameW == 0 || lastFrameH == 0) {
            // kare yoksa doğrudan görünüm oranı
            return Pair((sx / width).coerceIn(0f, 1f), (sy / height).coerceIn(0f, 1f))
        }
        val s = scale()
        val mx = if (mirror) width - sx else sx
        val fx = ((mx - offX()) / s) / lastFrameW
        val fy = ((sy - offY()) / s) / lastFrameH
        return Pair(fx.coerceIn(0f, 1f), fy.coerceIn(0f, 1f))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val saveCount = canvas.save()
        if (mirror) { canvas.translate(width.toFloat(), 0f); canvas.scale(-1f, 1f) }

        // --- Siyah-beyaz kamera görüntüsü (taban katman) ---
        val bmp = displayBmp
        if (bmp != null && !bmp.isRecycled && lastFrameW > 0) {
            srcRect.set(0, 0, bmp.width, bmp.height)
            dstRect.set(offX(), offY(), offX() + lastFrameW * scale(), offY() + lastFrameH * scale())
            canvas.drawBitmap(bmp, srcRect, dstRect, bmpPaint)
        }

        val r = result

        // --- Sabit merkez nişanı: HER ZAMAN çiz ---
        val rcx: Float; val rcy: Float
        if (r != null && r.frameW > 0) { rcx = mapX(r.refX); rcy = mapY(r.refY) }
        else { rcx = refFracX * width; rcy = refFracY * height }
        val rl = 40f
        canvas.drawLine(rcx - rl, rcy, rcx + rl, rcy, refPaint)
        canvas.drawLine(rcx, rcy - rl, rcx, rcy + rl, refPaint)
        canvas.drawCircle(rcx, rcy, 14f, refPaint)

        if (r != null) {
            val color = when (r.state) {
                NozzleProcessor.State.GREEN -> Color.rgb(0, 200, 0)
                NozzleProcessor.State.YELLOW -> Color.rgb(230, 180, 0)
                NozzleProcessor.State.RED -> Color.rgb(220, 40, 40)
                NozzleProcessor.State.NONE -> Color.GRAY
            }
            arrowPaint.color = color
            nozzlePaint.color = Color.argb(150, 255, 255, 255)

            // nozul çemberi (varsa, sadece görsel bağlam)
            r.nozzleCenter?.let { nc ->
                canvas.drawCircle(mapX(nc.x), mapY(nc.y), (r.nozzleRadiusPx * scale()).toFloat(), nozzlePaint)
            }
            // ışın noktası + merkezden ışına ok
            r.beamCenter?.let { bc ->
                val bxp = mapX(bc.x); val byp = mapY(bc.y)
                canvas.drawCircle(bxp, byp, 12f, beamPaint)
                if (r.state != NozzleProcessor.State.GREEN) {
                    canvas.drawLine(rcx, rcy, bxp, byp, arrowPaint)
                }
            }
        }

        canvas.restoreToCount(saveCount)
    }
}
