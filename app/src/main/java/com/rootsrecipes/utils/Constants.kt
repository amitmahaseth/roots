package com.rootsrecipes.utils

import com.rootsrecipes.view.createAccount.model.RecipeData

class Constants {
    companion object {
        var recipeShare = ""
        var userIdShare = ""
        var recipeUserIdShare = ""
        val typeFrom = "typeFrom"
        val userTypeFrom = "userTypeFrom"
        val userId = "userId"
        val savedTypeForm = "savedTypeForm"
        val filterTypeForm = "filterTypeForm"
        val isLogin = "isLogin"
        val TOKEN = "token"
        val USER_PASSWORD = "userPassword"
        var otpBackHandle = false
        var recipeInformation = "recipe_information"
        var userInformation = "userInformation"
        var TUTORIAL_DIALOG = "tutorial_dialog"
        var CONFIRM_DIALOG = "confirm_dialog"
        var categoryName = "categoryName"
        var MyRecipesScroll = false
        var DiscoverRecipesScroll = false
        var SavedRecipesScroll = false
        var recipeListTypeFrom = "recipeListTypeFrom"
        var loadInitialData = false
        val NESTEDSCROLL_POSITION = "nestedScrollPosition"

        val targetUserId = "target_user_id"
        val saveRecipe = "saveRecipe"
        var EDITRECIPE = false
        var recipeDetailsEdit = RecipeData()


        var onDiscover = true // for blinking on discover screen


        /**Recipe Status**/
        var recipeStatusPublic = "Public"
        var recipeStatusPrivate = "Private"

        var recipePublish = "Public"
        var recipeUnPublish = "Private"

        /**AWS key**/
        val access_key = "access_key"
        val secret_key = "secret_key"
        val aws_region = "aws_region"
        val aws_bucket_name = "aws_bucket_name"
        const val AWS_SHARED_PREF_NAME = "AWS_ROOTS"
        val recipe_image = "recipe_image"

        /**API key parameter**/
        val user_name = "user_name"
        val full_name = "full_name"
        val first_name = "first_name"
        val last_name = "last_name"
        val email = "email"
        val otp = "otp"
        val phone_number = "phone_number"
        val country_code = "country_code"
        val password = "password"
        val personal_info = "personal_info"
        val newPassword = "newPassword"
        val confirmPassword = "confirmPassword"
        val aboutMe = "about_me"
        val oldPassword = "oldPassword"
        val mobile_otp = "mobile_otp"
        val profile_image = "profile_image"
        val cuisine = "cuisine"

        /**NOTIFICATION PREF*/
        const val RATINGS_NP = "rating_notification"
        const val COMMENTS_NP = "new_comment"
        const val RECIPE_NP = "add_new_recipe"

        /**Category Keys*/
        const val DISCOVER = "Discover"
        const val BREAKFAST = "Breakfast"
        const val LUNCH = "Lunch"
        const val DINNER = "Dinner"
        const val SNACKS = "Snacks"
        const val DESSERTS = "Dessert"
        const val DRINK = "Drink"

        /**Connections Keys*/
        const val FOLLOWER_KEY = "followers"
        const val FOLLOWING_KEY = "following"

        /**Chats keys**/
        const val OPPONENT_ID = "opponentId"
        const val OPPONENT_NAME = "opponentName"
        const val OPPONENT_IMAGE = "opponentImage"
        const val USERS_COLLECTION = "Users"
        const val CHATS_COLLECTION = "Chats"
        const val UNREAD_MESSAGES_COLLECTION = "UnreadMessages"
        const val FCM_TOKEN = "fcmToken"
        const val OS_TYPE = 1
        /**API END POINT**/
        const val SIGNUPENDPOINT = "users/signup"
        const val VERIFYOTP = "users/verify-otp"
        const val RESENDOTP = "users/resend-otp"
        const val SIGNINENDPOINT = "users/login"
        const val SENDEMAILOTP = "users/send-email-otp"
        const val UPDATEPASSWORDVERIFYOTP = "users/forgot-password-verify-otp"
        const val RESETPASSWORDUSER = "users/reset-password"
        const val CHECK_USERNAME = "users/check-username"
        const val EDIT_PROFILE = "users/edit-profile"
        const val CHANGE_PASSWORD = "users/change-password"
        const val DELETE_ACCOUNT = "users/delete-user"
        const val GENERATE_MOBILE_OTP = "users/generate-mobile-otp"
        const val MOBILE_OTP_VERIFY = "users/mobile-otp-verify"
        const val SEND_NOTIFICATION = "users/send-notification"
        const val GET_NOTIFICATIONS = "users/get-notifications"
        const val GET_ALL_TYPE_CONNECTION = "users/all-my-connections"
        const val GET_SEARCH_USERS = "users/search-users"
        const val GET_USERS_NOTIFY = "users/notify-users"

        //update only recipe image
        const val ONLYRECIPEIMAGE = "recipes/update-recipe-image"

        //notification
        const val UPDATE_NOTIFICATION_PREFERENCES = "notification/update-notification-preferences"

        //image upload
        const val UPDATE_PROFILE_IMAGE = "users/update-profile-image"

        //AWS credential
        const val AWS_CREDENTIAL = "users/credentials"

        //Recipe
        const val EXTRACT_DETAILS = "recipes/extract-details"
        const val MY_RECIPES = "recipes/my-recipes"

        //get user details
        const val GET_USER_DATA = "users/get-user"

        //save recipe
        const val SAVE_RECIPE = "recipes/save"

        //get cuisines details
        const val GET_CUISINES = "recipes/get-cuisines"

        //add cuisines
        const val ADD_CUISINES = "recipes/add-cuisine"

        //delete recipe
        const val DELETE_RECIPE = "recipes/delete-recipe"

        //update recipe status
        const val UPDATE_RECIPE_STATUS = "recipes/update-recipe-status"

        // user connection
        const val GET_USER_CONNECTION = "users/connections"

        //discover recipes
        const val GET_DISCOVER_RECIPES = "recipes/discover-recipes"

        //get-all-home-recipes
        const val GET_ALL_HOME_RECIPES = "recipes/get-all-home-recipes"

        //update - recipe
        const val UPDATE_RECIPE = "recipes/update-recipe"

        //home-page-recipes
        const val HOME_PAGE_RECIPES = "recipes/home-page-recipes-filters"

        //user-saved-recipes
        const val USER_SAVED_RECIPES = "recipes/user-saved-recipes"

        //count recipes
        const val MY_COUNT_RECIPES = "recipes/count-my-recipe"
        const val COUNT_PUBLIC_RECIPES = "recipes/count-public-recipes"
        const val COUNT_SAVED_RECIPES = "recipes/count-my-saved-recipes"

        //follow
        const val FOLLOW_USER = "users/follow"

        //unfollow
        const val UNFOLLOW_USER = "users/unfollow"

        //connection
        const val MY_CONNECTIONS = "users/my-connections"
        const val USERS_CONNECTIONS = "users/user-connections"

        //remove follower
        const val REMOVE_FOLLOWER = "users/remove-follower"

        //comments api end point
        const val ADD_COMMENT = "recipes/add-comment"
        const val GET_RECIPE_COMMENTS = "recipes/get-recipe-comments"
        const val DELETE_COMMENT = "recipes/delete-comment"
        const val EDIT_COMMENT = "recipes/delete-comment"

        //get recipe
        const val GET_RECIPE = "recipes/get-Recipe"

        //save recipe
        const val SAVE_RECIPE_TO_WISHLIST = "recipes/save-to-wishlist"
        const val UNSAVE_RECIPE_TO_WISHLIST = "recipes/unsave-from-wishlist"

        //rate recipe
        const val RATE_RECIPE = "recipes/rate-recipe"

        //user recipe
        const val USER_RECIPES = "recipes/get-user-recipes"
        //privacy policy
        const val PRIVACY_POLICY = "recipes/privacy-policy"
        //terms and conditions
        const val TERMS_AND_CONDITIONS = "recipes/terms-and-conditions"




    }
}