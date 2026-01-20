package com.rootsrecipes.view.messages.viewModel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.gson.JsonObject
import com.rootsrecipes.di.data.repository.MainRepository
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.MyApplication
import com.rootsrecipes.utils.NetworkHelper
import com.rootsrecipes.utils.SharedPref
import com.rootsrecipes.view.messages.model.ChatMessages
import com.rootsrecipes.view.messages.model.GetAllMessages
import com.rootsrecipes.view.messages.model.UserDataChat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class ChatViewModel(
    private val sharedPref: SharedPref,
    private val mainRepository: MainRepository,
    private val networkHelper: NetworkHelper
) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var messageRef: CollectionReference
    private var listener: ListenerRegistration? = null

    val chatAllMessagesList = MutableLiveData<ArrayList<GetAllMessages>>()


    fun fetchLoginUserRecentDataMethod(myId: String, opponentId: String, chatId: String) {
        firestore.collection(Constants.CHATS_COLLECTION).whereArrayContains("members", myId).get()
            .addOnSuccessListener {
                fetchAllMessages(chatId, myId, opponentId)
            }
            .addOnFailureListener {
                fetchAllMessages(chatId, myId, opponentId)
            }
    }

    private fun fetchAllMessages(
        chatId: String,
        myId: String,
        opponentId: String
    ) {
        val chatRef = firestore.collection(Constants.CHATS_COLLECTION).document(chatId)

        chatRef.update("unreadMessageCount.$myId", 0)

        messageRef = chatRef.collection("Messages")

        listener = messageRef.addSnapshotListener { snapshot, _ ->
            val messages = snapshot?.documents?.mapNotNull { doc ->
                val msg = doc.toObject(ChatMessages::class.java)
                msg?.let {
                    val time = it.messageTime?.times(1000) ?: return@mapNotNull null
                    run {
                        if (MyApplication.currentChatId == chatId) {
                            if (it.senderID == opponentId && it.readStatus != true) {
                                doc.reference.update("readStatus", true)
                            }
                        }
                        GetAllMessages(
                            messageId = doc.id,
                            message = it.message,
                            messageTime = time,
                            messageType = it.messageType,
                            opponentID = it.opponentID,
                            readStatus = it.readStatus,
                            senderID = it.senderID
                        )
                    }
                }
            }?.sortedBy { it.messageTime }?.toCollection(ArrayList())

            messages?.let {
                chatAllMessagesList.value = it
            }
            if (MyApplication.currentChatId == chatId) {
                chatRef.update("unreadMessageCount.$myId", 0)
            }
        }
    }

    fun clearUnreadCount(chatId: String, myId: String) {
        firestore.collection(Constants.CHATS_COLLECTION).document(chatId)
            .update("unreadMessageCount.$myId", 0)
    }

    fun fetchUnreadCountMethod(
        chatId: String,
        opponentData: UserDataChat,
        message: ChatMessages,
        fcmToken: String,
        messageNotifications: Boolean
    ) {
        firestore.collection(Constants.CHATS_COLLECTION).document(chatId).get()
            .addOnSuccessListener {
                sendMessage(message, chatId, opponentData)
            }
            .addOnFailureListener {
                sendMessage(message, chatId, opponentData)
            }
    }

    private fun sendMessage(message: ChatMessages, chatId: String, opponentData: UserDataChat) {
        val myId = sharedPref.getSignInData()?._id ?: return
        val myName =
            "${sharedPref.getSignInData()?.first_name} ${sharedPref.getSignInData()?.last_name}"
        val myProfile = sharedPref.getSignInData()?.profile_image

        val chatDocRef = firestore.collection(Constants.CHATS_COLLECTION).document(chatId)
        messageRef = chatDocRef.collection("Messages")

        // Fetch opponent data from Chats collection
        chatDocRef.get()
            .addOnSuccessListener {
                val opponentName = opponentData.name
                val opponentProfile = opponentData.image

                val chatInitData = hashMapOf(
                    "members" to listOf(myId, opponentData.userID),
                    "chatID" to chatId,
                    myId to hashMapOf(
                        "name" to myName,
                        "image" to myProfile
                    ),
                    opponentData.userID to hashMapOf(
                        "name" to opponentName,
                        "image" to opponentProfile
                    )
                )

                chatDocRef.set(chatInitData, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d("ChatInit", "Chat initialized/updated with users")
                        messageRef.add(message)

                        val lastMsgData = hashMapOf(
                            "lastMessage" to message,
                            "updatedAt" to message.messageTime
                        )
                        chatDocRef.set(lastMsgData, SetOptions.merge())

                        chatDocRef.update(
                            "unreadMessageCount.${opponentData.userID}",
                            FieldValue.increment(1)
                        )
                            .addOnSuccessListener {
                                Log.d("UnreadMessage", "Unread count incremented")
                                incrementUnreadCountForUser(opponentData.userID, chatId)
                            }
                            .addOnFailureListener { e ->
                                Log.e("UnreadMessage", "Failed: ${e.message}")
                                val fallbackMap = hashMapOf(
                                    "unreadMessageCount.${opponentData.userID}" to FieldValue.increment(
                                        1
                                    )
                                )
                                chatDocRef.set(fallbackMap, SetOptions.merge())
                                incrementUnreadCountForUser(opponentData.userID, chatId)
                            }

                        sendMessageNotification(
                            myName, opponentData.userID, myId, chatId, message.message,
                            Constants.OS_TYPE, 1
                        )

                    }
                    .addOnFailureListener {
                        Log.e("ChatInit", "Failed to merge chat users: ${it.message}")
                    }
            }
            .addOnFailureListener {
                Log.e("UserFetch", "Failed to get opponent data: ${it.message}")
            }
    }

    fun stopMessageListener() {
        listener?.remove()
    }

    private fun incrementUnreadCountForUser(userId: String, chatId: String) {
        firestore.runTransaction { transaction ->
            val unreadRef =
                firestore.collection(Constants.UNREAD_MESSAGES_COLLECTION).document(userId)
            val unreadDoc = transaction.get(unreadRef)
            val unreadMap = unreadDoc.data?.toMutableMap() ?: hashMapOf()
            val currentUnread = (unreadMap[chatId] as? Long ?: 0)
            unreadMap[chatId] = currentUnread + 1
            transaction.set(unreadRef, unreadMap, SetOptions.merge())
        }.addOnSuccessListener {
            Log.d("UnreadCount", "Successfully incremented unread count for $userId in $chatId")
        }.addOnFailureListener { e ->
            Log.e("UnreadCount", "Failed to increment unread count: ${e.message}")
        }
    }

    fun listenToUserOnlineStatus(
        userId: String,
        onResult: (Boolean?) -> Unit
    ): ListenerRegistration? {
        return if (userId.isNotEmpty()) {
            firestore.collection(Constants.USERS_COLLECTION)
                .document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        onResult(null)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val isOnline = snapshot.getBoolean("isOnline")
                        onResult(isOnline)
                    } else {
                        onResult(null) // Document doesn't exist
                    }
                }
        } else {
            null
        }
    }

    private fun sendMessageNotification(
        name: String,
        otherUserId: String?,
        loginUserId: String?,
        chatId: String?,
        message: String?,
        osType: Int,
        messageType: Int?
    ) {
        CoroutineScope(Dispatchers.IO).launch {

            try {
                val root = JsonObject()
                val messageObj = JsonObject()
                val notification = JsonObject()
                val data = JsonObject()

                notification.addProperty("title", "$name sent a message to you")
                notification.addProperty(
                    "body", when (messageType) {
                        1 -> message
                        else -> message
                    }
                )
                when (messageType) {
                    1 -> {
                        data.addProperty("message", message)
                        data.addProperty("body", message)
                    }

                }
                data.addProperty("receiverID", otherUserId)
                data.addProperty("senderID", loginUserId)
                data.addProperty("type", "message")
                data.addProperty("messageType", messageType)
                data.addProperty("chatId", chatId)
                data.addProperty("userName", name)


                data.addProperty("notification_type", "chat")
                data.addProperty("notification_user_type", "")

                data.addProperty("notification_title", "$name sent a message to you")


                messageObj.addProperty("topic", "$otherUserId")

                if (osType == 1) {
                    messageObj.add("notification", notification)
                }

                messageObj.add("data", data)

                root.add("message", messageObj)
                hitApiForSendNotification(root)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private suspend fun hitApiForSendNotification(jsonObject: JsonObject) {
        if (networkHelper.isNetworkConnected()) {
            mainRepository.sendNotification(jsonObject).let {
                if (it.isSuccessful) {
                    Log.d("sendMessageDebug", it.body().toString())
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    Log.d("sendMessageDebug", errorMessage.toString())
                }
            }

        } else {
            Log.d("sendMessageDebug", "No internet connection")
        }
    }


}