package com.example.bluetoothsmsapp4

import android.Manifest
import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.telephony.SmsManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import java.io.InputStream
import java.util.*

class BluetoothBackgroundService : Service() {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var isConnected = false
    private val hc06Uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private lateinit var sharedPreferences: SharedPreferences
    private var savedPhoneNumber: String = ""

    companion object {
        private const val CHANNEL_ID = "bluetooth_service_channel"
        // 현재 연결 상태를 static하게 저장
        @Volatile
        private var currentConnectionStatus = false

        // 현재 상태를 가져오는 함수
        fun getCurrentStatus(): Boolean {
            return currentConnectionStatus
        }
    }

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == "phoneNumber") {
            savedPhoneNumber = prefs.getString(key, "") ?: ""
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            savedPhoneNumber = sharedPreferences.getString("phoneNumber", "") ?: ""

            sharedPreferences.registerOnSharedPreferenceChangeListener(prefsListener)

            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            bluetoothAdapter = bluetoothManager?.adapter

            startForegroundService()
            connectToHC06()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 서비스 시작할 때 현재 연결 상태를 바로 브로드캐스트
        updateConnectionStatus(isConnected)
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun startForegroundService() {
        try {
            val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel()
            } else {
                ""
            }

            val notificationIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )

            val notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("Bluetooth SMS App")
                .setContentText("블루투스 상태: 연결 시도 중...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .build()

            startForeground(1, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = CHANNEL_ID
            val channelName = "Bluetooth Service Channel"
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Bluetooth 서비스 상태를 표시합니다"
            }

            val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            service.createNotificationChannel(channel)
            return channelId
        }
        return ""
    }

    private fun connectToHC06() {
        if (bluetoothAdapter?.isEnabled == false) {
            updateConnectionStatus(false)
            return
        }

        Thread {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
                    val hc06 = pairedDevices?.find { it.name == "HC-06" }

                    if (hc06 == null) {
                        updateConnectionStatus(false)
                        return@Thread
                    }

                    bluetoothSocket = hc06.createRfcommSocketToServiceRecord(hc06Uuid)  // UUID 변수명 수정
                    bluetoothSocket?.connect()
                    inputStream = bluetoothSocket?.inputStream
                    isConnected = true
                    updateConnectionStatus(true)

                    listenForData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                updateConnectionStatus(false)
                reconnectWithDelay()
            }
        }.start()
    }

    private fun listenForData() {
        val buffer = ByteArray(1024)
        while (isConnected) {
            try {
                val bytesRead = inputStream?.read(buffer)
                if (bytesRead != null && bytesRead > 0) {
                    val data = String(buffer, 0, bytesRead)
                    if (data.trim() == "1") {
                        sendSMS()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                break
            }
        }
    }

    private fun sendSMS() {
        if (savedPhoneNumber.isEmpty()) return

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    this.getSystemService(SmsManager::class.java)
                } else {
                    SmsManager.getDefault()
                }
                smsManager.sendTextMessage(
                    savedPhoneNumber,
                    null,
                    "긴급상황입니다.",
                    null,
                    null
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun reconnectWithDelay() {
        Thread {
            Thread.sleep(5000) // 5초 후 재연결 시도
            if (!isConnected) {
                connectToHC06()
            }
        }.start()
    }

    private fun updateConnectionStatus(connected: Boolean) {
        isConnected = connected
        currentConnectionStatus = connected  // static 변수 업데이트

        val intent = Intent("BLUETOOTH_CONNECTION_STATUS")
        intent.setPackage(packageName)
        intent.putExtra("isConnected", connected)
        sendBroadcast(intent)

        updateNotification(connected)
    }

    private fun updateNotification(connected: Boolean) {
        // Android 13 이상에서 알림 권한 체크
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // 권한이 없으면 알림을 보내지 않음
                return
            }
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text,
                if (connected) getString(R.string.status_connected)
                else getString(R.string.status_disconnected)))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }

    private fun sendBroadcast(connected: Boolean) {
        val intent = Intent("BLUETOOTH_CONNECTION_STATUS")
        intent.setPackage(packageName)  // 명시적 인텐트로 변경
        intent.putExtra("isConnected", connected)
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            isConnected = false
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(prefsListener)
            bluetoothSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}