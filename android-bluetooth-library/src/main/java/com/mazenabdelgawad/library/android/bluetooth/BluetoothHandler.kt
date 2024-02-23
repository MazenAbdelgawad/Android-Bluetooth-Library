package com.mazenabdelgawad.library.android.bluetooth

import android.bluetooth.BluetoothDevice
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log

/**
 * Created by Mazen Abdelgawad on 23/2/2024.
 * Email: Mazen.Abdalgawad@gamil.com
 */


/**
 * The Handler that gets information back from the BluetoothKit
 */
class BluetoothHandler(
    private var bluetoothHandlerCallback: OnBluetoothHandlerCallback,
    looper: Looper = Looper.getMainLooper()
) : Handler(looper) {

    override fun handleMessage(message: Message) {
//        super.handleMessage(message)
        when (message.what) {
            BluetoothKit.HandlerMessage.DISCONNECTED.value -> onDisconnected()
            BluetoothKit.HandlerMessage.CONNECTING.value -> onConnecting()
            BluetoothKit.HandlerMessage.CONNECTED.value -> onConnected(message)
            BluetoothKit.HandlerMessage.CONNECTING_FAILED.value -> onConnectingFailed()
            BluetoothKit.HandlerMessage.RECEIVED_MESSAGE.value -> onReceivedMessage(message)
            BluetoothKit.HandlerMessage.SENT_MESSAGE.value -> onSentMessage(message)
        }
    }

    private fun onDisconnected() = bluetoothHandlerCallback.onDisconnected()

    private fun onConnecting() = bluetoothHandlerCallback.onConnecting()

    private fun onConnectingFailed() = bluetoothHandlerCallback.onConnectingFailed()

    private fun onConnected(message: Message) {
        try {
            val bluetoothDevice =
                message.data.getParcelable(BluetoothKit.CONNECTED_BLUETOOTH_DEVICE) as BluetoothDevice?
                    ?: return
            this.bluetoothHandlerCallback.onConnected(bluetoothDevice = bluetoothDevice)
        } catch (e: Throwable) {
            Log.d(javaClass.simpleName, "Error can't Parcelable the connected BluetoothDevice")
            e.printStackTrace()
        }
    }

    private fun onReceivedMessage(message: Message) {
        val readBuf = message.obj as ByteArray
        // construct a string from the valid bytes in the buffer
        val readMessage = String(readBuf, 0, message.arg1)
        bluetoothHandlerCallback.onReceivedMessage(readMessage)
    }

    private fun onSentMessage(message: Message) {
        val writeBuf = message.obj as ByteArray
        // construct a string from the buffer
        val writeMessage = String(writeBuf)
        bluetoothHandlerCallback.onSentMessage(writeMessage)
    }

    interface OnBluetoothHandlerCallback {
        fun onDisconnected()
        fun onConnecting()
        fun onConnectingFailed()
        fun onConnected(bluetoothDevice: BluetoothDevice)
        fun onReceivedMessage(message: String)
        fun onSentMessage(message: String)
    }
}