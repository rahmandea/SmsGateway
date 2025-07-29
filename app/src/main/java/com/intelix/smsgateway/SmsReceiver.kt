package com.intelix.smsgateway

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.intelix.smsgateway.helper.LogHelper
import com.intelix.smsgateway.services.SmsProcessingService

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            val serviceIntent = Intent(context, SmsProcessingService::class.java).apply {
                putExtras(intent.extras!!)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}