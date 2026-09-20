package com.example.screentime

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class CustomizeActivity : Activity() {

    private val bg = Color.parseColor("#0F1115")
    private val card = Color.parseColor("#1A1D24")
    private val textMain = Color.parseColor("#F2F4F8")
    private val textDim = Color.parseColor("#9AA3B2")

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

    private fun addItem(root: LinearLayout, title: String, description: String, target: Class<*>) {
        val item = LinearLayout(this)
        item.orientation = LinearLayout.VERTICAL
        item.background = rounded(card, 16)
        item.setPadding(dp(20), dp(18), dp(20), dp(18))
        item.addView(label(title, 17f, textMain, true))
        val descParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        descParams.topMargin = dp(4)
        item.addView(label(description, 14f, textDim), descParams)
        item.setOnClickListener { startActivity(Intent(this, target)) }

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = dp(12)
        root.addView(item, params)
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

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(20), dp(48), dp(20), dp(32))

        root.addView(label("Customize", 28f, textMain, true))
        root.addView(label("Make the app work your way", 15f, textDim))

        addItem(
            root,
            "Lock screen",
            "Your own message, background color and text size",
            LockScreenSettingsActivity::class.java
        )

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(bg)
        scroll.isFillViewport = true
        scroll.addView(root)
        setContentView(scroll)
    }
}
