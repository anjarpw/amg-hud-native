package com.haskell.amghud.ble

import android.content.Intent

inline fun <reified T : Enum<T>> Intent.putEnumExtra(key: String, victim: T): Intent {
    val value = victim.ordinal
    val extra = putExtra(key, value)
    return extra
}

inline fun <reified T : Enum<T>> Intent.getEnumExtra(key: String): T? {
    val i = getIntExtra(key, -1)

    if (i < 0) {
        return null
    }
    val data = T::class.java.enumConstants?.get(i)
    return data
}

object BLEConstants {
    const val BLE_ALIVE = "BLE_ALIVE"
    const val MESSAGE_RECEIVED = "MESSAGE_RECEIVED"
    const val SETUP_STATUS_CHANGED = "SETUP_STATUS_CHANGED"
}

enum class BLESetupStatus(label: String) {
    UNPAIRED("Unpaired"),
    SCANNING("Scanning"),
    DEVICE_FOUND("Device Found"),
    CONNECTING("Connecting"),
    CONNECTED("Device Connected"),

}


