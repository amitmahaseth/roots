package com.rootsrecipes.view.createAccount.model

import android.os.Parcelable
import android.view.inputmethod.InlineSuggestion
import com.rootsrecipes.model.User
import com.rootsrecipes.view.myRecipes.model.CommentData
import kotlinx.parcelize.Parcelize

data class RecipesModel(val image: Int, val headerRecipes: String, val detailsRecipes: String)

@Parcelize
data class RecipeData(
    val __v: Int? = 0,
    val _id: String? = "",
    var avg_rating: Float? = 0F,
    val category: String? = "",
    val sub_category: String? = "",
//    val cooking_time: String? = "",
    val createdAt: String? = "",
    val cuisine: String? = "",
    val recipe_image: String? = "",
    val ingredients: ArrayList<String>? = null,
    val is_Shared: Boolean? = false,
//    val serving_count: String? = "",
    val short_description: String? = "",
    val status: Boolean? = false,
    val steps: ArrayList<String>? = null,
    val title: String? = "",
    val updatedAt: String? = "",
    val user_id: String? = "",
    val transcribed_text: String? = "",
    var my_recipe_status: String? = "",
    var user: User? = null,
    var isSaved: Boolean? = false,
    var latestComment: CommentData? = null,
    var hasRated : Boolean ? = false,
    var notes : String ? = "",
    var suggestions : String ? = "",
    var min_cooking_time : String ? = "",
    var max_cooking_time : String ? = "",
    var min_serving_count : String ? = "",
    var max_serving_count : String ? = "",

) : Parcelable

@Parcelize
class MediaImage : Parcelable
