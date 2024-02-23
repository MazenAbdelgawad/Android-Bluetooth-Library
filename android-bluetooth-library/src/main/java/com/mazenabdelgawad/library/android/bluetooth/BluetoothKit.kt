package com.mazenabdelgawad.library.android.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.annotation.RequiresPermission
import com.mazenabdelgawad.library.android.bluetooth.BuildConfig.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Created by Mazen Abdelgawad on 18/2/2024.
 * Email: Mazen.Abdalgawad@gamil.com
 */

object BluetoothKit {
    private val TAG: String = javaClass.simpleName

    private val mBluetoothAdapter: BluetoothAdapter? by lazy { BluetoothAdapter.getDefaultAdapter() }
    private var mHandler: Handler? = null
    private var mState: State = State.DISCONNECTED
    private var mConnectedThread: ConnectedThread? = null  // Host/Client Side [depend on running]

    private var secureNameOfSDP = "BluetoothSecure"
    private var secureUUIDOfSDP: UUID = UUID.fromString("df6b743c-1959-4442-9c8a-3b9204dc164b")

    private var mSecureAcceptThread: AcceptThread? = null  // Server Side (Host)
    private var mConnectThread: ConnectThread? = null      // Client Side

    /**
     * Connect Bluetooth Device: used to send the connected bluetooth device in [Bundle] to handler
     *
     * @see notifyHandlerWithDeviceConnected
     */
    const val CONNECTED_BLUETOOTH_DEVICE = "CONNECTED_BLUETOOTH_DEVICE"


    /**
     * enum class that represent the current connection state.
     *
     * DISCONNECTED:  now not connected to a device
     * CONNECTING:    for
     *                  [Client Side]: now initiating an outgoing connection
     *                  [Host Side]: now listening for incoming connections
     * CONNECTED:     now connected to a device
     */
    enum class State(val value: Int) {
        DISCONNECTED(0),
        CONNECTING(1),
        CONNECTED(2)
    }

    /**
     * Handler message: used to notify the handler
     *
     * @property value the value of message type that send to handler
     *
     * @see setHandler
     */
    enum class HandlerMessage(val value: Int) {
        DISCONNECTED(0),
        CONNECTING(1),
        CONNECTED(2),
        CONNECTING_FAILED(3),
        RECEIVED_MESSAGE(4),
        SENT_MESSAGE(5)
    }

    /**
     * @param secureName to change secure connection name to custom name
     * rather than using default secure name "BluetoothSecure"
     *
     * @Note: should change custom secure name before start the connection
     */
    @Synchronized
    fun setCustomSecureNameOfSDP(secureName: String) {
        this.secureNameOfSDP = secureName
    }

    /**
     * @param secureUUID to change secure connection UUID to custom UUID
     * rather than using default secure UUID "df6b743c-1959-4442-9c8a-3b9204dc164b"
     *
     * @Note: should change custom secure UUID before start the connection
     */
    @Synchronized
    fun setCustomSecureUUIDOfSDP(secureUUID: UUID) {
        this.secureUUIDOfSDP = secureUUID
    }

    /**
     * @param handler The Handler to notify UI with changes
     */
    @Synchronized
    fun setHandler(handler: Handler) {
        mHandler = handler
    }

    /**
     * To remove The Handler and stop to notify UI.
     */
    @Synchronized
    fun removeHandler() {
        mHandler = null
    }

    /**
     * Return the current connection state @see BluetoothKit.State
     */
    @Synchronized
    fun getState(): State {
        return mState
    }

    /**
     * Check is bluetooth feature Supported on this hardware platform
     *
     * @Return
     * true if Bluetooth adapter is supported on this hardware platform
     * false if Bluetooth adapter not supported on this hardware platform
     */
    fun isBluetoothSupported(): Boolean = this.mBluetoothAdapter != null

    /**
     * Check is bluetooth enabled
     *
     * @Return
     * true if Bluetooth adapter is Enable (open)
     * false if Bluetooth adapter is Disable (close)
     */
    fun isBluetoothEnable(): Boolean = this.mBluetoothAdapter?.isEnabled ?: false

    /**
     * Map a Bluetooth hardware address to object of BluetoothDevice.
     * Valid Bluetooth hardware addresses must be upper case, in a format such as "00:11:22:33:AA:BB".
     *
     * @param hardwareAddress valid Bluetooth MAC address
     *
     * @return BluetoothDevice for a valid hardware address even if this adapter has never seen that device,
     * or null if hardware address not valid
     */
    fun mapBluetoothDevice(hardwareAddress: String): BluetoothDevice? {
        try {
            if (!BluetoothAdapter.checkBluetoothAddress(hardwareAddress)) return null
            return mBluetoothAdapter?.getRemoteDevice(hardwareAddress) ?: return null
        } catch (e: Throwable) {
            if (DEBUG) e.printStackTrace()
            return null
        }
    }


    /**
     * Start the ConnectedThread to that managing a Bluetooth connection

     * @param socket The BluetoothSocket on which the connection was made
     * *
     * @param device The BluetoothDevice that has been connected
     */
    @Synchronized
    private fun connected(socket: BluetoothSocket, device: BluetoothDevice, isHost: Boolean) {
        Log.d(TAG, "connected, Socket Type: [Secure]")

        // Cancel any thread currently running
        this.stop()

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = ConnectedThread(socket, isHost)
        mConnectedThread?.start()

        // Send the name of the connected device back to the UI
        this.notifyHandlerWithDeviceConnected(device)
    }

    /**
     * Disconnect the Connection

     * Note: This method Stop All Connections So, in the
     * [Host Side]: the host listening will Stop, and for start listening again to accept
     * client connection will Need to start connection again.
     * [Client Side]: the client will disconnect the connection and to connect again to Server
     * Will need to connect to host again
     */
    fun disconnect() {
        this.stop()
    }

    /**
     * Stop all threads
     */
    @Synchronized
    private fun stop() {
        Log.d(TAG, "stop")

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread?.cancel()
            mConnectThread = null
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread?.cancel()
            mConnectedThread = null
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread?.cancel()
            mSecureAcceptThread = null
        }

        mState = State.DISCONNECTED
    }


    /**
     * Indicate that the connection attempt failed and notify the UI.
     */
    private fun connectionFailed() {
        Log.w(TAG, "connectionFailed")
        // Send a failure message back to the UI
        this.notifyHandler(HandlerMessage.CONNECTING_FAILED)

        mState = State.DISCONNECTED
        // Update UI
    }

    /**
     * Indicate that the connection was lost and notify the UI.
     */
    private fun connectionLost(isHost: Boolean) {
        Log.w(TAG, "connectionLost")
        // Send a failure message back to the UI
        this.notifyHandler(HandlerMessage.DISCONNECTED)

        mState = State.DISCONNECTED

        if (isHost && mBluetoothAdapter?.isEnabled == true) {
            // Start the service over to restart listening mode
            this.createBluetoothServerSocket()
        }
    }

    /**
     * Notify Handler with [HandlerMessage]

     * @param message the message type send to handler
     * *
     * @see HandlerMessage
     */
    private fun notifyHandler(message: HandlerMessage) {
        val msg = mHandler?.obtainMessage(message.value)
        if (msg != null) {
            mHandler?.sendMessage(msg)
        }
    }

    /**
     * Notify Handler with [HandlerMessage]

     * @param message the message type send to handler
     * @param size the size of data [ByteArray] send to handler
     * @param data the data [ByteArray] send to handler
     * *
     * @see HandlerMessage
     */
    private fun notifyHandler(message: HandlerMessage, size: Int, data: ByteArray) {
        mHandler?.obtainMessage(message.value, size, -1, data)?.sendToTarget()
    }

    /**
     * Notify Handler with [HandlerMessage]

     * [HandlerMessage.CONNECTED] the message type send to handler
     * @param bluetoothDevice the connected bluetooth Device to send to handler in the [Bundle]
     * *
     * @see HandlerMessage
     * @see CONNECTED_BLUETOOTH_DEVICE [BluetoothDevice] send in the message Bundle contain
     * the Parcelable of connected BluetoothDevice
     */
    private fun notifyHandlerWithDeviceConnected(bluetoothDevice: BluetoothDevice) {
        val msg = mHandler?.obtainMessage(HandlerMessage.CONNECTED.value)
        val bundle = Bundle()
        bundle.putParcelable(CONNECTED_BLUETOOTH_DEVICE, bluetoothDevice)
        msg?.data = bundle
        if (msg != null) {
            mHandler?.sendMessage(msg)
        }
    }


    /**
     * Write the String of Message, out to the connected bluetooth device

     * @param message The String to write
     * *
     * @see ConnectedThread.write
     */
    fun write(message: String) {
        if (message.isBlank()) return

        val byteMessage = message.toByteArray()
        // Create temporary object
        var r: ConnectedThread? = null
        // Synchronize a copy of the ConnectedThread
        synchronized(this) {
            if (mState != State.CONNECTED) return
            r = mConnectedThread
        }
        // Perform the write unSynchronized
        r?.write(byteMessage)
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    // Host/Client Side [depend on running]
    private class ConnectedThread(
        private val mmSocket: BluetoothSocket,
        private val isHost: Boolean
    ) : Thread() {
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?
        private val BU_MSG_SIZE = "BU_MSG_SIZE" //BU_MSG_SIZE0009999, bytes=18, actual size[12:18]
        //Use BU_MSG_SIZE to fix issue of [BluetoothSocket reads only 990 bytes at one time [mmInStream?.read(buffer)]]

        init {
            Log.d(TAG, "create [ConnectedThread]")
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = mmSocket.inputStream
                tmpOut = mmSocket.outputStream
            } catch (e: IOException) {
                Log.e(TAG, "temp sockets not created", e)
            }

            mmInStream = tmpIn
            mmOutStream = tmpOut
            mState = BluetoothKit.State.CONNECTED
        }

        override fun run() {
            Log.d(TAG, "run [ConnectedThread]")
            val buffer = ByteArray(1024) //BluetoothSocket reads only 990 bytes at one time
            var bytes: Int

            // Keep listening to the InputStream while connected
            while (mState == BluetoothKit.State.CONNECTED) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream?.read(buffer) ?: 0

                    val bu_msg_size = String(buffer, 0, 18)
                    var readBuffer: ByteArray

                    if (bu_msg_size.contains(BU_MSG_SIZE)) {
                        val messageSize = bu_msg_size.subSequence(12, 18).toString().toInt()
                        var readSize = bytes - 18
                        readBuffer = buffer.copyOfRange(18, bytes)

                        while (readSize < messageSize) {
                            bytes = mmInStream?.read(buffer) ?: 0
                            readBuffer += buffer.copyOfRange(0, bytes)
                            readSize += bytes
                        }
                    } else {
                        readBuffer = buffer.copyOfRange(0, bytes)
                    }
                    // Send the obtained bytes to the UI
                    notifyHandler(HandlerMessage.RECEIVED_MESSAGE, readBuffer.size, readBuffer)
                } catch (e: Throwable) { //IOException
                    Log.d(TAG, "Exception in [ConnectedThread] during read..")
                    if (DEBUG) e.printStackTrace()
                    connectionLost(isHost)
                    break
                }

            }
        }

        /**
         * Write to the connected OutStream.

         * @param buffer The bytes to write
         *
         * @Note: Can't write more than 9999999 bytes
         */
        fun write(buffer: ByteArray) {
            try {
                var writeBuffer = buffer

                if (buffer.size > 990) { //BluetoothSocket reads only 990 bytes at one time
                    var size = buffer.size.toString()
                    while (size.length < 7) {
                        size = "0$size"
                    }

                    size = "$BU_MSG_SIZE$size"
                    writeBuffer = size.toByteArray() + buffer
                }

                mmOutStream?.write(writeBuffer)

                // Share the sent message back to the UI
                notifyHandler(HandlerMessage.SENT_MESSAGE, writeBuffer.size, writeBuffer)
            } catch (e: Throwable) { //IOException
                Log.d(TAG, "Exception in [ConnectedThread] during write..")
                if (DEBUG) e.printStackTrace()
            }

        }

        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: Throwable) { //IOException
                Log.d(TAG, "Exception in [ConnectedThread] during close the socket")
                if (DEBUG) e.printStackTrace()
            }
        }

    }


    //********************************* (Host) Server-Kit **************************************
    /**
     * Start the bluetooth Host Connection. Specifically start AcceptThread to begin a
     * session in listening (server-side).
     *
     * Note: if the Bluetooth server was created before will be canceled and will create a new one (Restart).
     */
    @Synchronized
    fun createBluetoothHostServerConnection() {
        this.createBluetoothServerSocket()
    }

    /**
     * Start the bluetooth service. Specifically start AcceptThread to begin a
     * session in listening (server-side).
     *
     * Note: if the Bluetooth server was created before will be canceled and will create a new one (Restart).
     */
    @Synchronized
    private fun createBluetoothServerSocket() {
        Log.i(TAG, "createBluetoothServerSocket")

        // Cancel any thread currently running
        this.stop()

        // Start the thread to listen on a BluetoothServerSocket
        mSecureAcceptThread = AcceptThread()
        mSecureAcceptThread?.start()
        // Update UI
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side(host). It runs until a connection is accepted
     * (or until cancelled).
     */
    // ServerSide(host)
    private class AcceptThread() : Thread() {
        // The local server socket
        private val mmServerSocket: BluetoothServerSocket?

        init {
            var tmp: BluetoothServerSocket? = null

            // Create a new listening server socket
            try {
                tmp = mBluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                    secureNameOfSDP,
                    secureUUIDOfSDP
                )
                mState = BluetoothKit.State.CONNECTING //STATE_LISTEN
                notifyHandler(HandlerMessage.CONNECTING)
            } catch (e: IOException) {
                mState = BluetoothKit.State.DISCONNECTED
                Log.e(TAG, "Socket Type: [Secure], listen() failed", e)
            }

            mmServerSocket = tmp
        }

        override fun run() {
            if (mmServerSocket == null) {
                connectionFailed()
                return
            }
            Log.d(TAG, "Socket Type: [Secure], BEGIN mAcceptThread $this")
            name = "AcceptThreadSecure"

            var socket: BluetoothSocket?

            // Listen to the server socket if we're not connected
            while (mState != BluetoothKit.State.CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Socket's accept() method failed", e)
                    break
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized(this) {
                        if (mState == BluetoothKit.State.CONNECTING) { //or STATE_LISTEN
                            // Situation normal. Start the connected thread.
                            connected(socket, socket.remoteDevice, true)
                        } else {
                            // Either not ready or already connected. Terminate new socket.
                            try {
                                socket.close()
                            } catch (e: IOException) {
                                Log.e(TAG, "Could not close unwanted [accept] socket", e)
                            }
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread")
        }

        fun cancel() {
            Log.d(TAG, "Accept Socket cancel $this")
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Socket Type [Secure], close() of server failed", e)
            }

        }
    }


    //************************************ Client-Kit ***************************************

    /**
     * This function get currently paired devices.
     * @return an List<BluetoothDevice> of currently paired devices.
     *
     * @Note: be sure that bluetooth is enable before call this method to get list of devices
     * @see isBluetoothEnable
     * @see isBluetoothSupported
     */
    @RequiresPermission(value = "android.permission.BLUETOOTH")
    fun getPairedBluetoothDevices(): List<BluetoothDevice>? {
        return mBluetoothAdapter?.bondedDevices?.toList()
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device (toServer "host").

     * @param hardwareAddress The String of the paired BluetoothDevice to connect
     */
    fun connectToBluetoothHostServer(hardwareAddress: String) {
        val bluetoothDevice = this.mapBluetoothDevice(hardwareAddress) ?: return
        this.connectClientToBluetoothServer(bluetoothDevice)
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device (toServer "host").

     * @param device The BluetoothDevice to connect
     */
    fun connectToBluetoothHostServer(device: BluetoothDevice) {
        if (device == null) return
        this.connectClientToBluetoothServer(device)
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.

     * @param device The BluetoothDevice to connect
     *
     */
    @Synchronized
    private fun connectClientToBluetoothServer(device: BluetoothDevice) {
        Log.d(TAG, "connect to: ${device.name}/${device.type}/${device.address}")

        // Cancel any thread currently running
        this.stop()

        // Start the thread to connect with the given device
        mConnectThread = ConnectThread(device)
        mConnectThread?.start()

        // Update UI here
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    // Client Side
    private class ConnectThread(private val mmDevice: BluetoothDevice) : Thread() {
        private val mmSocket: BluetoothSocket?

        init {
            var tmp: BluetoothSocket? = null

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = mmDevice.createRfcommSocketToServiceRecord(secureUUIDOfSDP)
                mState = BluetoothKit.State.CONNECTING
                notifyHandler(HandlerMessage.CONNECTING)
            } catch (e: IOException) {
                mState = BluetoothKit.State.DISCONNECTED
                Log.e(TAG, "Connect to Server socket create() failed", e)
            }

            mmSocket = tmp
        }

        override fun run() {
            if (mmSocket == null) {
                connectionFailed()
                return
            }

            Log.i(TAG, "BEGIN mConnectThread")
            name = "ConnectThreadSecure"

            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter?.cancelDiscovery()

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect()

            } catch (e: IOException) {
                // Close the socket
                try {
                    mmSocket.close()
                } catch (e2: IOException) {
                    Log.e(TAG, "unable to close() connect socket during connection failure", e2)
                }

                connectionFailed()
                return
            }

            // Reset the ConnectThread because we're done
            synchronized(this) {
                mConnectThread = null
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, false)
        }

        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "close() of connect socket failed", e)
            }
        }

    }

}