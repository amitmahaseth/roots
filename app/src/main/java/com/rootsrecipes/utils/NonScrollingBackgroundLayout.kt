package com.rootsrecipes.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.WindowInsets
import androidx.constraintlayout.widget.ConstraintLayout


class NonScrollingBackgroundLayout : ConstraintLayout {
    private var background: Drawable? = null
    private var originalHeight = 0
    private var hasInitializedBackground = false

    constructor(context: Context?) : super(context!!) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    ) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!, attrs, defStyleAttr
    ) {
        init()
    }

    private fun init() {
        // Initialize any needed variables
        setWillNotDraw(false)
    }

    override fun setBackground(background: Drawable) {
        this.background = background
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!hasInitializedBackground && h > 0) {
            originalHeight = h
            hasInitializedBackground = true
        }
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        // Let the system handle insets for child views but don't let it affect our drawing
        return super.onApplyWindowInsets(insets)
    }

    override fun onDraw(canvas: Canvas) {
        // Draw the background manually at the original height
        if (background != null) {
            val width = width
            val height = if (hasInitializedBackground) originalHeight else height

            background!!.setBounds(0, 0, width, height)
            background!!.draw(canvas)
        }

        super.onDraw(canvas)
    }
}