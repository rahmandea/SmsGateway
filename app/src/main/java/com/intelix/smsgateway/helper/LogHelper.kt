package com.intelix.smsgateway.helper

import android.content.Context
import com.intelix.smsgateway.WebhookPreferences
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object LogHelper {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun getLogFileForDate(context: Context, date: Date): File {
        val name = "SMS-LOG-${dateFormat.format(date)}.txt"
        return File(context.filesDir, name)
    }

    fun log(context: Context, text: String) {
        val pref = WebhookPreferences(context)
        val maxFiles = pref.getNumberOfLogFiles().toInt()

        val logFiles = context.filesDir.listFiles { file ->
            file.name.startsWith("SMS-LOG-") && file.name.endsWith(".txt")
        }?.toList() ?: emptyList()

        if (logFiles.size > maxFiles) {
            val sortedFiles = logFiles.sortedBy { it.name }
            val toDeleteCount = logFiles.size - maxFiles + 1
            sortedFiles.take(toDeleteCount).forEach { it.delete() }
        }

        getLogFileForDate(context, Date()).appendText("$text\n")
    }

    fun loadLogsBetweenDates(context: Context, startDate: Date, endDate: Date): String {
        val logs = StringBuilder()
        val calendar = Calendar.getInstance()
        calendar.time = startDate

        while (!calendar.time.after(endDate)) {
            val file = getLogFileForDate(context, calendar.time)
            if (file.exists()) {
                logs.append(file.readText())
            }
            calendar.add(Calendar.DATE, 1)
        }

        return if (logs.isEmpty()) "There are no logs for that date range." else logs.toString()
    }
}
