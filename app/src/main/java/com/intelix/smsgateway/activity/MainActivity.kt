package com.intelix.smsgateway.activity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.intelix.smsgateway.MenuAdapter
import com.intelix.smsgateway.R
import com.intelix.smsgateway.WebhookPreferences
import com.intelix.smsgateway.data.MenuItem
import com.intelix.smsgateway.databinding.ActivityMainBinding
import com.intelix.smsgateway.helper.FCMHelper
import com.intelix.smsgateway.helper.LogHelper
import com.intelix.smsgateway.helper.SmsUtils
import com.intelix.smsgateway.helper.ThemeHelper
import com.intelix.smsgateway.helper.WebhookHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val pref by lazy { WebhookPreferences(this) }
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnToggleTheme: ImageButton
    private val REQUEST_DEFAULT_SMS = 200

    private val PERMISSION_REQUEST_CODE = 100
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 101

    private lateinit var smsDefaultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeHelper.applyTheme(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        smsDefaultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "The app is now the default SMS", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "The application is not made the default SMS", Toast.LENGTH_SHORT).show()
            }
        }

        checkAndRequestAllPermissions()

        checkAndRequestDefaultSmsApp()

        if (allPermissionsGranted()) {
            WebhookHelper.sendToServer(
                this,
                pref.getWebHookUrl(),
                mutableMapOf(
                    "message_type" to "DEBUGGING",
                    "api_key" to pref.getAPIKey(),
                    "message_timestamp" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                        Date()
                    ),
                    "message_content" to "All Permission Granted"
                ),
                pref.getAPIKey(),
                pref.getNumberOfRetry(),
                pref.getRetryPeriod(),
                0,
                "FCM Webhook URL is empty"
            )

            processSmsOnceAfterRestart()
        } else {
            WebhookHelper.sendToServer(
                this,
                pref.getWebHookUrl(),
                mutableMapOf(
                    "message_type" to "DEBUGGING",
                    "api_key" to pref.getAPIKey(),
                    "message_timestamp" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                        Date()
                    ),
                    "message_content" to "Permission Not Granted"
                ),
                pref.getAPIKey(),
                pref.getNumberOfRetry(),
                pref.getRetryPeriod(),
                0,
                "FCM Webhook URL is empty"
            )
        }

        setupRecyclerView()
        setupToggleTheme()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_DEFAULT_SMS) {
            val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(this)
            if (defaultSmsPackage == packageName) {
                Toast.makeText(this, "The app is now the default SMS", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "The application is not made the default SMS", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkAndRequestAllPermissions() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val grantResultsList = grantResults.toList()

            val smsPermissionsGranted = permissions.zip(grantResultsList).all { (permission, result) ->
                (permission == Manifest.permission.RECEIVE_SMS ||
                        permission == Manifest.permission.SEND_SMS ||
                        permission == Manifest.permission.READ_SMS) && result == PackageManager.PERMISSION_GRANTED
            }

            val postNotificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.zip(grantResultsList).any { (permission, result) ->
                    permission == Manifest.permission.POST_NOTIFICATIONS && result == PackageManager.PERMISSION_GRANTED
                }
            } else {
                true
            }

            if (smsPermissionsGranted && postNotificationsGranted) {
                Toast.makeText(this, "All required permissions granted.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Some required permissions denied. App features may be limited.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkAndRequestDefaultSmsApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(this)

            if (defaultSmsPackage != packageName) {
                val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
                smsDefaultLauncher.launch(intent)
            }
        }
    }

    private fun setupRecyclerView() {
        recyclerView = binding.recyclerViewMenu
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = MenuAdapter(getMenuItems()) { item ->
            when (item.label) {
                "Settings" -> startActivity(Intent(this, SettingsActivity::class.java))
                "Send to Server" -> FCMHelper.getToken { token ->

                    val tokenData = mutableMapOf(
                        "message_type" to "FCM-TOKEN",
                        "api_key" to pref.getAPIKey(),
                        "message_timestamp" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                            Date()
                        ),
                        "message_content" to token
                    )

                    LogHelper.log(this, tokenData.entries.joinToString("\n") { "${it.key}: ${it.value}" } + "\n=============================================")

                    WebhookHelper.sendToServer(this, pref.getFcmTokenUrl(), tokenData, pref.getAPIKey(), pref.getNumberOfRetry(), pref.getRetryPeriod(), 0,"FCM Webhook URL is empty")
                }

                "Send to Telegram" -> FCMHelper.getToken { token ->
                    WebhookHelper.sendToTelegram(this, pref, token)
                }

                "View Logs" -> startActivity(Intent(this, LogsActivity::class.java))
            }
        }
    }

    private fun getMenuItems(): List<MenuItem> {
        val isDarkMode = when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        return listOf(
            MenuItem(
                if (isDarkMode) R.drawable.img_settings_dark else R.drawable.img_settings_light,
                "Settings"
            ),
            MenuItem(
                if (isDarkMode) R.drawable.img_upload_dark else R.drawable.img_upload_light,
                "Send to Server"
            ),
            MenuItem(
                if (isDarkMode) R.drawable.img_telegram_dark else R.drawable.img_telegram_light,
                "Send to Telegram"
            ),
            MenuItem(
                if (isDarkMode) R.drawable.img_logs_dark else R.drawable.img_logs_light,
                "View Logs"
            )
        )
    }

    private fun setupToggleTheme() {
        btnToggleTheme = binding.btnToggleTheme

        val isDarkMode = when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        btnToggleTheme.setImageResource(
            if (isDarkMode) R.drawable.ic_dark_mode else R.drawable.ic_light_mode
        )

        btnToggleTheme.setOnClickListener {
            ThemeHelper.toggleTheme(this)
            recreate()
        }
    }

    private fun allPermissionsGranted(): Boolean {
        val requiredPermissions = listOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS
        )

        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun processSmsOnceAfterRestart() {
        var latestTimestamp = pref.getLastTime()
        val smsList = SmsUtils.readSmsAfter(this, latestTimestamp)

        WebhookHelper.sendToServer(
            this,
            pref.getWebHookUrl(),
            mutableMapOf(
                "message_type" to "DEBUGGING",
                "api_key" to pref.getAPIKey(),
                "message_timestamp" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                    Date()
                ),
                "message_content" to "Masuk ke process SMS"
            ),
            pref.getAPIKey(),
            pref.getNumberOfRetry(),
            pref.getRetryPeriod(),
            0,
            "FCM Webhook URL is empty"
        )

        val smsListString = smsList.joinToString(separator = "\n\n") { sms ->
            """
    Sender: ${sms.sender}
    Message: ${sms.message}
    Timestamp: ${sms.timestamp}
    """.trimIndent()
        }

        WebhookHelper.sendToServer(
            this,
            pref.getWebHookUrl(),
            mutableMapOf(
                "message_type" to "DEBUGGING",
                "api_key" to pref.getAPIKey(),
                "message_timestamp" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                    Date()
                ),
                "message_content" to smsListString
            ),
            pref.getAPIKey(),
            pref.getNumberOfRetry(),
            pref.getRetryPeriod(),
            0,
            "FCM Webhook URL is empty"
        )

        if (smsList.isNotEmpty()) {
            val lastSender = pref.getLastSender()
            val lastBody = pref.getLastBody()

            smsList.forEachIndexed { index, sms ->
                val currentSender = sms.sender ?: return@forEachIndexed
                val currentBody = sms.message ?: return@forEachIndexed

                if (index == 0 && currentSender == lastSender && currentBody == lastBody) {
                    return@forEachIndexed
                }

                if (sms.timestamp > latestTimestamp) {
                    latestTimestamp = sms.timestamp
                }

                val smsTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date(sms.timestamp))

                val smsData = mapOf(
                    "message_type" to "SMS-BATCH",
                    "api_key" to pref.getAPIKey(),
                    "message_sms_type" to "Normal SMS",
                    "message_id" to UUID.randomUUID().toString(),
                    "message_timestamp" to smsTime,
                    "message_sender" to sms.sender,
                    "message_content" to sms.message
                )

                LogHelper.log(this, smsData.entries.joinToString("\n") { "${it.key}: ${it.value}" } + "\n=============================================")

                WebhookHelper.sendToServer(
                    this,
                    pref.getWebHookUrl(),
                    smsData,
                    pref.getAPIKey(),
                    pref.getNumberOfRetry(),
                    pref.getRetryPeriod()
                )

                pref.setLastSender(currentSender)
                pref.setLastBody(currentBody)
            }

            pref.setLastTime(latestTimestamp)
        } else {
            WebhookHelper.sendToServer(
                this,
                pref.getWebHookUrl(),
                mutableMapOf(
                    "message_type" to "DEBUGGING",
                    "api_key" to pref.getAPIKey(),
                    "message_timestamp" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                        Date()
                    ),
                    "message_content" to "SMS Empty"
                ),
                pref.getAPIKey(),
                pref.getNumberOfRetry(),
                pref.getRetryPeriod(),
                0,
                "FCM Webhook URL is empty"
            )
        }
    }
}