package com.rootsrecipes.view.messages.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.rootsrecipes.view.messages.model.ChatData
import com.rootsrecipes.view.messages.model.ChatMessages
import com.rootsrecipes.view.messages.model.MessageData
import com.rootsrecipes.view.messages.model.UserDataChat

class MessageViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _chatList = MutableLiveData<List<ChatData>>()
    val chatList: LiveData<List<ChatData>> = _chatList

    fun fetchChatsForUser(userId: String) {
        firestore.collection("Chats")
            .whereArrayContains("members", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MessageViewModel", "Error fetching chats: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    Log.d("MessageViewModel", "No chats found for user: $userId")
                    _chatList.value = emptyList()
                    return@addSnapshotListener
                }

                val chatList = snapshot.documents.mapNotNull { doc ->
                    val chatId = doc.id
                    Log.d("ChatDebugMessage", "Document: $doc")

                    val lastMessageMap = doc.get("lastMessage") as? Map<*, *>
                    val message = lastMessageMap?.let {
                        MessageData(
                            message = it["message"] as? String,
                            messageTime = it["messageTime"] as? Long,
                            messageType = it["messageType"] as? Int,
                            senderID = it["senderID"] as? String,
                            readStatus = it["readStatus"] as? Boolean,
                            opponentID = it["opponentID"] as? String
                        )
                    }

                    val opponentId = (doc.get("members") as? List<*>)?.find { it != userId } as? String

                    if (opponentId == null || message == null) return@mapNotNull null

                    val opponentMap = doc.get(opponentId) as? Map<*, *>
                    val opponentUser = UserDataChat(
                        userID = opponentId,
                        name = opponentMap?.get("name") as? String ?: "",
                        image = opponentMap?.get("image") as? String ?: "",
                        osType = 0
                    )

                    val unreadMap = doc.get("unreadMessageCount") as? Map<*, *>
                    val unreadCount = (unreadMap?.get(userId) as? Long ?: 0).toInt()
                    Log.d("ChatDebugMessage", "Unread count for $userId in $chatId: $unreadCount")

                    val blocked = doc.getBoolean("blocked") ?: false

                    ChatData(
                        chatID = chatId,
                        message = message,
                        opponentUser = opponentUser,
                        unreadMessageCount = unreadCount,
                        blockedStatus = blocked
                    )
                }

                Log.d("MessageViewModel", "Fetched ${chatList.size} chats for user $userId")
                _chatList.value = chatList
            }
    }
}
