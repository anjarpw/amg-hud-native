package com.haskell.amghud.ble

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object BLEPermissionUtils {

    private val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private const val REQUEST_BLUETOOTH_PERMISSIONS = 1

    fun ensureNecessaryPermissions(activity: Activity, onPermissionsGranted: () -> Unit) {
        if (hasPermissions(activity)) {
            onPermissionsGranted()
        } else {
            requestPermissions(activity)
        }
    }

    fun hasPermissions(activity: Activity): Boolean {
        return bluetoothPermissions.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            bluetoothPermissions,
            REQUEST_BLUETOOTH_PERMISSIONS
        )
    }


    fun handlePermissionResult(
        requestCode: Int,
        grantResults: IntArray,
        onPermissionsGranted: () -> Unit,
        onPermissionsDenied: () -> Unit
    ) {
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                onPermissionsGranted()
            } else {
                onPermissionsDenied()
            }
        }
    }

}