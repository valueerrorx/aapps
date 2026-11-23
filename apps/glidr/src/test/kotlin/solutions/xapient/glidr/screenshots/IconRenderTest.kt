package solutions.xapient.glidr.screenshots

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import solutions.xapient.glidr.R
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

// Adaptives Icon in den gaengigen Launcher-Masken, Monochrom, Store-Icon (512 px) und Splash bis Android 11
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IconRenderTest {
    private val ctx get() = RuntimeEnvironment.getApplication()
    private val options = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f))

    private enum class Mask { CIRCLE, SQUIRCLE, ROUNDED, NONE }

    private fun maskPath(mask: Mask, size: Float): Path? = when (mask) {
        Mask.NONE -> null
        Mask.CIRCLE -> Path().apply { addCircle(size / 2, size / 2, size / 2, Path.Direction.CW) }
        Mask.ROUNDED -> Path().apply { addRoundRect(0f, 0f, size, size, size * 0.2f, size * 0.2f, Path.Direction.CW) }
        Mask.SQUIRCLE -> Path().apply {
            // Superellipse |x|^4 + |y|^4 = 1
            val n = 240
            for (i in 0..n) {
                val t = 2.0 * Math.PI * i / n
                val c = cos(t); val s = sin(t)
                val x = sign(c) * abs(c).pow(0.5)
                val y = sign(s) * abs(s).pow(0.5)
                val px = (size / 2 * (1 + x)).toFloat()
                val py = (size / 2 * (1 + y)).toFloat()
                if (i == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }
    }

    // Ebenen sind 108 dp, sichtbar ist die Mitte von 72 dp
    private fun drawIcon(canvas: Canvas, x: Float, y: Float, size: Int, mask: Mask, mono: Boolean) {
        canvas.save()
        canvas.translate(x, y)
        maskPath(mask, size.toFloat())?.let { canvas.clipPath(it) }
        val full = size * 3 / 2
        val off = -size / 4
        if (mono) {
            canvas.drawColor(0xFFD9E8F5.toInt())
            ctx.getDrawable(R.drawable.ic_launcher_monochrome)!!.apply {
                setTint(0xFF1A2A3A.toInt())
                setBounds(off, off, off + full, off + full)
                draw(canvas)
            }
        } else {
            for (id in listOf(R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground)) {
                ctx.getDrawable(id)!!.apply {
                    setBounds(off, off, off + full, off + full)
                    draw(canvas)
                }
            }
        }
        canvas.restore()
    }

    @Test
    fun icon_masks() {
        val size = 192
        val pad = 40
        val variants = listOf(
            Mask.CIRCLE to false, Mask.SQUIRCLE to false, Mask.ROUNDED to false, Mask.CIRCLE to true,
        )
        val w = variants.size * (size + pad) + pad
        val h = size + 2 * pad
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        // Wallpaper-artiger Hintergrund, damit die Masken sichtbar werden
        canvas.drawPaint(Paint().apply {
            shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), 0xFF3A4A5C.toInt(), 0xFF6E8296.toInt(), Shader.TileMode.CLAMP)
        })
        variants.forEachIndexed { i, (mask, mono) ->
            drawIcon(canvas, (pad + i * (size + pad)).toFloat(), pad.toFloat(), size, mask, mono)
        }
        bmp.captureRoboImage("screenshots/icon_masks.png", options)
    }

    @Test
    fun icon_store_512() {
        val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        drawIcon(Canvas(bmp), 0f, 0f, 512, Mask.NONE, false)
        bmp.captureRoboImage("screenshots/icon_store_512.png", options)
    }

    // windowBackground bis Android 11, Querformat 800x480 bei mdpi
    @Test
    @Config(qualifiers = "w800dp-h480dp-land-mdpi")
    fun splash_pre31() {
        val bmp = Bitmap.createBitmap(800, 480, Bitmap.Config.ARGB_8888)
        ctx.getDrawable(R.drawable.splash)!!.apply {
            setBounds(0, 0, 800, 480)
            draw(Canvas(bmp))
        }
        bmp.captureRoboImage("screenshots/splash_pre31.png", options)
    }
}
