package com.haskell.amghud

import android.annotation.SuppressLint
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
    private lateinit var scanButton: Button
    private lateinit var connectButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var resetButton: Button
    private lateinit var setupStatusTextView: TextView
    private lateinit var scanResultsTextView: TextView
    private var bleService: BLEServiceInterface? = null
    private lateinit var bleViewModel: BLEViewModel
    private lateinit var bleReceiver: BLEBroadcastReceiverForViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_setting)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        scanButton = findViewById(R.id.scanButton)
        connectButton = findViewById(R.id.connectButton)
        disconnectButton = findViewById(R.id.disconnectButton)
        resetButton = findViewById(R.id.resetButton)
        setupStatusTextView = findViewById(R.id.setupStatusTextView)
        scanResultsTextView = findViewById(R.id.scanResultsTextView)

        bleViewModel = ViewModelProvider(this)[BLEViewModel::class.java]
        bleReceiver = BLEBroadcastReceiverForViewModel(bleViewModel)

        scanButton.setOnClickListener{
            bleService?.startScanAndConnect()
        }
        connectButton.setOnClickListener{
            bleService?.connectToDevice()
        }
        disconnectButton.setOnClickListener{
            bleService?.disconnectDevice()
        }
        resetButton.setOnClickListener{
            bleService?.reset()
        }
        val closeButton = findViewById<Button>(R.id.closeButton)

        closeButton.setOnClickListener {
            finish()
        }
        lifecycleScope.launch {
            bleViewModel.state.collectLatest {
                setupStatusTextView.text = it.setupStatus
                scanResultsTextView.text = it.scanResults.entries.joinToString(separator = "\n") { (key, value) ->
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