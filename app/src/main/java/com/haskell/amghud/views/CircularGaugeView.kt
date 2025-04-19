package com.haskell.amghud.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.haskell.amghud.GearMode
import com.haskell.amghud.TransitioningValue


data class GaugeDisplayedConfig(
    var mode: GearMode = GearMode.P,
    var min: Float = 0.0f,
    var max: Float = 0.0f,
    var limit: Float = 0.0f,
    var circularSize: Float = 0.0f,
    var backgroundCircularSize: Float = 1.0f,
    var needleType: NeedleType = NeedleType.FULL
    )

enum class NeedleType {
    FULL,
    FLOATING,
}

data class GaugePosition(
    val tx: Float = 0.0f,
    val ty: Float = 0.0f,
    val w: Float = 0.0f,
    val h: Float = 0.0f,
    val radius: Float = 0.0f,
    val innerRadius: Float = 0.0f,
)


val DefaultGearP =  GaugeDisplayedConfig(mode=GearMode.P, min = 0f, max = 6f, limit = 0f, circularSize = 0.75f, backgroundCircularSize = 0.9f, needleType = NeedleType.FULL)
val gearConfigMap: Map<GearMode, GaugeDisplayedConfig> = mapOf(
    GearMode.T to GaugeDisplayedConfig(mode=GearMode.T, min = 0f, max = 6f, limit = 0f, circularSize = 0f, backgroundCircularSize = 0.9f, needleType = NeedleType.FULL),
    GearMode.P to DefaultGearP,
    GearMode.R to GaugeDisplayedConfig(mode=GearMode.R, min = 0f, max = 6f, limit = 4f, circularSize = 0.75f, backgroundCircularSize = 0.9f, needleType = NeedleType.FULL),
    GearMode.D to GaugeDisplayedConfig(mode=GearMode.D, min = 0f, max = 6f, limit = 4f, circularSize = 0.75f, backgroundCircularSize = 0.9f, needleType = NeedleType.FULL),
    GearMode.S to GaugeDisplayedConfig(mode=GearMode.S, min = 0f, max = 8f, limit = 6f, circularSize = 0.65f, backgroundCircularSize = 0.70f, needleType = NeedleType.FULL),
    GearMode.S_PLUS to GaugeDisplayedConfig(mode=GearMode.S_PLUS, min = 0f, max = 8f, limit = 9f, circularSize = 0.5f, backgroundCircularSize = 0.6f, needleType = NeedleType.FULL)
)


class CircularGaugeView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    val CUMULATED_POWER_FRACTION = 0.5f;
    val MODE_TRANSITION_FRACTION = 0.1f;

    private val gaugeRect = RectF()
    private var snapshotBitmap: Bitmap =  Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888)
    private var snapshotCanvas: Canvas = Canvas()
    private val srcBufferedRectangle: Rect = Rect()
    private val destBufferedRectangle: Rect = Rect()

    private val transitioningMode: TransitioningValue<GaugeDisplayedConfig> =
        object : TransitioningValue<GaugeDisplayedConfig>(DefaultGearP) {
            override fun extrapolate(
            ): GaugeDisplayedConfig {
                return GaugeDisplayedConfig(
                    mode = target.mode,
                    needleType = target.needleType,
                    min = from.min + (target.min - from.min) * progress,
                    max = from.max + (target.max - from.max) * progress,
                    limit = from.limit + (target.limit - from.limit) * progress,
                    circularSize = from.circularSize + (target.circularSize - from.circularSize) * progress,
                    backgroundCircularSize = from.backgroundCircularSize + (target.backgroundCircularSize - from.backgroundCircularSize) * progress,
                )
            }
        }
    private val transitioningCumulatedPower: TransitioningValue<Float> =
        object : TransitioningValue<Float>(0.0f) {
            override fun extrapolate(
            ): Float {
                return from + (target - from) * progress
            }
        }

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

    fun setGearMode(mode: GearMode){
        transitioningMode.resetTarget(gearConfigMap[mode] ?: DefaultGearP) { prevTarget ->
            prevTarget.mode != mode
        }
        checkToInvalidate()
    }

    fun setCumulatedPower(value: Float){
        transitioningCumulatedPower.resetTarget(value*8.0f) { prevTarget ->
            Math.abs(prevTarget-value) > transitioningCumulatedPower.tolerance
        }
        checkToInvalidate()
    }

    fun checkToInvalidate(){
        if(transitioningMode.isUpdateRequired() || transitioningCumulatedPower.isUpdateRequired()){
            invalidate()
        }
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        snapshotBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    }

    fun concludeGraphicalComputation(): GaugePosition {
        val mode = transitioningMode.current
        var xSize = 2f
        if(mode.backgroundCircularSize<0.5f){
            xSize = 2*Math.sin(mode.backgroundCircularSize*Math.PI).toFloat()
        }
        val ySize = 1f-Math.cos(mode.backgroundCircularSize*Math.PI).toFloat()

        val expectedAspectRatio =  xSize/ySize

        var w = width*0.7f
        var h = w/expectedAspectRatio
        if(h>height){
            h = height*1f
            w = h*expectedAspectRatio
        }
        var radius =  w/2

        if(mode.backgroundCircularSize<0.5f){
            radius = ((w/2)/Math.sin(mode.backgroundCircularSize*Math.PI)).toFloat()
        }
        val tx = width*0.5f
        val ty = height*0.5f - h*0.5f + radius

        return GaugePosition(tx, ty, w, h, radius, radius * 0.8f)
    }

    fun goldenShader(angle: Float, radius: Float): Shader{
        val shader = LinearGradient(
            0f, -radius,
            0f, -radius*Math.cos(angle*1.0).toFloat(),
            intArrayOf(
                Color.parseColor("#FFF6B26B"),
                Color.parseColor("#FFB45F06"),
                Color.parseColor("#11B45F06"),
            ),
            floatArrayOf(0.0f, 0.5f, 1.0f),
            Shader.TileMode.CLAMP
        )
        return shader
    }

    fun chromeShader(angle: Float, radius: Float): Shader{
        val shader = LinearGradient(
            0f, -radius,
            0f, -radius*Math.cos(angle*1.0).toFloat(),
            intArrayOf(
                Color.parseColor("#FFFFFFFF"),
                Color.parseColor("#EEFFFFFF"),
                Color.parseColor("#44FFFFFF"),
                Color.parseColor("#11FFFFFF"),
                Color.parseColor("#00FFFFFF"),
            ),
            floatArrayOf(0.0f, 0.1f, 0.4f,  0.9f, 1.0f),
            Shader.TileMode.CLAMP
        )
        return shader
    }
    fun thinChromeShader(angle: Float, radius: Float): Shader{
        val shader = LinearGradient(
            0f, -radius,
            0f, -radius*Math.cos(angle*1.0).toFloat(),
            intArrayOf(
                Color.parseColor("#55FFFFFF"),
                Color.parseColor("#22FFFFFF"),
            ),
            floatArrayOf( 0.8f, 1.0f),
            Shader.TileMode.CLAMP
        )
        return shader
    }


    fun drawBackground(gaugePosition: GaugePosition){
        snapshotCanvas = Canvas(snapshotBitmap)
        snapshotCanvas.save()
        snapshotCanvas.translate(gaugePosition.tx, gaugePosition.ty)
        snapshotCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        val mode = transitioningMode.current
        val radius = gaugePosition.innerRadius

        // drawBackground
        val chromeEdgePaint = Paint()
        chromeEdgePaint.style = Paint.Style.STROKE
        chromeEdgePaint.shader = thinChromeShader((mode.circularSize*Math.PI).toFloat(), radius)
        chromeEdgePaint.strokeWidth = 4f

        val chromePaint = Paint()
        chromePaint.style = Paint.Style.STROKE
        chromePaint.shader = chromeShader((mode.circularSize*Math.PI).toFloat(), radius)
        chromePaint.strokeWidth = 12f

        val goldenPaint = Paint()
        goldenPaint.shader = goldenShader((mode.circularSize*Math.PI).toFloat(), radius)
        goldenPaint.strokeWidth = 8f
        goldenPaint.style = Paint.Style.STROKE

        val redPaint = Paint()
        redPaint.style = Paint.Style.STROKE
        redPaint.color = Color.parseColor("#44FF0000")
        redPaint.strokeWidth = 160f

        val redLimit = mode.limit.coerceIn(mode.min, mode.max)
        val angleFrom = 270-mode.circularSize*180
        val angleTo = 270+mode.circularSize*180
        val redLimitAngle = (redLimit-mode.min)/(mode.max-mode.min)*mode.circularSize*360
        drawArcWithRadius(snapshotCanvas, radius, angleFrom, angleTo, chromeEdgePaint)
        drawArcWithRadius(snapshotCanvas, radius+12, angleFrom, angleTo, goldenPaint)
        drawArcWithRadius(snapshotCanvas, radius - 20f, angleFrom, angleTo, chromeEdgePaint)
        drawArcWithRadius(snapshotCanvas, radius-10f, angleFrom, angleTo, chromePaint)
        drawArcWithRadius(snapshotCanvas, radius-10f, angleFrom, angleTo, chromePaint)
        drawArcWithRadius(snapshotCanvas, radius-100f,angleFrom + redLimitAngle,angleTo, redPaint)


        drawScales(gaugePosition)
        snapshotCanvas.restore()

    }

    fun drawScales(gaugePosition: GaugePosition){
        val mode = transitioningMode.current
        val radius = gaugePosition.innerRadius

        val scalePaint = Paint()
        scalePaint.style = Paint.Style.STROKE

        val backgroundScalePaint = Paint()
        backgroundScalePaint.style = Paint.Style.STROKE
        backgroundScalePaint.color = Color.parseColor("#CC222222")
        val tolerance = 0.05f


        fun iterateScale(scaleFraction: Int, eachIteration: (idx: Int, scaleFraction: Int) -> Unit){
            val start = Math.floor(mode.min.toDouble()).toInt()
            val end = Math.ceil(Math.max(mode.max, transitioningMode.target.max).toDouble()).toInt()
            var idx: Int = start*scaleFraction
            var canContinue = true
            while (idx <= end*scaleFraction && canContinue) {
                eachIteration(idx, scaleFraction)
                idx++
                if(idx.toFloat()/scaleFraction > mode.max+tolerance){
                    canContinue = false
                }
            }
        }

        iterateScale (10) { idx, scaleFraction ->
            scalePaint.color = Color.parseColor("#88FFFFFF")
            var rInner = radius - 60f
            var rOuter = radius - 30f
            backgroundScalePaint.strokeWidth = 14f
            backgroundScalePaint.strokeWidth = 5f
            scalePaint.strokeWidth = 1f

            if(idx.toFloat()/scaleFraction>=mode.limit-tolerance){
                scalePaint.color = Color.RED
            }

            if(idx % scaleFraction == 0){
                rInner = radius - 60f
                rOuter = radius - 20f
                backgroundScalePaint.strokeWidth = 16f
                scalePaint.strokeWidth = 8f
            }
            val angle = (-1 + 2*(idx.toFloat()/scaleFraction-mode.min)/(mode.max-mode.min))*Math.PI*mode.circularSize
            snapshotCanvas.drawLine(
                (Math.sin(angle)*(rInner-4)).toFloat(),
                -(Math.cos(angle)*(rInner-4)).toFloat(),
                (Math.sin(angle)*(rOuter+4)).toFloat(),
                -(Math.cos(angle)*(rOuter+4)).toFloat(), backgroundScalePaint)

            snapshotCanvas.drawLine(
                (Math.sin(angle)*rInner).toFloat(),
                -(Math.cos(angle)*rInner).toFloat(),
                (Math.sin(angle)*rOuter).toFloat(),
                -(Math.cos(angle)*rOuter).toFloat(), scalePaint)

        }

        val textPaint = Paint()
        textPaint.typeface = typeface
        textPaint.textSize = 60f
        val bounds = Rect()
        textPaint.setShadowLayer(5f, 1f, 1f, Color.BLACK) // shadow radius, dx, dy, shadow color

        iterateScale(10) { idx, scaleFraction ->
            val rInner = radius - 100f;

            if(idx%scaleFraction == 0){
                textPaint.color = Color.WHITE
                if(idx.toFloat()/scaleFraction>=mode.limit-tolerance){
                    textPaint.color = Color.RED
                }

                var text = (idx/scaleFraction).toString()
                textPaint.getTextBounds(text, 0, text.length, bounds)
                val angle = (-1 + 2*(idx.toFloat()/scaleFraction-mode.min)/(mode.max-mode.min))*Math.PI*mode.circularSize
                snapshotCanvas.drawText(text,
                    0+(Math.sin(angle)*(rInner-4)).toFloat() - bounds.width()/2,
                    0-(Math.cos(angle)*(rInner-4)).toFloat() + bounds.height()/2,
                    textPaint)
            }
        }


    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if(width/height < 1){
            return
        }

        transitioningCumulatedPower.update(CUMULATED_POWER_FRACTION)
        transitioningMode.update(MODE_TRANSITION_FRACTION)

        render(canvas)

        checkToInvalidate()
    }


    private val paint = Paint()
    private fun render(canvas: Canvas) {
        val gaugePosition = concludeGraphicalComputation()

        srcBufferedRectangle.set(0, 0, snapshotBitmap.width, snapshotBitmap.height) // Source rectangle
        destBufferedRectangle.set(-gaugePosition.tx.toInt(), -gaugePosition.ty.toInt(), -gaugePosition.tx.toInt()+srcBufferedRectangle.width(), -gaugePosition.ty.toInt()+srcBufferedRectangle.height()) // Source rectangle


        canvas.save()
        canvas.translate(gaugePosition.tx,gaugePosition.ty)

        // Draw your content here. Example: a rectangle
        paint.setColor(-0x10000) // Red

        if(transitioningMode.current.circularSize > 0) {
            if(transitioningMode.isUpdateRequired()){
                drawBackground(gaugePosition)
            }else{
                Log.i("CircularGaugeView", "Use buffer")
            }
            drawBackgroundNeedle(canvas, gaugePosition)
            canvas.drawBitmap(snapshotBitmap, srcBufferedRectangle, destBufferedRectangle, Paint())
            drawNeedle(canvas, gaugePosition)
        }
        drawMode(canvas, gaugePosition)
        canvas.restore()
    }

    fun drawMode(canvas: Canvas, gaugePosition: GaugePosition){
        val gearOrders = arrayListOf(GearMode.T, GearMode.P, GearMode.R, GearMode.D, GearMode.S, GearMode.S_PLUS)
        val textTargetPaint = Paint()
        val radius = gaugePosition.innerRadius
        textTargetPaint.typeface = typeface
        textTargetPaint.textSize = (0.2f+0.8f*transitioningMode.progress)*radius/2.5f

        val textOriginPaint = Paint()
        textOriginPaint.typeface = typeface
        textOriginPaint.textSize = (1f-0.8f*transitioningMode.progress)*radius/2.5f
        val textTargetIndex = gearOrders.indexOf(transitioningMode.target.mode)
        val textOriginIndex = gearOrders.indexOf(transitioningMode.from.mode)

        val boundsTarget = Rect()
        val textTarget = transitioningMode.target.mode.stringAlias
        textTargetPaint.getTextBounds(textTarget, 0, textTarget.length, boundsTarget)
        textTargetPaint.setShadowLayer(5f, 1f, 1f, Color.BLACK) // shadow radius, dx, dy, shadow color
        textTargetPaint.color = Color.WHITE
        textTargetPaint.alpha = (transitioningMode.progress*255).toInt()

        val boundsOrigin = Rect()
        val textOrigin = transitioningMode.from.mode.stringAlias
        textOriginPaint.getTextBounds(textOrigin, 0, textOrigin.length, boundsOrigin)
        textOriginPaint.setShadowLayer(5f, 1f, 1f, Color.BLACK) // shadow radius, dx, dy, shadow color
        textOriginPaint.color = Color.WHITE
        textOriginPaint.alpha = 255-(transitioningMode.progress*255).toInt()

        val direction = if (textTargetIndex > textOriginIndex) 1 else -1
        var verticalTargetOffset = (1-transitioningMode.progress)* direction * radius/4f
        var verticalOriginOffset = -transitioningMode.progress* direction * radius/4f

        val bottom = -Math.cos(Math.PI*transitioningMode.current.circularSize).toFloat()
        canvas.drawText(textOrigin,
            -boundsOrigin.width().toFloat()/2,
            Math.min(0f, bottom) + boundsOrigin.height()/2 + verticalOriginOffset,
            textOriginPaint)


        canvas.drawText(textTarget,
            -boundsTarget.width().toFloat()/2,
            Math.min(0f, bottom) + boundsTarget.height()/2 + verticalTargetOffset,
            textTargetPaint)

    }

    fun drawArcWithRadius(canvas: Canvas, radius: Float, from: Float, to: Float, paint: Paint) {
        gaugeRect.set(-radius, -radius, radius, radius)
        canvas.drawArc(gaugeRect, from, to-from, false, paint)
    }

    fun drawBackgroundNeedle(canvas: Canvas, gaugePosition: GaugePosition) {
        val mode = transitioningMode.current
        val radius = gaugePosition.innerRadius

        val powerPercentage = (transitioningCumulatedPower.current.coerceIn(mode.min, mode.limit)-mode.min)/(mode.max-mode.min)
        val powerAngle = powerPercentage*mode.circularSize*360


        val startAngle = 0.75f-mode.circularSize*0.5f
        var stopAngle = startAngle + (mode.circularSize*powerPercentage)

        val sweepGradient = SweepGradient(
            0f,
            0f,
            intArrayOf(
                Color.parseColor("#11004488"),
                Color.parseColor("#883388FF"),
            ),
            floatArrayOf(startAngle, stopAngle)
        )

        // Set the shader for the Paint object
        val blueShadePaint = Paint()
        blueShadePaint.style = Paint.Style.STROKE
        blueShadePaint.shader = sweepGradient
        blueShadePaint.strokeWidth = 160f

        drawArcWithRadius(canvas, radius-100f, 270-mode.circularSize*180, 270-mode.circularSize*180+powerAngle, blueShadePaint)

    }
    fun drawNeedle(canvas: Canvas, gaugePosition: GaugePosition){
        val mode = transitioningMode.current
        var cumulatedPower = transitioningCumulatedPower.current.coerceIn(mode.min, mode.limit)
        val radius = gaugePosition.innerRadius
        val radialGradient = RadialGradient(
            0f,
            0f,
            radius-20,
            intArrayOf(
                Color.parseColor("#00FF0000"),
                Color.parseColor("#88FF0000"),
                Color.parseColor("#FFFF0000"),
                Color.parseColor("#88FFFFFF"),
                Color.parseColor("#44FF0000"),

                ), // Colors in the gradient
            floatArrayOf(0f, 0.1f, 0.96f, 0.98f, 1f), // Relative positions of the colors (null for evenly distributed)
            Shader.TileMode.CLAMP // Tile mode (CLAMP, REPEAT, MIRROR)
        )
        val paint = Paint()
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#FF0000")
        paint.shader =radialGradient
        paint.strokeWidth = 8f


        var angle = mode.circularSize*Math.PI * (-1 + 2*(cumulatedPower-mode.min)/(mode.max-mode.min))
        var rInner = radius*0.1f
        val rOuter = radius-20f

        if(mode.needleType == NeedleType.FLOATING){
            rInner = radius*0.8f
        }
        canvas.drawLine(
            (Math.sin(angle)*rOuter).toFloat(),
            -(Math.cos(angle)*rOuter).toFloat(),
            (Math.sin(angle)*rInner).toFloat(),
            -(Math.cos(angle)*rInner).toFloat(),
            paint)

    }
}