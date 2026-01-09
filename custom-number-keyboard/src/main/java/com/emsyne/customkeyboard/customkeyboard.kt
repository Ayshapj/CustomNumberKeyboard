package com.emsyne.customkeyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.text.InputType
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView

class CustomNumberKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    // ---------- STATE ----------
    private var targetEditText: EditText? = null
    private var parentScrollView: NestedScrollView? = null

    private var originalPaddingBottom = 0
    private var originalScrollY = 0
    private var isMpinVisible = false

    private val numbers = (1..9).map { it.toString() }.toMutableList()
    private val KEY_SIZE = dp(56)

    // ---------- UI ----------
    private val keyboardGrid = GridLayout(context).apply {
        columnCount = 3
        setBackgroundColor(Color.WHITE)
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
    }

    init {
        orientation = VERTICAL
        gravity = Gravity.BOTTOM
        visibility = GONE
        addView(keyboardGrid)
    }

    // ---------- PUBLIC API ----------
    fun attachToEditText(editText: EditText) {
        editText.showSoftInputOnFocus = false
        editText.isCursorVisible = false

        parentScrollView = findParentScrollView(editText)

        editText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                targetEditText = editText
                applyVisibilityState()
                showKeyboard()
            }
        }

        editText.setOnClickListener {
            targetEditText = editText
            applyVisibilityState()
            showKeyboard()
        }
    }

    fun isKeyboardVisible(): Boolean = visibility == VISIBLE

    // ---------- VISIBILITY ----------
    fun showKeyboard() {
        if (visibility == VISIBLE) return

        numbers.shuffle()
        createKeys()
        visibility = VISIBLE

        post { adjustScrollForKeyboard() }
    }

    fun hideKeyboard() {
        if (visibility == GONE) return

        visibility = GONE
        restoreScroll()
    }

    // ---------- INTERNAL SCROLL LOGIC ----------
    private fun adjustScrollForKeyboard() {
        val scrollView = parentScrollView ?: return
        val editText = targetEditText ?: return

        originalPaddingBottom = scrollView.paddingBottom
        originalScrollY = scrollView.scrollY

        scrollView.setPadding(
            scrollView.paddingLeft,
            scrollView.paddingTop,
            scrollView.paddingRight,
            height
        )

        scrollView.post {
            val location = IntArray(2)
            editText.getLocationOnScreen(location)

            val editTextBottom = location[1] + editText.height
            val screenHeight = resources.displayMetrics.heightPixels
            val overlap = editTextBottom - (screenHeight - height)

            if (overlap > 0) {
                scrollView.smoothScrollBy(0, overlap + dp(24))
            }
        }
    }

    private fun restoreScroll() {
        parentScrollView?.let { scrollView ->
            scrollView.setPadding(
                scrollView.paddingLeft,
                scrollView.paddingTop,
                scrollView.paddingRight,
                originalPaddingBottom
            )

            scrollView.post {
                scrollView.smoothScrollTo(0, originalScrollY)
            }
        }
    }

    // ---------- KEYS ----------
    private fun createKeys() {
        keyboardGrid.removeAllViews()

        numbers.forEach {
            keyboardGrid.addView(createTextKey(it) { appendNumber(it) })
        }

        keyboardGrid.addView(createEyeKey {
            toggleMpinVisibility()
            createKeys()
        })

        keyboardGrid.addView(createTextKey("0") { appendNumber("0") })
        keyboardGrid.addView(createTextKey("⌫") { deleteLast() })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createTextKey(text: String, action: () -> Unit): TextView {
        return TextView(context).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = KEY_SIZE
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
            this.text = text
            textSize = 22f
            gravity = Gravity.CENTER
            background = ContextCompat.getDrawable(context, R.drawable.key_divider_bg)

            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> v.alpha = 0.4f
                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> v.alpha = 1f
                }
                false
            }

            setOnClickListener { action() }
        }
    }

    private fun createEyeKey(action: () -> Unit): ImageView {
        return ImageView(context).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = KEY_SIZE
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
            background = ContextCompat.getDrawable(context, R.drawable.key_divider_bg)
            scaleType = ImageView.ScaleType.CENTER
            setImageResource(
                if (isMpinVisible)
                    R.drawable.ic_eye_closed
                else
                    R.drawable.ic_close_eye
            )
            setOnClickListener { action() }
        }
    }

    // ---------- ACTIONS ----------
    private fun appendNumber(number: String) {
        targetEditText?.let {
            if (it.text.length < 4) it.append(number)
        }
    }

    private fun deleteLast() {
        targetEditText?.text?.let {
            if (it.isNotEmpty()) it.delete(it.length - 1, it.length)
        }
    }

    private fun toggleMpinVisibility() {
        isMpinVisible = !isMpinVisible
        applyVisibilityState()
    }

    private fun applyVisibilityState() {
        targetEditText?.let { et ->
            val text = et.text.toString()
            et.inputType =
                if (isMpinVisible)
                    InputType.TYPE_CLASS_NUMBER
                else
                    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD

            et.setText(text)
            et.setSelection(text.length)
        }
    }

    // ---------- HELPERS ----------
    private fun findParentScrollView(view: View): NestedScrollView? {
        var parent = view.parent
        while (parent is View) {
            if (parent is NestedScrollView) return parent
            parent = parent.parent
        }
        return null
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
