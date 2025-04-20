package com.haskell.amghud.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Size
import android.view.View
import com.haskell.amghud.TransitioningValue
import kotlin.math.abs

class LeverThrottleView(context: Context, attrs: AttributeSet?) : BaseView(context, attrs)  {

    private val transitioningThrottle: TransitioningValue<Float> = generateTransition(0.0f, 0.1f,
        fun (from: Float, target: Float, progress: Float): Float {
            return from + (target - from) * progress
        })

    fun setThrottle(value: Float){
        transitioningThrottle.resetTarget(value) { prevTarget ->
            abs(prevTarget-value) > transitioningThrottle.tolerance
        }
        checkToInvalidate()
    }


    override fun doResize(width: Int, height: Int): Size {
        return Size((width*0.05f).toInt(), height)
    }

    private val paint = Paint()

    override fun doDrawing(canvas: Canvas) {
        val boxWidth = width*0.3f
        val boxHeight = height*0.6f
        paint.strokeWidth = 1f
        paint.color = Color.RED
        canvas.drawRect((width-boxWidth)/2f, (height-boxHeight)/2f, (width+boxWidth)/2f, (height+boxHeight)/2f, paint)
    }

}