package com.satir.nozzlealigner

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

/**
 * Telefonun kendi (arka) kamerasını CameraX ile okur, her kareyi NozzleProcessor'a verir.
 * USB kamera gerektirmez — MVP'yi sıfır donanımla denemek için.
 */
class PhoneCameraController(
    private val activity: AppCompatActivity,
    private val previewView: PreviewView,
    private val processor: NozzleProcessor,
    private val lensFacing: Int = CameraSelector.LENS_FACING_FRONT,  // ÖN kamera (varsayılan)
    private val onResult: (NozzleProcessor.Result) -> Unit
) {
    private var provider: ProcessCameraProvider? = null
    private val exec = Executors.newSingleThreadExecutor()
    @Volatile private var busy = false

    fun start() {
        val future = ProcessCameraProvider.getInstance(activity)
        future.addListener({
            try {
                provider = future.get()
                bind()
            } catch (t: Throwable) {
                Log.e(TAG, "CameraX başlatılamadı", t)
            }
        }, ContextCompat.getMainExecutor(activity))
    }

    private fun bind() {
        val p = provider ?: return
        p.unbindAll()

        // Renkli önizleme yok; görüntü siyah-beyaz olarak overlay'de çizilir.
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        analysis.setAnalyzer(exec) { proxy -> analyze(proxy) }

        try {
            val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            p.bindToLifecycle(activity, selector, analysis)
        } catch (t: Throwable) {
            Log.e(TAG, "bindToLifecycle hatası", t)
        }
    }

    private fun analyze(proxy: ImageProxy) {
        if (busy) { proxy.close(); return }
        busy = true
        try {
            val nv21 = yuv420ToNv21(proxy)
            val r = processor.process(nv21, proxy.width, proxy.height, proxy.imageInfo.rotationDegrees)
            onResult(r)
        } catch (t: Throwable) {
            Log.e(TAG, "İşleme hatası", t)
        } finally {
            busy = false
            proxy.close()
        }
    }

    fun stop() {
        try { provider?.unbindAll() } catch (_: Throwable) {}
    }

    fun release() {
        stop()
        exec.shutdown()
    }

    /** YUV_420_888 -> NV21 (Y düzlemi + araya geçmeli V,U). Stride'ları dikkate alır. */
    private fun yuv420ToNv21(image: ImageProxy): ByteArray {
        val w = image.width
        val h = image.height
        val ySize = w * h
        val out = ByteArray(ySize + ySize / 2)

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        // --- Y ---
        val yBuf = yPlane.buffer
        val yRowStride = yPlane.rowStride
        for (row in 0 until h) {
            yBuf.position(row * yRowStride)
            yBuf.get(out, row * w, w)
        }

        // --- VU (NV21) ---
        val uBuf = uPlane.buffer
        val vBuf = vPlane.buffer
        val uRowStride = uPlane.rowStride
        val vRowStride = vPlane.rowStride
        val uPixStride = uPlane.pixelStride
        val vPixStride = vPlane.pixelStride

        val chromaW = w / 2
        val chromaH = h / 2
        var offset = ySize
        for (row in 0 until chromaH) {
            for (col in 0 until chromaW) {
                out[offset++] = vBuf.get(row * vRowStride + col * vPixStride)
                out[offset++] = uBuf.get(row * uRowStride + col * uPixStride)
            }
        }
        return out
    }

    companion object { private const val TAG = "PhoneCameraController" }
}
