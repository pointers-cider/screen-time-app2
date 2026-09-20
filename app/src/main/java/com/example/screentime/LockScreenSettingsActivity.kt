package com.example.screentime

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class LockScreenSettingsActivity : Activity() {

    private val bg = Color.parseColor("#0F1115")
    private val card = Color.parseColor("#1A1D24")
    private val field = Color.parseColor("#252A34")
    private val textMain = Color.parseColor("#F2F4F8")
    private val textDim = Color.parseColor("#9AA3B2")
    private val accent = Color.parseColor("#4F8CFF")

    private val defaultMessage =
        "Daily screen time limit reached.\n\nCalls and texts still work. Go to your home screen to use them."

    private val colors = intArrayOf(
        Color.parseColor("#121212"),
        Color.parseColor("#0D2B5C"),
        Color.parseColor("#3B1F5C"),
        Color.parseColor("#0F3D2E"),
        Color.parseColor("#5C1A1A"),
        Color.parseColor("#F5F5F5")
    )
    private val sizes = intArrayOf(18, 22, 28)
    private val sizeNames = arrayOf("Small", "Medium", "Large")

    private var selectedColor = Color.parseColor("#121212")
    private var selectedSize = 22

    private lateinit var previewBox: LinearLayout
    private lateinit var previewText: TextView
    private lateinit var messageInput: EditText
    private val swatches = ArrayList<View>()
    private val sizeButtons = ArrayList<Button>()

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

    private fun cardLayout(): LinearLayout {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.background = rounded(card, 20)
        layout.setPadding(dp(20), dp(20), dp(20), dp(20))
        return layout
    }

    private fun addTo(parent: LinearLayout, child: View, topDp: Int) {
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = dp(topDp)
        parent.addView(child, params)
    }

    private fun textColorFor(background: Int): Int {
        val brightness = 0.299 * Color.red(background) +
            0.587 * Color.green(background) +
            0.114 * Color.blue(background)
        return if (brightness > 150) Color.parseColor("#111111") else Color.WHITE
    }

    private fun updatePreview() {
        val message = messageInput.text.toString().trim()
        previewText.text = if (message.isEmpty()) defaultMessage else message
        previewText.textSize = selectedSize.toFloat()
        previewText.setTextColor(textColorFor(selectedColor))
        previewBox.background = rounded(selectedColor, 20)
    }

    private fun refreshSwatches() {
        for (i in colors.indices) {
            val drawable = GradientDrawable()
            drawable.setColor(colors[i])
            drawable.cornerRadius = dp(12).toFloat()
            if (colors[i] == selectedColor) {
                drawable.setStroke(dp(3), accent)
            } else {
                drawable.setStroke(dp(1), Color.parseColor("#3A4050"))
            }
            swatches[i].background = drawable
        }
    }

    private fun refreshSizes() {
        for (i in sizes.indices) {
            val button = sizeButtons[i]
            if (sizes[i] == selectedSize) {
                button.background = rounded(accent, 12)
                button.setTextColor(Color.WHITE)
            } else {
                button.background = rounded(field, 12)
                button.setTextColor(textMain)
            }
        }
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
        selectedColor = prefs.getInt("lock_color", colors[0])
        selectedSize = prefs.getInt("lock_text_size", 22)
        val savedMessage = prefs.getString("lock_message", "") ?: ""

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(20), dp(48), dp(20), dp(32))

        root.addView(label("Lock screen", 28f, textMain, true))
        addTo(root, label("Customize what you see when apps are blocked.", 15f, textDim), 4)

        // Preview
        previewBox = LinearLayout(this)
        previewBox.orientation = LinearLayout.VERTICAL
        previewBox.gravity = Gravity.CENTER
        previewBox.minimumHeight = dp(180)
        previewBox.setPadding(dp(20), dp(28), dp(20), dp(28))
        previewText = TextView(this)
        previewText.gravity = Gravity.CENTER
        previewBox.addView(previewText)
        addTo(root, previewBox, 16)

        // Message
        val messageCard = cardLayout()
        messageCard.addView(label("Message", 16f, textMain, true))
        messageInput = EditText(this)
        messageInput.hint = "Leave empty to use the default message"
        messageInput.setHintTextColor(textDim)
        messageInput.setTextColor(textMain)
        messageInput.textSize = 16f
        messageInput.inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE or
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        messageInput.minLines = 3
        messageInput.gravity = Gravity.TOP
        messageInput.background = rounded(field, 12)
        messageInput.setPadding(dp(16), dp(12), dp(16), dp(12))
        messageInput.setText(savedMessage)
        messageInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updatePreview()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        addTo(messageCard, messageInput, 12)
        addTo(root, messageCard, 16)

        // Colors
        val colorCard = cardLayout()
        colorCard.addView(label("Background color", 16f, textMain, true))
        val swatchRow = LinearLayout(this)
        swatchRow.orientation = LinearLayout.HORIZONTAL
        for (i in colors.indices) {
            val swatch = View(this)
            swatch.setOnClickListener {
                selectedColor = colors[i]
                refreshSwatches()
                updatePreview()
            }
            val params = LinearLayout.LayoutParams(0, dp(44), 1f)
            if (i > 0) params.leftMargin = dp(8)
            swatchRow.addView(swatch, params)
            swatches.add(swatch)
        }
        addTo(colorCard, swatchRow, 12)
        addTo(root, colorCard, 16)

        // Text size
        val sizeCard = cardLayout()
        sizeCard.addView(label("Text size", 16f, textMain, true))
        val sizeRow = LinearLayout(this)
        sizeRow.orientation = LinearLayout.HORIZONTAL
        for (i in sizes.indices) {
            val button = Button(this)
            button.text = sizeNames[i]
            button.isAllCaps = false
            button.textSize = 15f
            button.stateListAnimator = null
            button.setOnClickListener {
                selectedSize = sizes[i]
                refreshSizes()
                updatePreview()
            }
            val params = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            if (i > 0) params.leftMargin = dp(8)
            sizeRow.addView(button, params)
            sizeButtons.add(button)
        }
        addTo(sizeCard, sizeRow, 12)
        addTo(root, sizeCard, 16)

        // Save and reset
        val saveButton = Button(this)
        saveButton.text = "Save"
        saveButton.isAllCaps = false
        saveButton.textSize = 16f
        saveButton.setTextColor(Color.WHITE)
        saveButton.background = rounded(accent, 14)
        saveButton.stateListAnimator = null
        saveButton.setOnClickListener {
            prefs.edit()
                .putString("lock_message", messageInput.text.toString().trim())
                .putInt("lock_color", selectedColor)
                .putInt("lock_text_size", selectedSize)
                .apply()
            Toast.makeText(this, "Lock screen saved", Toast.LENGTH_SHORT).show()
            finish()
        }
        addTo(root, saveButton, 20)

        val resetButton = Button(this)
        resetButton.text = "Reset to default"
        resetButton.isAllCaps = false
        resetButton.textSize = 16f
        resetButton.setTextColor(textMain)
        resetButton.background = rounded(card, 14)
        resetButton.stateListAnimator = null
        resetButton.setOnClickListener {
            messageInput.setText("")
            selectedColor = colors[0]
            selectedSize = 22
            refreshSwatches()
            refreshSizes()
            updatePreview()
        }
        addTo(root, resetButton, 10)

        refreshSwatches()
        refreshSizes()
        updatePreview()

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(bg)
        scroll.isFillViewport = true
        scroll.addView(root)
        setContentView(scroll)
    }
}
