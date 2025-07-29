package com.intelix.smsgateway.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.intelix.smsgateway.R
import com.intelix.smsgateway.WebhookPreferences
import com.intelix.smsgateway.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private val pref by lazy { WebhookPreferences(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Settings"

        loadInputs()

        binding.btnSave.setOnClickListener {
            applyInputs()
            finish()
        }

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun loadInputs() = with(binding) {
        editAPIKey.setText(pref.getAPIKey())
        editWebHook.setText(pref.getWebHookUrl())
        editFcmWebhook.setText(pref.getFcmTokenUrl())
        editBotToken.setText(pref.getBotToken())
        editChatId.setText(pref.getChatId())
        editNumberOfRetry.setText(pref.getNumberOfRetry().toString())
        editRetryPeriod.setText(pref.getRetryPeriod().toString())
        editNumberOfLogFiles.setText(pref.getNumberOfLogFiles().toString())
    }

    private fun applyInputs() = with(binding) {
        pref.apply {
            setAPIKey(editAPIKey.text.toString())
            setWebHookUrl(editWebHook.text.toString())
            setFcmTokenUrl(editFcmWebhook.text.toString())
            setBotToken(editBotToken.text.toString())
            setChatId(editChatId.text.toString())
            setNumberOfRetry(editNumberOfRetry.text.toString().toLongOrNull()?.coerceAtLeast(0) ?: 0)
            setRetryPeriod(editRetryPeriod.text.toString().toLongOrNull()?.coerceAtLeast(0) ?: 0)
            setNumberLogFiles(editNumberOfLogFiles.text.toString().toLongOrNull()?.coerceAtLeast(0) ?: 0)
        }
    }
}
