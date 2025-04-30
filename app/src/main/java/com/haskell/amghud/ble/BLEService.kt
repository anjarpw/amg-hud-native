package com.haskell.amghud.ble


import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.haskell.amghud.ILocalBinder
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.Executors


interface BLEServiceInterface {
    fun startScanAndConnect()
    fun connectToDevice()
    fun disconnectDevice()
    fun reset()
    fun runDemo()
}

private const val TAG = "BLEService"
private const val TAG_STATUS = "BLEServiceStatus"
private val SERVICE_UUID =
    UUID.fromString("5fafc201-1fb5-459e-8fcc-c5c9c331914C") // Replace with your service UUID
private val CHARACTERISTIC_UUID =
    UUID.fromString("ceb5483e-36e1-4688-b7f5-ea07361b26af") // Replace with your characteristic UUID
private val DESCRIPTOR_UUID =
    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb") // Standard descriptor UUID

class BLEService : Service(), BLEServiceInterface {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }
    var isDemoing = false
    private var isAutoSearching = false
    private var bleSetupStatus: BLESetupStatus = BLESetupStatus.UNPAIRED
    private val bluetoothLeScanner: BluetoothLeScanner? by lazy { bluetoothAdapter?.bluetoothLeScanner }
    private var bluetoothGatt: BluetoothGatt? = null
    private var targetDevice: BluetoothDevice? = null
    private val targetDeviceName = "ESP32_BLE_AMG" // Replace with your target device name
    private val scanPeriod: Long = 10000
    private val scope = CoroutineScope(Dispatchers.IO)
    private val executorService = Executors.newSingleThreadExecutor()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var bleCharacteristic: BluetoothGattCharacteristic? = null


    private val binder = LocalBinder()

    inner class LocalBinder : Binder(), ILocalBinder<BLEService> {
        override fun getService(): BLEService = this@BLEService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    @SuppressLint("MissingPermission")
    private fun writeMessage(value: String) {
        bleCharacteristic!!.setValue(value)
        val success = bluetoothGatt!!.writeCharacteristic(bleCharacteristic!!)
        if (success) {
            Log.i(TAG_STATUS, "Message sent successfully.")
            // You might receive a confirmation in the onCharacteristicWrite callback
        } else {
            Log.e(TAG_STATUS, "Failed to send message.")
        }
    }


    private val leScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device = result.device
            Log.d(TAG_STATUS, "Device found: ${device.name}")
            if (device.name == targetDeviceName) {
                targetDevice = device
                Log.d(TAG_STATUS, "Target device found: ${device.address}")
                broadcastSetupStatus(BLESetupStatus.DEVICE_FOUND, {
                    it.putExtra("DEVICE_ADDRESS", device.address)
                })
                connectToDevice()
                bluetoothLeScanner?.stopScan(this)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG_STATUS, "Scan failed: $errorCode")
            broadcastSetupStatus(BLESetupStatus.UNPAIRED, {
                it.putExtra("ERROR_CODE", errorCode)
            })
            if(errorCode == 1){
                bluetoothLeScanner?.stopScan(this)
            }
        }
    }


    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG_STATUS, "Connected to GATT server.")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG_STATUS, "Disconnected from GATT server.")
                bluetoothGatt!!.close()
                broadcastSetupStatus(BLESetupStatus.DEVICE_FOUND)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG_STATUS, "Services discovered.")
                val service = gatt.getService(SERVICE_UUID)
                bleCharacteristic = service?.getCharacteristic(CHARACTERISTIC_UUID)
                if (bleCharacteristic != null) {
                    gatt.setCharacteristicNotification(bleCharacteristic, true)
                    val descriptor = bleCharacteristic!!.getDescriptor(DESCRIPTOR_UUID)
                    descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor)
                }
                broadcastSetupStatus(BLESetupStatus.CONNECTED)
            } else {
                Log.w(TAG_STATUS, "onServicesDiscovered received: $status")
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            executorService.execute {
                if(bleSetupStatus != BLESetupStatus.CONNECTED){
                    return@execute
                }
                if (isDemoing) {
                    return@execute
                }
                val data = characteristic.value
                if (data != null && data.isNotEmpty()) {
                    val message = String(data)
                    Log.d(TAG, "Received message: $message")
                    if (message.startsWith("PING=")) {
                        sendBroadcast(
                            Intent(BLEConstants.BLE_ALIVE).putExtra(
                                "IS_ALIVE",
                                true
                            )
                        )
                        restartBLEHeartBeatCheck()
                    }
                    sendBroadcast(
                        Intent(BLEConstants.MESSAGE_RECEIVED).putExtra(
                            "MESSAGE",
                            message
                        )
                    )
                }
            }
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_REDELIVER_INTENT
    }

    override fun runDemo() {
        reset()
        isDemoing = true
        broadcastSetupStatus(bleSetupStatus)
    }

    private fun broadcastSetupStatus(status: BLESetupStatus, putExtraFun: (intent: Intent) -> Unit = {}){
        bleSetupStatus = status
        val intent = Intent(BLEConstants.SETUP_STATUS_CHANGED)
            .putEnumExtra("STATUS", bleSetupStatus)
            .putExtra("IS_DEMO", isDemoing)
        putExtraFun(intent)
        sendBroadcast(
            intent
        )
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        val randomV: Int = (Math.random() * 500).toInt()
        Log.i("Service is created", randomV.toString())
        val context = this
        val fakeState = FakeState(0, 0)
        broadcastSetupStatus(BLESetupStatus.UNPAIRED)
        startScanAndConnect()

        serviceScope.launch {
            while (true) {
                if (isDemoing) {
                    Log.i("COROUTINES", randomV.toString())
                    broadcastFakeCumulatedPowerMessages(context, fakeState)
                }
                delay(500) // 500 millisecondss
            }
        }
        serviceScope.launch {
            while (true) {
                if (isDemoing) {
                    broadcastModeChanges(context, fakeState)
                }
                delay(5000) // 500 milliseconds
            }
        }
        var counter = 0
        serviceScope.launch {
            while (true) {
                if (!isDemoing) {
                    when(bleSetupStatus){
                        BLESetupStatus.CONNECTED -> {
                            writeMessage(counter.toString())
                            counter++
                        }

                        BLESetupStatus.UNPAIRED -> {
                            if(isAutoSearching){
                                startScanAndConnect()
                            }
                        }
                        BLESetupStatus.SCANNING -> {}
                        BLESetupStatus.DEVICE_FOUND -> {
                            if(isAutoSearching){
                                connectToDevice()
                            }
                        }
                        BLESetupStatus.CONNECTING -> {}
                    }
                }
                delay(2000)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun startScanAndConnect() {
        isDemoing = false
        isAutoSearching = true
        if (bluetoothLeScanner == null) {
            Log.e("BLE", "BluetoothLeScanner is null! Is Bluetooth enabled?")
            return
        }
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Log.e(TAG_STATUS, "Bluetooth not enabled")
            broadcastSetupStatus(BLESetupStatus.UNPAIRED)
            return
        }
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.e(TAG_STATUS, "Location services are OFF! Please enable GPS.")
            return
        }
        targetDevice = null
        if(bleSetupStatus == BLESetupStatus.SCANNING){
            return
        }
        broadcastSetupStatus(BLESetupStatus.SCANNING)
        bluetoothLeScanner?.startScan(leScanCallback)
        scope.launch {
            delay(scanPeriod)
            if(bleSetupStatus === BLESetupStatus.SCANNING){
                stopScan()
            }
        }
        Log.d(TAG_STATUS, "Scan started")
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        isDemoing = false
        bluetoothLeScanner?.stopScan(leScanCallback)
        Log.d(TAG_STATUS, "Scan stopped")
        if(bleSetupStatus === BLESetupStatus.SCANNING){
            broadcastSetupStatus(BLESetupStatus.UNPAIRED)
        }
    }

    @SuppressLint("MissingPermission")
    override fun connectToDevice() {
        isDemoing = false
        isAutoSearching = true
        if (targetDevice == null) {
            Log.w(TAG_STATUS, "Target device not found.")
            return
        }
        broadcastSetupStatus(BLESetupStatus.CONNECTING)
        bluetoothGatt = targetDevice?.connectGatt(this, false, gattCallback)
    }

    @SuppressLint("MissingPermission")
    override fun disconnectDevice() {
        isDemoing = false
        isAutoSearching = false
        bluetoothGatt!!.disconnect()

    }

    override fun onDestroy() {
        super.onDestroy()
        reset()
        serviceScope.cancel()
        executorService.shutdown()
        isRunning = false
    }

    override fun reset() {
        when(bleSetupStatus){
            BLESetupStatus.UNPAIRED -> {}
            BLESetupStatus.SCANNING -> {
                isAutoSearching = false
                stopScan()
            }
            BLESetupStatus.DEVICE_FOUND -> {}
            BLESetupStatus.CONNECTING -> {
                disconnectDevice()
            }
            BLESetupStatus.CONNECTED -> {
                disconnectDevice()
            }
        }
    }


    companion object {
        var isRunning: Boolean = false
    }

    private val handler =
        Handler(Looper.getMainLooper()) // Use the main thread's looper for UI updates
    private var timeoutRunnable: Runnable? = null

    private fun ping() {
        Log.d("InterruptibleTimer", "PING is finally called")
        sendBroadcast(
            Intent(BLEConstants.BLE_ALIVE).putExtra(
                "IS_ALIVE",
                false
            )
        )

    }

    private fun restartBLEHeartBeatCheck() {
        val delayMillis: Long = 3000
        timeoutRunnable?.let { handler.removeCallbacks(it) } // Clear any existing callback
        timeoutRunnable = Runnable { ping() }
        handler.postDelayed(timeoutRunnable!!, delayMillis) // Schedule the new ping
        Log.d(
            "InterruptibleTimer",
            "Timer restarted. Will ping in ${delayMillis / 1000.0} seconds."
        )
    }

}
