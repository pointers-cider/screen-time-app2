package com.example.screentime

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.Calendar

object UsageHelper {

    fun todayPerAppMs(context: Context): Map<String, Long> {
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
        val totals = HashMap<String, Long>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> openApps[pkg] = event.timeStamp
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val opened = openApps.remove(pkg)
                    if (opened != null) {
                        totals[pkg] = (totals[pkg] ?: 0L) + (event.timeStamp - opened)
                    }
                }
            }
        }
        for ((pkg, opened) in openApps) {
            totals[pkg] = (totals[pkg] ?: 0L) + (now - opened)
        }
        return totals
    }

    fun todayScreenTimeMs(context: Context): Long {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val perApp = todayPerAppMs(context)
        if (!prefs.getBoolean("count_only_selected", false)) {
            return perApp.values.sum()
        }
        val counted = prefs.getStringSet("counted_apps", emptySet()) ?: emptySet()
        var total = 0L
        for ((pkg, ms) in perApp) {
            if (counted.contains(pkg)) total += ms
        }
        return total
    }

    fun todayLimitMinutes(context: Context): Int {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val perDay = prefs.getInt("limit_day_$day", -1)
        return if (perDay >= 0) perDay else prefs.getInt("limit_minutes", 0)
    }
}
