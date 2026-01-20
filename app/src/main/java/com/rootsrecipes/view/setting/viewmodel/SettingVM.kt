package com.rootsrecipes.view.setting.viewmodel

import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.JsonObject
import com.rootsrecipes.di.data.repository.MainRepository
import com.rootsrecipes.model.CommonResponse
import com.rootsrecipes.model.NotificationUpdateResponse
import com.rootsrecipes.model.SignInResponse
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.NetworkHelper
import com.rootsrecipes.utils.Resource
import com.rootsrecipes.utils.SharedPref
import com.rootsrecipes.utils.SingleLiveEvent
import com.rootsrecipes.view.createAccount.model.RecipeData
import com.rootsrecipes.view.myRecipes.model.MyRecipesResponse
import com.rootsrecipes.view.recipeRecording.model.ExtractRecipeResponse
import org.json.JSONObject

class SettingVM(
    private val mainRepository: MainRepository,
    private val networkHelper: NetworkHelper,
    private val sharedPref: SharedPref
) : ViewModel() {

    var fixedSearchBarVisibility: Int = View.GONE
    private val firestore = FirebaseFirestore.getInstance()

    //edit profile
    private val _editProfileData = SingleLiveEvent<Resource<SignInResponse>>()
    val editProfileData: LiveData<Resource<SignInResponse>>
        get() = _editProfileData

    //delete account
    private val _deleteAccountData = MutableLiveData<Resource<CommonResponse>>()
    val deleteAccountData: LiveData<Resource<CommonResponse>>
        get() = _deleteAccountData

    //update notification preferences
    private val _updateNotificationData = MutableLiveData<Resource<NotificationUpdateResponse>>()
    val updateNotificationData: LiveData<Resource<NotificationUpdateResponse>>
        get() = _updateNotificationData

    //update user profile image
    private val _updateProfileImageData = SingleLiveEvent<Resource<SignInResponse>>()
    val updateProfileImageData: LiveData<Resource<SignInResponse>>
        get() = _updateProfileImageData

    //user details
    private val _getUserDetailsData = SingleLiveEvent<Resource<SignInResponse>>()
    val getUserDetailsData: LiveData<Resource<SignInResponse>>
        get() = _getUserDetailsData


    //generate mobile otp
    private val _generateMobileOtpData = MutableLiveData<Resource<CommonResponse>>()
    val generateMobileOtpData: LiveData<Resource<CommonResponse>>
        get() = _generateMobileOtpData

    //my recipes
    private val _myRecipesData = MutableLiveData<Resource<MyRecipesResponse>>()
    val myRecipesData: LiveData<Resource<MyRecipesResponse>>
        get() = _myRecipesData

    // Store the accumulated data
    private val _allRecipesData = MutableLiveData<ArrayList<RecipeData>>()
    val allRecipesData: LiveData<ArrayList<RecipeData>> = _allRecipesData


    //my recipes saved
    private val _myRecipesSavedData = MutableLiveData<Resource<MyRecipesResponse>>()
    val myRecipesSavedData: LiveData<Resource<MyRecipesResponse>>
        get() = _myRecipesSavedData


    //home page recipes
    private val _homePageRecipesData = MutableLiveData<Resource<MyRecipesResponse>>()
    val homePageRecipesData: LiveData<Resource<MyRecipesResponse>>
        get() = _homePageRecipesData

    //user saved recipes
    private val _userSavedRecipesData = MutableLiveData<Resource<MyRecipesResponse>>()
    val userSavedRecipesData: LiveData<Resource<MyRecipesResponse>>
        get() = _userSavedRecipesData

    //save generated recipes
    private val _saveGenerateRecipesData = MutableLiveData<Resource<ExtractRecipeResponse>>()
    val saveGenerateRecipesData: LiveData<Resource<ExtractRecipeResponse>>
        get() = _saveGenerateRecipesData

    //delete recipe
    private val _deleteRecipeData = MutableLiveData<Resource<CommonResponse>>()
    val deleteRecipeData: LiveData<Resource<CommonResponse>>
        get() = _deleteRecipeData

    //get recipe data
    private val _getRecipeData = MutableLiveData<Resource<ExtractRecipeResponse>>()
    val getRecipeData: LiveData<Resource<ExtractRecipeResponse>>
        get() = _getRecipeData

    //update recipe status
    private val _updateRecipeStatusData = MutableLiveData<Resource<CommonResponse>>()
    val updateRecipeStatusData: LiveData<Resource<CommonResponse>>
        get() = _updateRecipeStatusData

    //save recipe to wishlist
    private val _saveRecipeData = SingleLiveEvent<Resource<CommonResponse>>()
    val saveRecipeData: LiveData<Resource<CommonResponse>>
        get() = _saveRecipeData

    //un save recipe to wishlist
    private val _unSaveRecipeData = SingleLiveEvent<Resource<CommonResponse>>()
    val unSaveRecipeData: LiveData<Resource<CommonResponse>>
        get() = _unSaveRecipeData

    //save
    private val _savedRecipesData = MutableLiveData<Resource<MyRecipesResponse>>()
    val savedRecipesData: LiveData<Resource<MyRecipesResponse>> get() = _savedRecipesData

    /**Save recipe image only**/
    private val _saveRecipesImageData = MutableLiveData<Resource<ExtractRecipeResponse>>()
    val saveRecipesImageData: LiveData<Resource<ExtractRecipeResponse>>
        get() = _saveRecipesImageData


    private var hasLoadedData = false
    private var hasLoadedHomePageData = false
    private var hasLoadedSavedData = false
    private var hasLoadedMyRecipesData = false
    private var hasLoadedUserData = false


    private var currentPage = 1


    private var isLoading = false
    private var hasMoreData = true

    fun isLoading() = isLoading
    fun hasMoreData() = hasMoreData

    fun setLoading(loading: Boolean) {
        isLoading = loading
    }

    fun setHasMoreData(hasMore: Boolean) {
        hasMoreData = hasMore
    }

    suspend fun saveRecipeImage(jsonObject: JsonObject) {
        _saveRecipesImageData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.saveImageRecipes(jsonObject).let {
                if (it.isSuccessful) {
                    _saveRecipesImageData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _saveRecipesImageData.postValue(Resource.error(errorMessage, null))
                }
            }

        } else {
            _saveRecipesImageData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun editProfile(jsonObject: JsonObject) {
        _editProfileData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.editProfile(jsonObject).let {
                if (it.isSuccessful) {
                    _editProfileData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _editProfileData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _editProfileData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun deleteAccount() {
        _deleteAccountData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.deleteAccount().let {
                if (it.isSuccessful) {
                    _deleteAccountData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _deleteAccountData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _deleteAccountData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun updateNotificationPreferences(jsonObject: JsonObject) {
        _updateNotificationData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.updateNotificationPreferences(jsonObject).let {
                if (it.isSuccessful) {
                    _updateNotificationData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _updateNotificationData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _updateNotificationData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun generateMobileOtp() {
        _generateMobileOtpData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.generateMobileOtp().let {
                if (it.isSuccessful) {
                    _generateMobileOtpData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _generateMobileOtpData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _generateMobileOtpData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun updateProfileImageData(jsonObject: JsonObject) {
        _updateProfileImageData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.updateProfileImage(jsonObject).let {
                if (it.isSuccessful) {
                    _updateProfileImageData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _updateProfileImageData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _updateProfileImageData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun getUserDetails(forceRefresh: Boolean = false, userID: String) {
        if (hasLoadedUserData && !forceRefresh) {
            return
        }
        _getUserDetailsData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.getUserDetails(userID).let {
                if (it.isSuccessful) {
                    _getUserDetailsData.postValue(Resource.success(it.body()))
                    hasLoadedUserData = true
                } else {
                    hasLoadedUserData = false
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _getUserDetailsData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _getUserDetailsData.postValue(Resource.error("No internet connection", null))
        }

    }

    /**My recipes list**/
    suspend fun myRecipes(
        searchKeyword: String = "",
        category: String,
        cuisine: String,
        pageNumber: Int,
        limit: Int,
        forceRefresh: Boolean = false
    ) {
        if (forceRefresh) {
            currentPage = 1
            _allRecipesData.value = ArrayList()
        }

        _myRecipesData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.myRecipes(searchKeyword, category, cuisine, pageNumber, limit).let {
                if (it.isSuccessful) {
                    val newData = it.body()?.data ?: ArrayList()
                    if (pageNumber == 1) {
                        // For first page, always update with new data (even if empty)
                        _allRecipesData.value = newData
                        currentPage = pageNumber
                    } else {
                        // For subsequent pages, only update if we get new data
                        if (newData.isNotEmpty()) {
                            val currentData = _allRecipesData.value ?: ArrayList()
                            currentData.addAll(newData)
                            _allRecipesData.value = currentData
                            currentPage = pageNumber
                        }
                    }
                    _myRecipesData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _myRecipesData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _myRecipesData.postValue(Resource.error("No internet connection", null))
        }
    }

    /**saved recipes**/
    suspend fun myRecipesSaved(
        searchKeyword: String = "",
        category: String,
        cuisine: String,
        pageNumber: Int,
        limit: Int,
        forceRefresh: Boolean = false
    ) {
        if (hasLoadedMyRecipesData && !forceRefresh) {
            return
        }

        _myRecipesSavedData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.myRecipes(searchKeyword, category, cuisine, pageNumber, limit).let {
                if (it.isSuccessful) {
                    _myRecipesSavedData.postValue(Resource.success(it.body()))
                    hasLoadedMyRecipesData = true
                } else {
                    hasLoadedMyRecipesData = false
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _myRecipesSavedData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _myRecipesSavedData.postValue(Resource.error("No internet connection", null))
        }
    }

    fun getCurrentPage() = currentPage
    suspend fun homePageRecipes(
        users: String,
        search: String,
        category: String,
        cuisine: String,
        page: Int,
        limit: Int,
        forceRefresh: Boolean = false
    ) {
        if (hasLoadedHomePageData && !forceRefresh) {
            return
        }

        _homePageRecipesData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.homePageRecipes(users, search, category, cuisine, page, limit).let {
                if (it.isSuccessful) {
                    _homePageRecipesData.postValue(Resource.success(it.body()))
                    hasLoadedHomePageData = true
                } else {
                    hasLoadedHomePageData = false
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _homePageRecipesData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _homePageRecipesData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun userSavedRecipes(
        page: Int,
        limit: Int,
        search: String,
        category: String,
        cuisine: String,
        forceRefresh: Boolean = false
    ) {
        if (hasLoadedSavedData && !forceRefresh) {
            return
        }

        _userSavedRecipesData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.userSavedRecipes(page, limit, search, category, cuisine).let {
                if (it.isSuccessful) {
                    _userSavedRecipesData.postValue(Resource.success(it.body()))
                    hasLoadedSavedData = true
                } else {
                    hasLoadedSavedData = false
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _userSavedRecipesData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _userSavedRecipesData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun savedRecipes(
        page: Int,
        limit: Int,
        search: String,
        category: String,
        cuisine: String,
        forceRefresh: Boolean = false
    ) {
        if (hasLoadedSavedData && !forceRefresh) {
            return
        }

        _savedRecipesData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.userSavedRecipes(page, limit, search, category, cuisine).let {
                if (it.isSuccessful) {
                    _savedRecipesData.postValue(Resource.success(it.body()))
                    hasLoadedSavedData = true
                } else {
                    hasLoadedSavedData = false
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _savedRecipesData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _savedRecipesData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun saveGeneratedRecipes(jsonObject: JsonObject) {
        _saveGenerateRecipesData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.saveRecipesAfterExtract(jsonObject).let {
                if (it.isSuccessful) {
                    _saveGenerateRecipesData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _saveGenerateRecipesData.postValue(Resource.error(errorMessage, null))
                }
            }

        } else {
            _saveGenerateRecipesData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun deleteRecipe(id: String) {
        _deleteRecipeData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.deleteRecipe(id).let {
                if (it.isSuccessful) {
                    _deleteRecipeData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _deleteRecipeData.postValue(Resource.error(errorMessage, null))
                }
            }

        } else {
            _deleteRecipeData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun updateRecipeStatus(jsonObject: JsonObject) {
        _updateRecipeStatusData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.updateRecipeStatus(jsonObject).let {
                if (it.isSuccessful) {
                    _updateRecipeStatusData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _updateRecipeStatusData.postValue(Resource.error(errorMessage, null))
                }
            }

        } else {
            _updateRecipeStatusData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun getRecipeData(recipeId: String) {
        _getRecipeData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.getRecipe(recipeId).let {
                if (it.isSuccessful) {
                    _getRecipeData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _getRecipeData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _getRecipeData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun saveRecipe(jsonObject: JsonObject) {
        _saveRecipeData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.saveRecipeToWishlist(jsonObject).let {
                if (it.isSuccessful) {
                    _saveRecipeData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _saveRecipeData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _saveRecipeData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun unSaveRecipe(recipeId: String) {
        _unSaveRecipeData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.unSaveRecipeToWishlist(recipeId).let {
                if (it.isSuccessful) {
                    _unSaveRecipeData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _unSaveRecipeData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _unSaveRecipeData.postValue(Resource.error("No internet connection", null))
        }
    }


    fun clearCache() {
        hasLoadedData = false
        hasLoadedUserData = false
    }

    fun clearCacheSaved() {
        hasLoadedSavedData = false
    }

    fun clearCacheMySaved() {
        hasLoadedMyRecipesData = false
    }

    fun clearCacheHomePage() {
        hasLoadedHomePageData = false
        hasLoadedUserData = true
    }

    fun fetchTokenMethod(onResult: (String?) -> Unit) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task: Task<String> ->
            if (!task.isSuccessful) {
                Log.e(
                    "FirebaseToken", "Fetching FCM registration token failed", task.exception
                )
                onResult(null)
                return@addOnCompleteListener
            }
            onResult(task.result)
        }

    }

    fun updateUserActiveStatus(userId: String, isOnline: Boolean) {
        if (userId != null) {
            val userMap = hashMapOf(
                "isOnline" to isOnline,
                "fcmToken" to sharedPref.getString(Constants.FCM_TOKEN),
                "osType" to Constants.OS_TYPE
            )
            firestore.collection(Constants.USERS_COLLECTION)
                .document(userId)
                .set(
                    userMap,
                    SetOptions.merge()
                ) // This will create or update only the specified fields
                .addOnSuccessListener {
                    Log.d("Firestore", "User online status set/updated successfully.")
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Failed to set/update user online status", e)
                }
        }
    }

    fun deleteUserFromFireStore(userId: String) {
        firestore.collection(Constants.USERS_COLLECTION)
            .document(userId)
            .delete()
            .addOnSuccessListener {
                Log.d("Firestore", "User document deleted successfully.")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error deleting user document", e)
            }
    }

    fun stopNotifications(userId: String, onComplete: (Boolean) -> Unit) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic("user_$userId")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true)
                    Log.d("FirebaseMessagingDebug", "Unsubscribed from topic")
                } else {
                    onComplete(false)
                    Log.d("FirebaseMessagingDebug", "Failed to unsubscribe from topic")
                }
            }
    }

    fun unregisterFromFCM() {
        FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("sahuuu", "Token deleted successfully")
            } else {
                Log.e("sahuuu", "Failed to delete token")
            }
        }
    }

    fun startNotifications() {
        val userId = sharedPref.getSignInData()?._id
        stopNotifications(userId!!) {
            Log.d("topicFirebaseDebug","user_$userId")
            FirebaseMessaging.getInstance().subscribeToTopic("user_$userId")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("FirebaseMessagingDebug", "Subscribed to topic")
                    } else {
                        Log.d("FirebaseMessagingDebug", "Failed to subscribe to topic")
                    }
                }

        }

    }
}