package com.haskell.amghud.ble

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.haskell.amghud.GearMode
import com.haskell.amghud.ILocalBinder
import kotlinx.coroutines.*

class FakeBLEService: Service(), BLEServiceInterface {
    private val binder = LocalBinder()
    private var isConnected = false
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    inner class LocalBinder : Binder(), ILocalBinder<FakeBLEService> {
        override fun getService(): FakeBLEService = this@FakeBLEService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_REDELIVER_INTENT
    }

    private var cumulatedPowerCounter = 1
    private var modeCounter = 0
    private val modeList: List<GearMode> = listOf(GearMode.T, GearMode.P, GearMode.D, GearMode.S, GearMode.S_PLUS, GearMode.S, GearMode.D, GearMode.P )
    override fun onCreate() {
        serviceScope.launch {
            while (true) {
                if(isConnected){
                    val balanceLeft: Float = (2.0f*Math.random()).toFloat()-1.0f
                    val balanceRight: Float = (2.0f*Math.random()).toFloat()-1.0f
                    val cumulatedPower = cumulatedPowerCounter.toFloat()/10.0
                    sendBroadcast(Intent(BLEConstants.MESSAGE_RECEIVED).putExtra("MESSAGE",
                        "CUMULATED_POWER=$cumulatedPower"
                    ))
                    cumulatedPowerCounter = (cumulatedPowerCounter + 1) % 11
                    sendBroadcast(Intent(BLEConstants.MESSAGE_RECEIVED).putExtra("MESSAGE", "RIGHT_MOTOR="+(255*cumulatedPower*balanceLeft)))
                    sendBroadcast(Intent(BLEConstants.MESSAGE_RECEIVED).putExtra("MESSAGE", "LEFT_MOTOR="+(255*cumulatedPower*balanceRight)))
                }
                delay(500) // 500 milliseconds
            }
        }
        serviceScope.launch {
            while (true) {

                if(isConnected){
                    val mode = modeList[modeCounter].stringAlias
                    sendBroadcast(Intent(BLEConstants.MESSAGE_RECEIVED).putExtra("MESSAGE",
                        "MODE=$mode"
                    ))
                    modeCounter = (modeCounter + 1) % (modeList.size)
                }
                delay(5000) // 500 milliseconds
            }
        }
        super.onCreate()
    }

    override fun startScanAndConnect() {
        sendBroadcast(Intent(BLEConstants.SETUP_STATUS_CHANGED)
            .putEnumExtra("STATUS", BLESetupStatus.SCANNED_AND_FOUND)
        )
        serviceScope.launch {
            delay(500) // 500 milliseconds
            connectToDevice()
        }
    }

    override fun connectToDevice() {
        isConnected = true
        sendBroadcast(Intent(BLEConstants.SETUP_STATUS_CHANGED)
            .putEnumExtra("STATUS", BLESetupStatus.CONNECTED)
        )
    }

    override fun disconnectDevice() {
        isConnected = false
        sendBroadcast(Intent(BLEConstants.SETUP_STATUS_CHANGED)
            .putEnumExtra("STATUS", BLESetupStatus.DISCONNECTED)
        )
    }

    override fun reset() {
        isConnected = false
        sendBroadcast(Intent(BLEConstants.SETUP_STATUS_CHANGED)
            .putEnumExtra("STATUS", BLESetupStatus.DISCONNECTED)
            .putExtra("ERROR_CODE", -1)
        )
    }

}