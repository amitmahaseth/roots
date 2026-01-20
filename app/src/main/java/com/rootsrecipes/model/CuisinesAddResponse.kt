package com.rootsrecipes.model

data class CuisinesAddResponse(
    val `data`: CuisinesAddData,
    val message: String,
    val statusCode: Int
)

data class CuisinesAddData(
    val __v: Int,
    val _id: String,
    val createdAt: String,
    val cuisine: String,
    val updatedAt: String,
    val user_id: String
)