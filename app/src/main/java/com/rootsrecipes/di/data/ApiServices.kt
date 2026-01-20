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
import com.rootsrecipes.utils.Constants
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
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface ApiServices {
    @Headers("accept: application/json", "Content-Type: application/json")
    @POST(Constants.SIGNUPENDPOINT)
    suspend fun createAccount(@Body jsonObject: JsonObject): Response<CommonResponse>

    @PUT(Constants.VERIFYOTP)
    suspend fun verifyOtp(
        @Body jsonObject: JsonObject
    ): Response<OTPResponse>

    @PUT(Constants.RESENDOTP)
    suspend fun resendOtp(@Body jsonObject: JsonObject): Response<CommonResponse>

    @POST(Constants.SIGNINENDPOINT)
    suspend fun signInUser(@Body jsonObject: JsonObject): Response<SignInResponse>

    @PUT(Constants.SENDEMAILOTP)
    suspend fun sendOtpOnEmail(@Body jsonObject: JsonObject): Response<CommonResponse>

    @PUT(Constants.UPDATEPASSWORDVERIFYOTP)
    suspend fun verifyOtpPasswordChange(
        @Body jsonObject: JsonObject
    ): Response<CommonResponse>

    @PUT(Constants.RESETPASSWORDUSER)
    suspend fun resetPasswordUser(@Body jsonObject: JsonObject): Response<CommonResponse>

    @GET(Constants.CHECK_USERNAME)
    suspend fun checkUsername(
        @Query("user_name") user_name: String
    ): Response<CommonResponse>

    @PUT(Constants.EDIT_PROFILE)
    suspend fun editProfile(@Body jsonObject: JsonObject): Response<SignInResponse>

    @PUT(Constants.CHANGE_PASSWORD)
    suspend fun changePassword(@Body jsonObject: JsonObject): Response<CommonResponse>

    @DELETE(Constants.DELETE_ACCOUNT)
    suspend fun deleteAccount(): Response<CommonResponse>

    @PUT(Constants.UPDATE_NOTIFICATION_PREFERENCES)
    suspend fun updateNotificationPreferences(@Body jsonObject: JsonObject): Response<NotificationUpdateResponse>

    @POST(Constants.GENERATE_MOBILE_OTP)
    suspend fun generateMobileOtp(): Response<CommonResponse>

    @PUT(Constants.MOBILE_OTP_VERIFY)
    suspend fun mobileOtpVerify(@Body jsonObject: JsonObject): Response<OTPResponse>

    @PUT(Constants.UPDATE_PROFILE_IMAGE)
    suspend fun updateProfileImage(@Body jsonObject: JsonObject): Response<SignInResponse>

    @GET(Constants.AWS_CREDENTIAL)
    suspend fun awsCredential(): Response<AWSResponse>

    @POST(Constants.EXTRACT_DETAILS)
    suspend fun extractDetails(@Body jsonObject: JsonObject): Response<ExtractRecipeResponse>

    @GET(Constants.MY_RECIPES)
    suspend fun myRecipes(
        @Query("searchKeyword") searchKeyword: String,
        @Query("category") category: String,
        @Query("cuisine") cuisine: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): Response<MyRecipesResponse>

    @GET(Constants.GET_USER_DATA)
    suspend fun getUserProfile(@Query("id") userID: String): Response<SignInResponse>

    @POST(Constants.SAVE_RECIPE)
    suspend fun saveRecipesAfterExtract(@Body jsonObject: JsonObject): Response<ExtractRecipeResponse>

    @GET(Constants.GET_CUISINES)
    suspend fun getCuisines(): Response<CuisinResponse>

    @POST(Constants.ADD_CUISINES)
    suspend fun addCuisines(@Body jsonObject: JsonObject): Response<CuisinesAddResponse>

    @DELETE(Constants.DELETE_RECIPE)
    suspend fun deleteRecipe(@Query("recipe_id") id: String): Response<CommonResponse>

    @PUT(Constants.UPDATE_RECIPE_STATUS)
    suspend fun updateRecipeStatus(@Body jsonObject: JsonObject): Response<CommonResponse>

    @GET(Constants.GET_USER_CONNECTION)
    suspend fun userConnection(
        @Query("type") type: String, @Query("searchKeyword") searchKeyword: String
    ): Response<FollowerFollowingResponse>

    @GET(Constants.GET_ALL_HOME_RECIPES)
    suspend fun getAllHomeRecipes(): Response<AllHomeRecipeResponse>

    @PUT(Constants.UPDATE_RECIPE)
    suspend fun updateRecipe(@Body jsonObject: JsonObject): Response<ExtractRecipeResponse>

    @GET(Constants.HOME_PAGE_RECIPES)
    suspend fun homePageRecipes(
        @Query("users") users: String,
        @Query("search") search: String,
        @Query("category") category: String,
        @Query("cuisine") cuisine: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): Response<MyRecipesResponse>

    @GET(Constants.USER_SAVED_RECIPES)
    suspend fun userSavedRecipes(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("search") search: String,
        @Query("category") category: String,
        @Query("cuisine") cuisine: String,

        ): Response<MyRecipesResponse>

    @GET(Constants.MY_COUNT_RECIPES)
    suspend fun myCountRecipes(
        @Query("category") category: ArrayList<String>,
        @Query("cuisine") cuisine: ArrayList<String>,
    ): Response<CountRecipeResponse>

    @GET(Constants.COUNT_PUBLIC_RECIPES)
    suspend fun countPublicRecipes(
        @Query("users") users: String,
        @Query("category") category: String,
        @Query("cuisine") cuisine: String
    ): Response<CountRecipeResponse>

    @POST(Constants.FOLLOW_USER)
    suspend fun followUser(
        @Body jsonObject: JsonObject
    ): Response<CommonResponse>

    @PUT(Constants.UNFOLLOW_USER)
    suspend fun unfollowUser(
        @Body jsonObject: JsonObject
    ): Response<CommonResponse>

    @GET(Constants.MY_CONNECTIONS)
    suspend fun myConnections(
        @Query("type") type: String,
        @Query("searchKeyword") searchKeyword: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): Response<ConnectionResponse>

    @GET(Constants.USERS_CONNECTIONS)
    suspend fun userConnections(
        @Query("type") type: String,
        @Query("user_id") user_id: String,
        @Query("searchKeyword") searchKeyword: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): Response<ConnectionResponse>


    @POST(Constants.ADD_COMMENT)
    suspend fun addComment(
        @Body jsonObject: JsonObject
    ): Response<AddCommentResponse>

    @GET(Constants.GET_RECIPE_COMMENTS)
    suspend fun getRecipeComments(
        @Query("recipeId") recipeId: String, @Query("page") page: Int, @Query("limit") limit: Int
    ): Response<CommentListResponse>

    @DELETE(Constants.DELETE_COMMENT)
    suspend fun deleteComment(
        @Query("commentId") commentId: String
    ): Response<CommonResponse>

    @GET(Constants.GET_RECIPE)
    suspend fun getRecipe(
        @Query("recipe_id") recipeId: String
    ): Response<ExtractRecipeResponse>

    @POST(Constants.SAVE_RECIPE_TO_WISHLIST)
    suspend fun saveRecipeToWishlist(
        @Body jsonObject: JsonObject
    ): Response<CommonResponse>

    @DELETE(Constants.UNSAVE_RECIPE_TO_WISHLIST)
    suspend fun unSaveRecipeToWishlist(
        @Query("recipeId") recipeId: String
    ): Response<CommonResponse>

    @POST(Constants.RATE_RECIPE)
    suspend fun rateRecipe(
        @Body jsonObject: JsonObject
    ): Response<RateRecipeResponse>

    @GET(Constants.USER_RECIPES)
    suspend fun userRecipes(
        @Query("target_user_id") target_user_id: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): Response<UserRecipesResponse>

    @PUT(Constants.REMOVE_FOLLOWER)
    suspend fun removeFollower(
        @Body jsonObject: JsonObject
    ): Response<CommonResponse>

    @GET(Constants.COUNT_SAVED_RECIPES)
    suspend fun countSaveRecipes(
        @Query("category") category: String, @Query("cuisine") cuisine: String
    ): Response<CountRecipeResponse>

    @PUT(Constants.ONLYRECIPEIMAGE)
    suspend fun updateRecipeImageOnly(@Body jsonObject: JsonObject): Response<ExtractRecipeResponse>

    @POST(Constants.SEND_NOTIFICATION)
    suspend fun sendNotification(@Body jsonObject: JsonObject): Response<CommonResponse>

    @GET(Constants.GET_NOTIFICATIONS)
    suspend fun getNotifications(
        @Query("page") page: Int, @Query("limit") limit: Int
    ): Response<NotificationListResponse>

    @GET(Constants.GET_ALL_TYPE_CONNECTION)
    suspend fun getAllTypeConnection(
        @Query("page") page: String, @Query("limit") limit: String
    ): Response<AllTypeConnectionResponse>

 @GET(Constants.GET_USERS_NOTIFY)
    suspend fun getUserNotify(
        @Query("recipe_id") recipe_id: String, @Query("user_ids") user_ids: String
    ): Response<GetUserNotifyResponse>

    @GET(Constants.GET_SEARCH_USERS)
    suspend fun getSearchUser(
        @Query("search") search: String
    ): Response<SearchUserResponse>
}