package com.haskell.amghud


interface ITransitioningUpdate{
    fun update();
    fun isUpdateRequired(): Boolean
}


abstract class TransitioningValue<T>(
    initialValue: T,
    var fractionSpeed: Float,
    var tolerance: Float = 0.01f
): ITransitioningUpdate {
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

    override fun update() {
        progress += (1f - progress) * fractionSpeed
        if(!isUpdateRequired()){
            progress = 1.0f
        }
    }

    override fun isUpdateRequired(): Boolean{
        return progress < 1.0f - tolerance
    }

    abstract fun extrapolate(): T
}
