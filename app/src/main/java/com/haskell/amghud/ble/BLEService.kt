package com.haskell.amghud.ble


import android.R.attr.value
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
import android.os.IBinder
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
            Log.i(TAG, "Message sent successfully.")
            // You might receive a confirmation in the onCharacteristicWrite callback
        } else {
            Log.e(TAG, "Failed to send message.")
        }

    }


    private val leScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device = result.device
            if (device.name == targetDeviceName) {
                targetDevice = device
                Log.d(TAG, "Target device found: ${device.address}")
                sendBroadcast(
                    Intent(BLEConstants.SETUP_STATUS_CHANGED)
                        .putEnumExtra("STATUS", BLESetupStatus.SCANNED_AND_FOUND)
                        .putExtra("DEVICE_ADDRESS", device.address)
                )
                stopScan()
                connectToDevice()
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "Scan failed: $errorCode")
            sendBroadcast(
                Intent(BLEConstants.SETUP_STATUS_CHANGED)
                    .putEnumExtra("STATUS", BLESetupStatus.SCAN_FAILED)
                    .putExtra("ERROR_CODE", errorCode)
            )
        }
    }


    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.")
                sendBroadcast(
                    Intent(BLEConstants.SETUP_STATUS_CHANGED)
                        .putEnumExtra("STATUS", BLESetupStatus.CONNECTED)
                )
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.")
                sendBroadcast(
                    Intent(BLEConstants.SETUP_STATUS_CHANGED)
                        .putEnumExtra("STATUS", BLESetupStatus.DISCONNECTED)
                )

            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services discovered.")
                val service = gatt.getService(SERVICE_UUID)
                bleCharacteristic = service?.getCharacteristic(CHARACTERISTIC_UUID)
                if (bleCharacteristic != null) {
                    gatt.setCharacteristicNotification(bleCharacteristic, true)
                    val descriptor = bleCharacteristic!!.getDescriptor(DESCRIPTOR_UUID)
                    descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor)
                }
                sendBroadcast(
                    Intent(BLEConstants.SETUP_STATUS_CHANGED)
                        .putEnumExtra("STATUS", BLESetupStatus.SERVICE_DISCOVERED)
                )


            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            executorService.execute {
                val data = characteristic.value
                if (data != null && data.isNotEmpty()) {
                    val message = String(data)
                    Log.d(TAG, "Received message: $message")
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
        isDemoing = true
        sendBroadcast(
            Intent(BLEConstants.SETUP_STATUS_CHANGED)
                .putEnumExtra("STATUS", BLESetupStatus.DEMO)
        )

    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        val randomV: Int = (Math.random() * 500).toInt()
        Log.i("Service is created", randomV.toString())
        val context = this
        val fakeState = FakeState(0, 0)
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
        serviceScope.launch {
            while (true) {
                if (bluetoothGatt != null && bleCharacteristic != null) {
                    writeMessage("HELLO")
                }
                delay(2000) // 500 millisecondss
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun startScanAndConnect() {
        isDemoing = false
        if (bluetoothLeScanner == null) {
            Log.e("BLE", "BluetoothLeScanner is null! Is Bluetooth enabled?")
            return
        }
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Log.e(TAG, "Bluetooth not enabled")
            sendBroadcast(
                Intent(BLEConstants.SETUP_STATUS_CHANGED)
                    .putEnumExtra("STATUS", BLESetupStatus.CONNECTION_FAILED)
            )
            return
        }


        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.e("BLE", "Location services are OFF! Please enable GPS.")
            return
        }
        targetDevice = null


        bluetoothLeScanner?.startScan(leScanCallback)
        scope.launch {
            delay(scanPeriod)
            stopScan()
        }
        Log.d(TAG, "Scan started")
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        bluetoothLeScanner?.stopScan(leScanCallback)
        Log.d(TAG, "Scan stopped")
        sendBroadcast(
            Intent(BLEConstants.SETUP_STATUS_CHANGED)
                .putEnumExtra("STATUS", BLESetupStatus.SCAN_STOPPED)
        )
    }

    @SuppressLint("MissingPermission")
    override fun connectToDevice() {
        isDemoing = false
        if (targetDevice == null) {
            bluetoothGatt = null
            Log.w(TAG, "Target device not found.")
            return
        }
        bluetoothGatt = targetDevice?.connectGatt(this, false, gattCallback)
    }

    @SuppressLint("MissingPermission")
    override fun disconnectDevice() {
        isDemoing = false
        if (bluetoothGatt != null) {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
        }
    }

    override fun onDestroy() {
        isDemoing = false
        super.onDestroy()
        stopScan()
        disconnectDevice()
        executorService.shutdown()
        FakeBLEService.isRunning = false

    }

    override fun reset() {
        isDemoing = false
        stopScan()
        disconnectDevice()
    }


    companion object {
        var isRunning: Boolean = false
    }
}
