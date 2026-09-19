package com.example.screentime

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = TextView(this)
        text.text = "Screen Time app is working!"
        text.textSize = 24f
        text.setPadding(48, 96, 48, 48)
        setContentView(text)
    }
}
