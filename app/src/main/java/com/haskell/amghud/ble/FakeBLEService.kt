package com.haskell.amghud.ble

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.haskell.amghud.GearMode
import com.haskell.amghud.ILocalBinder
import kotlinx.coroutines.*

class FakeBLEService : Service(), BLEServiceInterface {
    private val binder = LocalBinder()
    private var isConnected = false
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    inner class LocalBinder : Binder(), ILocalBinder<FakeBLEService> {
        override fun getService(): FakeBLEService = this@FakeBLEService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }


    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceScope.cancel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_REDELIVER_INTENT
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        val randomV: Int = (Math.random()*500).toInt()
        Log.i("Service is created", randomV.toString())
        val context = this
        val fakeState = FakeState(0, 0)
        serviceScope.launch {
            while (true) {
                if (isConnected) {
                    Log.i("COROUTINES", randomV.toString())
                    broadcastFakeCumulatedPowerMessages(context, fakeState)
                }
                delay(500) // 500 millisecondss
            }
        }
        serviceScope.launch {
            while (true) {
                if (isConnected) {
                    broadcastModeChanges(context, fakeState)
                }
                delay(5000) // 500 milliseconds
            }
        }
    }

    override fun startScanAndConnect() {
        sendBroadcast(
            Intent(BLEConstants.SETUP_STATUS_CHANGED)
                .putEnumExtra("STATUS", BLESetupStatus.DEVICE_FOUND)
                .putExtra("IS_MOCKED", true)
        )
        serviceScope.launch {
            delay(500) // 500 milliseconds
            connectToDevice()
        }
    }

    override fun connectToDevice() {
        isConnected = true
        sendBroadcast(
            Intent(BLEConstants.SETUP_STATUS_CHANGED)
                .putEnumExtra("STATUS", BLESetupStatus.CONNECTED)
                .putExtra("IS_MOCKED", true)
        )
    }

    override fun disconnectDevice() {
        isConnected = false
        sendBroadcast(
            Intent(BLEConstants.SETUP_STATUS_CHANGED)
                .putEnumExtra("STATUS", BLESetupStatus.DEVICE_FOUND)
                .putExtra("IS_MOCKED", true)
        )
    }

    override fun reset() {
        isConnected = false
        sendBroadcast(
            Intent(BLEConstants.SETUP_STATUS_CHANGED)
                .putEnumExtra("STATUS", BLESetupStatus.DEVICE_FOUND)
                .putExtra("ERROR_CODE", -1)
                .putExtra("IS_MOCKED", true)
        )
    }

    override fun runDemo() {
        TODO("Not yet implemented")
    }

    companion object {
        var isRunning: Boolean = false
    }

}

data class FakeState(
    var cumulatedPowerCounter: Int = 1,
    var modeCounter: Int = 0
){
    fun nextCounter(){
        cumulatedPowerCounter = (cumulatedPowerCounter + 1)%11
    }
    private val modeList: List<GearMode> = listOf(
        GearMode.T,
        GearMode.P,
        GearMode.R,
        GearMode.D,
        GearMode.S,
        GearMode.S_PLUS,
        GearMode.S,
        GearMode.D,
        GearMode.R,
        GearMode.P
    )
    fun nextMode(){
        modeCounter = (modeCounter + 1) % (modeList.size)
    }
    fun getMode(): String{
        return modeList[modeCounter].stringAlias
    }
    fun  getCumulatedPower(): Float{
        return (1f*cumulatedPowerCounter)/10f
    }

}

fun broadcastFakeCumulatedPowerMessages(context: Context, fakeState: FakeState){
    val balanceLeft: Float = (2.0f * Math.random()).toFloat() - 1.0f
    val balanceRight: Float = (2.0f * Math.random()).toFloat() - 1.0f
    val cumulatedPower =  fakeState.getCumulatedPower()
    fakeState.nextCounter()
    val analogBrake = Math.random() * 500
    val analogThrottle = Math.random() * 500
    val analogSteer = Math.random() * 1024
    context.sendBroadcast(
        Intent(BLEConstants.MESSAGE_RECEIVED).putExtra(
            "MESSAGE",
            "CUMULATED_POWER=$cumulatedPower"
        )
    )
    context.sendBroadcast(
        Intent(BLEConstants.MESSAGE_RECEIVED).putExtra(
            "MESSAGE",
            "RIGHT_MOTOR=${255 * cumulatedPower * balanceLeft}"
        )
    )
    context.sendBroadcast(
        Intent(BLEConstants.MESSAGE_RECEIVED).putExtra(
            "MESSAGE",
            "LEFT_MOTOR=${255 * cumulatedPower * balanceRight}"
        )
    )
    context.sendBroadcast(
        Intent(BLEConstants.MESSAGE_RECEIVED).putExtra(
            "MESSAGE",
            "ANALOG_BRAKE=$analogBrake"
        )
    )
    context.sendBroadcast(
        Intent(BLEConstants.MESSAGE_RECEIVED).putExtra(
            "MESSAGE",
            "ANALOG_THROTTLE=$analogThrottle"
        )
    )
    context.sendBroadcast(
        Intent(BLEConstants.MESSAGE_RECEIVED).putExtra(
            "MESSAGE",
            "ANALOG_STEER=$analogSteer"
        )
    )
}

fun broadcastModeChanges(context: Context, fakeState: FakeState){
    val mode = fakeState.getMode()
    fakeState.nextMode()
    context.sendBroadcast(
        Intent(BLEConstants.MESSAGE_RECEIVED).putExtra(
            "MESSAGE",
            "MODE=$mode"
        )
    )
}