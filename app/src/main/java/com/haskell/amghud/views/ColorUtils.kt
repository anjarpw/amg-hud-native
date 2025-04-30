package com.haskell.amghud.views

import android.graphics.Color

val DarkShade = Color.parseColor("#FF335599")
val BlueShadeHigh = Color.parseColor("#AA0088FF")
val BlueShadeLow = Color.parseColor("#44000E2F")
val RedShadeHigh = Color.parseColor("#AAFF0000")
val RedShadeLow = Color.parseColor("#44880000")
val PurpleShadeHigh = Color.parseColor("#AA8F17FF")
val PurpleShadeLow = Color.parseColor("#4447008A")

fun setColorAlpha(existingColor: Int, alpha: Int): Int {
    val red = Color.red(existingColor)
    val green = Color.green(existingColor)
    val blue = Color.blue(existingColor)

    return Color.argb(alpha, red, green, blue)
}
fun mixColors(color1: Int, color2: Int, ratio1: Float = 0.6f): Int {
    require(ratio1 in 0f..1f) { "Ratio 1 must be between 0 and 1" }
    val ratio2 = 1f - ratio1

    val a1 = Color.alpha(color1) / 255f
    val r1 = Color.red(color1) / 255f
    val g1 = Color.green(color1) / 255f
    val b1 = Color.blue(color1) / 255f

    val a2 = Color.alpha(color2) / 255f
    val r2 = Color.red(color2) / 255f
    val g2 = Color.green(color2) / 255f
    val b2 = Color.blue(color2) / 255f

    val mixedAlpha = (a1 * ratio1) + (a2 * ratio2)
    val mixedRed = (r1 * ratio1) + (r2 * ratio2)
    val mixedGreen = (g1 * ratio1) + (g2 * ratio2)
    val mixedBlue = (b1 * ratio1) + (b2 * ratio2)

    return Color.argb(
        (mixedAlpha * 255).toInt(),
        (mixedRed * 255).toInt(),
        (mixedGreen * 255).toInt(),
        (mixedBlue * 255).toInt()
    )
}