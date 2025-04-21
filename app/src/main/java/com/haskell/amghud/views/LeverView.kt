package com.haskell.amghud.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.util.Size
import com.haskell.amghud.TransitioningValue
import kotlin.math.abs

class LeverView(context: Context, attrs: AttributeSet?) : BaseView(context, attrs)  {

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private var threshold = 0.0f


    private val transitioningValue: TransitioningValue<Float> = generateTransition(0.0f, 0.1f,
        fun (from: Float, target: Float, progress: Float): Float {
            return from + (target - from) * progress
        })

    fun setValue(value: Float){
        val cappedValue = value.coerceIn(0f, maxValue*1f)
        var displayedValue = 0f
        if(cappedValue<minValue){
            displayedValue = threshold*cappedValue/minValue
        }else{
            displayedValue = threshold + (1f-threshold)*(cappedValue-minValue)/(maxValue-minValue)
        }
        transitioningValue.resetTarget(displayedValue) { prevTarget ->
            abs(prevTarget-displayedValue) > transitioningValue.tolerance
        }
        checkToInvalidate()
    }

    private fun drawForegroundScales(canvas: Canvas){
        strokePaint.color = Color.parseColor("#AAFFFFFF")
        strokePaint.strokeWidth = 0.5f

        fun drawScaledLine(y: Float){
            canvas.drawLine(0f, y*scalingSize, boxWidth, y*scalingSize, strokePaint)
        }

        canvas.drawLine(0f, scalingSize, 0f, 0f, strokePaint)
        for (i in 0..10) { // i will take values 1, 2, 3, 4, 5
            drawScaledLine((i.toFloat()/10f)*(1f-threshold)+threshold)
        }
        drawScaledLine(0f)
    }

    private val foregroundScalesCanvasManager = CanvasManager(fun(): Boolean{
        return false
    }, fun(canvas: Canvas){
        transformCanvasPerspective(canvas, fun(canvas: Canvas){
            drawForegroundScales(canvas)
        })

    })

    override fun doResize(width: Int, height: Int): Size {
        return Size((width*0.05f).toInt(), height)
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        foregroundScalesCanvasManager.setSize(width, height)
    }
    private var minValue: Int = 0
    private var maxValue: Int = 0
    private var colorThemeHigh: Int = Color.WHITE
    private var colorThemeLow: Int = Color.BLACK

    fun setConfig(colorThemeHigh: Int, colorThemeLow: Int, min: Int, max: Int, threshold: Float){
        this.threshold = threshold
        minValue = min
        maxValue = max
        this.colorThemeHigh = colorThemeHigh
        this.colorThemeLow = colorThemeLow
    }

    private fun shader(value:Float): Shader {
        var colorArray = intArrayOf(
            colorThemeHigh,
            colorThemeLow
        )

        if(value<threshold){
            colorArray = intArrayOf(
                Color.parseColor("#66FFFFFF"),
                Color.parseColor("#22FFFFFF"),
            )
        }
        val shader = LinearGradient(
            0f, scalingSize*value,
            0f, 0f,
            colorArray,
            floatArrayOf(0f,1.0f),
            Shader.TileMode.CLAMP
        )
        return shader
    }

    private fun transformCanvasPerspective(canvas: Canvas, callback: (canvas: Canvas) -> Unit) {
        canvas.save()
        canvas.translate(width*0.5f-boxWidth*0.5f, height*0.5f + boxHeight*0.5f)
        canvas.scale(1f, -boxHeight/scalingSize)
        callback(canvas)
        canvas.restore()

    }

    private val paint = Paint()
    private val scalingSize = 100f
    private var boxWidth = 0f
    private var boxHeight = 0f
    override fun doDrawing(canvas: Canvas) {
        boxWidth = width*0.3f
        boxHeight = height*0.6f
        transformCanvasPerspective(canvas, fun(canvas: Canvas){
            paint.color = Color.parseColor("#FFFFFFFF")
            paint.shader = shader(transitioningValue.current)
            canvas.drawRect(0f, scalingSize*transitioningValue.current, boxWidth, 0f, paint)
        })
        foregroundScalesCanvasManager.applyToCanvas(canvas, 0, 0, width, height)

    }

}