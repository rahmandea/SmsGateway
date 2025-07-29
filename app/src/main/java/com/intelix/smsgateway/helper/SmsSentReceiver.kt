package com.intelix.smsgateway.helper

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import com.intelix.smsgateway.WebhookPreferences
import java.text.SimpleDateFormat
import java.util.*

class SmsSentReceiver : BroadcastReceiver() {

    companion object {
        const val SMS_SENT_ACTION = "com.intelix.smsgateway.SMS_SENT_ACTION"
        const val EXTRA_MESSAGE_ID = "extra_message_id"
        const val EXTRA_MESSAGE_TIMESTAMP = "extra_message_timestamp"
        const val EXTRA_TO_NUMBER = "extra_to_number"
        const val EXTRA_MESSAGE_CONTENT = "extra_message_content"
        const val EXTRA_RETRY_COUNT = "extra_retry_count"
        const val EXTRA_ORIGINAL_TIMESTAMP_MILLIS = "extra_original_timestamp_millis"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == SMS_SENT_ACTION) {
            val pref = WebhookPreferences(context)
            val messageId = intent.getStringExtra(EXTRA_MESSAGE_ID) ?: return
            val messageTimestamp = intent.getStringExtra(EXTRA_MESSAGE_TIMESTAMP) ?: return
            val toNumber = intent.getStringExtra(EXTRA_TO_NUMBER) ?: return
            val messageContent = intent.getStringExtra(EXTRA_MESSAGE_CONTENT) ?: return
            val retryCount = intent.getIntExtra(EXTRA_RETRY_COUNT, 0)
            val originalTimestampMillis = intent.getLongExtra(EXTRA_ORIGINAL_TIMESTAMP_MILLIS, 0L)

            val resultStatus = when (resultCode) {
                Activity.RESULT_OK -> "success"
                SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "failed_generic"
                SmsManager.RESULT_ERROR_NO_SERVICE -> "failed_no_service"
                SmsManager.RESULT_ERROR_NULL_PDU -> "failed_null_pdu"
                SmsManager.RESULT_ERROR_RADIO_OFF -> "failed_radio_off"
                else -> "failed_unknown"
            }

            val generalStatus = if (resultCode == Activity.RESULT_OK) "SUCCESS" else "FAILED"

            val statusData = mapOf(
                "message_type" to "SMS-STATUS",
                "api_key" to pref.getAPIKey(),
                "message_id" to messageId,
                "message_status" to resultStatus,
                "message_general_status" to generalStatus,
                "message_timestamp" to messageTimestamp
            )

            LogHelper.log(context, statusData.entries.joinToString("\n") { "${it.key}: ${it.value}" } + "\n=============================================")

            WebhookHelper.sendToServer(
                context,
                pref.getWebHookUrl(),
                statusData,
                pref.getAPIKey(),
                pref.getNumberOfRetry(),
                pref.getRetryPeriod()
            )

            if (resultCode != Activity.RESULT_OK) {
                val maxRetry = pref.getNumberOfRetry().toInt()
                val retryPeriod = pref.getRetryPeriod() * 1000L

                if (retryCount < maxRetry) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        SmsSenderHelper.sendSms(context, toNumber, messageContent, originalTimestampMillis, retryCount + 1)
                    }, retryPeriod)
                }
            }
        }
    }
}