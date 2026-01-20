package com.rootsrecipes.di.data.repository

import com.google.gson.JsonObject
import com.rootsrecipes.di.data.ApiHelper

class MainRepository(private val apiHelper: ApiHelper) {
    suspend fun createUserAccount(jsonObject: JsonObject) = apiHelper.createUserAccount(jsonObject)
    suspend fun otpVerify(jsonObject: JsonObject) = apiHelper.otpVerify(jsonObject)
    suspend fun resendOtp(jsonObject: JsonObject) = apiHelper.resendOtp(jsonObject)
    suspend fun signInUser(jsonObject: JsonObject) = apiHelper.signInUser(jsonObject)
    suspend fun sendOtpOnEmail(jsonObject: JsonObject) = apiHelper.sendOtpOnEmail(jsonObject)
    suspend fun verifyOtpPasswordChange(jsonObject: JsonObject) =
        apiHelper.verifyOtpPasswordChange(jsonObject)

    suspend fun resetPasswordUser(jsonObject: JsonObject) = apiHelper.resetPasswordUser(jsonObject)

    suspend fun checkUsername(username: String) = apiHelper.checkUsername(username)
    suspend fun editProfile(jsonObject: JsonObject) = apiHelper.editProfile(jsonObject)
    suspend fun changePassword(jsonObject: JsonObject) = apiHelper.changePassword(jsonObject)
    suspend fun deleteAccount() = apiHelper.deleteAccount()
    suspend fun generateMobileOtp() = apiHelper.generateMobileOtp()
    suspend fun mobileOtpVerify(jsonObject: JsonObject) = apiHelper.mobileOtpVerify(jsonObject)
    suspend fun updateNotificationPreferences(jsonObject: JsonObject) =
        apiHelper.updateNotificationPreferences(jsonObject)

    suspend fun updateProfileImage(jsonObject: JsonObject) =
        apiHelper.updateProfileImage(jsonObject)

    suspend fun awsCredential() = apiHelper.awsCredential()
    suspend fun extractDetails(jsonObject: JsonObject) = apiHelper.extractDetails(jsonObject)
    suspend fun myRecipes(
        searchKeyword: String, category: String, cuisine: String, page: Int, limit: Int
    ) = apiHelper.myRecipes(searchKeyword, category, cuisine, page, limit)

    suspend fun getUserDetails(userID: String) = apiHelper.getUserProfile(userID)
    suspend fun saveRecipesAfterExtract(jsonObject: JsonObject) =
        apiHelper.saveRecipesAfterExtract(jsonObject)

    suspend fun getCuisines() = apiHelper.getCuisines()
    suspend fun addCuisines(jsonObject: JsonObject) = apiHelper.addCuisines(jsonObject)
    suspend fun deleteRecipe(id: String) = apiHelper.deleteRecipe(id)
    suspend fun updateRecipeStatus(jsonObject: JsonObject) =
        apiHelper.updateRecipeStatus(jsonObject)

    suspend fun getUserConnection(type: String, searchKeyword: String) =
        apiHelper.getUserConnection(type, searchKeyword)

    suspend fun getAllHomeRecipes() = apiHelper.getAllHomeRecipes()
    suspend fun updateRecipe(jsonObject: JsonObject) = apiHelper.updateRecipe(jsonObject)
    suspend fun userSavedRecipes(
        page: Int, limit: Int, search: String, category: String, cuisine: String
    ) = apiHelper.userSavedRecipes(page, limit, search, category, cuisine)

    suspend fun homePageRecipes(
        users: String, search: String, category: String, cuisine: String, page: Int, limit: Int
    ) = apiHelper.homePageRecipes(users, search, category, cuisine, page, limit)

    suspend fun myCountRecipes(
        category: ArrayList<String>,
        cuisine: ArrayList<String>,
    ) = apiHelper.myCountRecipes(category, cuisine)

    suspend fun countPublicRecipes(
        users: String,
        category: String,
        cuisine: String,
    ) = apiHelper.countPublicRecipes(users, category, cuisine)

    suspend fun followUser(
        jsonObject: JsonObject
    ) = apiHelper.followUser(jsonObject)

    suspend fun unfollowUser(
        jsonObject: JsonObject
    ) = apiHelper.unfollowUser(jsonObject)

    suspend fun myConnections(
        type: String, searchKeyword: String, page: Int, limit: Int
    ) = apiHelper.myConnections(type, searchKeyword, page, limit)

    suspend fun userConnections(
        type: String, userId: String, searchKeyword: String, page: Int, limit: Int
    ) = apiHelper.userConnections(type, userId, searchKeyword, page, limit)

    suspend fun addComment(
        jsonObject: JsonObject
    ) = apiHelper.addComment(jsonObject)

    suspend fun commentList(
        recipeId: String, page: Int, limit: Int
    ) = apiHelper.commentList(recipeId, page, limit)

    suspend fun deleteComment(
        commentId: String
    ) = apiHelper.deleteComment(commentId)

    suspend fun getRecipe(
        recipeId: String
    ) = apiHelper.getRecipe(recipeId)

    suspend fun saveRecipeToWishlist(
        jsonObject: JsonObject
    ) = apiHelper.saveRecipeToWishlist(jsonObject)

    suspend fun unSaveRecipeToWishlist(
        recipeId: String
    ) = apiHelper.unSaveRecipeToWishlist(recipeId)

    suspend fun rateRecipe(
        jsonObject: JsonObject
    ) = apiHelper.rateRecipe(jsonObject)

    suspend fun userRecipes(
        userId: String, page: Int, limit: Int
    ) = apiHelper.userRecipes(userId, page, limit)

    suspend fun removeFollower(
        jsonObject: JsonObject
    ) = apiHelper.removeFollower(jsonObject)

    suspend fun countSaveRecipes(
        category: String,
        cuisine: String,
    ) = apiHelper.countSaveRecipes(category, cuisine)

    suspend fun saveImageRecipes(jsonObject: JsonObject) =
        apiHelper.updateRecipeImageOnly(jsonObject)

    suspend fun sendNotification(jsonObject: JsonObject) = apiHelper.sendNotification(jsonObject)

    suspend fun getNotifications(page: Int, limit: Int) = apiHelper.getNotifications(page, limit)
    suspend fun getUserNotify(recipe_id: String, user_ids: String) =
        apiHelper.getUserNotify(recipe_id, user_ids)

    suspend fun getAllTypeConnection(page: String, limit: String) =
        apiHelper.getAllTypeConnection(page, limit)

    suspend fun getSearchUser(search: String) = apiHelper.getSearchUser(search)

}