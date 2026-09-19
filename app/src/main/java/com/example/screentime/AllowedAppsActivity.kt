package com.example.screentime

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class AllowedAppsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val allowed = HashSet<String>(prefs.getStringSet("allowed_apps", emptySet()) ?: emptySet())

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(48, 96, 48, 48)

        val title = TextView(this)
        title.text = "Tick the apps that stay open after the limit is reached."
        title.textSize = 18f
        layout.addView(title)

        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(launcherIntent, 0)
            .map { Pair(it.loadLabel(packageManager).toString(), it.activityInfo.packageName) }
            .filter { it.second != packageName }
            .distinctBy { it.second }
            .sortedBy { it.first.lowercase() }

        for (app in apps) {
            val label = app.first
            val pkg = app.second
            val box = CheckBox(this)
            box.text = label
            box.textSize = 18f
            box.isChecked = allowed.contains(pkg)
            box.setOnCheckedChangeListener { _, checked ->
                if (checked) allowed.add(pkg) else allowed.remove(pkg)
                prefs.edit().putStringSet("allowed_apps", HashSet<String>(allowed)).apply()
            }
            layout.addView(box)
        }

        val scroll = ScrollView(this)
        scroll.addView(layout)
        setContentView(scroll)
    }
}
