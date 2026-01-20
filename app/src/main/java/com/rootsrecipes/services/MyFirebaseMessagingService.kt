package com.rootsrecipes.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.rootsrecipes.MainActivity
import com.rootsrecipes.R
import com.rootsrecipes.utils.MyApplication
import org.json.JSONObject
import java.util.*

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FirebaseMsgService"
        private const val DEFAULT_CHANNEL_ID = "default_channel_id"
        private const val DEFAULT_CHANNEL_NAME = "Default Channel"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        // Send token to server if required
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "From: ${message.from}")
        Log.d(TAG, "From: $message")
        Log.d(TAG, "From: ${message.data}")

        val payloadJson = parsePayload(message)
        if (payloadJson != null) {
//            val chatId = payloadJson.optString("chatId", "")
//            val isCurrentChat = MyApplication.currentChatId == chatId
//
//            val shouldNotify = !isCurrentChat
//            if (shouldNotify) {
                val title = payloadJson.optString("notification_title", message.notification?.title ?: "New Message")
                val body = payloadJson.optString("body", message.notification?.body ?: "")
                displayNotification(title, body, payloadJson.toString())
          //  }
        } else {
            Log.w(TAG, "Payload parsing failed or is empty")
        }
    }

    private fun parsePayload(message: RemoteMessage): JSONObject? {
        return try {
            when {
                message.data.isNotEmpty() -> {
                    message.data["data"]?.let { JSONObject(it) }
                        ?: JSONObject(message.data as Map<*, *>)
                }
                message.notification != null -> {
                    JSONObject(message.data as Map<*, *>)
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing payload", e)
            null
        }
    }

    private fun displayNotification(title: String, body: String, jsonPayload: String) {
        Log.d(TAG, "Displaying notification - Title: $title, Body: $body")

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId: String

        try {
            val payloadJson = JSONObject(jsonPayload)
            val notType=payloadJson.optString("notification_type", "")
            if(!notType.isNullOrEmpty()){
                Log.d("notificationTypeDebug",notType)
            }else{
                Log.d("notificationTypeDebug","No type found")
            }

            channelId = payloadJson.optString("chatId", DEFAULT_CHANNEL_ID)

            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("data", jsonPayload)
            }

            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getActivity(
                this, Random().nextInt(100000), intent, pendingIntentFlags
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId, DEFAULT_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for new messages"
                    enableLights(true)
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.drawable.app_icon)
                .setColor(getColor(R.color.white))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)

        } catch (e: Exception) {
            Log.e(TAG, "Error displaying notification", e)
        }
    }
}
