package com.haskell.amghud

abstract class TransitioningValue<T>(
    var initialValue: T,
    var tolerance: Float = 0.01f
) {
    var progress: Float = 0f
        private set

    var target: T = initialValue
        private set

    var from: T = initialValue
        private set

    val current: T
        get() {
            return extrapolate()
        }

    fun resetTarget(value: T, canReset: ((previousTarget: T) -> Boolean)? = null) {
        if(canReset != null && !canReset(target)){
            return
        }
        from = current
        target = value
        progress = 0.0f
    }

    fun update(fractionSpeed: Float) {
        progress += (1f - progress) * fractionSpeed
        if(!isUpdateRequired()){
            progress = 1.0f
        }
    }

    fun isUpdateRequired(): Boolean{
        return progress < 1.0f - tolerance
    }

    abstract fun extrapolate(): T
}
