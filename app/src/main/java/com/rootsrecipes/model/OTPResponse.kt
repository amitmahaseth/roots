package com.rootsrecipes.model

data class OTPResponse(
    val `data`: User,
    val message: String,
    val statusCode: Int,
    val token: String
)

data class Data(
    val __v: Int,
    val _id: String,
    val country_code: String,
    val createdAt: String,
    val email: String,
    val full_name: String,
    val is_phone_number_verified: Boolean,
    val mobile_otp: Any,
    val otp_expiry: Any,
    val password: String,
    val phone_number: String,
    val status: Boolean,
    val terms_of_use_accepted: Int,
    val updatedAt: String,
    val user_name: String
)