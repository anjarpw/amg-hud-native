package com.haskell.amghud.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Size
import com.haskell.amghud.GearMode
import com.haskell.amghud.TransitioningValue
import kotlin.math.abs


val GearModes: Array<GearMode> = arrayOf(GearMode.T, GearMode.P, GearMode.R, GearMode.D, GearMode.S, GearMode.S_PLUS)

class GearSelectorView(context: Context, attrs: AttributeSet?) : BaseView(context, attrs)  {

    private var typeface: Typeface? = null

    init {
        try {
            typeface = Typeface.createFromAsset(context.assets, "Exo-BoldItalic.ttf") // Replace with your font file
            typeface = Typeface.create(typeface, Typeface.ITALIC)
        } catch (e: Exception) {
            e.printStackTrace()
            typeface = Typeface.DEFAULT // Use default font if loading fails.
        }
    }

    private val transitioningGearSelectorIndex: TransitioningValue<Float> = generateTransition(0.0f, 0.1f,
        fun (from: Float, target: Float, progress: Float): Float {
           return from + (target - from) * progress
        })

    fun setGearMode(value: GearMode){
        val index = GearModes.indexOf(value)
        transitioningGearSelectorIndex.resetTarget(index.toFloat()) { prevTarget ->
            abs(prevTarget-index) > transitioningGearSelectorIndex.tolerance
        }
        checkToInvalidate()
    }

    override fun doResize(width: Int, height: Int): Size {
        return Size((width*0.08f).toInt(), height)
    }

    private val textPaint = Paint()
    private val redPaint = Paint()
    private val textBound = Rect()

    override fun doDrawing(canvas: Canvas) {
        redPaint.color = Color.RED
        redPaint.style = Paint.Style.FILL
        textPaint.typeface = typeface
        textPaint.setShadowLayer(5f, 1f, 1f, Color.BLACK) // shadow radius, dx, dy, shadow color
        textPaint.color = Color.WHITE

        val h = height
        val fontSize = h/15
        val gapSize = fontSize*1.1f
        val top = (height-gapSize*7f)/2f
        for ((index, gearMode) in GearModes.withIndex()) {
            val text = gearMode.stringAlias
            val intensity = computeActiveIntensity(index)

            textPaint.textSize = (intensity*0.4f + 0.6f)*fontSize
            textPaint.alpha = 255-((1f-intensity)*200).toInt()
            textPaint.getTextBounds(text, 0, text.length, textBound)

            canvas.drawText(text,
                30f,
                index*gapSize + top + textBound.height()*0.5f,
                textPaint)
        }
        val path = Path()
        path.moveTo(0f, -12f)
        path.lineTo(20f, 0f)
        path.lineTo(0f, 12f)
        path.close()
        canvas.save()
        canvas.translate(0f,transitioningGearSelectorIndex.current*gapSize + top)
        canvas.drawPath(path, redPaint)
        canvas.restore()
    }


    private fun computeActiveIntensity(index: Int): Float{
        var intensity = 1f-abs(index-transitioningGearSelectorIndex.current)
        if(intensity<0){
            intensity = 0f
        }
        return intensity
    }
}