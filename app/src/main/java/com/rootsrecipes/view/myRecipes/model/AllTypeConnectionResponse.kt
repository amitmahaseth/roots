package com.rootsrecipes.view.myRecipes.model

data class AllTypeConnectionResponse(
    val `data`: ArrayList<AllTypeConnection>, val statusCode: Int
)

data class AllTypeConnection(
    val email: String,
    val firstName: String,
    val lastName: String,
    val profileImage: String,
    val userName: String,
    val user_id: String,
    var isCheckedUser: Boolean? = false
)