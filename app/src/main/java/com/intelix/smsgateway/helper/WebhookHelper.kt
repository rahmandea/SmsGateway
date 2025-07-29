package com.intelix.smsgateway.helper

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.intelix.smsgateway.WebhookPreferences
import okhttp3.*
import java.io.IOException

object WebhookHelper {
    private val client = OkHttpClient()

    fun sendToServer(
        context: Context,
        url: String,
        params: Map<String, String>,
        apiKey: String = "",
        maxRetry: Long,
        retryPeriodSeconds: Long,
        attempt: Int = 0,
        urlBlankMsg: String = "Webhook URL is empty"
    ) {
        if (url.isBlank()) {
            showToastIfForeground(context, urlBlankMsg)
            return
        }

        val formBody = FormBody.Builder().apply {
            params.forEach { (key, value) -> add(key, value) }
        }.build()

        val requestBuilder = Request.Builder()
            .url(url)
            .post(formBody)

        if (apiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "API-Key $apiKey")
        }

        val request = requestBuilder.build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (attempt < maxRetry) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        sendToServer(
                            context, url, params, apiKey,
                            maxRetry, retryPeriodSeconds, attempt + 1
                        )
                    }, retryPeriodSeconds * 1000)
                } else {
                    showToastIfForeground(context, "Failed to send after retry")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val message = if (response.isSuccessful) "Sent successfully"
                else "Error sending data"
                showToastIfForeground(context, message)
            }
        })
    }

    fun sendToTelegram(context: Context, pref: WebhookPreferences, token: String) {
        val botToken = pref.getBotToken()
        val chatId = pref.getChatId()

        if (botToken.isEmpty() || chatId.isEmpty()) {
            showToastIfForeground(context, "Bot Token or Chat ID missing")
            return
        }

        val message = "FCM Token:\n\n$token"
        val url = "https://api.telegram.org/bot$botToken/sendMessage"
        val requestBody = FormBody.Builder()
            .add("chat_id", chatId)
            .add("text", message)
            .build()

        val request = Request.Builder().url(url).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToastIfForeground(context, "Failed to send to Telegram")
            }

            override fun onResponse(call: Call, response: Response) {
                val msg = if (response.isSuccessful) "Sent to Telegram"
                else "Error sending to Telegram"
                showToastIfForeground(context, msg)
            }
        })
    }

    private fun showToastIfForeground(context: Context, message: String) {
        if (AppVisibilityChecker.isAppInForeground(context)) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
