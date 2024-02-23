package com.mazenabdelgawad.androidbluetooth

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button_host).setOnClickListener {
            startActivity(Intent(this, MessagesActivity::class.java))
        }

        findViewById<Button>(R.id.button_client).setOnClickListener {
            startActivity(Intent(this, SelectDeviceActivity::class.java))
        }
    }
}