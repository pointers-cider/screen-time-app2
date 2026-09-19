package com.example.screentime

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.Calendar

object UsageHelper {
    fun todayScreenTimeMs(context: Context): Long {
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

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
