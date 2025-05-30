package com.haskell.amghud

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.haskell.amghud.ble.BLEConstants
import com.haskell.amghud.ble.BLESetupStatus
import com.haskell.amghud.ble.getEnumExtra

fun splitString(input: String): Pair<String?, String?> {
    val parts = input.split("=")
    return if (parts.size == 2) {
        Pair(parts[0], parts[1])
    } else {
        Pair(null, null) // Or handle the error as needed
    }
}


class BLEBroadcastReceiverForViewModel(private val bleViewModel: BLEViewModel) :
    BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        when (intent.action) {
            BLEConstants.MESSAGE_RECEIVED -> {
                val plainMessage = intent.getStringExtra("MESSAGE")
                if (plainMessage != null) {
                    val (key, value) = splitString(plainMessage)
                    if (key != null && value != null) {
                        bleViewModel.onAction(
                            BLEViewModelActions.UpdateMessage(
                                key.trim(),
                                value.trim()
                            )
                        )
                    }
                }

            }

            BLEConstants.SETUP_STATUS_CHANGED -> {
                val status = intent.getEnumExtra<BLESetupStatus>("STATUS")
                val isDemo = intent.getBooleanExtra("IS_DEMO", false)
                if (status != null) {
                    bleViewModel.onAction(BLEViewModelActions.UpdateSetupStatus(status, isDemo))
                }
            }

            BLEConstants.BLE_ALIVE -> {
                val isAlive = intent.getBooleanExtra("IS_ALIVE", false)
                bleViewModel.onAction(BLEViewModelActions.UpdateBLEAlive(isAlive))
            }

        }
    }
}
