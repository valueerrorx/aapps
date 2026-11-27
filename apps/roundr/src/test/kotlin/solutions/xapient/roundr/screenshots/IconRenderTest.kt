package solutions.xapient.roundr.screenshots

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import solutions.xapient.roundr.R
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

// Adaptives Icon in den gaengigen Launcher-Masken, Monochrom und Store-Icon (512 px)
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
            canvas.drawColor(0xFFFFE2D6.toInt())
            ctx.getDrawable(R.drawable.ic_launcher_monochrome)!!.apply {
                setTint(0xFF4A1A0A.toInt())
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
            shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), 0xFF3A4A5C.toInt(), 0xFF7C8C76.toInt(), Shader.TileMode.CLAMP)
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

    // iOS-App-Icon: 1024 px, randlos, ohne Alpha (iOS legt die Maske selbst an); nur beim Aufnehmen
    @Test
    fun icon_ios_1024() {
        if (System.getProperty("roborazzi.test.record") != "true") return
        val bmp = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888)
        drawIcon(Canvas(bmp), 0f, 0f, 1024, Mask.NONE, false)
        val px = IntArray(1024 * 1024)
        bmp.getPixels(px, 0, 1024, 0, 0, 1024, 1024)
        File("../../iosApp/roundr/roundr/Assets.xcassets/AppIcon.appiconset/icon-1024.png").writeBytes(rgbPng(px, 1024, 1024))
    }

    // Minimaler PNG-Schreiber, Farbtyp 2 (RGB, 8 bit)
    private fun rgbPng(px: IntArray, w: Int, h: Int): ByteArray {
        val raw = ByteArrayOutputStream()
        for (y in 0 until h) {
            raw.write(0)
            for (x in 0 until w) {
                val c = px[y * w + x]
                raw.write(c shr 16 and 0xFF); raw.write(c shr 8 and 0xFF); raw.write(c and 0xFF)
            }
        }
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 13, 10, 26, 10))
        fun chunk(type: String, data: ByteArray) {
            val d = DataOutputStream(out)
            d.writeInt(data.size)
            val td = type.toByteArray() + data
            d.write(td)
            d.writeInt(CRC32().apply { update(td) }.value.toInt())
        }
        val ihdr = ByteArrayOutputStream().also { DataOutputStream(it).apply { writeInt(w); writeInt(h); write(byteArrayOf(8, 2, 0, 0, 0)) } }
        chunk("IHDR", ihdr.toByteArray())
        val z = ByteArrayOutputStream()
        DeflaterOutputStream(z, Deflater(9)).use { it.write(raw.toByteArray()) }
        chunk("IDAT", z.toByteArray())
        chunk("IEND", ByteArray(0))
        return out.toByteArray()
    }
}
