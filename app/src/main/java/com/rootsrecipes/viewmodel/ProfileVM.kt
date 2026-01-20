package com.rootsrecipes.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.JsonObject
import com.rootsrecipes.di.data.repository.MainRepository
import com.rootsrecipes.model.CommonResponse
import com.rootsrecipes.model.FollowerFollowingResponse
import com.rootsrecipes.model.SignInResponse
import com.rootsrecipes.utils.NetworkHelper
import com.rootsrecipes.utils.Resource
import com.rootsrecipes.utils.SingleLiveEvent
import com.rootsrecipes.view.messages.model.SearchUserResponse
import com.rootsrecipes.view.myRecipes.model.ConnectionResponse
import com.rootsrecipes.view.myRecipes.model.UserRecipesResponse
import org.json.JSONObject

class ProfileVM(
    private val mainRepository: MainRepository, private val networkHelper: NetworkHelper
) : ViewModel() {
    private var hasLoadedUserRecipeData = false

    // user profile
    private val _userProfileData = MutableLiveData<Resource<CommonResponse>>()
    val userProfileData: LiveData<Resource<CommonResponse>>
        get() = _userProfileData

    //user followers and following
    private val _getFollowerFollowingData = MutableLiveData<Resource<FollowerFollowingResponse>>()
    val getFollowerFollowingData: LiveData<Resource<FollowerFollowingResponse>>
        get() = _getFollowerFollowingData

    //follow user
    private val _followData = MutableLiveData<Resource<CommonResponse>>()
    val followData: LiveData<Resource<CommonResponse>>
        get() = _followData

    //unfollow user
    private val _unfollowData = MutableLiveData<Resource<CommonResponse>>()
    val unfollowData: LiveData<Resource<CommonResponse>>
        get() = _unfollowData

    //connection user
    private val _connectionUserData = MutableLiveData<Resource<ConnectionResponse>>()
    val connectionUserData: LiveData<Resource<ConnectionResponse>>
        get() = _connectionUserData

    //search user
    private var _searchUserData = SingleLiveEvent<Resource<SearchUserResponse>>()
    val searchUserData: LiveData<Resource<SearchUserResponse>>
        get() = _searchUserData

    //user recipes
    private val _userRecipesData = MutableLiveData<Resource<UserRecipesResponse>>()
    val userRecipesData: LiveData<Resource<UserRecipesResponse>>
        get() = _userRecipesData

    //unfollow user
    private val _removeFollowerData = MutableLiveData<Resource<CommonResponse>>()
    val removeFollowerData: LiveData<Resource<CommonResponse>>
        get() = _removeFollowerData

    suspend fun userProfileData(jsonObject: JsonObject) {
        _userProfileData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.changePassword(jsonObject).let {
                if (it.isSuccessful) {
                    _userProfileData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _userProfileData.postValue(Resource.error(errorMessage, null))
                }
            }


        } else {
            _userProfileData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun getFollowerFollowing(type: String, searchKeyword: String) {
        _getFollowerFollowingData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.getUserConnection(type, searchKeyword).let {
                if (it.isSuccessful) {
                    _getFollowerFollowingData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _getFollowerFollowingData.postValue(Resource.error(errorMessage, null))
                }
            }


        } else {
            _getFollowerFollowingData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun followUser(jsonObject: JsonObject) {
        _followData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.followUser(jsonObject).let {
                if (it.isSuccessful) {
                    _followData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _followData.postValue(Resource.error(errorMessage, null))
                }
            }


        } else {
            _followData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun unfollowUser(jsonObject: JsonObject) {
        _unfollowData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.unfollowUser(jsonObject).let {
                if (it.isSuccessful) {
                    _unfollowData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _unfollowData.postValue(Resource.error(errorMessage, null))
                }
            }

        } else {
            _unfollowData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun removeFollower(jsonObject: JsonObject) {
        _removeFollowerData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.removeFollower(jsonObject).let {
                if (it.isSuccessful) {
                    _removeFollowerData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _removeFollowerData.postValue(Resource.error(errorMessage, null))
                }
            }

        } else {
            _removeFollowerData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun myConnections(
        type: String,
        searchKeyword: String,
        page: Int,
        limit: Int
    ) {
        _connectionUserData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.myConnections(type, searchKeyword, page, limit).let {
                if (it.isSuccessful) {
                    _connectionUserData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _connectionUserData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _connectionUserData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun searchUserChat(search: String) {
        _searchUserData = SingleLiveEvent()
        _searchUserData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.getSearchUser(search).let {
                if (it.isSuccessful) {
                    _searchUserData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _searchUserData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _searchUserData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun userConnections(
        type: String,
        userId: String,
        searchKeyword: String,
        page: Int,
        limit: Int
    ) {
        _connectionUserData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.userConnections(type, userId, searchKeyword, page, limit).let {
                if (it.isSuccessful) {
                    _connectionUserData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _connectionUserData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _connectionUserData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun userRecipes(
        userId: String,
        pageNumber: Int,
        limit: Int
    ) {
        _userRecipesData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.userRecipes(userId, pageNumber, limit).let {
                if (it.isSuccessful) {
                    _userRecipesData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _userRecipesData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _userRecipesData.postValue(Resource.error("No internet connection", null))
        }
    }

    fun clearCacheUserRecipe() {
        hasLoadedUserRecipeData = false
    }

    //user details
    private val _getUserDetailsData = SingleLiveEvent<Resource<SignInResponse>>()
    val getUserDetailsData: LiveData<Resource<SignInResponse>>
        get() = _getUserDetailsData

    suspend fun getUserDetails(userID: String) {

        _getUserDetailsData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.getUserDetails(userID).let {
                if (it.isSuccessful) {
                    _getUserDetailsData.postValue(Resource.success(it.body()))
                } else {
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
}