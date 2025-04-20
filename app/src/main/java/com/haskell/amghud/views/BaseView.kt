package com.haskell.amghud.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Size
import android.view.View
import com.haskell.amghud.ITransitioningUpdate
import com.haskell.amghud.TransitioningValue

abstract class BaseView(context: Context, attrs: AttributeSet?) : View(context, attrs)  {


    private val transitions = ArrayList<ITransitioningUpdate>()
    protected fun <T>generateTransition(initialValue: T, fractionSpeed: Float, extrapolate: (from: T, target: T, progress: Float) -> T): TransitioningValue<T>{
        val transition =  object : TransitioningValue<T>(initialValue, fractionSpeed) {
            override fun extrapolate(
            ): T {
                return extrapolate(from, target, progress)
            }
        }
        transitions.add(transition)
        return transition
    }


    protected fun checkToInvalidate(){
        val anyUpdate = transitions.any { it.isUpdateRequired() }
        if(anyUpdate){
            invalidate()
        }
    }

    protected abstract fun doDrawing(canvas: Canvas)


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val revisedSize = doResize(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels)
        // Set the measured dimensions to be a square
        setMeasuredDimension(revisedSize.width, revisedSize.height)
    }

    protected open fun doResize(width: Int, height: Int): Size{
        return Size(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (transition in transitions) {
            transition.update()
        }
        doDrawing(canvas)
        checkToInvalidate()

    }
}


class CanvasManager(
    val updateCondition: () -> Boolean,
    val onRender: (canvas: Canvas) -> Unit
){
    var bitmap: Bitmap =  Bitmap.createBitmap(1,1, Bitmap.Config.ARGB_8888)
    private var canvas: Canvas = Canvas()
    private val sourceRect: Rect = Rect()
    private val targetRect: Rect = Rect()

    private var hasBeenRendered = false

    fun setSize(width: Int, height: Int){
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        sourceRect.set(0, 0, width, height) // Source rectangle
        hasBeenRendered = false
    }

    private fun tryUpdate(){
        if(updateCondition() || !hasBeenRendered){
            canvas = Canvas(bitmap)
            onRender(canvas)
            hasBeenRendered = true
        }
    }

    fun applyToCanvas(c: Canvas, left: Int, top: Int, right: Int, bottom: Int){
        tryUpdate()
        targetRect.set(left, top, right, bottom)
        c.drawBitmap(bitmap, sourceRect, targetRect, Paint())
    }
}
