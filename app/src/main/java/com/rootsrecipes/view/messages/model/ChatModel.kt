package com.rootsrecipes.view.messages.model

import java.io.Serializable

data class ChatModel(
    var type:Int,
    var message : String
)


data class ChatData(
    var chatID: String = "",
    var unreadMessageCount: Int = 0,
    var timeStamp: Long = 0,
    var isTyping: Boolean = false,
    var isDeletedChat: Boolean = false,
    var blockedStatus: Boolean = false,
    var whoBlocked: String = "",
    var opponentUser: UserDataChat = UserDataChat(),
    var message: MessageData = MessageData(),
    var loginUserData: UserDataChat = UserDataChat(),
) : Serializable
data class UserDataChat(
    var userID: String = "",
    var name: String = "",
    var image: String? = "",
    var onlineStatus: Int? = 0,
    var deleteStatus: Int? = 0,
    var messageNotification: Boolean? = true,
    var deleteChatStatus:String?="",
    var deleteLastChatStatus:Any?=0,
    var chatType:String?="",
    var fcmToken: String? = "",
    var readMessage: Boolean? = true,
    var osType : Int = 0
) : Serializable
data class MessageData(
    var message: String? = "",
    var messageTime: Any? = 0,
    var messageType:Int?=0,
    var opponentID: String? = "",
    var readStatus: Boolean? = false,
    var senderID: String? = "",
    var messageID: String? = ""

) : Serializable
data class GetAllMessages (
    var messageId:String?="",
    var message:String?="",
    var messageTime:Long?=0,
    var messageType:Int?=0,
    var opponentID:String?="",
    var readStatus:Boolean?=false,
    var senderID:String?="",
    var isOpened:Boolean?=false
)
data class ChatMessages (
    var message:String?="",
    var messageTime:Long?=0,
    var messageType:Int?=0,
    var opponentID:String?="",
    var readStatus:Boolean?=false,
    var senderID:String?="",
) : Serializable

