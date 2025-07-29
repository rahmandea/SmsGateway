package com.intelix.smsgateway.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.intelix.smsgateway.WebhookPreferences
import com.intelix.smsgateway.helper.SmsSenderHelper
import com.intelix.smsgateway.helper.LogHelper
import com.intelix.smsgateway.helper.WebhookHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IntelixFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(msg: RemoteMessage) {
        val to = msg.data["to"] ?: return
        val text = msg.data["message"] ?: return
        val timestampMillis = msg.sentTime

        SmsSenderHelper.sendSms(this, to, text, timestampMillis)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val context = applicationContext
        val pref = WebhookPreferences(this)
        val url = pref.getFcmTokenUrl()

        val tokenData = mutableMapOf(
            "message_type" to "FCM-TOKEN",
            "api_key" to pref.getAPIKey(),
            "message_timestamp" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
            "message_content" to token
        )

        LogHelper.log(context, tokenData.entries.joinToString("\n") { "${it.key}: ${it.value}" } + "\n=============================================")

        WebhookHelper.sendToServer(context, url, tokenData, pref.getAPIKey(), pref.getNumberOfRetry(), pref.getRetryPeriod(),0, "FCM Webhook URL is empty")
    }
}