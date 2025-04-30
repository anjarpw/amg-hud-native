package com.haskell.amghud.views

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Size
import android.view.View
import com.haskell.amghud.ITransitioningUpdate
import com.haskell.amghud.R
import com.haskell.amghud.TransitioningValue

enum class OrientationVisibility(val code: Int){
    ORIENTATION_VISIBILITY_PORTRAIT_ONLY(1),
    ORIENTATION_VISIBILITY_LANDSCAPE_ONLY (2),
    ORIENTATION_VISIBILITY_BOTH (0);

    companion object {
        fun fromCode(code: Int): OrientationVisibility? {
            return entries.find { it.code == code }
        }
    }
}

abstract class BaseView(context: Context, var attrs: AttributeSet?) : View(context, attrs) {



    private var orientationVisibility = OrientationVisibility.ORIENTATION_VISIBILITY_BOTH // Default

    init {
        if (attrs != null) {
            val typedArray: TypedArray = context.obtainStyledAttributes(
                attrs,
                R.styleable.BaseView
            )
            try {
                val code =  typedArray.getInt(
                    R.styleable.BaseView_orientationVisibility,
                    OrientationVisibility.ORIENTATION_VISIBILITY_BOTH.code
                )
                orientationVisibility = OrientationVisibility.fromCode(code) ?: OrientationVisibility.ORIENTATION_VISIBILITY_BOTH
            } finally {
                typedArray.recycle()
            }
        }
        visibility = if(getVisibilityBasedOnOrientation(resources, orientationVisibility)) View.VISIBLE else View.GONE
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        visibility = if(getVisibilityBasedOnOrientation(resources, orientationVisibility)) View.VISIBLE else View.GONE
    }

    private val transitions = ArrayList<ITransitioningUpdate>()
    protected fun <T> generateTransition(
        initialValue: T,
        fractionSpeed: Float,
        extrapolate: (from: T, target: T, progress: Float) -> T
    ): TransitioningValue<T> {
        val transition = object : TransitioningValue<T>(initialValue, fractionSpeed) {
            override fun extrapolate(
            ): T {
                return extrapolate(from, target, progress)
            }
        }
        transitions.add(transition)
        return transition
    }


    protected fun checkToInvalidate() {
        val anyUpdate = transitions.any { it.isUpdateRequired() }
        if (anyUpdate) {
            invalidate()
        }
    }

    protected abstract fun doDrawing(canvas: Canvas)


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val revisedSize =
            doResize(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels)
        // Set the measured dimensions to be a square
        setMeasuredDimension(revisedSize.width, revisedSize.height)
        visibility = if(getVisibilityBasedOnOrientation(resources, orientationVisibility)) View.VISIBLE else View.GONE
    }

    protected open fun doResize(width: Int, height: Int): Size {
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

fun getVisibilityBasedOnOrientation(resources: Resources, orientationVisibility: OrientationVisibility ): Boolean{
    val orientation = resources.configuration.orientation
    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
        return orientationVisibility == OrientationVisibility.ORIENTATION_VISIBILITY_BOTH || orientationVisibility == OrientationVisibility.ORIENTATION_VISIBILITY_PORTRAIT_ONLY
    } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
        return orientationVisibility == OrientationVisibility.ORIENTATION_VISIBILITY_BOTH || orientationVisibility == OrientationVisibility.ORIENTATION_VISIBILITY_LANDSCAPE_ONLY
    }
    return true
}


public fun setVisibilityBasedOnOrientation(view: View, orientationVisibility: OrientationVisibility){
    val visibility =  getVisibilityBasedOnOrientation(view.resources, orientationVisibility)
    view.visibility = if(visibility) View.VISIBLE else View.GONE
}



class CanvasManager(
    val updateCondition: () -> Boolean,
    val onRender: (canvas: Canvas) -> Unit
) {
    private var bitmap: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    private var canvas: Canvas = Canvas()
    private val sourceRect: Rect = Rect()
    private val targetRect: Rect = Rect()

    private var hasBeenRendered = false

    fun setSize(width: Int, height: Int) {
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        sourceRect.set(0, 0, width, height) // Source rectangle
        hasBeenRendered = false
    }

    private fun tryUpdate() {
        if (updateCondition() || !hasBeenRendered) {
            canvas = Canvas(bitmap)
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            onRender(canvas)
            hasBeenRendered = true
        }
    }


    fun applyToCanvas(c: Canvas, left: Int, top: Int, right: Int, bottom: Int, paint: Paint = Paint()) {
        tryUpdate()
        targetRect.set(left, top, right, bottom)
        c.drawBitmap(bitmap, sourceRect, targetRect, paint)
    }
}
