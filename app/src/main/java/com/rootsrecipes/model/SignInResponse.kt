package com.rootsrecipes.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class SignInResponse(
    val `data`: User, val message: String, val statusCode: Int, val token: String
)

data class SignInData(
    val __v: Int,
    val _id: String,
    val country_code: String,
    val createdAt: String,
    val email: String,
    val email_otp: Any,
    val full_name: String,
    val is_phone_number_verified: Boolean,
    val otp_expiry: Any,
    val password: String,
    val phone_number: String,
    val status: Boolean,
    val terms_of_use_accepted: Int,
    val updatedAt: String,
    val user_name: String,
    val about_me: String,
    val profile_image: String,
    var notificationPreferenceData: NotificationPreferenceData? = null,
    val followers: ArrayList<String>? = null,
    val following: ArrayList<String>? = null,
    val followers_count: Int?=0,
    val following_count: Int?=0,
)

@Parcelize
data class NotificationPreferenceData(
    val __v: Int,
    val _id: String,
    val add_new_recipe: Boolean,
    val createdAt: String,
    val new_comment: Boolean,
    val rating_notification: Boolean,
    val updatedAt: String,
    val user_id: String
):Parcelable