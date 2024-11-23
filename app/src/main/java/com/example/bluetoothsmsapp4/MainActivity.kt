package com.example.bluetoothsmsapp4

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var statusTextView: TextView
    private lateinit var phoneNumberEditText: EditText
    private lateinit var saveNumberButton: Button
    private lateinit var currentNumberTextView: TextView
    private lateinit var testSmsButton: Button
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeUI()
        loadSavedPhoneNumber()
        checkPermissions()
        setupClickListeners()

        // Android 13 이상에서는 exported 플래그 설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                connectionStatusReceiver,
                IntentFilter("BLUETOOTH_CONNECTION_STATUS"),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(
                connectionStatusReceiver,
                IntentFilter("BLUETOOTH_CONNECTION_STATUS")
            )
        }

        startBluetoothService()
    }

    private fun initializeUI() {
        statusTextView = findViewById(R.id.statusTextView)
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText)
        saveNumberButton = findViewById(R.id.saveNumberButton)
        currentNumberTextView = findViewById(R.id.currentNumberTextView)
        testSmsButton = findViewById(R.id.testSmsButton)  // 이 부분이 제대로 초기화되어야 함
        sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    }

    private fun loadSavedPhoneNumber() {
        val savedNumber = sharedPreferences.getString("phoneNumber", "")
        if (!savedNumber.isNullOrEmpty()) {
            currentNumberTextView.text = "현재 설정된 번호: $savedNumber"
            phoneNumberEditText.setText(savedNumber)
        }
    }

    private fun startBluetoothService() {
        val serviceIntent = Intent(this, BluetoothBackgroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun setupClickListeners() {
        saveNumberButton.setOnClickListener {
            val number = phoneNumberEditText.text.toString()
            if(number.isEmpty()) {
                Toast.makeText(this, "전화번호를 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sharedPreferences.edit().apply {
                putString("phoneNumber", number)
                apply()
            }

            currentNumberTextView.text = "현재 설정된 번호: $number"
            Toast.makeText(this, "전화번호가 저장되었습니다", Toast.LENGTH_SHORT).show()
        }

        testSmsButton.setOnClickListener {
            if(sharedPreferences.getString("phoneNumber", "").isNullOrEmpty()) {
                Toast.makeText(this, "먼저 전화번호를 저장해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendTestSMS()
        }
    }

    private fun sendTestSMS() {
        val phoneNumber = sharedPreferences.getString("phoneNumber", "") ?: return

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    this.getSystemService(SmsManager::class.java)
                } else {
                    SmsManager.getDefault()
                }
                smsManager.sendTextMessage(
                    phoneNumber,
                    null,
                    "테스트 메시지입니다.",
                    null,
                    null
                )
                Toast.makeText(this, "테스트 메시지를 발송했습니다", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "SMS 발송 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "SMS 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            checkPermissions()
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        // Android 13 이상에서는 알림 권한도 추가
        val permissionList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions + Manifest.permission.POST_NOTIFICATIONS
        } else {
            permissions
        }

        val notGrantedPermissions = permissionList.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGrantedPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                notGrantedPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private val connectionStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "BLUETOOTH_CONNECTION_STATUS") {
                val isConnected = intent.getBooleanExtra("isConnected", false)
                updateConnectionStatus(isConnected)
            }
        }
    }

    private fun updateConnectionStatus(isConnected: Boolean) {
        runOnUiThread {
            statusTextView.text = if (isConnected) {
                "연결 상태: 연결됨"
            } else {
                "연결 상태: 연결 시도 중..."
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(connectionStatusReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}