package com.haskell.amghud

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
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
import com.haskell.amghud.views.BlueShadeHigh
import com.haskell.amghud.views.BlueShadeLow
import com.haskell.amghud.views.CircularGaugeView
import com.haskell.amghud.views.GearSelectorView
import com.haskell.amghud.views.LeverView
import com.haskell.amghud.views.MiscView
import com.haskell.amghud.views.OrientationVisibility
import com.haskell.amghud.views.PurpleShadeHigh
import com.haskell.amghud.views.PurpleShadeLow
import com.haskell.amghud.views.RedShadeHigh
import com.haskell.amghud.views.RedShadeLow
import com.haskell.amghud.views.TractionView
import com.haskell.amghud.views.setVisibilityBasedOnOrientation
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var bleReceiver: BLEBroadcastReceiverForViewModel
    private val bleViewModel: BLEViewModel by viewModels()

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
    private var bleService: BLEServiceInterface? = null

    private lateinit var serviceConnection: GenericServiceConnection<BLEService>


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

        val serviceIntent = Intent(this, BLEService::class.java)
        if (!BLEService.isRunning) {
            startService(serviceIntent)  // Start service only if it's not running
        }

        // Optional: ensure nav bar icons are white
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        }
        val setupStatusTextView = findViewById<TextView>(R.id.setupStatusTextView)
        val circularGaugeView = findViewById<CircularGaugeView>(R.id.circularGaugeView)
        val tractionView = findViewById<TractionView>(R.id.tractionView)
        val tractionViewForModeT = findViewById<TractionView>(R.id.tractionViewForModeT)
        val gearSelectorView = findViewById<GearSelectorView>(R.id.gearSelectorView)
        val brakeView = findViewById<LeverView>(R.id.leverBrakeView)
        val throttleView = findViewById<LeverView>(R.id.leverThrottleView)
        val miscView = findViewById<MiscView>(R.id.miscView)
        val usernameTextView =
            findViewById<TextView>(R.id.usernameTextView) // Initialize the EditText using its ID
        val notificationBoxView = findViewById<View>(R.id.notificationBoxView)
        setVisibilityBasedOnOrientation(notificationBoxView, OrientationVisibility.ORIENTATION_VISIBILITY_PORTRAIT_ONLY)

        brakeView.setConfig(RedShadeHigh, RedShadeLow, 300, 500, 0.2f)
        throttleView.setConfig(BlueShadeHigh, BlueShadeLow, 300, 500, 0.2f)

        tractionView.setSizeProportion(0.3f)
        tractionViewForModeT.setSizeProportion(0.6f)

        bleReceiver = BLEBroadcastReceiverForViewModel(bleViewModel)
        val settingsButton = findViewById<Button>(R.id.settingsButton)

        val app = applicationContext as AmgHudApplication
        bleViewModel.onAction(BLEViewModelActions.ForwardState(app.bleState))
        lifecycleScope.launch {
            userPreferencesViewModel.name.collect { savedName ->
                usernameTextView.setText(savedName)
            }
        }
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
                miscView.setSteerRack(it.analogSteer)
                if (it.mode == GearMode.R) {
                    throttleView.setConfig(PurpleShadeHigh, PurpleShadeLow, 300, 500, 0.2f)
                } else {
                    throttleView.setConfig(BlueShadeHigh, BlueShadeLow, 300, 500, 0.2f)
                }
                if (it.mode == GearMode.T) {
                    tractionView.setVisibility(false)
                    tractionViewForModeT.setVisibility(true)
                } else {
                    tractionView.setVisibility(true)
                    tractionViewForModeT.setVisibility(false)
                }
                miscView.setBrakeIndicator(it.analogBrake > 300)
                miscView.setPowerIndicator(it.analogThrottle > 300)
                miscView.setBLEDeviceAlive(it.isBLEDeviceAlive)

            }
        }
        BLEPermissionHandler.ensureNecessaryPermissions(this) { permissions ->
            if (permissions.any { !it.value }) {
                return@ensureNecessaryPermissions
            }
            serviceConnection = GenericServiceConnection(
                this,
                BLEService::class.java,
                { service ->
                    bleService = service
                },
                {}
            )
            serviceConnection.bindService()
        }

    }



    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val notificationBoxView = findViewById<View>(R.id.notificationBoxView)
        setVisibilityBasedOnOrientation(notificationBoxView, OrientationVisibility.ORIENTATION_VISIBILITY_PORTRAIT_ONLY)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()
        registerReceiver(bleReceiver, IntentFilter().apply {
            addAction(BLEConstants.MESSAGE_RECEIVED)
            addAction(BLEConstants.SETUP_STATUS_CHANGED)
            addAction(BLEConstants.BLE_ALIVE)
        })
        val app = applicationContext as AmgHudApplication
        bleViewModel.onAction(BLEViewModelActions.ForwardState(app.bleState))
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(bleReceiver)
        val app = applicationContext as AmgHudApplication
        app.updateBLEState(bleViewModel.state.value)
    }

    override fun onStop() {
        super.onStop()
        serviceConnection.unbindService()
    }

}