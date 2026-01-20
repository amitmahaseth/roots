package com.rootsrecipes.di.data

import com.google.gson.JsonObject
import com.rootsrecipes.model.AWSResponse
import com.rootsrecipes.model.AllHomeRecipeResponse
import com.rootsrecipes.model.CommonResponse
import com.rootsrecipes.model.CuisinResponse
import com.rootsrecipes.model.CuisinesAddResponse
import com.rootsrecipes.model.FollowerFollowingResponse
import com.rootsrecipes.model.NotificationListResponse
import com.rootsrecipes.model.NotificationUpdateResponse
import com.rootsrecipes.model.OTPResponse
import com.rootsrecipes.model.SignInResponse
import com.rootsrecipes.view.messages.model.SearchUserResponse
import com.rootsrecipes.view.myRecipes.model.AddCommentResponse
import com.rootsrecipes.view.myRecipes.model.AllTypeConnectionResponse
import com.rootsrecipes.view.myRecipes.model.CommentListResponse
import com.rootsrecipes.view.myRecipes.model.ConnectionResponse
import com.rootsrecipes.view.myRecipes.model.GetUserNotifyResponse
import com.rootsrecipes.view.myRecipes.model.MyRecipesResponse
import com.rootsrecipes.view.myRecipes.model.RateRecipeResponse
import com.rootsrecipes.view.myRecipes.model.UserRecipesResponse
import com.rootsrecipes.view.notification.viewModel.NotificationsListVM
import com.rootsrecipes.view.recipeRecording.model.CountRecipeResponse
import com.rootsrecipes.view.recipeRecording.model.ExtractRecipeResponse
import retrofit2.Response

interface ApiHelper {
    suspend fun createUserAccount(jsonObject: JsonObject): Response<CommonResponse>
    suspend fun otpVerify(jsonObject: JsonObject): Response<OTPResponse>
    suspend fun resendOtp(jsonObject: JsonObject): Response<CommonResponse>
    suspend fun signInUser(jsonObject: JsonObject): Response<SignInResponse>
    suspend fun sendOtpOnEmail(jsonObject: JsonObject): Response<CommonResponse>
    suspend fun verifyOtpPasswordChange(jsonObject: JsonObject): Response<CommonResponse>
    suspend fun resetPasswordUser(jsonObject: JsonObject): Response<CommonResponse>
    suspend fun checkUsername(username: String): Response<CommonResponse>

    suspend fun editProfile(jsonObject: JsonObject): Response<SignInResponse>
    suspend fun changePassword(jsonObject: JsonObject): Response<CommonResponse>
    suspend fun deleteAccount(): Response<CommonResponse>
    suspend fun updateNotificationPreferences(jsonObject: JsonObject): Response<NotificationUpdateResponse>
    suspend fun generateMobileOtp(): Response<CommonResponse>
    suspend fun mobileOtpVerify(jsonObject: JsonObject): Response<OTPResponse>
    suspend fun updateProfileImage(jsonObject: JsonObject): Response<SignInResponse>
    suspend fun awsCredential(): Response<AWSResponse>
    suspend fun extractDetails(jsonObject: JsonObject): Response<ExtractRecipeResponse>
    suspend fun myRecipes(
        searchKeyword: String, category: String, cuisine: String, page: Int, limit: Int
    ): Response<MyRecipesResponse>

    suspend fun getUserProfile(userID: String): Response<SignInResponse>
    suspend fun saveRecipesAfterExtract(jsonObject: JsonObject): Response<ExtractRecipeResponse>
    suspend fun getCuisines(): Response<CuisinResponse>
    suspend fun addCuisines(jsonObject: JsonObject): Response<CuisinesAddResponse>
    suspend fun deleteRecipe(id: String): Response<CommonResponse>
    suspend fun updateRecipeStatus(jsonObject: JsonObject): Response<CommonResponse>
    suspend fun getUserConnection(
        type: String, searchKeyword: String
    ): Response<FollowerFollowingResponse>

    suspend fun getAllHomeRecipes(): Response<AllHomeRecipeResponse>
    suspend fun updateRecipe(jsonObject: JsonObject): Response<ExtractRecipeResponse>
    suspend fun homePageRecipes(
        users: String, search: String, category: String, cuisine: String, page: Int, limit: Int
    ): Response<MyRecipesResponse>

    suspend fun userSavedRecipes(
        page: Int, limit: Int, search: String, category: String, cuisine: String
    ): Response<MyRecipesResponse>

    suspend fun myCountRecipes(
        category: ArrayList<String>,
        cuisine: ArrayList<String>,
    ): Response<CountRecipeResponse>

    suspend fun countPublicRecipes(
        users: String,
        category: String,
        cuisine: String,
    ): Response<CountRecipeResponse>

    suspend fun followUser(
        jsonObject: JsonObject
    ): Response<CommonResponse>

    suspend fun unfollowUser(
        jsonObject: JsonObject
    ): Response<CommonResponse>

    suspend fun myConnections(
        type: String, searchKeyword: String, page: Int, limit: Int
    ): Response<ConnectionResponse>

    suspend fun userConnections(
        type: String, userId: String, searchKeyword: String, page: Int, limit: Int
    ): Response<ConnectionResponse>

    suspend fun addComment(
        jsonObject: JsonObject
    ): Response<AddCommentResponse>

    suspend fun deleteComment(
        commentId: String
    ): Response<CommonResponse>

    suspend fun commentList(
        recipeId: String, page: Int, limit: Int
    ): Response<CommentListResponse>

    suspend fun getRecipe(
        recipeId: String
    ): Response<ExtractRecipeResponse>

    suspend fun saveRecipeToWishlist(
        jsonObject: JsonObject
    ): Response<CommonResponse>

    suspend fun unSaveRecipeToWishlist(
        recipeId: String
    ): Response<CommonResponse>

    suspend fun rateRecipe(
        jsonObject: JsonObject
    ): Response<RateRecipeResponse>

    suspend fun userRecipes(
        userId: String, page: Int, limit: Int
    ): Response<UserRecipesResponse>

    suspend fun removeFollower(
        jsonObject: JsonObject
    ): Response<CommonResponse>

    suspend fun countSaveRecipes(
        category: String,
        cuisine: String,
    ): Response<CountRecipeResponse>

    suspend fun updateRecipeImageOnly(jsonObject: JsonObject): Response<ExtractRecipeResponse>

    suspend fun sendNotification(jsonObject: JsonObject): Response<CommonResponse>

    suspend fun getNotifications(page: Int , limit: Int):Response<NotificationListResponse>
    suspend fun getAllTypeConnection(page: String , limit: String):Response<AllTypeConnectionResponse>
    suspend fun getUserNotify(recipe_id: String , user_ids: String):Response<GetUserNotifyResponse>
    suspend fun getSearchUser(search: String):Response<SearchUserResponse>
}