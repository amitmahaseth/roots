package com.rootsrecipes.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.SharedPref

class MainViewModel(private val sharedPref: SharedPref) : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    fun updateUserActiveStatus(isOnline: Boolean) {
        val userId = sharedPref.getSignInData()?._id
        if (userId != null) {
            val userMap = hashMapOf(
                "isOnline" to isOnline,
                "fcmToken" to sharedPref.getString(Constants.FCM_TOKEN),
                "osType" to Constants.OS_TYPE
            )
            firestore.collection(Constants.USERS_COLLECTION)
                .document(userId)
                .set(
                    userMap,
                    SetOptions.merge()
                ) // This will create or update only the specified fields
                .addOnSuccessListener {
                    Log.d("Firestore", "User online status set/updated successfully.")
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Failed to set/update user online status", e)
                }
        }
    }

    private var unreadListener: ListenerRegistration? = null

    fun listenToTotalUnreadCount(userId: String, onUnreadCountChanged: (Int) -> Unit) {
        val unreadRef = firestore.collection(Constants.UNREAD_MESSAGES_COLLECTION).document(userId)

        unreadListener = unreadRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("UnreadCount", "Listen failed: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val unreadMap = snapshot.data ?: emptyMap<String, Any>()
                val totalUnreadCount = unreadMap.values.sumOf { (it as? Long ?: 0L).toInt() }

                // Callback with total unread count
                onUnreadCountChanged(totalUnreadCount)
            } else {
                // If document doesn't exist or is empty
                onUnreadCountChanged(0)
            }
        }
    }

    fun getOpponentInfo(
        chatId: String,
        opponentId: String,
        callback: (String, String, String) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        db.collection("Chats")
            .document(chatId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val opponentData = document.get(opponentId) as? Map<*, *>
                    if (opponentData != null) {
                        val name = opponentData["name"] as? String ?: ""
                        val image = opponentData["image"] as? String ?: ""
                        val userId = opponentData["userId"] as? String ?: opponentId
                        callback(name, image, userId)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to fetch opponent info", e)
            }
    }


}