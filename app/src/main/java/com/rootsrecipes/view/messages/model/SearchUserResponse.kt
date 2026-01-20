package com.rootsrecipes.view.messages.model

data class SearchUserResponse(
    val `data`: ArrayList<Data>, val statusCode: Int
)

data class Data(
    val _id: String,
    val email: String,
    val first_name: String,
    val followers_count: Int,
    val following_count: Int,
    val is_followed_by: Boolean,
    var is_following: Boolean,
    val last_name: String? = "",
    val profile_image: String,
    val user_name: String
)