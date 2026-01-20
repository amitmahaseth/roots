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
import com.rootsrecipes.view.recipeRecording.model.CountRecipeResponse
import com.rootsrecipes.view.recipeRecording.model.ExtractRecipeResponse
import retrofit2.Response

class ApiHelperImpl(private val apiServices: ApiServices) : ApiHelper {
    override suspend fun createUserAccount(jsonObject: JsonObject): Response<CommonResponse> =
        apiServices.createAccount(jsonObject)

    override suspend fun otpVerify(jsonObject: JsonObject): Response<OTPResponse> =
        apiServices.verifyOtp(jsonObject)

    override suspend fun resendOtp(jsonObject: JsonObject): Response<CommonResponse> =
        apiServices.resendOtp(jsonObject)

    override suspend fun signInUser(jsonObject: JsonObject): Response<SignInResponse> =
        apiServices.signInUser(jsonObject)

    override suspend fun sendOtpOnEmail(jsonObject: JsonObject): Response<CommonResponse> =
        apiServices.sendOtpOnEmail(jsonObject)

    override suspend fun verifyOtpPasswordChange(
        jsonObject: JsonObject
    ): Response<CommonResponse> = apiServices.verifyOtpPasswordChange(jsonObject)

    override suspend fun resetPasswordUser(jsonObject: JsonObject): Response<CommonResponse> =
        apiServices.resetPasswordUser(jsonObject)

    override suspend fun checkUsername(username: String): Response<CommonResponse> =
        apiServices.checkUsername(username)

    override suspend fun editProfile(jsonObject: JsonObject): Response<SignInResponse> =
        apiServices.editProfile(jsonObject)


    override suspend fun changePassword(jsonObject: JsonObject): Response<CommonResponse> =
        apiServices.changePassword(jsonObject)

    override suspend fun deleteAccount(): Response<CommonResponse> = apiServices.deleteAccount()
    override suspend fun updateNotificationPreferences(jsonObject: JsonObject): Response<NotificationUpdateResponse> =
        apiServices.updateNotificationPreferences(jsonObject)

    override suspend fun generateMobileOtp(): Response<CommonResponse> =
        apiServices.generateMobileOtp()

    override suspend fun mobileOtpVerify(jsonObject: JsonObject): Response<OTPResponse> =
        apiServices.mobileOtpVerify(jsonObject)

    override suspend fun updateProfileImage(jsonObject: JsonObject): Response<SignInResponse> =
        apiServices.updateProfileImage(jsonObject)

    override suspend fun awsCredential(): Response<AWSResponse> = apiServices.awsCredential()

    override suspend fun extractDetails(jsonObject: JsonObject): Response<ExtractRecipeResponse> =
        apiServices.extractDetails(jsonObject)

    override suspend fun myRecipes(
        searchKeyword: String, category: String, cuisine: String, page: Int, limit: Int
    ): Response<MyRecipesResponse> =
        apiServices.myRecipes(searchKeyword, category, cuisine, page, limit)

    override suspend fun getUserProfile(userID: String): Response<SignInResponse> =
        apiServices.getUserProfile(userID)

    override suspend fun saveRecipesAfterExtract(jsonObject: JsonObject): Response<ExtractRecipeResponse> =
        apiServices.saveRecipesAfterExtract(jsonObject)

    override suspend fun getCuisines(): Response<CuisinResponse> = apiServices.getCuisines()

    override suspend fun addCuisines(jsonObject: JsonObject): Response<CuisinesAddResponse> =
        apiServices.addCuisines(jsonObject)

    override suspend fun deleteRecipe(id: String): Response<CommonResponse> =
        apiServices.deleteRecipe(id)

    override suspend fun updateRecipeStatus(jsonObject: JsonObject): Response<CommonResponse> =
        apiServices.updateRecipeStatus(jsonObject)

    override suspend fun getUserConnection(
        type: String, searchKeyword: String
    ): Response<FollowerFollowingResponse> = apiServices.userConnection(type, searchKeyword)

    override suspend fun getAllHomeRecipes(): Response<AllHomeRecipeResponse> =
        apiServices.getAllHomeRecipes()

    override suspend fun homePageRecipes(
        users: String, search: String, category: String, cuisine: String, page: Int, limit: Int
    ): Response<MyRecipesResponse> =
        apiServices.homePageRecipes(users, search, category, cuisine, page, limit)

    override suspend fun userSavedRecipes(
        page: Int, limit: Int, search: String, category: String, cuisine: String
    ): Response<MyRecipesResponse> =
        apiServices.userSavedRecipes(page, limit, search, category, cuisine)

    override suspend fun myCountRecipes(
        category: ArrayList<String>, cuisine: ArrayList<String>
    ): Response<CountRecipeResponse> = apiServices.myCountRecipes(category, cuisine)

    override suspend fun countPublicRecipes(
        users: String, category: String, cuisine: String
    ): Response<CountRecipeResponse> = apiServices.countPublicRecipes(users, category, cuisine)

    override suspend fun followUser(jsonObject: JsonObject): Response<CommonResponse> =
        apiServices.followUser(jsonObject)

    override suspend fun unfollowUser(jsonObject: JsonObject): Response<CommonResponse> =
        apiServices.unfollowUser(jsonObject)


    override suspend fun myConnections(
        type: String, searchKeyword: String, page: Int, limit: Int
    ): Response<ConnectionResponse> = apiServices.myConnections(type, searchKeyword, page, limit)

    override suspend fun userConnections(
        type: String, userId: String, searchKeyword: String, page: Int, limit: Int
    ): Response<ConnectionResponse> =
        apiServices.userConnections(type, userId, searchKeyword, page, limit)

    override suspend fun addComment(jsonObject: JsonObject): Response<AddCommentResponse> =
        apiServices.addComment(jsonObject)

    override suspend fun deleteComment(commentId: String): Response<CommonResponse> =
        apiServices.deleteComment(commentId)

    override suspend fun commentList(
        recipeId: String, page: Int, limit: Int
    ): Response<CommentListResponse> = apiServices.getRecipeComments(recipeId, page, limit)

    override suspend fun getRecipe(recipeId: String): Response<ExtractRecipeResponse> =
        apiServices.getRecipe(recipeId)

    override suspend fun saveRecipeToWishlist(jsonObject: JsonObject): Response<CommonResponse> =
        apiServices.saveRecipeToWishlist(jsonObject)

    override suspend fun unSaveRecipeToWishlist(recipeId: String): Response<CommonResponse> =
        apiServices.unSaveRecipeToWishlist(recipeId)

    override suspend fun rateRecipe(jsonObject: JsonObject): Response<RateRecipeResponse> =
        apiServices.rateRecipe(jsonObject)

    override suspend fun userRecipes(
        userId: String, page: Int, limit: Int
    ): Response<UserRecipesResponse> = apiServices.userRecipes(userId, page, limit)

    override suspend fun removeFollower(jsonObject: JsonObject): Response<CommonResponse> =
        apiServices.removeFollower(jsonObject)

    override suspend fun countSaveRecipes(
        category: String, cuisine: String
    ): Response<CountRecipeResponse> = apiServices.countSaveRecipes(category, cuisine)

    override suspend fun updateRecipeImageOnly(jsonObject: JsonObject): Response<ExtractRecipeResponse> =
        apiServices.updateRecipeImageOnly(jsonObject)

    override suspend fun updateRecipe(jsonObject: JsonObject): Response<ExtractRecipeResponse> =
        apiServices.updateRecipe(jsonObject)

    override suspend fun sendNotification(jsonObject: JsonObject): Response<CommonResponse> =
        apiServices.sendNotification(jsonObject)

    override suspend fun getNotifications(
        page: Int, limit: Int
    ): Response<NotificationListResponse> = apiServices.getNotifications(page, limit)

    override suspend fun getAllTypeConnection(
        page: String, limit: String
    ): Response<AllTypeConnectionResponse> = apiServices.getAllTypeConnection(page, limit)

    override suspend fun getUserNotify(
        recipe_id: String, user_ids: String
    ): Response<GetUserNotifyResponse> = apiServices.getUserNotify(recipe_id, user_ids)

    override suspend fun getSearchUser(search: String): Response<SearchUserResponse> =
        apiServices.getSearchUser(search)
}
