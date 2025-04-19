package com.haskell.amghud.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.haskell.amghud.TransitioningValue
import kotlin.math.abs

class LeverThrottleView(context: Context, attrs: AttributeSet?) : View(context, attrs)  {

    private val transitioningThrottle: TransitioningValue<Float> =
        object : TransitioningValue<Float>(0.0f) {
            override fun extrapolate(
            ): Float {
                return from + (target - from) * progress
            }
        }

    fun setThrottle(value: Float){
        transitioningThrottle.resetTarget(value) { prevTarget ->
            abs(prevTarget-value) > transitioningThrottle.tolerance
        }
        checkToInvalidate()
    }
    private fun checkToInvalidate(){
        if(transitioningThrottle.isUpdateRequired()){
            invalidate()
        }
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val screenWidth = resources.displayMetrics.widthPixels
        val resizedWidth = screenWidth * 0.5f
        setMeasuredDimension(resizedWidth.toInt(), resources.displayMetrics.heightPixels)
    }

    private val paint = Paint()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        transitioningThrottle.update(0.5f)
        val boxWidth = width*0.6f
        val boxHeight = height*0.6f
        paint.strokeWidth = 1f
        paint.color = Color.RED
        canvas.drawRect((width-boxWidth)/2f, (height-boxHeight)/2f, (width+boxWidth)/2f, (height+boxHeight)/2f, paint)
        checkToInvalidate()
    }

}