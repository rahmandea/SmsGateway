package com.intelix.smsgateway.helper

import android.content.Context
import android.net.Uri
import android.util.Log
import com.intelix.smsgateway.model.SmsMessageModel
import java.io.Console

object SmsUtils {
    fun readSmsAfter(context: Context, afterMillis: Long): List<SmsMessageModel> {
        val smsList = mutableListOf<SmsMessageModel>()
        val uriSms = Uri.parse("content://sms/inbox")
        val projection = arrayOf("address", "body", "date")
        val selection = "date > ?"
        val selectionArgs = arrayOf(afterMillis.toString())
        val sortOrder = "date ASC"

        val cursor = context.contentResolver.query(
            uriSms, projection, selection, selectionArgs, sortOrder
        )

        cursor?.use {
            val indexAddress = it.getColumnIndex("address")
            val indexBody = it.getColumnIndex("body")
            val indexDate = it.getColumnIndex("date")

            while (it.moveToNext()) {
                val address = it.getString(indexAddress) ?: "UNKNOWN"
                val body = it.getString(indexBody) ?: ""
                val date = it.getLong(indexDate)

                smsList.add(SmsMessageModel(address, body, date))
            }
        }

        return smsList
    }
}
