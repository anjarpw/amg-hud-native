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
    const val MESSAGE_RECEIVED = "MESSAGE_RECEIVED"
    const val SETUP_STATUS_CHANGED = "SETUP_STATUS_CHANGED"
}

enum class BLESetupStatus {
    NO_PAIR,
    SCANNED,
    CONNECTED,

    SERVICE_DISCOVERED,
    SCAN_STOPPED,
    CONNECTION_FAILED,
    DISCONNECTED,
    SCANNED_AND_FOUND,
    SCAN_FAILED,
    NO_DEVICE,
    SCANNING,
    CONNECTING
}

