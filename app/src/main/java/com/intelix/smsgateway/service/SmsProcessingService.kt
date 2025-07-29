package com.intelix.smsgateway.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.SmsMessage
import android.util.Log
import androidx.core.app.NotificationCompat
import com.intelix.smsgateway.WebhookPreferences
import com.intelix.smsgateway.R
import com.intelix.smsgateway.helper.LogHelper
import com.intelix.smsgateway.helper.WebhookHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class SmsProcessingService : Service() {

    private val CHANNEL_ID = "SmsGatewayChannel"
    private val NOTIFICATION_ID = 101

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())

        val bundle = intent?.extras
        val pdus = bundle?.get("pdus") as? Array<*>
        val format = bundle?.getString("format")

        if (pdus != null && format != null) {
            val pref = WebhookPreferences(this)
            val messageId = UUID.randomUUID().toString()

            val messages = pdus.mapNotNull {
                SmsMessage.createFromPdu(it as ByteArray, format)
            }

            CoroutineScope(Dispatchers.IO).launch {
                for (msg in messages) {
                    val sender = msg.originatingAddress ?: "Unknown"
                    val body = msg.messageBody
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val date = Date(msg.timestampMillis)
                    val messageTimestamp = sdf.format(date)

                    val messageClass = msg.messageClass
                    val type = when (messageClass) {
                        SmsMessage.MessageClass.CLASS_0 -> "Flash SMS"
                        SmsMessage.MessageClass.CLASS_1 -> "Normal SMS"
                        SmsMessage.MessageClass.CLASS_2 -> "SIM SMS"
                        SmsMessage.MessageClass.CLASS_3 -> "Terminal SMS"
                        else -> "Unknown"
                    }

                    val baseData = mutableMapOf(
                        "message_type" to "SMS",
                        "api_key" to pref.getAPIKey(),
                        "message_sms_type" to type,
                        "message_id" to messageId,
                        "message_timestamp" to messageTimestamp,
                        "message_sender" to sender,
                        "message_content" to body
                    )

                    pref.setLastTime(msg.timestampMillis);
                    pref.setLastSender(sender)
                    pref.setLastBody(body)

                    val url = pref.getWebHookUrl()

                    LogHelper.log(applicationContext, baseData.entries.joinToString("\n") { "${it.key}: ${it.value}" } + "\n=============================================")

                    WebhookHelper.sendToServer(applicationContext, url, baseData, pref.getAPIKey(), pref.getNumberOfRetry(), pref.getRetryPeriod())
                }

                stopSelf(startId)
            }
        } else {
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "SMS Gateway Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SMS Gateway Running")
            .setContentText("Processing incoming SMS messages...")
            .setSmallIcon(R.drawable.ic_notification)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }
}
