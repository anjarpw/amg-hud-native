package com.haskell.amghud

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
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
import com.haskell.amghud.views.CircularGaugeView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

const val isUsingFakeBLE = true

class MainActivity : AppCompatActivity() {

    private lateinit var setupStatusTextView: TextView
    private var bleService: BLEServiceInterface? = null
    private lateinit var bleViewModel: BLEViewModel
    private lateinit var bleReceiver: BLEBroadcastReceiverForViewModel
    private lateinit var circularGaugeView: CircularGaugeView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupStatusTextView = findViewById(R.id.setupStatusTextView)
        circularGaugeView = findViewById(R.id.circularGaugeView)

        bleViewModel = ViewModelProvider(this)[BLEViewModel::class.java]
        bleReceiver = BLEBroadcastReceiverForViewModel(bleViewModel)
        val settingsButton = findViewById<ImageButton>(R.id.settingsButton)

        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingActivity::class.java)
            startActivity(intent)
        }
        lifecycleScope.launch {
            bleViewModel.state.collectLatest { it ->
                setupStatusTextView?.text = it.setupStatus
                circularGaugeView?.setMode(it.mode)
                circularGaugeView?.setCumulatedPower(it.cumulatedPower*8)
            }
        }
        BLEPermissionHandler.ensureNecessaryPermissions(this, { permissions ->
            if (permissions.any { !it.value }) {
                return@ensureNecessaryPermissions
            }
            if(isUsingFakeBLE){
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
            }else{
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
        })


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