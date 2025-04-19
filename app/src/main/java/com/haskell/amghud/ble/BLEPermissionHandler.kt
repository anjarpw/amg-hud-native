package com.haskell.amghud.ble

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

object BLEPermissionHandler {

    private val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION)
    } else {
        arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun ensureNecessaryPermissions(activity: AppCompatActivity, callback: (Map<String, Boolean>) -> Unit) {

        val permissionsToRequest = bluetoothPermissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            val requestPermissionLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                callback.invoke(permissions)
            }
            requestPermissionLauncher.launch(permissionsToRequest)
        } else {
            // All permissions are already granted
            val grantedPermissions = bluetoothPermissions.associateWith { true }
            callback(grantedPermissions)
        }
    }
}
