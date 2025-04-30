package com.haskell.amghud.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Size
import com.haskell.amghud.R
import com.haskell.amghud.TransitioningValue
import kotlin.math.abs

class MiscView(context: Context, attrs: AttributeSet?) : BaseView(context, attrs) {

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private var threshold = 0.0f


    private val transitioningSteerRack: TransitioningValue<Float> = generateTransition(0.0f, 0.1f,
        fun(from: Float, target: Float, progress: Float): Float {
            return from + (target - from) * progress
        })

    fun setSteerRack(analogValue: Float) {
        val value = (analogValue - 512f) / 512f
        transitioningSteerRack.resetTarget(value) { prevTarget ->
            abs(prevTarget - value) > transitioningSteerRack.tolerance
        }
        checkToInvalidate()
    }

    private fun drawForegroundScales(canvas: Canvas) {
        strokePaint.color = Color.parseColor("#44FFFFFF")
        strokePaint.strokeWidth = 0.5f

        fun drawScaledLine(x: Float, h: Float) {
            canvas.drawLine(x * scalingSize, 0f, x * scalingSize, h, strokePaint)
        }

        for (i in -10..10) { // i will take values 1, 2, 3, 4, 5
            drawScaledLine(i.toFloat() / 10f, 10f)
        }
        drawScaledLine(0f, 30f)
        drawScaledLine(-0.5f, 20f)
        drawScaledLine(0.5f, 20f)
        drawScaledLine(-1f, 30f)
        drawScaledLine(1f, 30f)
    }

    private val foregroundScalesCanvasManager = CanvasManager(fun(): Boolean {
        return false
    }, fun(canvas: Canvas) {
        transformCanvasPerspective(canvas, fun(canvas: Canvas) {
            drawForegroundScales(canvas)
        })

    })

    override fun doResize(width: Int, height: Int): Size {
        return Size((width * 0.5f).toInt(), (height * 0.15f).toInt())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        foregroundScalesCanvasManager.setSize(width, height)
    }


    private fun transformCanvasPerspective(canvas: Canvas, callback: (canvas: Canvas) -> Unit) {
        canvas.save()
        val widthMinusMargin = width - 50
        canvas.translate(width * 0.5f, height * 1f)
        canvas.scale(widthMinusMargin * 0.5f / scalingSize, -1f)
        callback(canvas)
        canvas.restore()

    }

    private val arrowPaint = Paint()
    private val scalingSize = 100f
    override fun doDrawing(canvas: Canvas) {
        transformCanvasPerspective(canvas, fun(canvas: Canvas) {
            arrowPaint.color = DarkShade
            arrowPaint.style = Paint.Style.FILL
            val path = Path()
            path.moveTo(-4f, 50f)
            path.lineTo(0f, 0f)
            path.lineTo(4f, 50f)
            path.close()
            canvas.save()
            canvas.translate(transitioningSteerRack.current * scalingSize, 0f)
            canvas.drawPath(path, arrowPaint)
            canvas.restore()
        })
        drawBrakeIndicator(canvas)
        drawPowerIndicator(canvas)
        drawBLEIndicator(canvas)
        foregroundScalesCanvasManager.applyToCanvas(canvas, 0, 0, width, height)
    }

    val bitmapPaint = Paint()
    val iconRect = Rect(-48, -96, 48, 0)
    private var brakeBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.brake)
    private var powerBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.power)
    private var bleBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.ble)


    private var isBrakeOn = false
    private var isPowerOn = false
    private var isBLEAlive = false
    fun setBrakeIndicator(isOn: Boolean) {
        isBrakeOn = isOn
        invalidate()
    }

    fun setPowerIndicator(isOn: Boolean) {
        isPowerOn = isOn
        invalidate()
    }
    fun setBLEDeviceAlive(isOn: Boolean) {
        isBLEAlive = isOn
        invalidate()
    }

    private fun drawBrakeIndicator(canvas: Canvas) {
        bitmapPaint.style = Paint.Style.FILL
        bitmapPaint.color = if (isBrakeOn) Color.WHITE else Color.parseColor("#22000000")
        canvas.save()
        canvas.translate(width * 0.5f - 120f, height - 50f)
        canvas.drawBitmap(
            brakeBitmap,
            Rect(0, 0, brakeBitmap.width, brakeBitmap.height),
            iconRect,
            bitmapPaint
        )
        canvas.restore()
    }

    private fun drawPowerIndicator(canvas: Canvas) {
        bitmapPaint.style = Paint.Style.FILL
        bitmapPaint.color = if (isPowerOn) Color.WHITE else Color.parseColor("#22000000")
        canvas.save()
        canvas.translate(width * 0.5f + 120, height - 50f)
        canvas.drawBitmap(
            powerBitmap,
            Rect(0, 0, powerBitmap.width, powerBitmap.height),
            iconRect,
            bitmapPaint
        )
        canvas.restore()
    }

    private fun drawBLEIndicator(canvas: Canvas) {
        bitmapPaint.style = Paint.Style.FILL
        bitmapPaint.color = if (isBLEAlive) Color.WHITE else Color.parseColor("#22000000")
        canvas.save()
        canvas.translate(width * 0.5f, height - 50f)
        canvas.drawBitmap(
            bleBitmap,
            Rect(0, 0, bleBitmap.width, bleBitmap.height),
            iconRect,
            bitmapPaint
        )
        canvas.restore()
    }


}