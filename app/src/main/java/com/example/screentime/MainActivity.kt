package com.example.screentime

import android.Manifest
import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var infoText: TextView
    private lateinit var usageButton: Button
    private lateinit var overlayButton: Button
    private lateinit var limitInput: EditText
    private lateinit var saveButton: Button
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(48, 96, 48, 48)

        infoText = TextView(this)
        infoText.textSize = 24f

        usageButton = Button(this)
        usageButton.text = "1. Allow usage access"
        usageButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        overlayButton = Button(this)
        overlayButton.text = "2. Allow display over other apps"
        overlayButton.setOnClickListener {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }

        limitInput = EditText(this)
        limitInput.hint = "Daily limit in minutes (e.g. 120)"
        limitInput.inputType = InputType.TYPE_CLASS_NUMBER
        val saved = getPrefs().getInt("limit_minutes", 0)
        if (saved > 0) limitInput.setText(saved.toString())

        saveButton = Button(this)
        saveButton.text = "Save limit"
        saveButton.setOnClickListener {
            val value = limitInput.text.toString().toIntOrNull() ?: 0
            getPrefs().edit().putInt("limit_minutes", value).apply()
            refresh()
        }

        startButton = Button(this)
        startButton.text = "Start blocker"
        startButton.setOnClickListener {
            startForegroundService(Intent(this, BlockerService::class.java))
            Toast.makeText(this, "Blocker started", Toast.LENGTH_SHORT).show()
        }

        stopButton = Button(this)
        stopButton.text = "Stop blocker"
        stopButton.setOnClickListener {
            stopService(Intent(this, BlockerService::class.java))
            Toast.makeText(this, "Blocker stopped", Toast.LENGTH_SHORT).show()
        }

        layout.addView(infoText)
        layout.addView(usageButton)
        layout.addView(overlayButton)
        layout.addView(limitInput)
        layout.addView(saveButton)
        layout.addView(startButton)
        layout.addView(stopButton)
        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun getPrefs() = getSharedPreferences("settings", Context.MODE_PRIVATE)

    private fun refresh() {
        val hasUsage = hasUsagePermission()
        val hasOverlay = Settings.canDrawOverlays(this)

        usageButton.visibility = if (hasUsage) View.GONE else View.VISIBLE
        overlayButton.visibility = if (hasOverlay) View.GONE else View.VISIBLE
        val ready = hasUsage && hasOverlay
        startButton.visibility = if (ready) View.VISIBLE else View.GONE
        stopButton.visibility = if (ready) View.VISIBLE else View.GONE

        if (hasUsage) {
            val used = (UsageHelper.todayScreenTimeMs(this) / 60000).toInt()
            val limit = getPrefs().getInt("limit_minutes", 0)
            var text = "Used today: ${used / 60}h ${used % 60}m"
            if (limit > 0) {
                val left = limit - used
                text += "\nLimit: $limit min"
                if (left > 0) {
                    text += "\nTime left: ${left / 60}h ${left % 60}m"
                } else {
                    text += "\nLimit reached!"
                }
            } else {
                text += "\nNo limit set yet."
            }
            infoText.text = text
        } else {
            infoText.text = "Please allow the permissions below so the app can work."
        }
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
    
