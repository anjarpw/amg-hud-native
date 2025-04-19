package com.haskell.amghud.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Camera
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import com.haskell.amghud.R
import com.haskell.amghud.TransitioningValue
import kotlin.math.abs

class TractionView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val camera: Camera = Camera()
    private val matrix: Matrix = Matrix()
    private val additionalPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private var bitmapA: Bitmap? = null
    private var bitmapB: Bitmap? = null
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private val transitioningLeftMotor: TransitioningValue<Float> =
        object : TransitioningValue<Float>(0.0f) {
            override fun extrapolate(
            ): Float {
                return from + (target - from) * progress
            }
        }

    private val transitioningRightMotor: TransitioningValue<Float> =
        object : TransitioningValue<Float>(0.0f) {
            override fun extrapolate(
            ): Float {
                return from + (target - from) * progress
            }
        }

    init{
        bitmapA = BitmapFactory.decodeResource(resources,R.drawable.car5a)
        bitmapB = BitmapFactory.decodeResource(resources,R.drawable.car5b)
        invalidate() // Redraw the view
        fillPaint.style = Paint.Style.FILL
        strokePaint.style = Paint.Style.STROKE
        additionalPaint.style = Paint.Style.FILL
        additionalPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val screenWidth = resources.displayMetrics.widthPixels
        val resizedWidth = screenWidth * 0.3f

        // Set the measured dimensions to be a square
        setMeasuredDimension(resizedWidth.toInt(), resources.displayMetrics.heightPixels)
    }

    fun setLeftMotor(value: Float){
        transitioningLeftMotor.resetTarget(value) { prevTarget ->
            abs(prevTarget-value) > transitioningLeftMotor.tolerance
        }
        checkToInvalidate()
    }
    fun setRightMotor(value: Float){
        transitioningRightMotor.resetTarget(value) { prevTarget ->
            abs(prevTarget-value) > transitioningRightMotor.tolerance
        }
        checkToInvalidate()
    }
    private fun checkToInvalidate(){
        if(transitioningRightMotor.isUpdateRequired() || transitioningLeftMotor.isUpdateRequired()){
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {

        super.onDraw(canvas)
        transitioningLeftMotor.update(0.1f)
        transitioningRightMotor.update(0.1f)

        val centerX = width * 0.65f
        val centerY = height*0.5f//width * 0.5f

        canvas.save()
        camera.save()
        // Rotate around the X-axis (you can adjust the angle)
        camera.rotateX(30f)
        camera.rotateY(0f)
        camera.getMatrix(matrix)
        camera.restore()

        // Set the pivot point for the matrix transformation
        matrix.preTranslate(-centerX, -centerY)
        matrix.postTranslate(centerX, centerY)

        canvas.concat(matrix)
        canvas.translate(centerX, centerY)
        drawScales(canvas)
        canvas.restore()
        canvas.save()
        canvas.translate(centerX, centerY)
        drawCar(canvas)
        canvas.restore()

        checkToInvalidate()
    }
    private val forwardTop = -0.25f
    private val reverseBottom = 0.25f
    private fun shader(): Shader {
        val shader = LinearGradient(
            0f, width*forwardTop,
            0f, width*reverseBottom,
            intArrayOf(
                Color.parseColor("#00FFFFFF"),
                Color.parseColor("#11FFFFFF"),
                Color.parseColor("#11FFFFFF"),
                Color.parseColor("#00FFFFFF"),
            ),
            floatArrayOf(0f, 0.2f, 0.9f, 1.0f),
            Shader.TileMode.CLAMP
        )
        return shader
    }
    private fun forwardShader(value:Float): Shader {
        val shader = LinearGradient(
            0f, width*forwardTop*value,
            0f, 0f,
            intArrayOf(
                Color.parseColor("#FF00AEFF"),
                Color.parseColor("#44001E80"),
            ),
            floatArrayOf(0f,1.0f),
            Shader.TileMode.CLAMP
        )
        return shader
    }
    private fun reverseShader(value:Float): Shader {
        val shader = LinearGradient(
            0f, -width*reverseBottom*value,
            0f, 0f,
            intArrayOf(
                Color.parseColor("#FF8F17FF"),
                Color.parseColor("#4447008A"),
            ),
            floatArrayOf(0f,1.0f),
            Shader.TileMode.CLAMP
        )
        return shader
    }
    private fun drawScale(canvas: Canvas, value: Float){
        val thickness = 0.06f
        fillPaint.shader = shader()
        canvas.drawRect(0f, width*forwardTop, -width*thickness, width*reverseBottom, fillPaint)
        if(value>0){
            fillPaint.shader = forwardShader(value)
        }else{
            fillPaint.shader = reverseShader(value)
        }
        canvas.drawRect(0f, width*forwardTop*value, -width*thickness, 0f, fillPaint)
        strokePaint.color = Color.parseColor("#88FFFFFF")
        strokePaint.strokeWidth = 2f

        fun drawScaledLine(index: Int, totalScale: Int,  scaleWidth: Float){
            val y = width*((index.toFloat()/totalScale)*(forwardTop-reverseBottom)+reverseBottom)
            canvas.drawLine(0f, y, -width*scaleWidth, y, strokePaint)

        }

        strokePaint.color = Color.parseColor("#44FFFFFF")
        canvas.drawLine(0f, width*forwardTop, 0f, width*reverseBottom, strokePaint)
        for (i in 0..10) { // i will take values 1, 2, 3, 4, 5
            drawScaledLine(i, 10, thickness*0.5f)
        }
        drawScaledLine(0, 10, thickness)
        drawScaledLine(5, 10, thickness)
        drawScaledLine(10, 10, thickness)

    }

    private fun drawScales(canvas: Canvas){
        val shift = 0.22f
        fillPaint.color = Color.WHITE
        //canvas.drawRect(-width*shift, width*forwardTop, width*shift, width*reverseBottom, fillPaint)
        canvas.translate(width*shift, 0f)
        drawScale(canvas, transitioningRightMotor.current/255.0f)
        canvas.scale(-1f,1f)
        canvas.translate(width*2*shift, 0f)
        drawScale(canvas, transitioningLeftMotor.current/255.0f)

    }

    private fun drawCar(canvas: Canvas){

        val carWidth = width*0.45f
        val carHeight = carWidth*782/651

        val rectSource = Rect(0,0, bitmapA!!.width, bitmapA!!.height)
        val rectTarget = Rect(
            (-carWidth*0.5f).toInt(),
            (-carHeight*0.5f).toInt(),
            (carWidth*0.5f).toInt(),
            (carHeight*0.5f).toInt(),
        )
        strokePaint.color = Color.parseColor("#88FFFFFF")

        canvas.drawBitmap(bitmapA!!, rectSource , rectTarget, fillPaint)
        additionalPaint.color = Color.parseColor("#FFFFFFFF")
        canvas.drawBitmap(bitmapB!!, rectSource , rectTarget, additionalPaint)
    }
}