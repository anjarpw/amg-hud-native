package com.haskell.amghud

import android.annotation.SuppressLint
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.haskell.amghud.ble.BLEConstants
import com.haskell.amghud.ble.BLEPermissionHandler
import com.haskell.amghud.ble.BLEService
import com.haskell.amghud.ble.BLEServiceInterface
import com.haskell.amghud.ble.FakeBLEService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SettingActivity : AppCompatActivity() {
    private var bleService: BLEServiceInterface? = null
    private val bleViewModel: BLEViewModel by viewModels()
    private lateinit var bleReceiver: BLEBroadcastReceiverForViewModel

    private val userPreferencesViewModel: UserPreferencesViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(UserPreferencesViewModel::class.java)) {
                    return UserPreferencesViewModel(applicationContext) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_setting)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.setting)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        window.navigationBarColor = Color.BLACK

        val scanButton = findViewById<Button>(R.id.scanButton)
        val toggleConnectionButton = findViewById<Button>(R.id.toggleConnectionButton)
        val resetButton = findViewById<Button>(R.id.resetButton)
        val setupStatusTextView = findViewById<TextView>(R.id.setupStatusTextView)
        val scanResultsTextView = findViewById<TextView>(R.id.scanResultsTextView)
        val closeButton = findViewById<Button>(R.id.backButton)
        val nameEditText =
            findViewById<TextView>(R.id.nameEditText) // Initialize the EditText using its ID
        val saveButton = findViewById<Button>(R.id.saveButton)
        var isBluetoothConnected = false

        bleReceiver = BLEBroadcastReceiverForViewModel(bleViewModel)

        lifecycleScope.launch {
            bleViewModel.state.collectLatest {
                isBluetoothConnected = it.isConnected
                toggleConnectionButton.text = if (isBluetoothConnected) "Disconnect" else "Connect"
            }
        }

        scanButton.setOnClickListener {
            bleService?.startScanAndConnect()
        }
        toggleConnectionButton.setOnClickListener {
            if(bleService == null){
                return@setOnClickListener
            }

            if(isBluetoothConnected){
                bleService?.disconnectDevice()
            }else{
                bleService?.connectToDevice()

            }
        }
        resetButton.setOnClickListener {
            bleService?.reset()
        }

        closeButton.setOnClickListener {
            finish()
        }


        // Observe the name StateFlow from the ViewModel
        lifecycleScope.launch {
            userPreferencesViewModel.name.collect { savedName ->
                nameEditText.setText(savedName)
            }
        }

        saveButton.setOnClickListener {
            val currentName = nameEditText.text.toString() // Get the text entered by the user
            userPreferencesViewModel.saveName(currentName)
        }

        lifecycleScope.launch {
            bleViewModel.state.collectLatest {
                setupStatusTextView.text = it.setupStatus
                scanResultsTextView.text =
                    it.scanResults.entries.joinToString(separator = "\n") { (key, value) ->
                        "$key = $value"
                    }
            }
        }


        BLEPermissionHandler.ensureNecessaryPermissions(this) { permissions ->
            if (permissions.any { !it.value }) {
                return@ensureNecessaryPermissions
            }
            if (isUsingFakeBLE) {
                val fakeServiceConnection = GenericServiceConnection(
                    this,
                    FakeBLEService::class.java,
                    object : GenericServiceConnection.ServiceConnectionListener<FakeBLEService?> {
                        override fun onServiceConnected(service: FakeBLEService?) {
                            bleService = service
                        }

                        override fun onServiceDisconnected() {
                        }
                    }
                )
                fakeServiceConnection.bindService()
            } else {
                val serviceConnection = GenericServiceConnection(
                    this,
                    BLEService::class.java,
                    object : GenericServiceConnection.ServiceConnectionListener<BLEService?> {
                        override fun onServiceConnected(service: BLEService?) {
                            bleService = service
                        }

                        override fun onServiceDisconnected() {
                        }
                    }
                )
                serviceConnection.bindService()
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()
        registerReceiver(bleReceiver, IntentFilter().apply {
            addAction(BLEConstants.MESSAGE_RECEIVED)
            addAction(BLEConstants.SETUP_STATUS_CHANGED)

        })
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(bleReceiver)
    }
}