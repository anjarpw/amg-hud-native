package com.haskell.amghud

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
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
import com.haskell.amghud.views.BlueShadeHigh
import com.haskell.amghud.views.BlueShadeLow
import com.haskell.amghud.views.CircularGaugeView
import com.haskell.amghud.views.GearSelectorView
import com.haskell.amghud.views.LeverView
import com.haskell.amghud.views.PurpleShadeHigh
import com.haskell.amghud.views.PurpleShadeLow
import com.haskell.amghud.views.RedShadeHigh
import com.haskell.amghud.views.RedShadeLow
import com.haskell.amghud.views.TractionView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

const val isUsingFakeBLE = true

class MainActivity : AppCompatActivity() {

    private lateinit var setupStatusTextView: TextView
    private var bleService: BLEServiceInterface? = null
    private lateinit var bleViewModel: BLEViewModel
    private lateinit var bleReceiver: BLEBroadcastReceiverForViewModel
    private lateinit var circularGaugeView: CircularGaugeView
    private lateinit var tractionView: TractionView
    private lateinit var tractionViewForModeT: TractionView
    private lateinit var gearSelectorView: GearSelectorView
    private lateinit var brakeView: LeverView
    private lateinit var throttleView: LeverView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        window.navigationBarColor = Color.BLACK

        // Optional: ensure nav bar icons are white
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        }
        setupStatusTextView = findViewById(R.id.setupStatusTextView)
        circularGaugeView = findViewById(R.id.circularGaugeView)
        tractionView = findViewById(R.id.tractionView)
        tractionViewForModeT = findViewById(R.id.tractionViewForModeT)
        gearSelectorView = findViewById(R.id.gearSelectorView)
        brakeView = findViewById(R.id.leverBrakeView)
        throttleView = findViewById(R.id.leverThrottleView)
        brakeView.setConfig(RedShadeHigh, RedShadeLow, 300, 500, 0.2f)
        throttleView.setConfig(BlueShadeHigh, BlueShadeLow, 300, 500, 0.2f)

        tractionView.setSizeProportion(0.3f)
        tractionViewForModeT.setSizeProportion(0.6f)

        bleViewModel = ViewModelProvider(this)[BLEViewModel::class.java]
        bleReceiver = BLEBroadcastReceiverForViewModel(bleViewModel)
        val settingsButton = findViewById<ImageButton>(R.id.settingsButton)

        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingActivity::class.java)
            startActivity(intent)
        }
        lifecycleScope.launch {
            bleViewModel.state.collectLatest {
                setupStatusTextView.text = it.setupStatus
                circularGaugeView.setGearMode(it.mode)
                circularGaugeView.setCumulatedPower(it.cumulatedPower)
                tractionView.setLeftMotor(it.leftMotor)
                tractionView.setRightMotor(it.rightMotor)
                tractionViewForModeT.setLeftMotor(it.leftMotor)
                tractionViewForModeT.setRightMotor(it.rightMotor)
                gearSelectorView.setGearMode(it.mode)
                throttleView.setValue(it.analogThrottle)
                brakeView.setValue(it.analogBrake)
                if(it.mode == GearMode.R){
                    throttleView.setConfig(PurpleShadeHigh, PurpleShadeLow, 300, 500, 0.2f)
                }else{
                    throttleView.setConfig(BlueShadeHigh, BlueShadeLow, 300, 500, 0.2f)
                }
                if(it.mode == GearMode.T){
                    tractionView.setVisibility(false)
                    tractionViewForModeT.setVisibility(true)
                }else{
                    tractionView.setVisibility(true)
                    tractionViewForModeT.setVisibility(false)
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