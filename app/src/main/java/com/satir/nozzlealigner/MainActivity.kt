package com.satir.nozzlealigner

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.core.content.ContextCompat
import com.satir.nozzlealigner.databinding.ActivityMainBinding
import org.opencv.android.OpenCVLoader
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private val processor = NozzleProcessor()
    private var phoneCam: PhoneCameraController? = null
    private var lastResult: NozzleProcessor.Result? = null
    private var adjustCenter = false

    // Ön kamera ile başla
    private var lensFacing = CameraSelector.LENS_FACING_FRONT

    private val camPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else Toast.makeText(this, "Kamera izni gerekli", Toast.LENGTH_LONG).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!OpenCVLoader.initLocal()) {
            Toast.makeText(this, "OpenCV yüklenemedi", Toast.LENGTH_LONG).show()
        }

        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        val prefs = getSharedPreferences("cal", MODE_PRIVATE)
        processor.mmPerPx = prefs.getFloat("mmPerPx", 0f).toDouble()
        b.editKnownDia.setText(prefs.getFloat("knownDia", 1.5f).toString())

        // Sabit merkez referansı (kayıtlıysa yükle)
        processor.refFracX = prefs.getFloat("refFracX", 0.5f)
        processor.refFracY = prefs.getFloat("refFracY", 0.5f)
        b.overlay.refFracX = processor.refFracX
        b.overlay.refFracY = processor.refFracY

        b.seekThreshold.progress = processor.beamThreshold
        b.txtThreshold.text = "Kırmızı eşik: ${processor.beamThreshold}"
        b.seekThreshold.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, u: Boolean) {
                processor.beamThreshold = p
                b.txtThreshold.text = "Kırmızı eşik: $p"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        b.btnCalibrate.setOnClickListener { calibrate() }
        b.btnInvertX.setOnClickListener {
            processor.invertX = !processor.invertX
            Toast.makeText(this, "X: ${if (processor.invertX) "ters" else "normal"}", Toast.LENGTH_SHORT).show()
        }
        b.btnInvertY.setOnClickListener {
            processor.invertY = !processor.invertY
            Toast.makeText(this, "Y: ${if (processor.invertY) "ters" else "normal"}", Toast.LENGTH_SHORT).show()
        }
        b.btnMode.setOnClickListener { toggleLens() }

        // --- Sabit merkez ayarı ---
        b.btnCenter.setOnClickListener {
            adjustCenter = !adjustCenter
            b.btnCenter.text = if (adjustCenter) "Merkez: Ayarla ✎" else "Merkez: Kilitli"
            if (adjustCenter)
                Toast.makeText(this, "Ekrana dokunarak merkezi taşı. Bitince tekrar bas.", Toast.LENGTH_LONG).show()
        }
        b.btnCenter.setOnLongClickListener {
            setRef(0.5f, 0.5f)
            Toast.makeText(this, "Merkez ortalandı", Toast.LENGTH_SHORT).show()
            true
        }
        b.overlay.setOnTouchListener { _, ev ->
            if (adjustCenter) {
                val frac = b.overlay.screenToFrac(ev.x, ev.y)
                if (frac != null) setRef(frac.first, frac.second)
                true
            } else false
        }

        updateModeLabel()
        requestOrStart()
    }

    private fun setRef(fx: Float, fy: Float) {
        processor.refFracX = fx
        processor.refFracY = fy
        b.overlay.refFracX = fx
        b.overlay.refFracY = fy
        b.overlay.invalidate()
        getSharedPreferences("cal", MODE_PRIVATE).edit()
            .putFloat("refFracX", fx).putFloat("refFracY", fy).apply()
    }

    private fun requestOrStart() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        if (granted) startCamera() else camPermission.launch(Manifest.permission.CAMERA)
    }

    private fun toggleLens() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
            CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
        updateModeLabel()
        phoneCam?.stop()
        phoneCam = null
        startCamera()
    }

    private fun updateModeLabel() {
        val front = lensFacing == CameraSelector.LENS_FACING_FRONT
        b.btnMode.text = if (front) "Kamera: Ön" else "Kamera: Arka"
        b.overlay.mirror = front   // ön kamera aynalı
    }

    private fun startCamera() {
        if (phoneCam == null) {
            phoneCam = PhoneCameraController(this, b.previewView, processor, lensFacing) { r ->
                runOnUiThread { render(r) }
            }
        }
        phoneCam?.start()
    }

    private fun render(r: NozzleProcessor.Result) {
        lastResult = r
        b.overlay.update(r)
        val color = when (r.state) {
            NozzleProcessor.State.GREEN -> Color.rgb(0, 200, 0)
            NozzleProcessor.State.YELLOW -> Color.rgb(230, 180, 0)
            NozzleProcessor.State.RED -> Color.rgb(220, 40, 40)
            NozzleProcessor.State.NONE -> Color.GRAY
        }
        b.txtDirection.setTextColor(color)
        b.txtDirection.text = r.direction
        b.txtOffset.text = if (!r.ok) "—"
        else if (processor.mmPerPx > 0)
            String.format(Locale.US, "Ofset: %.3f mm  (%.0f px)", r.offsetMm, r.offsetPx)
        else
            String.format(Locale.US, "Ofset: %.0f px  (kalibre edilmedi)", r.offsetPx)
    }

    private fun calibrate() {
        val r = lastResult
        if (r == null || !r.ok || r.nozzleRadiusPx <= 0) {
            Toast.makeText(this, "Önce nozul çemberi net görünmeli", Toast.LENGTH_SHORT).show()
            return
        }
        val knownDia = b.editKnownDia.text.toString().replace(',', '.').toFloatOrNull()
        if (knownDia == null || knownDia <= 0f) {
            Toast.makeText(this, "Geçerli nozul çapı (mm) girin", Toast.LENGTH_SHORT).show()
            return
        }
        val mmPerPx = knownDia / (2.0 * r.nozzleRadiusPx)
        processor.mmPerPx = mmPerPx
        getSharedPreferences("cal", MODE_PRIVATE).edit()
            .putFloat("mmPerPx", mmPerPx.toFloat())
            .putFloat("knownDia", knownDia)
            .apply()
        Toast.makeText(this, String.format(Locale.US, "Kalibre: %.5f mm/px", mmPerPx), Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        phoneCam?.release()
        processor.release()
    }
}
