package com.example.screentime

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class AppUsageActivity : Activity() {

    private val bg = Color.parseColor("#0F1115")
    private val card = Color.parseColor("#1A1D24")
    private val track = Color.parseColor("#2A2F3A")
    private val textMain = Color.parseColor("#F2F4F8")
    private val textDim = Color.parseColor("#9AA3B2")
    private val accent = Color.parseColor("#4F8CFF")

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radiusDp: Int): GradientDrawable {
        val drawable = GradientDrawable()
        drawable.setColor(color)
        drawable.cornerRadius = dp(radiusDp).toFloat()
        return drawable
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean = false): TextView {
        val view = TextView(this)
        view.text = text
        view.textSize = size
        view.setTextColor(color)
        if (bold) view.setTypeface(null, Typeface.BOLD)
        return view
    }

    private fun formatTime(ms: Long): String {
        val minutes = ms / 60000
        return if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
    }

    private fun launcherPackage(): String? {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val info = packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
        return info?.activityInfo?.packageName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        actionBar?.hide()
        window.statusBarColor = bg
        window.navigationBarColor = bg
        var flags = window.decorView.systemUiVisibility
        flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        flags = flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        window.decorView.systemUiVisibility = flags

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(20), dp(48), dp(20), dp(32))

        root.addView(label("App usage today", 28f, textMain, true))

        val perApp = UsageHelper.todayPerAppMs(this)
        val homePkg = launcherPackage()

        val rows = ArrayList<Triple<String, String, Long>>()
        for ((pkg, ms) in perApp) {
            if (pkg == homePkg) continue
            if (ms < 60000L) continue
            try {
                val appName = packageManager.getApplicationInfo(pkg, 0)
                    .loadLabel(packageManager).toString()
                rows.add(Triple(appName, pkg, ms))
            } catch (e: Exception) {
            }
        }
        rows.sortByDescending { it.third }

        var totalMs = 0L
        for (row in rows) totalMs += row.third

        val subtitle = label("Total across apps: ${formatTime(totalMs)}", 15f, textDim)
        val subtitleParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        subtitleParams.bottomMargin = dp(8)
        root.addView(subtitle, subtitleParams)

        if (rows.isEmpty()) {
            root.addView(
                label(
                    "No app usage recorded yet today. Make sure usage access is allowed.",
                    16f,
                    textDim
                )
            )
        }

        val topMs = if (rows.isNotEmpty()) rows[0].third else 1L

        for (row in rows) {
            val item = LinearLayout(this)
            item.orientation = LinearLayout.HORIZONTAL
            item.gravity = Gravity.CENTER_VERTICAL
            item.background = rounded(card, 16)
            item.setPadding(dp(16), dp(14), dp(16), dp(14))

            val icon = ImageView(this)
            try {
                icon.setImageDrawable(packageManager.getApplicationIcon(row.second))
            } catch (e: Exception) {
            }
            item.addView(icon, LinearLayout.LayoutParams(dp(40), dp(40)))

            val middle = LinearLayout(this)
            middle.orientation = LinearLayout.VERTICAL
            middle.addView(label(row.first, 16f, textMain, true))

            val bar = LinearLayout(this)
            bar.orientation = LinearLayout.HORIZONTAL
            bar.background = rounded(track, 4)
            val fill = View(this)
            fill.background = rounded(accent, 4)
            val empty = View(this)
            val percent = (row.third * 100 / topMs).toFloat()
            bar.addView(fill, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, percent))
            bar.addView(empty, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 100f - percent))
            val barParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8))
            barParams.topMargin = dp(8)
            middle.addView(bar, barParams)

            val middleParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            middleParams.leftMargin = dp(14)
            middleParams.rightMargin = dp(14)
            item.addView(middle, middleParams)

            item.addView(label(formatTime(row.third), 16f, textMain, true))

            val itemParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            itemParams.topMargin = dp(10)
            root.addView(item, itemParams)
        }

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(bg)
        scroll.isFillViewport = true
        scroll.addView(root)
        setContentView(scroll)
    }
}
