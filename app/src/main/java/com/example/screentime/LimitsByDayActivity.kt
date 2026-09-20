package com.example.screentime

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar

class LimitsByDayActivity : Activity() {

    private val bg = Color.parseColor("#0F1115")
    private val card = Color.parseColor("#1A1D24")
    private val field = Color.parseColor("#252A34")
    private val textMain = Color.parseColor("#F2F4F8")
    private val textDim = Color.parseColor("#9AA3B2")
    private val accent = Color.parseColor("#4F8CFF")

    private val dayNumbers = intArrayOf(2, 3, 4, 5, 6, 7, 1)
    private val dayNames = arrayOf(
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    )
    private val inputs = ArrayList<EditText>()

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

    private fun addTo(parent: LinearLayout, child: View, topDp: Int) {
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = dp(topDp)
        parent.addView(child, params)
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

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val defaultLimit = prefs.getInt("limit_minutes", 0)
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(20), dp(48), dp(20), dp(32))

        root.addView(label("Limits by day", 28f, textMain, true))
        val info = if (defaultLimit > 0) {
            "Default limit: $defaultLimit min. Leave a day empty to use it."
        } else {
            "No default limit is set. Leave a day empty for no limit."
        }
        addTo(root, label(info, 15f, textDim), 4)
        addTo(root, label("Type 0 for a day with no limit.", 15f, textDim), 2)

        val listCard = LinearLayout(this)
        listCard.orientation = LinearLayout.VERTICAL
        listCard.background = rounded(card, 20)
        listCard.setPadding(dp(20), dp(10), dp(20), dp(20))

        for (i in dayNumbers.indices) {
            val isToday = dayNumbers[i] == today

            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL

            val dayLabel = label(dayNames[i], 16f, if (isToday) accent else textMain, isToday)
            row.addView(dayLabel, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

            val input = EditText(this)
            input.hint = if (defaultLimit > 0) defaultLimit.toString() else "none"
            input.setHintTextColor(textDim)
            input.setTextColor(textMain)
            input.textSize = 16f
            input.inputType = InputType.TYPE_CLASS_NUMBER
            input.gravity = Gravity.CENTER
            input.background = rounded(field, 12)
            input.setPadding(dp(12), dp(10), dp(12), dp(10))
            val saved = prefs.getInt("limit_day_${dayNumbers[i]}", -1)
            if (saved >= 0) input.setText(saved.toString())
            row.addView(input, LinearLayout.LayoutParams(dp(110), ViewGroup.LayoutParams.WRAP_CONTENT))
            inputs.add(input)

            val unit = label("min", 14f, textDim)
            val unitParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            unitParams.leftMargin = dp(8)
            row.addView(unit, unitParams)

            addTo(listCard, row, 10)
        }
        addTo(root, listCard, 16)

        val saveButton = Button(this)
        saveButton.text = "Save"
        saveButton.isAllCaps = false
        saveButton.textSize = 16f
        saveButton.setTextColor(Color.WHITE)
        saveButton.background = rounded(accent, 14)
        saveButton.stateListAnimator = null
        saveButton.setOnClickListener {
            val editor = prefs.edit()
            for (i in dayNumbers.indices) {
                val key = "limit_day_${dayNumbers[i]}"
                val value = inputs[i].text.toString().trim().toIntOrNull()
                if (value == null) {
                    editor.remove(key)
                } else {
                    editor.putInt(key, value)
                }
            }
            editor.apply()
            Toast.makeText(this, "Limits saved", Toast.LENGTH_SHORT).show()
            finish()
        }
        addTo(root, saveButton, 20)

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(bg)
        scroll.isFillViewport = true
        scroll.addView(root)
        setContentView(scroll)
    }
}
