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
    val scanResults: Map<String, String> = hashMapOf(), // or your BLE device data type
    val cumulatedPower: Float = 0.0f,
    val mode: GearMode = GearMode.P,
    val setupStatus: String = "RESET",
    val leftMotor: Float = 0.0f,
    val rightMotor: Float = 0.0f,
    val analogThrottle: Float = 0.0f,
    val analogBrake: Float = 0.0f,
    val analogSteer: Float = 0.0f,
    val isBLEDeviceAlive: Boolean = false,
    val isDemo: Boolean = false,
)
    /*
    : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        readStringStringMap(parcel),
        parcel.readFloat(),
        GearMode.fromString(parcel.readString() ?: "P") ?: GearMode.P,
        parcel.readString().toString(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readFloat()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte(if (isDeviceFound) 1 else 0)
        parcel.writeByte(if (isConnected) 1 else 0)
        parcel.writeFloat(cumulatedPower)
        parcel.writeString(setupStatus)
        writeStringStringMap(parcel, scanResults)
        parcel.writeFloat(leftMotor)
        parcel.writeFloat(rightMotor)
        parcel.writeFloat(analogThrottle)
        parcel.writeFloat(analogBrake)
        parcel.writeFloat(analogSteer)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<BLEState> {
        override fun createFromParcel(parcel: Parcel): BLEState {
            return BLEState(parcel)
        }

        override fun newArray(size: Int): Array<BLEState?> {
            return arrayOfNulls(size)
        }
        // Helper methods to write and read the Map<String, String>
        private fun writeStringStringMap(parcel: Parcel, map: Map<String, String>) {
            parcel.writeInt(map.size) // Write the size of the map
            for ((key, value) in map) {
                parcel.writeString(key)
                parcel.writeString(value)
            }
        }

        private fun readStringStringMap(parcel: Parcel): Map<String, String> {
            val size = parcel.readInt()
            val map = mutableMapOf<String, String>()
            for (i in 0 until size) {
                val key = parcel.readString()
                val value = parcel.readString()
                if (key != null && value != null) {
                    map[key] = value
                }
            }
            return map
        }

    }
}
     */

sealed class BLEViewModelActions {
    data class UpdateMessage(val key: String, val value: String) : BLEViewModelActions()
    data class UpdateSetupStatus(
        val setupStatus: BLESetupStatus,
        val isDemo: Boolean
    ) : BLEViewModelActions()
    data class ForwardState(
        val state: BLEState
    ): BLEViewModelActions()
    data class UpdateBLEAlive(
        val isAlive: Boolean
    ): BLEViewModelActions()
}

class BLEViewModel : ViewModel() {

    private val _state = MutableStateFlow(BLEState())
    val state: StateFlow<BLEState> = _state

    fun onAction(intent: BLEViewModelActions) {
        viewModelScope.launch {
            when (intent) {
                is BLEViewModelActions.UpdateSetupStatus -> {
                    var isConnected = _state.value.isConnected
                    var isDeviceFound = _state.value.isDeviceFound
                    when (intent.setupStatus) {
                        BLESetupStatus.CONNECTED -> {
                            isConnected = true
                            isDeviceFound = true
                        }
                        BLESetupStatus.SCANNING -> {}
                        BLESetupStatus.CONNECTING -> {}
                        BLESetupStatus.UNPAIRED -> {
                            isDeviceFound = false
                            isConnected = false
                        }

                        BLESetupStatus.DEVICE_FOUND -> {
                            isDeviceFound = true
                            isConnected = false
                        }
                    }
                    val prefix = if(intent.isDemo) "DEMO - " else ""
                    _state.value = _state.value.copy(
                        isDeviceFound = isDeviceFound,
                        isConnected = isConnected,
                        isDemo = intent.isDemo,
                        setupStatus =  prefix + intent.setupStatus.toString()
                    )


                }

                is BLEViewModelActions.UpdateMessage -> {
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
                is BLEViewModelActions.ForwardState -> {
                    _state.value = intent.state
                }
                is BLEViewModelActions.UpdateBLEAlive -> {
                    val isBLEDeviceAlive = intent.isAlive
                    _state.value = _state.value.copy(
                        isBLEDeviceAlive = isBLEDeviceAlive
                    )
                }
            }
        }
    }

}