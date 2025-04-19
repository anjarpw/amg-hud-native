package com.haskell.amghud.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.haskell.amghud.GearMode
import com.haskell.amghud.TransitioningValue


val GearModes: Array<GearMode> = arrayOf(GearMode.T, GearMode.P, GearMode.R, GearMode.D, GearMode.S, GearMode.S_PLUS)

class GearSelectorView(context: Context, attrs: AttributeSet?) : View(context, attrs)  {

    private var typeface: Typeface? = null

    init {
        try {
            typeface = Typeface.createFromAsset(context.assets, "Exo-BoldItalic.ttf") // Replace with your font file
            typeface = Typeface.create(typeface, Typeface.ITALIC);
        } catch (e: Exception) {
            e.printStackTrace()
            typeface = Typeface.DEFAULT // Use default font if loading fails.
        }
    }

    private val transitioningGearSelectorIndex: TransitioningValue<Float> =
        object : TransitioningValue<Float>(0.0f) {
            override fun extrapolate(
            ): Float {
                return from + (target - from) * progress
            }
        }


    fun setGearMode(value: GearMode){
        val index = GearModes.indexOf(value)
        transitioningGearSelectorIndex.resetTarget(index.toFloat()) { prevTarget ->
            Math.abs(prevTarget-index) > transitioningGearSelectorIndex.tolerance
        }
        checkToInvalidate()
    }
    private fun checkToInvalidate(){
        if(transitioningGearSelectorIndex.isUpdateRequired()){
            invalidate()
        }
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val screenWidth = resources.displayMetrics.widthPixels
        val resizedWidth = screenWidth * 0.1f

        // Set the measured dimensions to be a square
        setMeasuredDimension(resizedWidth.toInt(), resources.displayMetrics.heightPixels)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        transitioningGearSelectorIndex.update(0.5f)
        val textPaint = Paint()
        textPaint.typeface = typeface
        textPaint.setShadowLayer(5f, 1f, 1f, Color.BLACK) // shadow radius, dx, dy, shadow color
        textPaint.color = Color.WHITE

        val bounds = Rect()
        val h = height
        val fontSize = h/18;
        val gapSize = fontSize*1.1f
        val top = (height-gapSize*6f)/2f
        for ((index, gearMode) in GearModes.withIndex()) {
            val text = gearMode.stringAlias
            val intensity = computeActiveIntensity(index)

            textPaint.textSize = (intensity*0.4f + 0.6f)*fontSize
            textPaint.alpha = 255-((1f-intensity)*128).toInt()
            textPaint.getTextBounds(text, 0, text.length, bounds)

            canvas.drawText(text,
                width*0.5f,
                index*gapSize + top + bounds.height()*0.5f,
                textPaint)
        }
        checkToInvalidate()
    }

    private fun computeActiveIntensity(index: Int): Float{
        var intensity = 1f-Math.abs(index-transitioningGearSelectorIndex.current)
        if(intensity<0){
            intensity = 0f
        }
        return intensity
    }
}