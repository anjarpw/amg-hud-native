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
import android.util.Size
import com.haskell.amghud.R
import com.haskell.amghud.TransitioningValue
import kotlin.math.abs

class TractionView(context: Context, attrs: AttributeSet?) : BaseView(context, attrs) {
    private val camera: Camera = Camera()
    private val matrix: Matrix = Matrix()
    private val additionalPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private var bitmapA: Bitmap? = null
    private var bitmapB: Bitmap? = null
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private val transitioningLeftMotor: TransitioningValue<Float> = generateTransition(0f,0.1f,
        fun (from: Float, target: Float, progress: Float): Float{
            return from + (target - from) * progress
        })

    private val transitioningRightMotor: TransitioningValue<Float> = generateTransition(0f,0.1f,
        fun (from: Float, target: Float, progress: Float): Float{
            return from + (target - from) * progress
        })


    init{
        bitmapA = BitmapFactory.decodeResource(resources,R.drawable.car5a)
        bitmapB = BitmapFactory.decodeResource(resources,R.drawable.car5b)
        invalidate() // Redraw the view
        fillPaint.style = Paint.Style.FILL
        fillPaint.color = Color.WHITE
        strokePaint.style = Paint.Style.STROKE
        additionalPaint.style = Paint.Style.FILL
        additionalPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }

    override fun doResize(width: Int, height: Int): Size {
        return Size((width*0.3f).toInt(), height)
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

    private val foregroundScalesCanvasManager = CanvasManager(fun(): Boolean{
        return false
    }, fun(canvas: Canvas){
        transformCanvasPerspective(canvas, fun(canvas: Canvas){
            drawForegroundScales(canvas)
        })
        drawForegroundCar(canvas)
    })


    private fun transformCanvasPerspective(canvas: Canvas, callback: (canvas: Canvas) -> Unit){
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
        callback(canvas)
        canvas.restore()

    }


    var centerX = 0f
    var centerY = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        foregroundScalesCanvasManager.setSize(width, height)
    }

    override fun doDrawing(canvas: Canvas) {
        centerX = width * 0.65f
        centerY = height*0.5f//width * 0.5f

        transformCanvasPerspective(canvas, fun(canvas: Canvas){
            drawPowerDistribution(canvas)
        })
        foregroundScalesCanvasManager.applyToCanvas(canvas, 0, 0, width, height)
        drawCar(canvas)
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
    private fun drawPowerScale(canvas: Canvas, value: Float){
        val thickness = 0.06f
        fillPaint.shader = shader()
        canvas.drawRect(0f, width*forwardTop, -width*thickness, width*reverseBottom, fillPaint)
        if(value>0){
            fillPaint.shader = forwardShader(value)
        }else{
            fillPaint.shader = reverseShader(value)
        }
        canvas.drawRect(0f, width*forwardTop*value, -width*thickness, 0f, fillPaint)
    }

    private fun drawBackgroundScale(canvas: Canvas){
        val thickness = 0.06f
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


    private fun drawForegroundScales(canvas: Canvas){
        val shift = 0.22f
        canvas.translate(width*shift, 0f)
        drawBackgroundScale(canvas)
        canvas.scale(-1f,1f)
        canvas.translate(width*2*shift, 0f)
        drawBackgroundScale(canvas)
    }
    private fun drawPowerDistribution(canvas: Canvas){
        val shift = 0.22f
        canvas.translate(width*shift, 0f)
        drawPowerScale(canvas, transitioningRightMotor.current/255.0f)
        canvas.scale(-1f,1f)
        canvas.translate(width*2*shift, 0f)
        drawPowerScale(canvas, transitioningLeftMotor.current/255.0f)
    }

    private fun drawForegroundCar(canvas: Canvas){
        canvas.save()
        canvas.translate(centerX, centerY)


        val carWidth = width*0.45f
        val carHeight = carWidth*782/651

        val rectSource = Rect(0,0, bitmapA!!.width, bitmapA!!.height)
        val rectTarget = Rect(
            (-carWidth*0.5f).toInt(),
            (-carHeight*0.5f).toInt(),
            (carWidth*0.5f).toInt(),
            (carHeight*0.5f).toInt(),
        )
        canvas.drawBitmap(bitmapA!!, rectSource , rectTarget, fillPaint)
        canvas.restore()

    }
    private fun drawCar(canvas: Canvas){
        canvas.save()
        canvas.translate(centerX, centerY)


        val carWidth = width*0.45f
        val carHeight = carWidth*782/651

        val rectSource = Rect(0,0, bitmapB!!.width, bitmapB!!.height)
        val rectTarget = Rect(
            (-carWidth*0.5f).toInt(),
            (-carHeight*0.5f).toInt(),
            (carWidth*0.5f).toInt(),
            (carHeight*0.5f).toInt(),
        )
        additionalPaint.color = Color.parseColor("#FFFFFFFF")
        canvas.drawBitmap(bitmapB!!, rectSource , rectTarget, additionalPaint)
        canvas.restore()

    }

}