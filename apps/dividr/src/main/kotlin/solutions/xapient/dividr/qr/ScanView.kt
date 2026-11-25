package solutions.xapient.dividr.qr

import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.util.concurrent.Executors

// Liest QR-Codes aus dem Y-Kanal; nur dieser Teil kennt CameraX
private class QrAnalyzer(private val onText: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val reader = QRCodeReader()
    private val hints = mapOf(
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
    )

    override fun analyze(image: ImageProxy) {
        try {
            val plane = image.planes[0]
            val buf = plane.buffer
            val data = ByteArray(buf.remaining())
            buf.get(data)
            val src = PlanarYUVLuminanceSource(data, plane.rowStride, image.height, 0, 0, image.width, image.height, false)
            onText(reader.decode(BinaryBitmap(HybridBinarizer(src)), hints).text)
        } catch (_: Exception) {
            // kein Code im Bild
        } finally {
            reader.reset()
            image.close()
        }
    }
}

@Composable
fun CameraScan(onText: (String) -> Unit, onError: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val text = rememberUpdatedState(onText)
    val error = rememberUpdatedState(onError)
    val view = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    AndroidView({ view }, modifier)
    DisposableEffect(owner) {
        val executor = Executors.newSingleThreadExecutor()
        val main = ContextCompat.getMainExecutor(context)
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        var gone = false
        future.addListener({
            if (gone) return@addListener
            try {
                val p = future.get()
                provider = p
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                // hohe Aufloesung: dichte Codes bis Version 40
                val selector = ResolutionSelector.Builder()
                    .setResolutionStrategy(ResolutionStrategy(Size(1920, 1080), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                    .build()
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setResolutionSelector(selector)
                    .build()
                analysis.setAnalyzer(executor, QrAnalyzer { t -> main.execute { if (!gone) text.value(t) } })
                val camera = if (p.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
                p.unbindAll()
                p.bindToLifecycle(owner, camera, preview, analysis)
            } catch (_: Exception) {
                error.value()
            }
        }, main)
        onDispose {
            gone = true
            provider?.unbindAll()
            executor.shutdown()
        }
    }
}
