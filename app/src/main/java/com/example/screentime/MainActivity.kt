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
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Calendar

class MainActivity : Activity() {

    private lateinit var infoText: TextView
    private lateinit var permissionButton: Button

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

        layout.addView(infoText)
        layout.addView(permissionButton)
        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        if (hasUsagePermission()) {
            permissionButton.visibility = View.GONE
            val minutes = getTodayScreenTimeMs() / 60000
            infoText.text = "Screen time today: ${minutes / 60}h ${minutes % 60}m"
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
