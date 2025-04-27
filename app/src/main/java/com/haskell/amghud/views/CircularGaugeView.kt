package com.haskell.amghud.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.util.AttributeSet
import com.haskell.amghud.GearMode
import com.haskell.amghud.TransitioningValue
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin


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


val DefaultGearP = GaugeDisplayedConfig(
    mode = GearMode.P,
    min = 0f,
    max = 6f,
    limit = 0f,
    circularSize = 0.75f,
    backgroundCircularSize = 0.9f,
    needleType = NeedleType.FULL
)
val gearConfigMap: Map<GearMode, GaugeDisplayedConfig> = mapOf(
    GearMode.T to GaugeDisplayedConfig(
        mode = GearMode.T,
        min = 0f,
        max = 6f,
        limit = 0f,
        circularSize = 0f,
        backgroundCircularSize = 0.9f,
        needleType = NeedleType.FULL
    ),
    GearMode.P to DefaultGearP,
    GearMode.R to GaugeDisplayedConfig(
        mode = GearMode.R,
        min = 0f,
        max = 6f,
        limit = 4f,
        circularSize = 0.75f,
        backgroundCircularSize = 0.9f,
        needleType = NeedleType.FULL
    ),
    GearMode.D to GaugeDisplayedConfig(
        mode = GearMode.D,
        min = 0f,
        max = 6f,
        limit = 4f,
        circularSize = 0.75f,
        backgroundCircularSize = 0.9f,
        needleType = NeedleType.FULL
    ),
    GearMode.S to GaugeDisplayedConfig(
        mode = GearMode.S,
        min = 0f,
        max = 8f,
        limit = 6f,
        circularSize = 0.65f,
        backgroundCircularSize = 0.70f,
        needleType = NeedleType.FULL
    ),
    GearMode.S_PLUS to GaugeDisplayedConfig(
        mode = GearMode.S_PLUS,
        min = 0f,
        max = 8f,
        limit = 9f,
        circularSize = 0.5f,
        backgroundCircularSize = 0.65f,
        needleType = NeedleType.FULL
    )
)

private const val CUMULATED_POWER_FRACTION = 0.5f
private const val MODE_TRANSITION_FRACTION = 0.1f

class CircularGaugeView(context: Context, attrs: AttributeSet?) : BaseView(context, attrs) {
    private val gaugeRect = RectF()

    private val transitioningMode: TransitioningValue<GaugeDisplayedConfig> = generateTransition(
        DefaultGearP, MODE_TRANSITION_FRACTION,
        fun(
            from: GaugeDisplayedConfig,
            target: GaugeDisplayedConfig,
            progress: Float
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

        })
    private val transitioningCumulatedPower: TransitioningValue<Float> =
        generateTransition(0.0f, CUMULATED_POWER_FRACTION,
            fun(from: Float, target: Float, progress: Float): Float {
                return from + (target - from) * progress

            })

    private var typeface: Typeface? = null

    init {
        try {
            typeface = Typeface.createFromAsset(
                context.assets,
                "Exo-BoldItalic.ttf"
            ) // Replace with your font file
            typeface = Typeface.create(typeface, Typeface.ITALIC)
        } catch (e: Exception) {
            e.printStackTrace()
            typeface = Typeface.DEFAULT // Use default font if loading fails.
        }
    }

    fun setGearMode(mode: GearMode) {
        transitioningMode.resetTarget(gearConfigMap[mode] ?: DefaultGearP) { prevTarget ->
            prevTarget.mode != mode
        }
        checkToInvalidate()
    }

    fun setCumulatedPower(value: Float) {
        transitioningCumulatedPower.resetTarget(value * 8.0f) { prevTarget ->
            abs(prevTarget - value) > transitioningCumulatedPower.tolerance
        }
        checkToInvalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        backgroundCanvasManager.setSize(width, height)
    }

    private fun concludeGraphicalComputation() {
        val mode = transitioningMode.current
        var xSize = 2f
        if (mode.backgroundCircularSize < 0.5f) {
            xSize = 2 * sin(mode.backgroundCircularSize * Math.PI).toFloat()
        }
        val ySize = 1f - cos(mode.backgroundCircularSize * Math.PI).toFloat()

        val expectedAspectRatio = xSize / ySize

        var w = width * 0.7f
        var h = w / expectedAspectRatio
        if (h > height) {
            h = height * 1f
            w = h * expectedAspectRatio
        }
        var radius = w / 2

        if (mode.backgroundCircularSize < 0.5f) {
            radius = ((w / 2) / sin(mode.backgroundCircularSize * Math.PI)).toFloat()
        }
        val tx = width * 0.5f
        val ty = height * 0.5f - h * 0.5f + radius

        gaugePosition = GaugePosition(tx, ty, w, h, radius, radius * 0.8f)
    }

    private fun radialBlackShader(radius1: Float, radius2: Float): Shader {
        return RadialGradient(
            0f, 0f, radius2, intArrayOf(
                Color.parseColor("#AA000000"),
                Color.TRANSPARENT
            ),
            floatArrayOf(radius1 / radius2, 1f),
            Shader.TileMode.CLAMP
        )
    }

    private fun goldenShader(angle: Float, radius: Float): Shader {
        val shader = LinearGradient(
            0f, -radius,
            0f, -radius * cos(angle * 1.0).toFloat(),
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

    private fun chromeShader(angle: Float, radius: Float): Shader {
        val shader = LinearGradient(
            0f, -radius,
            0f, -radius * cos(angle * 1.0).toFloat(),
            intArrayOf(
                Color.parseColor("#FFFFFFFF"),
                Color.parseColor("#EEFFFFFF"),
                Color.parseColor("#44FFFFFF"),
                Color.parseColor("#11FFFFFF"),
                Color.parseColor("#00FFFFFF"),
            ),
            floatArrayOf(0.0f, 0.1f, 0.4f, 0.9f, 1.0f),
            Shader.TileMode.CLAMP
        )
        return shader
    }

    private fun thinChromeShader(angle: Float, radius: Float): Shader {
        val shader = LinearGradient(
            0f, -radius,
            0f, -radius * cos(angle * 1.0).toFloat(),
            intArrayOf(
                Color.parseColor("#55FFFFFF"),
                Color.parseColor("#22FFFFFF"),
            ),
            floatArrayOf(0.8f, 1.0f),
            Shader.TileMode.CLAMP
        )
        return shader
    }

    private var gaugePosition: GaugePosition = GaugePosition();

    private val backgroundCanvasManager = CanvasManager(fun(): Boolean {
        return transitioningMode.isUpdateRequired()
    },
        fun(canvas: Canvas) {
            canvas.save()
            canvas.translate(gaugePosition.tx, gaugePosition.ty)
            val mode = transitioningMode.current
            val radius = gaugePosition.innerRadius

            // drawBackground
            val chromeEdgePaint = Paint()
            chromeEdgePaint.style = Paint.Style.STROKE
            chromeEdgePaint.shader =
                thinChromeShader((mode.circularSize * Math.PI).toFloat(), radius)
            chromeEdgePaint.strokeWidth = 4f

            val chromePaint = Paint()
            chromePaint.style = Paint.Style.STROKE
            chromePaint.shader = chromeShader((mode.circularSize * Math.PI).toFloat(), radius)
            chromePaint.strokeWidth = 12f

            val goldenPaint = Paint()
            goldenPaint.shader = goldenShader((mode.circularSize * Math.PI).toFloat(), radius)
            goldenPaint.strokeWidth = 8f
            goldenPaint.style = Paint.Style.STROKE


            val blackShadePaint = Paint()
            blackShadePaint.shader = radialBlackShader(radius - 180, radius - 50)
            blackShadePaint.style = Paint.Style.FILL

            val redPaint = Paint()
            redPaint.style = Paint.Style.STROKE
            redPaint.color = Color.parseColor("#44FF0000")
            redPaint.strokeWidth = 160f

            val redLimit = mode.limit.coerceIn(mode.min, mode.max)
            val angleFrom = 270 - mode.circularSize * 180
            val angleTo = 270 + mode.circularSize * 180
            val redLimitAngle =
                (redLimit - mode.min) / (mode.max - mode.min) * mode.circularSize * 360
            drawArcWithRadius(canvas, radius, angleFrom, angleTo, chromeEdgePaint)
            drawArcWithRadius(canvas, radius + 12, angleFrom, angleTo, goldenPaint)
            drawArcWithRadius(canvas, radius - 20f, angleFrom, angleTo, chromeEdgePaint)
            drawArcWithRadius(canvas, radius - 10f, angleFrom, angleTo, chromePaint)
            drawArcWithRadius(canvas, radius - 10f, angleFrom, angleTo, chromePaint)
            drawArcWithRadius(canvas, radius - 100f, angleFrom + redLimitAngle, angleTo, redPaint)
            canvas.drawCircle(0f, 0f, radius, blackShadePaint)
            drawScales(canvas)
            canvas.restore()
        })


    private fun drawScales(canvas: Canvas) {
        val mode = transitioningMode.current
        val radius = gaugePosition.innerRadius

        val scalePaint = Paint()
        scalePaint.style = Paint.Style.STROKE

        val backgroundScalePaint = Paint()
        backgroundScalePaint.style = Paint.Style.STROKE
        backgroundScalePaint.color = Color.parseColor("#CC222222")
        val tolerance = 0.05f


        fun iterateScale(
            scaleFraction: Int,
            eachIteration: (idx: Int, scaleFraction: Int) -> Unit
        ) {
            val start = floor(mode.min.toDouble()).toInt()
            val end = ceil(max(mode.max, transitioningMode.target.max).toDouble()).toInt()
            var idx: Int = start * scaleFraction
            var canContinue = true
            while (idx <= end * scaleFraction && canContinue) {
                eachIteration(idx, scaleFraction)
                idx++
                if (idx.toFloat() / scaleFraction > mode.max + tolerance) {
                    canContinue = false
                }
            }
        }

        iterateScale(10) { idx, scaleFraction ->
            scalePaint.color = Color.parseColor("#88FFFFFF")
            var rInner = radius - 60f
            var rOuter = radius - 30f
            backgroundScalePaint.strokeWidth = 14f
            backgroundScalePaint.strokeWidth = 5f
            scalePaint.strokeWidth = 1f

            if (idx.toFloat() / scaleFraction >= mode.limit - tolerance) {
                scalePaint.color = Color.RED
            }

            if (idx % scaleFraction == 0) {
                rInner = radius - 60f
                rOuter = radius - 20f
                backgroundScalePaint.strokeWidth = 16f
                scalePaint.strokeWidth = 8f
            }
            val angle =
                (-1 + 2 * (idx.toFloat() / scaleFraction - mode.min) / (mode.max - mode.min)) * Math.PI * mode.circularSize
            canvas.drawLine(
                (sin(angle) * (rInner - 4)).toFloat(),
                -(cos(angle) * (rInner - 4)).toFloat(),
                (sin(angle) * (rOuter + 4)).toFloat(),
                -(cos(angle) * (rOuter + 4)).toFloat(), backgroundScalePaint
            )

            canvas.drawLine(
                (sin(angle) * rInner).toFloat(),
                -(cos(angle) * rInner).toFloat(),
                (sin(angle) * rOuter).toFloat(),
                -(cos(angle) * rOuter).toFloat(), scalePaint
            )

        }

        val textPaint = Paint()
        textPaint.typeface = typeface
        textPaint.textSize = 60f
        val bounds = Rect()
        textPaint.setShadowLayer(5f, 1f, 1f, Color.BLACK) // shadow radius, dx, dy, shadow color

        iterateScale(10) { idx, scaleFraction ->
            val rInner = radius - 100f

            if (idx % scaleFraction == 0) {
                textPaint.color = Color.WHITE
                if (idx.toFloat() / scaleFraction >= mode.limit - tolerance) {
                    textPaint.color = Color.RED
                }

                val text = (idx / scaleFraction).toString()
                textPaint.getTextBounds(text, 0, text.length, bounds)
                val angle =
                    (-1 + 2 * (idx.toFloat() / scaleFraction - mode.min) / (mode.max - mode.min)) * Math.PI * mode.circularSize
                canvas.drawText(
                    text,
                    0 + (sin(angle) * (rInner - 4)).toFloat() - bounds.width() / 2,
                    0 - (cos(angle) * (rInner - 4)).toFloat() + bounds.height() / 2,
                    textPaint
                )
            }
        }
    }

    override fun doDrawing(canvas: Canvas) {
        render(canvas)
    }


    private val paint = Paint()
    private fun render(canvas: Canvas) {
        concludeGraphicalComputation()

        //destBufferedRectangle.set() // Source rectangle


        canvas.save()
        canvas.translate(gaugePosition.tx, gaugePosition.ty)

        // Draw your content here. Example: a rectangle
        paint.color = -0x10000 // Red

        if (transitioningMode.current.circularSize > 0) {
            drawBackgroundNeedle(canvas)
            backgroundCanvasManager.applyToCanvas(
                canvas,
                -gaugePosition.tx.toInt(),
                -gaugePosition.ty.toInt(),
                -gaugePosition.tx.toInt() + width,
                -gaugePosition.ty.toInt() + height
            )
            drawNeedle(canvas)
        }
        drawMode(canvas)
        canvas.restore()
    }

    private fun drawMode(canvas: Canvas) {
        val gearOrders =
            arrayListOf(GearMode.T, GearMode.P, GearMode.R, GearMode.D, GearMode.S, GearMode.S_PLUS)
        val textTargetPaint = Paint()
        val radius = gaugePosition.innerRadius
        textTargetPaint.typeface = typeface
        textTargetPaint.textSize = (0.2f + 0.8f * transitioningMode.progress) * radius / 2.5f

        val textOriginPaint = Paint()
        textOriginPaint.typeface = typeface
        textOriginPaint.textSize = (1f - 0.8f * transitioningMode.progress) * radius / 2.5f
        val textTargetIndex = gearOrders.indexOf(transitioningMode.target.mode)
        val textOriginIndex = gearOrders.indexOf(transitioningMode.from.mode)

        val boundsTarget = Rect()
        val textTarget = transitioningMode.target.mode.stringAlias
        textTargetPaint.getTextBounds(textTarget, 0, textTarget.length, boundsTarget)
        textTargetPaint.setShadowLayer(
            5f,
            1f,
            1f,
            Color.BLACK
        ) // shadow radius, dx, dy, shadow color
        textTargetPaint.color = Color.WHITE
        textTargetPaint.alpha = (transitioningMode.progress * 255).toInt()

        val boundsOrigin = Rect()
        val textOrigin = transitioningMode.from.mode.stringAlias
        textOriginPaint.getTextBounds(textOrigin, 0, textOrigin.length, boundsOrigin)
        textOriginPaint.setShadowLayer(
            5f,
            1f,
            1f,
            Color.BLACK
        ) // shadow radius, dx, dy, shadow color
        textOriginPaint.color = Color.WHITE
        textOriginPaint.alpha = 255 - (transitioningMode.progress * 255).toInt()

        val direction = if (textTargetIndex > textOriginIndex) 1 else -1
        val verticalTargetOffset = (1 - transitioningMode.progress) * direction * radius / 4f
        val verticalOriginOffset = -transitioningMode.progress * direction * radius / 4f

        val bottom = -cos(Math.PI * transitioningMode.current.circularSize).toFloat()
        canvas.drawText(
            textOrigin,
            -boundsOrigin.width().toFloat() * 0.6f,
            0f.coerceAtMost(bottom) + boundsOrigin.height() / 2 + verticalOriginOffset,
            textOriginPaint
        )


        canvas.drawText(
            textTarget,
            -boundsTarget.width().toFloat() * 0.6f,
            0f.coerceAtMost(bottom) + boundsTarget.height() / 2 + verticalTargetOffset,
            textTargetPaint
        )

    }

    private fun drawArcWithRadius(
        canvas: Canvas,
        radius: Float,
        from: Float,
        to: Float,
        paint: Paint
    ) {
        gaugeRect.set(-radius, -radius, radius, radius)
        canvas.drawArc(gaugeRect, from, to - from, false, paint)
    }

    private fun drawBackgroundNeedle(canvas: Canvas) {
        val mode = transitioningMode.current
        val radius = gaugePosition.innerRadius

        val powerPercentage = (transitioningCumulatedPower.current.coerceIn(
            mode.min,
            mode.limit
        ) - mode.min) / (mode.max - mode.min)
        val powerAngle = powerPercentage * mode.circularSize * 360


        val startAngle = 0.75f - mode.circularSize * 0.5f
        val stopAngle = startAngle + (mode.circularSize * powerPercentage)

        var colorArray = intArrayOf(
            Color.TRANSPARENT,
            BlueShadeHigh,
        )

        if (transitioningMode.current.mode == GearMode.R) {
            colorArray = intArrayOf(
                Color.TRANSPARENT,
                PurpleShadeHigh,
            )
        }
        val sweepGradient = SweepGradient(
            0f,
            0f,
            colorArray,
            floatArrayOf(startAngle, stopAngle)
        )

        // Set the shader for the Paint object
        val powerShadePaint = Paint()
        powerShadePaint.style = Paint.Style.STROKE
        powerShadePaint.shader = sweepGradient
        powerShadePaint.strokeWidth = 160f

        drawArcWithRadius(
            canvas,
            radius - 100f,
            270 - mode.circularSize * 180,
            270 - mode.circularSize * 180 + powerAngle,
            powerShadePaint
        )

    }

    private fun drawNeedle(canvas: Canvas) {
        val mode = transitioningMode.current
        val cumulatedPower = transitioningCumulatedPower.current.coerceIn(mode.min, mode.limit)
        val radius = gaugePosition.innerRadius
        val radialGradient = RadialGradient(
            0f,
            0f,
            radius - 20,
            intArrayOf(
                Color.parseColor("#00FF0000"),
                Color.parseColor("#FFFF0000"),
                Color.parseColor("#FFFF0000"),
                Color.parseColor("#44FF0000"),

                ), // Colors in the gradient
            floatArrayOf(
                0f,
                0.80f,
                0.90f,
                1f
            ), // Relative positions of the colors (null for evenly distributed)
            Shader.TileMode.CLAMP // Tile mode (CLAMP, REPEAT, MIRROR)
        )
        val paint = Paint()
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#FF0000")
        paint.shader = radialGradient
        paint.strokeWidth = 8f


        val angle =
            mode.circularSize * Math.PI * (-1 + 2 * (cumulatedPower - mode.min) / (mode.max - mode.min))
        var rInner = radius * 0.1f
        val rOuter = radius - 20f

        if (mode.needleType == NeedleType.FLOATING) {
            rInner = radius * 0.8f
        }
        canvas.drawLine(
            (sin(angle) * rOuter).toFloat(),
            -(cos(angle) * rOuter).toFloat(),
            (sin(angle) * rInner).toFloat(),
            -(cos(angle) * rInner).toFloat(),
            paint
        )

    }
}