package solutions.xapient.sleepr

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
import androidx.core.net.toUri
import solutions.xapient.aapps.core.billing.GateState
import solutions.xapient.aapps.core.billing.Notice
import solutions.xapient.aapps.core.billing.R as BR

const val SOURCE_URL = "https://github.com/valueerrorx/aapps"
private const val ON_ACCENT = 0xFF0D1A20.toInt()

/** Einstellungen und Info als Views, Aufbau wie SettingsScreen/AboutScreen aus core/ui */
class Pages(private val a: Activity, private val look: Look) {
    private val dp = { v: Int -> with(look) { v.dp } }

    private fun page(top: View, padH: Int, fill: LinearLayout.() -> Unit): View {
        val col = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(padH), 0, dp(padH), dp(32))
            fill()
        }
        val scroll = ScrollView(a).apply { isFillViewport = true; isVerticalScrollBarEnabled = false; addView(col) }
        return LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(look.bg)
            addView(top, look.lp())
            addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }
    }

    fun settings(display: Display, onPreset: (Preset) -> Unit, onChanged: () -> Unit, onAbout: () -> Unit, onBack: () -> Unit): View =
        page(look.topBar(a.getString(R.string.settings), onBack, a.getString(R.string.back)), 16) {
            with(look) {
                val first = 0
                group(a.getString(R.string.group_display), this@page, first).apply {
                    addView(toggleRow(a.getString(R.string.show_waves), a.getString(R.string.show_waves_sub), display.showWaves) { display.showWaves = it; onChanged() })
                    addView(divider())
                    addView(toggleRow(a.getString(R.string.show_mixes), a.getString(R.string.show_mixes_sub), display.showMixes) { display.showMixes = it; onChanged() })
                }
                group(a.getString(R.string.group_mixes), this@page).apply {
                    addView(small(a.getString(R.string.explain_mixes)).apply { setPadding(dp(18), dp(14), dp(18), dp(4)) })
                    Mixer.PRESETS.forEach { p ->
                        addView(divider())
                        addView(actionRow(a.getString(p.name), Mixer.describe(a, p), Mixer.matches(p)) { onPreset(p) })
                    }
                }
                group(a.getString(R.string.group_colors), this@page).apply {
                    addView(body(a.getString(R.string.explain_colors), muted).apply { setPadding(dp(18), dp(16), dp(18), dp(4)) })
                    val hints = intArrayOf(R.string.explain_white, R.string.explain_pink, R.string.explain_brown, R.string.explain_grey, R.string.explain_green)
                    Mixer.COLORS.forEachIndexed { i, c ->
                        val name = a.getString(c.name); val slope = a.getString(c.slope)
                        addView(LinearLayout(a).apply {
                            orientation = LinearLayout.HORIZONTAL
                            setPadding(dp(18), dp(10), dp(18), if (i == 4) dp(16) else dp(2))
                            addView(View(a).apply { background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(c.color) } },
                                LinearLayout.LayoutParams(dp(10), dp(10)).apply { topMargin = dp(6); rightMargin = dp(12) })
                            addView(LinearLayout(a).apply {
                                orientation = LinearLayout.VERTICAL
                                addView(body("$name · $slope"))
                                addView(small(a.getString(hints[i])))
                            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                            // Farbe als eine Ansage
                            isFocusable = true
                            contentDescription = "$name, $slope: ${a.getString(hints[i])}"
                        })
                    }
                }
                group(a.getString(R.string.group_waves), this@page).apply {
                    addView(body(a.getString(R.string.explain_waves), muted).apply { setPadding(dp(18), dp(16), dp(18), dp(16)) })
                }
                group(a.getString(R.string.group_volume), this@page).apply {
                    addView(body(a.getString(R.string.explain_volume), muted).apply { setPadding(dp(18), dp(16), dp(18), dp(16)) })
                }
                group(a.getString(R.string.group_timer), this@page).apply {
                    addView(body(a.getString(R.string.explain_timer), muted).apply { setPadding(dp(18), dp(16), dp(18), dp(16)) })
                }
                group(null, this@page).apply {
                    addView(actionRow(a.getString(R.string.about)) { onAbout() })
                }
            }
        }

    fun about(onBack: () -> Unit): View =
        page(look.topBar(null, onBack, a.getString(R.string.back)), 24) {
            with(look) {
                addView(ImageView(a).apply {
                    setImageResource(R.drawable.ic_launcher_about)
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }, LinearLayout.LayoutParams(dp(88), dp(88)))
                addView(headline(a.getString(R.string.app_name)).apply { accessibilityHeading() }, lp(top = 20))
                addView(number(a.getString(R.string.version, BuildConfig.VERSION_NAME), 13.2f, accent), lp(top = 4))
                addView(body(a.getString(R.string.tagline), muted), lp(top = 16))
                group(null, this@page, 28).apply {
                    setPadding(0, dp(10), 0, dp(10))
                    for (s in intArrayOf(R.string.promise_ads, R.string.promise_tracking, R.string.promise_internet)) addView(promise(a.getString(s)))
                }
                group(null, this@page).apply {
                    addView(actionRow(a.getString(R.string.source), SOURCE_URL.removePrefix("https://")) { openSource() })
                }
                var top = 28
                for (s in intArrayOf(R.string.note_privacy, R.string.note_background, R.string.license, R.string.fonts)) {
                    addView(small(a.getString(s)), lp(top = top)); top = 10
                }
            }
        }

    /** Dankes-Bildschirm nach der Testphase, Aufbau wie ThanksScreen aus core/ui */
    fun thanks(state: GateState, onBuy: () -> Unit, onRestore: () -> Unit): View =
        page(Space(a), 24) {
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(paddingLeft, dp(32), paddingRight, paddingBottom)
            with(look) {
                fun center(v: TextView) = v.apply { gravity = Gravity.CENTER; maxWidth = dp(480) }
                // Hero: Schloss im Akzent-Halo
                addView(ImageView(a).apply {
                    setImageResource(R.drawable.ic_unlock)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setPadding(dp(32), dp(32), dp(32), dp(32))
                    imageTintList = ColorStateList.valueOf(accent)
                    background = LayerDrawable(arrayOf(
                        GradientDrawable().apply { gradientType = GradientDrawable.RADIAL_GRADIENT; gradientRadius = dp(56).toFloat()
                            colors = intArrayOf(alpha(accent, 0.3f), alpha(accent, 0f)) },
                        GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(surface); setStroke(dp(1) + dp(1) / 2, accent) },
                    ))
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }, LinearLayout.LayoutParams(dp(112), dp(112)))
                addView(label(a.getString(R.string.app_name), accent), lp(w = ViewGroup.LayoutParams.WRAP_CONTENT, top = 24))
                addView(center(headline(a.getString(BR.string.billing_title))).apply { accessibilityHeading() },
                    lp(w = ViewGroup.LayoutParams.WRAP_CONTENT, top = 6))
                addView(center(body(a.getString(BR.string.billing_text), muted)), lp(w = ViewGroup.LayoutParams.WRAP_CONTENT, top = 14))
                group(null, this@page, 24).apply {
                    setPadding(0, dp(6), 0, dp(6))
                    addView(point(R.drawable.ic_shield, a.getString(BR.string.billing_point_data)))
                    addView(point(R.drawable.ic_offline, a.getString(BR.string.billing_point_offline)))
                    addView(point(R.drawable.ic_code, a.getString(BR.string.billing_point_source)))
                }
                val price = state.price
                val canBuy = price != null && !state.pending
                addView(pill(if (price != null) a.getString(BR.string.billing_unlock_price, price) else a.getString(BR.string.billing_unlock), canBuy, onBuy),
                    lp(w = ViewGroup.LayoutParams.WRAP_CONTENT, top = 28))
                val hint = when {
                    state.pending -> BR.string.billing_hint_pending
                    state.notice == Notice.OFFLINE -> BR.string.billing_hint_offline
                    state.notice == Notice.NOT_FOUND -> BR.string.billing_hint_notfound
                    state.notice == Notice.UNAVAILABLE -> BR.string.billing_hint_unavailable
                    price == null -> BR.string.billing_hint_noprice
                    else -> null
                }
                if (hint != null) addView(center(body(a.getString(hint))), lp(w = ViewGroup.LayoutParams.WRAP_CONTENT, top = 14))
                addView(label(a.getString(if (state.busy) BR.string.billing_restoring else BR.string.billing_restore)).apply {
                    gravity = Gravity.CENTER; minHeight = dp(48); setPadding(dp(16), dp(15), dp(16), dp(15))
                    isClickable = true; isFocusable = true
                    springPress(this, 0.94f); asButton(this)
                    setOnClickListener { press(it); if (!state.busy) onRestore() }
                }, lp(w = ViewGroup.LayoutParams.WRAP_CONTENT, top = 8))
                addView(center(small(a.getString(BR.string.billing_once))), lp(w = ViewGroup.LayoutParams.WRAP_CONTENT, top = 12))
            }
        }

    private fun openSource() {
        try {
            a.startActivity(Intent(Intent.ACTION_VIEW, SOURCE_URL.toUri()))
        } catch (_: ActivityNotFoundException) {
            // kein Browser installiert: nichts tun
        }
    }

    private fun point(icon: Int, text: String) = LinearLayout(a).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(48)
        setPadding(dp(18), dp(10), dp(18), dp(10))
        addView(ImageView(a).apply {
            setImageResource(icon)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dp(7), dp(7), dp(7), dp(7))
            imageTintList = ColorStateList.valueOf(look.accent)
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(look.alpha(look.accent, 0.14f)) }
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(32), dp(32)).apply { rightMargin = dp(14) })
        addView(look.body(text), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    }

    // Hauptaktion wie AappsPillButton: Akzent gefuellt, wenn kaufbar
    private fun pill(text: String, enabled: Boolean, onClick: () -> Unit) = LinearLayout(a).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
        minimumHeight = dp(52)
        setPadding(dp(24), dp(12), dp(24), dp(12))
        val fg = if (enabled) ON_ACCENT else look.faint
        background = GradientDrawable().apply {
            cornerRadius = dp(100).toFloat()
            setColor(if (enabled) look.accent else look.surface); setStroke(dp(1), if (enabled) look.accent else look.line)
        }
        val fs = a.resources.configuration.fontScale.coerceAtMost(2f)
        addView(ImageView(a).apply {
            setImageResource(R.drawable.ic_unlock)
            imageTintList = ColorStateList.valueOf(fg)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams((dp(20) * fs).toInt(), (dp(20) * fs).toInt()).apply { rightMargin = dp(10) })
        addView(look.label(text, fg).apply { textSize = 13.8f })
        isEnabled = enabled; isClickable = enabled; isFocusable = true
        contentDescription = text
        look.springPress(this, 0.95f); look.asButton(this)
        setOnClickListener { look.press(it); onClick() }
    }

    private fun promise(text: String) = LinearLayout(a).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(18), dp(10), dp(18), dp(10))
        addView(ImageView(a).apply {
            setImageResource(R.drawable.ic_check)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dp(6), dp(6), dp(6), dp(6))
            imageTintList = ColorStateList.valueOf(look.accent)
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(look.alpha(look.accent, 0.14f)) }
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(28), dp(28)).apply { rightMargin = dp(14) })
        addView(look.body(text), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    }
}
