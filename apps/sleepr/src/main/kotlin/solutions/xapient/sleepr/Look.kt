package solutions.xapient.sleepr

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.StateListAnimator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.widget.TextViewCompat

/** Aussehen der aapps (docs/DESIGN.md, core/ui) als Views nachgebaut, weil sleepr kein Compose nutzt */
class Look(private val ctx: Context) {
    val bg = 0xFF10161B.toInt()
    val surface = 0xFF172028.toInt()
    val surfaceHigh = 0xFF1E2932.toInt()
    val line = 0xFF26323C.toInt()
    val fg = 0xFFDCE3E8.toInt()
    val muted = 0xFF8A97A2.toInt()
    val faint = 0xFF55626D.toInt()
    val accent = 0xFF86B7CC.toInt()

    private val res = ctx.resources
    val inter: Typeface = res.getFont(R.font.inter_regular)
    val interSemi: Typeface = res.getFont(R.font.inter_semibold)
    val groteskMedium: Typeface = res.getFont(R.font.space_grotesk_medium)
    val groteskBold: Typeface = res.getFont(R.font.space_grotesk_bold)

    val Int.dp get() = (this * res.displayMetrics.density).toInt()
    private fun sp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, res.displayMetrics).toInt()

    fun alpha(c: Int, a: Float) = (c and 0x00FFFFFF) or ((a * 255).toInt() shl 24)

    // ---------- Schrift wie Aapps.type ----------
    private fun base(s: CharSequence, tf: Typeface, size: Float, color: Int) = TextView(ctx).apply {
        text = s; typeface = tf; textSize = size; setTextColor(color); includeFontPadding = false
    }

    fun title(s: CharSequence, color: Int = fg) = base(s, interSemi, 18f, color)
    fun body(s: CharSequence, color: Int = fg) = base(s, inter, 15f, color).apply { TextViewCompat.setLineHeight(this, sp(22f)) }
    // Untertitel und Kleingedrucktes: body mit 1,1 × Label-Größe
    fun small(s: CharSequence, color: Int = muted) = base(s, inter, 13.2f, color).apply { TextViewCompat.setLineHeight(this, sp(22f)) }
    fun label(s: String, color: Int = muted) = base(s.uppercase(), interSemi, 12f, color).apply { letterSpacing = 0.14f }
    fun headline(s: CharSequence, color: Int = fg) = base(s, groteskBold, 34f, color).apply { letterSpacing = -0.02f }
    fun number(s: CharSequence, size: Float, color: Int) = base(s, groteskMedium, size, color).apply { fontFeatureSettings = "tnum" }

    fun lp(w: Int = ViewGroup.LayoutParams.MATCH_PARENT, h: Int = ViewGroup.LayoutParams.WRAP_CONTENT, top: Int = 0) =
        LinearLayout.LayoutParams(w, h).apply { topMargin = top.dp }

    // Tasten-Haptik über die View, folgt der System-Touch-Haptik (wie press() in core/ui)
    fun press(v: View) = v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

    // Kurz eindrücken, federnd zurück (Feder statt Ripple)
    fun springPress(v: View, scale: Float) {
        fun anim(s: Float, ms: Long, ip: TimeInterpolator) = ObjectAnimator.ofPropertyValuesHolder(v,
            PropertyValuesHolder.ofFloat(View.SCALE_X, s), PropertyValuesHolder.ofFloat(View.SCALE_Y, s)).apply { duration = ms; interpolator = ip }
        v.stateListAnimator = StateListAnimator().apply {
            addState(intArrayOf(android.R.attr.state_pressed), anim(scale, 90, DecelerateInterpolator()))
            addState(intArrayOf(), anim(1f, 260, OvershootInterpolator(3f)))
        }
    }

    fun asButton(v: View) {
        v.accessibilityDelegate = object : View.AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(host, info); info.className = Button::class.java.name
            }
        }
    }

    /** Rundes 48-dp-Symbol wie AappsIconButton (Zahnrad, Zurück) */
    fun iconButton(icon: Int, desc: String, onClick: () -> Unit) = ImageView(ctx).apply {
        setImageResource(icon)
        scaleType = ImageView.ScaleType.CENTER
        imageTintList = ColorStateList.valueOf(muted)
        contentDescription = desc
        isClickable = true; isFocusable = true
        background = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(surfaceHigh) })
            addState(intArrayOf(android.R.attr.state_focused), GradientDrawable().apply { shape = GradientDrawable.OVAL; setStroke(2.dp, fg) })
            addState(intArrayOf(), 0.toDrawable())
        }
        springPress(this, 0.88f)
        asButton(this)
        setOnClickListener { press(it); onClick() }
    }

    /** Kopfzeile wie AappsTopBar: 64 dp, Zurück links, Titel in Inter SemiBold 18 sp */
    fun topBar(heading: String?, onBack: () -> Unit, backDesc: String) = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        minimumHeight = 64.dp
        setPadding(8.dp, 0, 8.dp, 0)
        addView(iconButton(R.drawable.ic_back, backDesc, onBack), LinearLayout.LayoutParams(48.dp, 48.dp))
        if (heading != null) {
            addView(title(heading).apply { maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END; isFocusable = false
                accessibilityHeading() },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = 8.dp; rightMargin = 8.dp })
        }
    }

    fun TextView.accessibilityHeading() { if (android.os.Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true }

    /** Gruppe wie SettingsGroup: Versal-Label, darunter Fläche mit 22-dp-Ecken */
    fun group(name: String?, into: LinearLayout, top: Int = 20): LinearLayout {
        if (name != null) {
            into.addView(label(name).apply { accessibilityHeading() },
                lp(top = top).apply { leftMargin = 12.dp; bottomMargin = 8.dp })
        }
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply { setColor(surface); setStroke(1.dp, alpha(line, 0.6f)); cornerRadius = 22.dp.toFloat() }
            clipToOutline = true
        }
        into.addView(card, lp(top = if (name != null) 0 else top))
        return card
    }

    fun divider() = View(ctx).apply { setBackgroundColor(alpha(line, 0.6f)) }.let { v ->
        LinearLayout(ctx).apply { setPadding(18.dp, 0, 18.dp, 0); addView(v, lp(h = 1.dp)) }
    }

    private fun rowTexts(title: String, sub: String?) = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        addView(body(title))
        if (sub != null) addView(small(sub), lp(top = 2))
    }

    private fun row() = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        minimumHeight = 60.dp
        setPadding(18.dp, 12.dp, 18.dp, 12.dp)
        isClickable = true; isFocusable = true
        background = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), surfaceHigh.toDrawable())
            addState(intArrayOf(android.R.attr.state_focused), surfaceHigh.toDrawable())
            addState(intArrayOf(), 0.toDrawable())
        }
    }

    /** Zeile wie SettingsAction: Titel, Untertitel, Pfeil (oder Haken bei aktiver Auswahl) */
    fun actionRow(title: String, sub: String? = null, checked: Boolean = false, onClick: () -> Unit) = row().apply {
        addView(rowTexts(title, sub), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addView(ImageView(ctx).apply {
            setImageResource(if (checked) R.drawable.ic_check else R.drawable.ic_chevron)
            imageTintList = ColorStateList.valueOf(if (checked) accent else muted)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(20.dp, 20.dp).apply { leftMargin = 12.dp })
        if (checked) stateDescriptionCompat(this, ctx.getString(R.string.state_active))
        asButton(this)
        setOnClickListener { press(it); onClick() }
    }

    /** Zeile wie SettingsToggle; für TalkBack ein Schalter mit Zustand */
    fun toggleRow(title: String, sub: String?, checked: Boolean, onChange: (Boolean) -> Unit) = row().apply {
        clipChildren = false // Halo des Knopfs darf über den Schalter hinaus
        val sw = SwitchView(ctx, this@Look, checked).apply { importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }
        addView(rowTexts(title, sub), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addView(Space(ctx), LinearLayout.LayoutParams(12.dp, 0))
        addView(sw, LinearLayout.LayoutParams(48.dp, 28.dp))
        accessibilityDelegate = object : View.AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.className = android.widget.Switch::class.java.name
                info.isCheckable = true; info.isChecked = sw.checked
            }
        }
        setOnClickListener {
            press(it)
            sw.set(!sw.checked)
            onChange(sw.checked)
        }
    }

    fun stateDescriptionCompat(v: View, s: String) { if (android.os.Build.VERSION.SDK_INT >= 30) v.stateDescription = s }

    /** Schalter wie AappsSwitch: 48 × 28 dp, Knopf mit Glow in Akzentfarbe */
    class SwitchView(ctx: Context, private val look: Look, initial: Boolean) : View(ctx) {
        var checked = initial; private set
        private var t = if (initial) 1f else 0f
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var anim: ValueAnimator? = null
        private var halo: RadialGradient? = null

        fun set(on: Boolean) {
            if (on == checked) return
            checked = on
            anim?.cancel()
            anim = ValueAnimator.ofFloat(t, if (on) 1f else 0f).apply {
                duration = 280; interpolator = OvershootInterpolator(1.2f)
                addUpdateListener { t = it.animatedValue as Float; invalidate() }
                start()
            }
        }

        override fun onDraw(c: Canvas) {
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat(); val r = h / 2
            val k = t.coerceIn(0f, 1f)
            paint.shader = null
            paint.style = Paint.Style.FILL
            paint.color = blend(look.bg, look.alpha(look.accent, 0.22f), k)
            c.drawRoundRect(0f, 0f, w, h, r, r, paint)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = d
            paint.color = if (checked) look.alpha(look.accent, 0.5f) else look.line
            c.drawRoundRect(d / 2, d / 2, w - d / 2, h - d / 2, r, r, paint)
            val cx = 4 * d + 10 * d + 20 * d * t; val cy = h / 2
            // weicher Halo um den Knopf, nur im An-Zustand; Verlauf einmal angelegt, Stärke über Alpha
            if (k > 0f) {
                val g = halo ?: RadialGradient(0f, 0f, 22 * d,
                    intArrayOf(look.alpha(look.accent, 0.5f), look.alpha(look.accent, 0.225f), look.alpha(look.accent, 0.06f), look.alpha(look.accent, 0f)),
                    floatArrayOf(0f, 0.35f, 0.7f, 1f), Shader.TileMode.CLAMP).also { halo = it }
                paint.style = Paint.Style.FILL
                paint.shader = g; paint.alpha = (k * 255).toInt()
                c.save(); c.translate(cx, cy); c.drawCircle(0f, 0f, 22 * d, paint); c.restore()
                paint.shader = null; paint.alpha = 255
            }
            paint.style = Paint.Style.FILL
            paint.color = blend(look.faint, look.accent, k)
            c.drawCircle(cx, cy, 10 * d, paint)
        }

        private fun blend(a: Int, b: Int, f: Float): Int {
            fun ch(s: Int) = ((a shr s and 0xFF) * (1 - f) + (b shr s and 0xFF) * f).toInt() shl s
            return ch(24) or ch(16) or ch(8) or ch(0)
        }
    }
}
