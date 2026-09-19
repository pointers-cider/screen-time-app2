package com.example.screentime

import android.app.Activity
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Calendar

class MainActivity : Activity() {

    private lateinit var infoText: TextView
    private lateinit var permissionButton: Button
    private lateinit var limitInput: EditText
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(48, 96, 48, 48)

        infoText = TextView(this)
        infoText.textSize = 24f

        permissionButton = Button(this)
        permissionButton.text = "Allow usage access"
        permissionButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
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

        layout.addView(infoText)
        layout.addView(permissionButton)
        layout.addView(limitInput)
        layout.addView(saveButton)
        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun getPrefs() = getSharedPreferences("settings", Context.MODE_PRIVATE)

    private fun refresh() {
        if (hasUsagePermission()) {
            permissionButton.visibility = View.GONE
            val used = (getTodayScreenTimeMs() / 60000).toInt()
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
            permissionButton.visibility = View.VISIBLE
            infoText.text = "Please allow usage access so the app can measure your screen time."
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

    private fun getTodayScreenTimeMs(): Long {
        val manager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        val now = System.currentTimeMillis()

        val events = manager.queryEvents(start, now)
        val event = UsageEvents.Event()
        val openApps = HashMap<String, Long>()
        var total = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> openApps[pkg] = event.timeStamp
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val opened = openApps.remove(pkg)
                    if (opened != null) total += event.timeStamp - opened
                }
            }
        }
        for (opened in openApps.values) total += now - opened
        return total
    }
}

    
