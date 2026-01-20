package com.rootsrecipes.model

data class NotificationUpdateResponse(
    val `data`: NotificationUpdateData,
    val message: String,
    val statusCode: Int
)

data class NotificationUpdateData(
    val __v: Int,
    val _id: String,
    val add_new_recipe: Boolean,
    val createdAt: String,
    val new_comment: Boolean,
    val rating_notification: Boolean,
    val updatedAt: String,
    val user_id: String
)

data class NotificationListResponse(
    val `data`: NotificationListData,
    val statusCode: Int
)

data class NotificationListData(
    val notifications: ArrayList<Notification>,
    val pagination: Pagination
)

data class Notification(
    val __v: Int,
    val _id: String,
    val createdAt: String,
    val user: FromUser,
    val is_deleted: Boolean,
    val notification_text: String,
    val notification_title: String,
    val notification_type: String,
    val comment: String? = "",
    val rate: Float? = null,
    val read_status: Boolean,
    val recipeData: RecipeNotification? = null,
    val updatedAt: String
)

data class Pagination(
    val limit: Int,
    val page: Int,
    val total: Int,
    val totalPages: Int
)

data class FromUser(
    val _id: String,
    val first_name: String,
    val last_name: String,
    val profile_image: String? = "",
    val user_name: String
)

data class RecipeNotification(
    val _id: String,
    val recipe_image: String? = "",
    val title: String,
    val user_id: String
)