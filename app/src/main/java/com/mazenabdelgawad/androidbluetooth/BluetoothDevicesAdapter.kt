package com.mazenabdelgawad.androidbluetooth

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BluetoothDevicesAdapter(val mDeviceList: List<Device>, val context: Context) :
    RecyclerView.Adapter<BluetoothDevicesAdapter.ViewHolder>() {
    data class Device(val name: String, val bluetoothDevice: BluetoothDevice)

    private var listener: ItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(context).inflate(R.layout.device_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.label?.text = mDeviceList[position].name
    }

    override fun getItemCount(): Int {
        return mDeviceList.size
    }

    inner class ViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView!!) {

        var label: TextView? = itemView?.findViewById(R.id.name)

        init {
            itemView?.setOnClickListener {
                listener?.itemClicked(mDeviceList[adapterPosition].bluetoothDevice)
            }
        }
    }

    fun setItemClickListener(listener: ItemClickListener) {
        this.listener = listener
    }

    interface ItemClickListener {
        fun itemClicked(bluetoothDevice: BluetoothDevice)
    }
}