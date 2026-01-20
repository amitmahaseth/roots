package com.rootsrecipes.view.myRecipes.model

import android.os.Parcelable
import com.rootsrecipes.model.User
import com.rootsrecipes.view.createAccount.model.RecipeData
import kotlinx.android.parcel.Parcelize


data class MyRecipesResponse(
    val data: ArrayList<RecipeData>,
    val message: String,
    val statusCode: Int
)

data class ConnectionResponse(
    val `data`: ArrayList<ConnectionUserData>,
    val message: String,
    val statusCode: Int
)

data class ConnectionUserData(
    val email: String,
    val firstName: String,
    val lastName: String,
    var isFollow: Boolean,
    val toFollow: Boolean,
    val userName: String,
    val user_id: String,
    val profileImage : String ?=""
)
data class CommentListResponse(
    val `data`: ArrayList<CommentData>,
    val message: String,
    val statusCode: Int
)

data class AddCommentResponse(
    val `data`: CommentData,
    val message: String,
    val statusCode: Int
)

@Parcelize
data class CommentData(
    val __v: Int?=0,
    val _id: String?="",
    val comment_text: String?="",
    val createdAt: String?="",
    val recipe_id: String?="",
    val updatedAt: String?="",
    var user: User ? = null,
    val user_id: String?=""
):Parcelable

data class RateRecipeResponse(
    val `data`: RecipeData,
    val message: String,
    val statusCode: Int
)

data class Data(
    val __v: Int,
    val _id: String,
    val avg_rating: Int,
    val category: String,
    val cooking_time: String,
    val createdAt: String,
    val cuisine: String,
    val ingredients: List<String>,
    val my_recipe_status: String,
    val recipe_image: String,
    val serving_count: String,
    val short_description: String,
    val status: Boolean,
    val steps: List<String>,
    val title: String,
    val transcribed_text: String,
    val updatedAt: String,
    val user_id: String
)

data class UserRecipesResponse(
    val `data`: UserRecipesData,
    val message: String,
    val statusCode: Int
)

data class UserRecipesData(
    val currentPage: Int,
    val followStatus: Boolean,
    val recipes: ArrayList<RecipeData>,
    val totalPages: Int,
    val totalRecipes: Int,
    val user: User
)




