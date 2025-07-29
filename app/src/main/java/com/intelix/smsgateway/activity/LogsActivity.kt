package com.intelix.smsgateway.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.datepicker.MaterialDatePicker
import com.intelix.smsgateway.R
import com.intelix.smsgateway.databinding.ActivityLogsBinding
import com.intelix.smsgateway.helper.LogHelper
import java.text.SimpleDateFormat
import java.util.*

class LogsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogsBinding
    private val dateFormat by lazy { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupDefaultLogs()
        setupDatePicker()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Logs"
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupDefaultLogs() {
        val todayStart = getStartOfDay()
        val todayEnd = getEndOfDay()

        binding.editDate.setText("${dateFormat.format(todayStart)} s/d ${dateFormat.format(todayEnd)}")
        displayLogs(todayStart, todayEnd)
    }

    private fun setupDatePicker() {
        binding.editDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Date Range")
                .setTheme(R.style.MyDatePickerTheme)
                .build()

            picker.addOnPositiveButtonClickListener { selection ->
                val start = selection.first?.let { Date(it) }
                val end = selection.second?.let { Date(it) }

                if (start != null && end != null) {
                    binding.editDate.setText("${dateFormat.format(start)} s/d ${dateFormat.format(end)}")
                    displayLogs(start, end)
                }
            }

            if (!isFinishing && !isDestroyed) {
                picker.show(supportFragmentManager, "date_range_picker")
            }
        }
    }

    private fun displayLogs(start: Date, end: Date) {
        binding.txtLogs.text = LogHelper.loadLogsBetweenDates(this, start, end)
    }

    private fun getStartOfDay(): Date {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    private fun getEndOfDay(): Date {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time
    }
}