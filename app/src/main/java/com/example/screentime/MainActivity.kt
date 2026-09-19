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
            startActivity(Intent(Settings.
