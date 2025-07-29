package com.intelix.smsgateway.model

data class SmsMessageModel(
    val sender: String,
    val message: String,
    val timestamp: Long
)
