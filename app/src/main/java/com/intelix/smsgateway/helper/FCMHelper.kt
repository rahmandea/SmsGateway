package com.intelix.smsgateway.helper

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging

object FCMHelper {
    fun getToken(onResult: (String) -> Unit) {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                onResult(token)
            }
            .addOnFailureListener { e ->
                Log.e("FCMHelper", "Failed to get FCM token", e)
            }
    }
}
