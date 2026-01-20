package com.rootsrecipes.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.rootsrecipes.R

class AlphabetScrollBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val alphabetList = listOf("#") + ('A'..'Z').toList()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 12f * resources.displayMetrics.density // 12sp
        color = context.getColor(R.color.green)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(ResourcesCompat.getFont(context, R.font.work_sans), Typeface.BOLD)
    }

    private var selectedIndex = -1
    private var itemHeight = 0f
    private var onLetterSelectedListener: ((String) -> Unit)? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        itemHeight = h.toFloat() / alphabetList.size
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        alphabetList.forEachIndexed { index, letter ->
            val y = itemHeight * index + itemHeight / 2 + paint.textSize / 2

            paint.color = if (index == selectedIndex) Color.BLACK else context.getColor(R.color.green)
            paint.alpha = if (index == selectedIndex) 255 else 180

            canvas.drawText(letter.toString(), width / 2f, y, paint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {
                val index = (event.y / itemHeight).toInt().coerceIn(0, alphabetList.size - 1)
                if (selectedIndex != index) {
                    selectedIndex = index
                    onLetterSelectedListener?.invoke(alphabetList[index].toString())
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                selectedIndex = -1
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun setOnLetterSelectedListener(listener: (String) -> Unit) {
        onLetterSelectedListener = listener
    }
}
