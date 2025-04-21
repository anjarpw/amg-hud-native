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

    private var bitmapA: Bitmap =  BitmapFactory.decodeResource(resources,R.drawable.car5a)
    private var bitmapB: Bitmap =  BitmapFactory.decodeResource(resources,R.drawable.car5b)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private val transitioningLeftMotor: TransitioningValue<Float> = generateTransition(0f,0.5f,
        fun (from: Float, target: Float, progress: Float): Float{
            return from + (target - from) * progress
        })

    private val transitioningRightMotor: TransitioningValue<Float> = generateTransition(0f,0.5f,
        fun (from: Float, target: Float, progress: Float): Float{
            return from + (target - from) * progress
        })

    private val transitioningAlpha: TransitioningValue<Float> = generateTransition(0f,0.1f,
        fun (from: Float, target: Float, progress: Float): Float{
            return from + (target - from) * progress
        })


    init{
        fillPaint.style = Paint.Style.FILL
        fillPaint.color = Color.WHITE
        strokePaint.style = Paint.Style.STROKE
        additionalPaint.style = Paint.Style.FILL
        additionalPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
        invalidate() // Redraw the view
    }

    private var sizeProportion = 0.3f
    fun setSizeProportion(sizeRatio: Float){
        this.sizeProportion= sizeRatio
    }

    override fun doResize(width: Int, height: Int): Size {
        return Size((width*sizeProportion).toInt(), height)
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
    private var isVisible = false
    fun setVisibility(isVisible: Boolean){
        if(this.isVisible == isVisible){
            return
        }
        this.isVisible = isVisible
        val a = if(this.isVisible) 1f else 0f
        transitioningAlpha.resetTarget(a){ prevTarget ->
            abs(prevTarget-a) > transitioningAlpha.tolerance
        }
    }

    private val foregroundScalesCanvasManager = CanvasManager(fun(): Boolean{
        return transitioningAlpha.isUpdateRequired()
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


    private var centerX = 0f
    private var centerY = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        foregroundScalesCanvasManager.setSize(width, height)
    }

    override fun doDrawing(canvas: Canvas) {
        alpha = transitioningAlpha.current
        if(transitioningAlpha.current <= 0f){
            return
        }
        grandProportion = transitioningAlpha.current*0.5f + 0.5f
        centerX = width * 0.5f
        centerY = height*0.5f//width * 0.5f

        transformCanvasPerspective(canvas, fun(canvas: Canvas){
            drawPowerDistribution(canvas)
        })
        foregroundScalesCanvasManager.applyToCanvas(canvas, 0, 0, width, height)
        drawCar(canvas)
    }
    private val scalingSize = 100f
    private fun whiteShader(): Shader {
        val shader = LinearGradient(
            0f, scalingSize,
            0f, -scalingSize,
            intArrayOf(
                Color.parseColor("#00FFFFFF"),
                Color.parseColor("#22FFFFFF"),
                Color.parseColor("#22FFFFFF"),
                Color.parseColor("#00FFFFFF"),
            ),
            floatArrayOf(0f, 0.2f, 0.9f, 1.0f),
            Shader.TileMode.CLAMP
        )
        return shader
    }
    private fun blackShader(w: Float): Shader {
        val shader = LinearGradient(
            0f, 0f,
            w, 0f,
            intArrayOf(
                Color.parseColor("#00000000"),
                Color.parseColor("#33000000"),
                Color.parseColor("#FF000000"),
            ),
            floatArrayOf(0f, 0.5f, 1.0f),
            Shader.TileMode.CLAMP
        )
        return shader
    }

    private fun forwardShader(value:Float): Shader {
        val shader = LinearGradient(
            0f, scalingSize*value,
            0f, 0f,
            intArrayOf(
                BlueShadeHigh,
                BlueShadeLow,
            ),
            floatArrayOf(0f,1.0f),
            Shader.TileMode.CLAMP
        )
        return shader
    }
    private fun reverseShader(value:Float): Shader {
        val shader = LinearGradient(
            0f, scalingSize*value,
            0f, 0f,
            intArrayOf(
                PurpleShadeHigh,
                PurpleShadeLow,
            ),
            floatArrayOf(0f,1.0f),
            Shader.TileMode.CLAMP
        )
        return shader
    }
    private val thickness1 = 0.06f
    private val thickness2 = 0.12f
    private fun drawPowerScale(canvas: Canvas, value: Float){
        fillPaint.shader = whiteShader()
        canvas.drawRect(0f, -scalingSize, -width*thickness1, scalingSize, fillPaint)
        if(value>0){
            fillPaint.shader = forwardShader(value)
        }else{
            fillPaint.shader = reverseShader(value)
        }
        canvas.drawRect(0f, value*scalingSize, -width*thickness2, 0f, fillPaint)
    }

    private fun drawBackgroundScale(canvas: Canvas){
        strokePaint.strokeWidth = 2f
        fillPaint.shader = blackShader(-width*thickness2)
        canvas.drawRect(0f, -scalingSize, -width*thickness2, scalingSize, fillPaint)

        fun drawScaledLine(y: Float, scaleWidth: Float){
            canvas.drawLine(0f, y*scalingSize, -width*scaleWidth, y*scalingSize, strokePaint)
        }

        strokePaint.color = Color.parseColor("#22FFFFFF")
        for (i in 0..10) { // i will take values 1, 2, 3, 4, 5
            drawScaledLine(-1f+0.2f*i, thickness1*0.5f)
        }
        strokePaint.color = Color.parseColor("#44FFFFFF")
        canvas.drawLine(0f, scalingSize, 0f, -scalingSize, strokePaint)
        drawScaledLine(0f, thickness1)
        drawScaledLine(-1f,  thickness1)
        drawScaledLine(1f, thickness1)

    }


    private fun drawForegroundScales(canvas: Canvas){
        val shift = 0.20f*grandProportion
        canvas.scale(1f,-width*0.25f/scalingSize)
        canvas.translate(width*shift, 0f)
        drawBackgroundScale(canvas)
        canvas.scale(-1f,1f)
        canvas.translate(width*2*shift, 0f)
        drawBackgroundScale(canvas)
    }
    private fun drawPowerDistribution(canvas: Canvas){
        val shift = 0.20f*grandProportion
        canvas.scale(1f,-width*0.25f/scalingSize)
        canvas.translate(width*shift, 0f)
        drawPowerScale(canvas, transitioningRightMotor.current/255.0f)
        canvas.scale(-1f,1f)
        canvas.translate(width*2*shift, 0f)
        drawPowerScale(canvas, transitioningLeftMotor.current/255.0f)
    }

    private fun drawForegroundCar(canvas: Canvas){
        canvas.save()
        canvas.translate(centerX, centerY)
        val carWidth = width*0.40f
        val carHeight = carWidth*782/651
        val rectSource = Rect(0,0, bitmapA.width, bitmapA.height)
        val rectTarget = Rect(
            (-carWidth*0.5f).toInt(),
            (-carHeight*0.5f).toInt(),
            (carWidth*0.5f).toInt(),
            (carHeight*0.5f).toInt(),
        )
        canvas.drawBitmap(bitmapA, rectSource , rectTarget, fillPaint)
        canvas.restore()

    }
    private var grandProportion = 1f
    private fun drawCar(canvas: Canvas){
        canvas.save()
        canvas.translate(centerX, centerY)

        val carWidth = width*0.4f*grandProportion
        val carHeight = carWidth*782/651

        val rectSource = Rect(0,0, bitmapB.width, bitmapB.height)
        val rectTarget = Rect(
            (-carWidth*0.5f).toInt(),
            (-carHeight*0.5f).toInt(),
            (carWidth*0.5f).toInt(),
            (carHeight*0.5f).toInt(),
        )
        additionalPaint.color = Color.parseColor("#FFFFFFFF")
        canvas.drawBitmap(bitmapB, rectSource , rectTarget, additionalPaint)
        canvas.restore()

    }

}