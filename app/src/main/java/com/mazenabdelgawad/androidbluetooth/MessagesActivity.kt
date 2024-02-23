package com.mazenabdelgawad.androidbluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.mazenabdelgawad.library.android.bluetooth.BluetoothHandler
import com.mazenabdelgawad.library.android.bluetooth.BluetoothKit

class MessagesActivity : AppCompatActivity(), BluetoothHandler.OnBluetoothHandlerCallback {

    private var hostBluetoothDevice: BluetoothDevice? = null
    private var bluetoothHandler: BluetoothHandler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        this.hostBluetoothDevice =
            intent.getParcelableExtra(HOST_BLUETOOTH_DEVICE) as BluetoothDevice?

        this.enableSendMessage(false)
        this.initBluetoothConnection()
    }


    private fun initBluetoothConnection() {
        if (!this.checkPermission()) return

        if (!BluetoothKit.isBluetoothSupported()) {
            addReceivedMessage(
                "******* ERROR!! ******* \n" +
                        " Bluetooth feature not compatible with this device, try to use another device"
            )
            return
        }

        if (!BluetoothKit.isBluetoothEnable()) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH)
            return
        }

        //Create BluetoothHandler to receive messages from BluetoothKit
        this.bluetoothHandler = BluetoothHandler(this)
        BluetoothKit.setHandler(this.bluetoothHandler!!)

        if (this.hostBluetoothDevice == null) {
            BluetoothKit.createBluetoothHostServerConnection()
        } else {
            BluetoothKit.connectToBluetoothHostServer(hostBluetoothDevice!!)
        }

        findViewById<Button>(R.id.button_send).setOnClickListener {
            this.sendMessage()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        BluetoothKit.removeHandler()
        BluetoothKit.disconnect()
    }


    private fun sendMessage() {
        val messageEditText = findViewById<EditText>(R.id.editText_Message)
        val message = messageEditText?.text?.toString()
        if (message.isNullOrBlank()) {
            Toast.makeText(this, "No Message to send", Toast.LENGTH_LONG).show()
            return
        } else {
            messageEditText.setText("")
        }

        BluetoothKit.write(message.toString())
    }

    override fun onDisconnected() {
        Toast.makeText(this, "onDisconnected", Toast.LENGTH_LONG).show()
        this.enableSendMessage(false)
    }

    override fun onConnecting() {
        Toast.makeText(this, "onConnecting", Toast.LENGTH_LONG).show()
    }

    override fun onConnectingFailed() {
        Toast.makeText(this, "onConnectingFailed", Toast.LENGTH_LONG).show()
        this.enableSendMessage(false)
    }

    override fun onConnected(bluetoothDevice: BluetoothDevice) {
        var deviceName = bluetoothDevice.address
        if (checkPermission()) {
            deviceName = bluetoothDevice.name
        }

        Toast.makeText(this, "onConnected to device: $deviceName", Toast.LENGTH_LONG)
            .show()

        this.enableSendMessage(true)
    }

    override fun onReceivedMessage(message: String) {
        Toast.makeText(this, "onReceivedMessage", Toast.LENGTH_LONG).show()
        this.addReceivedMessage(message)
    }

    override fun onSentMessage(message: String) {
        Toast.makeText(this, "onSentMessage", Toast.LENGTH_LONG).show()
    }

    private fun addReceivedMessage(message: String) {
        findViewById<TextView>(R.id.received_message).append("$message\n---------\n")
    }

    private fun enableSendMessage(enable: Boolean) {
        findViewById<EditText>(R.id.editText_Message).isEnabled = enable
        findViewById<Button>(R.id.button_send).isEnabled = enable
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_ENABLE_BLUETOOTH || requestCode == PERMISSION_REQUEST_LOCATION) {
            initBluetoothConnection()
        }
    }
}