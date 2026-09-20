package com.example.screentime

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Telephony
import android.telecom.TelecomManager
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Calendar

class BlockerService : Service() {

    companion object {
        const val CHANNEL_ID = "blocker"
    }

    private val handler = Handler(Looper.getMainLooper())
    private var overlay: View? = null
    private var overlayText: TextView? = null
    private var currentPkg: String? = null
    private var lastEventTime = 0L
    private var usedMinutes = 0
    private var lastUsageCheck = 0L

    private val tick = object : Runnable {
        override fun run() {
            try {
                checkAndBlock()
            } catch (e: Exception) {
            }
            handler.postDelayed(this, 1500)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Screen Time blocker",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Time is active")
            .setContentText("Watching your limit and blocked hours")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
        handler.removeCallbacks(tick)
        handler.post(tick)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        hideOverlay()
        super.onDestroy()
    }

    private fun formatClock(minutes: Int): String {
        return "%02d:%02d".format(minutes / 60, minutes % 60)
    }

    private fun textColorFor(background: Int): Int {
        val brightness = 0.299 * Color.red(background) +
            0.587 * Color.green(background) +
            0.114 * Color.blue(background)
        return if (brightness > 150) Color.parseColor("#111111") else Color.WHITE
    }

    private fun inBlockedWindow(prefs: SharedPreferences): Boolean {
        if (!prefs.getBoolean("window_enabled", false)) return false
        val start = prefs.getInt("window_start", 8 * 60)
        val end = prefs.getInt("window_end", 15 * 60)
        if (start == end) return false
        val calendar = Calendar.getInstance()
        val now = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        return if (start < end) {
            now >= start && now < end
        } else {
            now >= start || now < end
        }
    }

    private fun checkAndBlock() {
        updateForegroundApp()

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val limit = UsageHelper.todayLimitMinutes(this)
        val windowBlocked = inBlockedWindow(prefs)

        var limitReached = false
        if (limit > 0) {
            val now = System.currentTimeMillis()
            if (now - lastUsageCheck > 15000) {
                usedMinutes = (UsageHelper.todayScreenTimeMs(this) / 60000).toInt()
                lastUsageCheck = now
            }
            limitReached = usedMinutes >= limit
        }

        val pkg = currentPkg
        val shouldBlock = (windowBlocked || limitReached) &&
            pkg != null && !allowedPackages().contains(pkg)

        if (shouldBlock) {
            val message = if (windowBlocked) {
                val end = prefs.getInt("window_end", 15 * 60)
                "Blocked hours are active until ${formatClock(end)}.\n\nCalls and texts still work. Go to your home screen to use them."
            } else {
                "Daily screen time limit reached.\n\nCalls and texts still work. Go to your home screen to use them."
            }
            showOverlay(message)
        } else {
            hideOverlay()
        }
    }

    private fun updateForegroundApp() {
        val manager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val from = if (lastEventTime == 0L) now - 3600000 else lastEventTime
        val events = manager.queryEvents(from, now)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                currentPkg = event.packageName
            }
        }
        lastEventTime = now
    }

    private fun allowedPackages(): Set<String> {
        val set = mutableSetOf(
            packageName,
            "com.android.systemui",
            "com.android.settings",
            "com.android.phone",
            "com.android.incallui",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller"
        )
        val telecom = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        telecom.defaultDialerPackage?.let { set.add(it) }
        Telephony.Sms.getDefaultSmsPackage(this)?.let { set.add(it) }
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val info = packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
        info?.activityInfo?.packageName?.let { set.add(it) }

        val chosen = getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getStringSet("allowed_apps", emptySet())
        if (chosen != null) set.addAll(chosen)

        return set
    }

    private fun showOverlay(defaultMessage: String) {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val custom = (prefs.getString("lock_message", "") ?: "").trim()
        val message = if (custom.isEmpty()) defaultMessage else custom
        val backgroundColor = prefs.getInt("lock_color", Color.parseColor("#121212"))
        val textSize = prefs.getInt("lock_text_size", 22)

        if (overlay != null) {
            overlay?.setBackgroundColor(backgroundColor)
            overlayText?.text = message
            overlayText?.setTextColor(textColorFor(backgroundColor))
            overlayText?.textSize = textSize.toFloat()
            return
        }
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setBackgroundColor(backgroundColor)
        layout.setPadding(64, 64, 64, 64)

        val text = TextView(this)
        text.text = message
        text.setTextColor(textColorFor(backgroundColor))
        text.textSize = textSize.toFloat()
        text.gravity = Gravity.CENTER
        layout.addView(text)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.OPAQUE
        )
        wm.addView(layout, params)
        overlay = layout
        overlayText = text
    }

    private fun hideOverlay() {
        val view = overlay ?: return
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.removeView(view)
        overlay = null
        overlayText = null
    }
}
