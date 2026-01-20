package com.rootsrecipes.model

import android.os.Parcelable
import com.rootsrecipes.view.createAccount.model.RecipeData
import kotlinx.parcelize.Parcelize

data class DiscoverResponse(
    val `data`: ArrayList<DiscoverData>,
    val message: String,
    val statusCode: Int
)

data class DiscoverData(
    val __v: Int,
    val _id: String,
    val avg_rating: Float,
    val category: String,
    val cooking_time: String,
    val createdAt: String,
    val cuisine: String,
    val images: ArrayList<Any>,
    val ingredients: ArrayList<String>,
    val isSaved: Boolean,
    val my_recipe_status: String,
    val recipe_image: String,
    val serving_count: String,
    val short_description: String,
    val status: Boolean,
    val steps: ArrayList<String>,
    val title: String,
    val transcribed_text: String,
    val updatedAt: String,
    val user: User,
    val user_id: String
)

@Parcelize
data class User(
    val __v: Int?=0,
    val _id: String?="",
    var about_me: String?="",
    var country_code: String?="",
    val createdAt: String?="",
    val email: String?="",
    val email_otp: Int?=0,
    val followers: ArrayList<String>?=null,
    var followers_count: Int?=0,
    val following: ArrayList<String>?=null,
    val following_count: Int?=0,
//    var full_name: String?="",
    val is_email_verified: Boolean?=false,
    var is_phone_number_verified: Boolean?=false,
    val mobile_otp: String?="",
//    val otp_expiry: Int?=0,
    val otp_expiry: String?="",
    val password: String?="",
    var phone_number: String?="",
    val profile_image: String?="",
    val status: Boolean?=false,
    val terms_of_use_accepted: Int?=0,
    val updatedAt: String?="",
    var user_name: String?="",
    var first_name: String?="",
    var last_name: String?="",
    var followStatus: Boolean? = false,
    var notificationPreferenceData: NotificationPreferenceData? = null,

    ) : Parcelable

data class AllHomeRecipeResponse(
    val `data`: ArrayList<AllHomeRecipeData>? = null,
    val message: String,
    val statusCode: Int
)

data class AllHomeRecipeData(
    val category: String,
    val recipes: ArrayList<RecipeData>? = null
)

