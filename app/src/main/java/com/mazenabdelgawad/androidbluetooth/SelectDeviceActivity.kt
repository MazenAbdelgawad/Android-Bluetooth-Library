package com.mazenabdelgawad.androidbluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mazenabdelgawad.library.android.bluetooth.BluetoothKit

class SelectDeviceActivity : AppCompatActivity(), BluetoothDevicesAdapter.ItemClickListener {

    private lateinit var recyclerView: RecyclerView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_device)

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        findViewById<Button>(R.id.button_search).setOnClickListener {
            this.showPairedDevices()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showPairedDevices() {
        if (!checkPermission()) return

        if (!BluetoothKit.isBluetoothSupported()) {
            showAlert(
                "Bluetooth feature not compatible with this device, try to use another device",
                "Not compatible",
            )
            return
        }

        if (!BluetoothKit.isBluetoothEnable()) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH)
            return
        }

        val mPairedDeviceList: List<BluetoothDevice>? = BluetoothKit.getPairedBluetoothDevices()
        if (mPairedDeviceList?.isNotEmpty() == true) {
            val devices = mPairedDeviceList.map {
                val name = it.name ?: it.address
                BluetoothDevicesAdapter.Device(name, it)
            }
            val devicesAdapter = BluetoothDevicesAdapter(context = this, mDeviceList = devices)
            this.recyclerView.adapter = devicesAdapter
            devicesAdapter.setItemClickListener(this)
            devicesAdapter.notifyDataSetChanged()
        } else {
            showAlert("No paired devices founded")
        }
    }

    override fun itemClicked(bluetoothDevice: BluetoothDevice) {
        this.openMessageScreenWithHostBluetoothDevice(bluetoothDevice)
    }

    private fun openMessageScreenWithHostBluetoothDevice(bluetoothDevice: BluetoothDevice) {
        val intent = Intent(this, MessagesActivity::class.java)
        intent.putExtra(HOST_BLUETOOTH_DEVICE, bluetoothDevice)
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_ENABLE_BLUETOOTH || requestCode == PERMISSION_REQUEST_LOCATION) {
            showPairedDevices()
        }
    }

    private fun showAlert(message: String, title: String = "BluetoothKit-Demo") {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .show()
    }

}