package com.example.bluetoothsmsapp4

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BluetoothConnectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 내부 브로드캐스트만 처리
        if (intent.action == "BLUETOOTH_CONNECTION_STATUS") {
            val isConnected = intent.getBooleanExtra("isConnected", false)
            // 필요한 처리 수행
        }
    }
}