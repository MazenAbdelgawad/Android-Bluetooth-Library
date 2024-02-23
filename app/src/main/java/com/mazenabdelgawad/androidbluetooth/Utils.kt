package com.mazenabdelgawad.androidbluetooth

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

const val REQUEST_ENABLE_BLUETOOTH = 100
const val PERMISSION_REQUEST_LOCATION = 200
const val HOST_BLUETOOTH_DEVICE = "HOST_BLUETOOTH_DEVICE"

fun AppCompatActivity.checkPermission(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true

    val permissions = mutableListOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
    }

    if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        || this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                (this.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                        || this.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
                        || this.checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED)
                )
    ) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Permission Request")
        builder.setMessage("Please grant location access to enable find bluetooth devices")
        builder.setPositiveButton(android.R.string.ok, null)
        builder.setOnDismissListener {
            requestPermissions(
                permissions.toTypedArray(),
                PERMISSION_REQUEST_LOCATION
            )
        }
        builder.show()
        return false;
    } else {
        return true
    }
}