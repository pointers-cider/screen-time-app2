package com.example.screentime

import android.Manifest
import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val bg = Color.parseColor("#0F1115")
    private val card = Color.parseColor("#1A1D24")
    private val textMain = Color.parseColor("#F2F4F8")
    private val textDim = Color.parseColor("#9AA3B2")
    private val accent = Color.parseColor("#4F8CFF")
    private val warn = Color.parseColor("#FFB020")
    private val danger = Color.parseColor("#FF5C5C")
    private val good = Color.parseColor("#3DDC97")

    private lateinit var usedText: TextView
    private lateinit var statusText: TextView
    private lateinit var barFill: View
    private lateinit var barEmpty: View
    private lateinit var usageButton: Button
    private lateinit var overlayButton: Button
    private lateinit var limitInput: EditText
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var controlsRow: LinearLayout

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

    private fun styledButton(text: String, fill: Int, textColor: Int): Button {
        val button = Button(this)
        button.text = text
        button.isAllCaps = false
        button.textSize = 16f
        button.setTextColor(textColor)
        button.background = rounded(fill, 14)
        button.stateListAnimator = null
        button.setPadding(dp(16), dp(14), dp(16), dp(14))
        return button
    }

    private fun cardLayout(): LinearLayout {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.background = rounded(card, 20)
        layout.setPadding(dp(20), dp(20), dp(20), dp(20))
        return layout
    }

    private fun addTo(parent: LinearLayout, child: View, topDp: Int) {
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = dp(topDp)
        parent.addView(child, params)
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

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(20), dp(48), dp(20), dp(32))

        root.addView(label("Screen Time", 30f, textMain, true))
        root.addView(label("Stay in control of your day", 15f, textDim))

        // Usage card with progress bar
        val usageCard = cardLayout()
        usageCard.addView(label("TODAY", 12f, textDim, true))
        usedText = label("--", 44f, textMain, true)
        usageCard.addView(usedText)

        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.background = rounded(Color.parseColor("#2A2F3A"), 6)
        barFill = View(this)
        barEmpty = View(this)
        bar.addView(barFill, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0f))
        bar.addView(barEmpty, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 100f))
        val barParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(12))
        barParams.topMargin = dp(12)
        usageCard.addView(bar, barParams)

        statusText = label("", 16f, textDim)
        addTo(usageCard, statusText, 12)
        addTo(root, usageCard, 20)

        // Permission buttons (only shown when needed)
        usageButton = styledButton("Step 1: Allow usage access", warn, Color.parseColor("#1A1300"))
        usageButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        addTo(root, usageButton, 16)

        overlayButton = styledButton("Step 2: Allow display over other apps", warn, Color.parseColor("#1A1300"))
        overlayButton.setOnClickListener {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }
        addTo(root, overlayButton, 10)

        // Limit card
        val limitCard = cardLayout()
        limitCard.addView(label("Daily limit", 16f, textMain, true))
        addTo(limitCard, label("Minutes allowed per day", 13f, textDim), 2)

        limitInput = EditText(this)
        limitInput.hint = "e.g. 120"
        limitInput.setHintTextColor(textDim)
        limitInput.setTextColor(textMain)
        limitInput.textSize = 18f
        limitInput.inputType = InputType.TYPE_CLASS_NUMBER
        limitInput.background = rounded(Color.parseColor("#252A34"), 12)
        limitInput.setPadding(dp(16), dp(12), dp(16), dp(12))
        val saved = getPrefs().getInt("limit_minutes", 0)
        if (saved > 0) limitInput.setText(saved.toString())
        addTo(limitCard, limitInput, 12)

        val saveButton = styledButton("Save limit", accent, Color.WHITE)
        saveButton.setOnClickListener {
            val value = limitInput.text.toString().toIntOrNull() ?: 0
            getPrefs().edit().putInt("limit_minutes", value).apply()
            Toast.makeText(this, "Limit saved", Toast.LENGTH_SHORT).show()
            refresh()
        }
        addTo(limitCard, saveButton, 12)
        addTo(root, limitCard, 16)

        // App usage today
        val appUsageButton = styledButton("App usage today", card, textMain)
        appUsageButton.setOnClickListener {
            startActivity(Intent(this, AppUsageActivity::class.java))
        }
        addTo(root, appUsageButton, 16)

        // Allowed apps
        val chooseButton = styledButton("Choose allowed apps", card, textMain)
        chooseButton.setOnClickListener {
            startActivity(Intent(this, AllowedAppsActivity::class.java))
        }
        addTo(root, chooseButton, 10)

        // Start / stop blocker
        controlsRow = LinearLayout(this)
        controlsRow.orientation = LinearLayout.HORIZONTAL

        startButton = styledButton("Start blocker", good, Color.parseColor("#06210F"))
        startButton.setOnClickListener {
            startForegroundService(Intent(this, BlockerService::class.java))
            Toast.makeText(this, "Blocker started", Toast.LENGTH_SHORT).show()
        }

        stopButton = styledButton("Stop blocker", card, danger)
        stopButton.setOnClickListener {
            stopService(Intent(this, BlockerService::class.java))
            Toast.makeText(this, "Blocker stopped", Toast.LENGTH_SHORT).show()
        }

        val leftParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        val rightParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        rightParams.leftMargin = dp(10)
        controlsRow.addView(startButton, leftParams)
        controlsRow.addView(stopButton, rightParams)
        addTo(root, controlsRow, 16)

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(bg)
        scroll.isFillViewport = true
        scroll.addView(root)
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun getPrefs() = getSharedPreferences("settings", Context.MODE_PRIVATE)

    private fun setBar(percent: Int, color: Int) {
        val fillParams = barFill.layoutParams as LinearLayout.LayoutParams
        fillParams.weight = percent.toFloat()
        barFill.layoutParams = fillParams
        val emptyParams = barEmpty.layoutParams as LinearLayout.LayoutParams
        emptyParams.weight = (100 - percent).toFloat()
        barEmpty.layoutParams = emptyParams
        barFill.background = rounded(color, 6)
    }

    private fun refresh() {
        val hasUsage = hasUsagePermission()
        val hasOverlay = Settings.canDrawOverlays(this)

        usageButton.visibility = if (hasUsage) View.GONE else View.VISIBLE
        overlayButton.visibility = if (hasOverlay) View.GONE else View.VISIBLE
        val ready = hasUsage && hasOverlay
        controlsRow.visibility = if (ready) View.VISIBLE else View.GONE

        if (!hasUsage) {
            usedText.text = "--"
            statusText.text = "Allow usage access below so the app can measure your screen time."
            statusText.setTextColor(textDim)
            setBar(0, accent)
            return
        }

        val used = (UsageHelper.todayScreenTimeMs(this) / 60000).toInt()
        val limit = getPrefs().getInt("limit_minutes", 0)
        usedText.text = "${used / 60}h ${used % 60}m"

        if (limit <= 0) {
            statusText.text = "No limit set yet"
            statusText.setTextColor(textDim)
            setBar(0, accent)
            return
        }

        val percent = (used * 100 / limit).coerceAtMost(100)
        val left = limit - used
        if (left > 0) {
            statusText.text = "${left / 60}h ${left % 60}m left of $limit min"
            statusText.setTextColor(textDim)
        } else {
            statusText.text = "Limit reached"
            statusText.setTextColor(danger)
        }
        val color = if (percent >= 100) danger else if (percent >= 80) warn else accent
        setBar(percent, color)
    }

    private fun hasUsagePermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
