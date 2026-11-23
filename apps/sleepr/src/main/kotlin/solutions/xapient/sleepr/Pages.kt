package solutions.xapient.sleepr

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.core.net.toUri

const val SOURCE_URL = "https://github.com/valueerrorx/aapps"

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
                    addView(actionRow(a.getString(R.string.source), SOURCE_URL.removePrefix("https://")) {
                        try {
                            a.startActivity(Intent(Intent.ACTION_VIEW, SOURCE_URL.toUri()))
                        } catch (_: ActivityNotFoundException) {
                            // kein Browser installiert: nichts tun
                        }
                    })
                }
                var top = 28
                for (s in intArrayOf(R.string.note_privacy, R.string.note_background, R.string.license, R.string.fonts)) {
                    addView(small(a.getString(s)), lp(top = top)); top = 10
                }
            }
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
