package com.intelix.smsgateway.helper

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import com.intelix.smsgateway.WebhookPreferences
import java.text.SimpleDateFormat
import java.util.*

object SmsSenderHelper {
    fun sendSms(
        context: Context,
        to: String,
        message: String,
        timestampMillis: Long,
        retryCount: Int = 0
    ) {
        val pref = WebhookPreferences(context)
        val messageId = UUID.randomUUID().toString()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val messageTimestamp = sdf.format(Date(timestampMillis))

        val baseData = mapOf(
            "message_type" to "SMS",
            "api_key" to pref.getAPIKey(),
            "message_sms_type" to "Normal SMS",
            "message_id" to messageId,
            "message_timestamp" to messageTimestamp,
            "message_receiver" to to,
            "message_content" to message
        )

        val url = pref.getWebHookUrl()

        val sentIntent = Intent(context, SmsSentReceiver::class.java).apply {
            action = SmsSentReceiver.SMS_SENT_ACTION
            putExtra(SmsSentReceiver.EXTRA_MESSAGE_ID, messageId)
            putExtra(SmsSentReceiver.EXTRA_MESSAGE_TIMESTAMP, messageTimestamp)
            putExtra(SmsSentReceiver.EXTRA_TO_NUMBER, to)
            putExtra(SmsSentReceiver.EXTRA_MESSAGE_CONTENT, message)
            putExtra(SmsSentReceiver.EXTRA_RETRY_COUNT, retryCount)
            putExtra(SmsSentReceiver.EXTRA_ORIGINAL_TIMESTAMP_MILLIS, timestampMillis)
        }

        val pendingSentIntent = PendingIntent.getBroadcast(
            context,
            messageId.hashCode(), // Use a unique request code
            sentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        WebhookHelper.sendToServer(
            context,
            url,
            baseData,
            pref.getAPIKey(),
            pref.getNumberOfRetry(),
            pref.getRetryPeriod()
        )

        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

        smsManager.sendTextMessage(to, null, message, pendingSentIntent, null)

        LogHelper.log(context, baseData.entries.joinToString("\n") { "${it.key}: ${it.value}" } + "\n=============================================")
    }
}