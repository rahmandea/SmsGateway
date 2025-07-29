package com.intelix.smsgateway

import android.content.Context

class WebhookPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("gateway_prefs", Context.MODE_PRIVATE)

    fun getAPIKey(): String = prefs.getString("api_key", "") ?: ""
    fun getWebHookUrl(): String = prefs.getString("webhook_url", "") ?: ""
    fun getFcmTokenUrl(): String = prefs.getString("fcm_token_url", "") ?: ""
    fun getBotToken(): String = prefs.getString("bot_token", "") ?: ""
    fun getChatId(): String = prefs.getString("chat_id", "") ?: ""
    fun getNumberOfRetry(): Long = prefs.getLong("number_of_retry", 3)
    fun getRetryPeriod(): Long = prefs.getLong("retry_period", 60)
    fun getNumberOfLogFiles(): Long = prefs.getLong("number_of_log_files", 1)
    fun getLastTime(): Long {
        val saved = prefs.getLong("last_time", 0L)
        return if (saved == 0L) {
            val now = System.currentTimeMillis()
            setLastTime(now)
            now
        } else saved
    }
    fun getLastSender(): String = prefs.getString("last_sender", "") ?: ""
    fun getLastBody(): String = prefs.getString("last_body", "") ?: ""


    fun setAPIKey(apiKey: String) = prefs.edit().putString("api_key", apiKey).apply()
    fun setWebHookUrl(webHookUrl: String) = prefs.edit().putString("webhook_url", webHookUrl).apply()
    fun setFcmTokenUrl(fcmTokenUrl: String) = prefs.edit().putString("fcm_token_url", fcmTokenUrl).apply()
    fun setBotToken(token: String) = prefs.edit().putString("bot_token", token).apply()
    fun setChatId(id: String) = prefs.edit().putString("chat_id", id).apply()
    fun setNumberOfRetry(value: Long) = prefs.edit().putLong("number_of_retry", value).apply()
    fun setRetryPeriod(value: Long) = prefs.edit().putLong("retry_period", value).apply()
    fun setNumberLogFiles(value: Long) = prefs.edit().putLong("number_of_log_files", value).apply()
    fun setLastTime(timestamp: Long) = prefs.edit().putLong("last_time", timestamp).apply()
    fun setLastSender(sender: String) = prefs.edit().putString("last_sender", sender).apply()
    fun setLastBody(body: String) = prefs.edit().putString("last_body", body).apply()
}