package com.haskell.amghud

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haskell.amghud.ble.BLESetupStatus
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


data class BLEState(
    val isDeviceFound: Boolean = false,
    val isConnected: Boolean = false,
    val isServiceDiscovered: Boolean = false,
    val scanResults: Map<String, String> = hashMapOf(), // or your BLE device data type
    val cumulatedPower: Float = 0.0f,
    val mode: GearMode = GearMode.P,
    val setupStatus: String = "RESET",
    val leftMotor: Float = 0.0f,
    val rightMotor: Float = 0.0f,
    val analogThrottle: Float = 0.0f,
    val analogBrake: Float = 0.0f,
    val analogSteer: Float = 0.0f
)

sealed class BLEIntent {
    data class UpdateMessage(val key: String, val value: String) : BLEIntent()
    data class UpdateSetupStatus(
        val setupStatus: BLESetupStatus,
        val extraMessages: Map<String, String>
    ) : BLEIntent()
}

class BLEViewModel : ViewModel() {

    private val _state = MutableStateFlow(BLEState())
    val state: StateFlow<BLEState> = _state

    fun processIntent(intent: BLEIntent) {
        viewModelScope.launch {
            when (intent) {
                is BLEIntent.UpdateSetupStatus -> {
                    when (intent.setupStatus) {
                        BLESetupStatus.CONNECTED -> _state.value = _state.value.copy(
                            isConnected = true,
                            setupStatus = "Connected"
                        )

                        BLESetupStatus.CONNECTION_FAILED -> _state.value = _state.value.copy(
                            isConnected = false,
                            isServiceDiscovered = false,
                            setupStatus = "Connection Failed"
                        )

                        BLESetupStatus.DISCONNECTED -> _state.value = _state.value.copy(
                            isConnected = false,
                            isServiceDiscovered = false,
                            setupStatus = "Disconnected"
                        )

                        BLESetupStatus.SCAN_FAILED -> _state.value = _state.value.copy(
                            isDeviceFound = false,
                            isConnected = false,
                            isServiceDiscovered = false,
                            setupStatus = "Scanning Failed"
                        )

                        BLESetupStatus.SCAN_STOPPED -> _state.value = _state.value.copy(
                            setupStatus = "Stop Scanning"
                        )

                        BLESetupStatus.SERVICE_DISCOVERED -> _state.value = _state.value.copy(
                            isServiceDiscovered = true,
                            setupStatus = "Service is discovered"
                        )

                        BLESetupStatus.SCANNED_AND_FOUND -> _state.value = _state.value.copy(
                            isDeviceFound = true,
                            setupStatus = "Device Target Found"
                        )

                        BLESetupStatus.NO_DEVICE -> {}
                        BLESetupStatus.SCANNING -> {}
                        BLESetupStatus.CONNECTING -> {}
                        BLESetupStatus.NO_PAIR -> {}
                        BLESetupStatus.SCANNED -> {}
                    }

                }

                is BLEIntent.UpdateMessage -> {
                    val updatedMap = _state.value.scanResults.toMutableMap()
                    updatedMap[intent.key] = intent.value
                    var cumulatedPower = _state.value.cumulatedPower
                    var mode = _state.value.mode
                    var leftMotor = _state.value.leftMotor
                    var rightMotor = _state.value.rightMotor
                    var analogBrake = _state.value.analogBrake
                    var analogThrottle = _state.value.analogThrottle
                    var analogSteer = _state.value.analogSteer
                    var setupStatus = _state.value.setupStatus
                    when (intent.key) {
                        "CUMULATED_POWER" -> cumulatedPower = intent.value.toFloat()
                        "MODE" -> mode = GearMode.fromString(intent.value) ?: GearMode.P
                        "LEFT_MOTOR" -> leftMotor = intent.value.toFloat()
                        "RIGHT_MOTOR" -> rightMotor = intent.value.toFloat()
                        "ANALOG_BRAKE" -> analogBrake = intent.value.toFloat()
                        "ANALOG_THROTTLE" -> analogThrottle = intent.value.toFloat()
                        "ANALOG_STEER" -> analogSteer = intent.value.toFloat()
                    }
                    _state.value = _state.value.copy(
                        scanResults = updatedMap,
                        cumulatedPower = cumulatedPower,
                        mode = mode,
                        leftMotor = leftMotor,
                        rightMotor = rightMotor,
                        analogSteer = analogSteer,
                        analogBrake = analogBrake,
                        analogThrottle = analogThrottle,
                        setupStatus = setupStatus
                    )
                }
            }
        }
    }

}