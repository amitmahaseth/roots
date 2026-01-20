package com.rootsrecipes.view.myRecipes.viewModel

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.JsonObject
import com.rootsrecipes.di.data.repository.MainRepository
import com.rootsrecipes.model.SignInResponse
import com.rootsrecipes.utils.NetworkHelper
import com.rootsrecipes.utils.Resource
import com.rootsrecipes.utils.SingleLiveEvent
import com.rootsrecipes.view.createAccount.model.RecipeData
import com.rootsrecipes.view.myRecipes.model.AllTypeConnectionResponse
import com.rootsrecipes.view.myRecipes.model.GetUserNotifyResponse
import com.rootsrecipes.view.myRecipes.model.MyRecipesResponse
import org.json.JSONObject

class MyRecipesVM(
    private val mainRepository: MainRepository, private val networkHelper: NetworkHelper
) : ViewModel() {

    var fixedSearchBarVisibility: Int = View.GONE

    // Add isShowingMyRecipes as a MutableLiveData to persist and observe its state
    private val _isShowingMyRecipes = MutableLiveData(true) // Default to true (My Recipes)
    val isShowingMyRecipes: LiveData<Boolean> get() = _isShowingMyRecipes

    // My Recipes
    private val _myRecipesData = MutableLiveData<Resource<MyRecipesResponse>>()
    val myRecipesData: LiveData<Resource<MyRecipesResponse>> get() = _myRecipesData

    // Accumulated recipes data
    private val _allRecipesData = MutableLiveData<ArrayList<RecipeData>>()
    val allRecipesData: LiveData<ArrayList<RecipeData>> get() = _allRecipesData

    // Saved Recipes
    private val _savedRecipesData = MutableLiveData<Resource<MyRecipesResponse>>()
    val savedRecipesData: LiveData<Resource<MyRecipesResponse>> get() = _savedRecipesData

    // User Details
    private val _getUserDetailsData = SingleLiveEvent<Resource<SignInResponse>>()
    val getUserDetailsData: LiveData<Resource<SignInResponse>> get() = _getUserDetailsData

    //get All type connection
    private var _getAllTypeConnectionData = SingleLiveEvent<Resource<AllTypeConnectionResponse>>()
    val getAllTypeConnectionData: LiveData<Resource<AllTypeConnectionResponse>>
        get() = _getAllTypeConnectionData

    //getUserNotify
    private var _getUserNotifyData = SingleLiveEvent<Resource<GetUserNotifyResponse>>()
    val getUserNotifyData: LiveData<Resource<GetUserNotifyResponse>>
        get() = _getUserNotifyData

    // Update Profile Image
    private val _updateProfileImageData = SingleLiveEvent<Resource<SignInResponse>>()
    val updateProfileImageData: LiveData<Resource<SignInResponse>> get() = _updateProfileImageData

    private var currentPage = 1
    private var isLoading = false
    private var hasMoreData = true
    private var hasLoadedMyRecipesData = false
    private var hasLoadedSavedData = false
    private var hasLoadedUserData = false

    fun isLoading() = isLoading
    fun hasMoreData() = hasMoreData
    fun getCurrentPage() = currentPage

    fun updateCurrentPage() {
        currentPage++
    }


    fun setLoading(loading: Boolean) {
        isLoading = loading
    }

    fun setHasMoreData(hasMore: Boolean) {
        hasMoreData = hasMore
    }

    // Method to update isShowingMyRecipes
    fun setIsShowingMyRecipes(showingMyRecipes: Boolean) {
        _isShowingMyRecipes.value = showingMyRecipes
    }


    suspend fun getUserNotify(recipe_id: String, user_ids: String) {
        _getUserNotifyData = SingleLiveEvent()
        _getUserNotifyData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.getUserNotify(recipe_id, user_ids).let {
                if (it.isSuccessful) {
                    _getUserNotifyData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _getUserNotifyData.postValue(Resource.error(errorMessage, null))
                }
            }

        } else {
            _getUserNotifyData.postValue(Resource.error("No internet connection", null))
        }


    }

    suspend fun getAllTypeConnection(page: String, limit: String) {
        _getAllTypeConnectionData = SingleLiveEvent()
        _getAllTypeConnectionData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.getAllTypeConnection(page, limit).let {
                if (it.isSuccessful) {
                    _getAllTypeConnectionData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _getAllTypeConnectionData.postValue(Resource.error(errorMessage, null))
                }
            }

        } else {
            _getAllTypeConnectionData.postValue(Resource.error("No internet connection", null))
        }


    }

    suspend fun myRecipes(
        searchKeyword: String = "",
        category: String,
        cuisine: String,
        pageNumber: Int,
        limit: Int,
        forceRefresh: Boolean = false
    ) {/*if (hasLoadedMyRecipesData && !forceRefresh) {
            return
        }*/

        if (forceRefresh) {
            currentPage = 1
            _allRecipesData.value = ArrayList()
        }

        _myRecipesData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.myRecipes(searchKeyword, category, cuisine, pageNumber, limit).let {
                /*   if (it.isSuccessful) {
                       val newData = it.body()?.data ?: ArrayList()
                       if (pageNumber == 1) {
                           _allRecipesData.value = newData
                           currentPage = pageNumber
                       } else {
                           if (newData.isNotEmpty()) {
                               val currentData = _allRecipesData.value ?: ArrayList()
                               currentData.addAll(newData)
                               _allRecipesData.value = currentData
                               currentPage = pageNumber
                           }
                       }
                       _myRecipesData.postValue(Resource.success(it.body()))
                       hasLoadedMyRecipesData = true
                   } else {
                       hasLoadedMyRecipesData = false
                       val errorBodyString = it.errorBody()?.string()
                       val jsonObjectError = JSONObject(errorBodyString ?: "{}")
                       val errorMessage = jsonObjectError.getString("message")
                       _myRecipesData.postValue(Resource.error(errorMessage, null))
                   }*/
                if (it.isSuccessful) {
                    _myRecipesData.postValue(Resource.success(it.body()))
                    hasLoadedSavedData = true
                } else {
                    hasLoadedSavedData = false
                    val errorBodyString = it.errorBody()?.string()
                    val jsonObjectError = JSONObject(errorBodyString ?: "{}")
                    val errorMessage = jsonObjectError.getString("message")
                    _myRecipesData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _myRecipesData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun savedRecipes(
        search: String = "",
        category: String,
        cuisine: String,
        page: Int,
        limit: Int,
        forceRefresh: Boolean = false
    ) {/* if (hasLoadedSavedData && !forceRefresh) {
            return
        }*/

        _savedRecipesData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.userSavedRecipes(page, limit, search, category, cuisine).let {
                if (it.isSuccessful) {
                    _savedRecipesData.postValue(Resource.success(it.body()))
                    hasLoadedSavedData = true
                } else {
                    hasLoadedSavedData = false
                    val errorBodyString = it.errorBody()?.string()
                    val jsonObjectError = JSONObject(errorBodyString ?: "{}")
                    val errorMessage = jsonObjectError.getString("message")
                    _savedRecipesData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _savedRecipesData.postValue(Resource.error("No internet connection", null))
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
                    val jsonObjectError = JSONObject(errorBodyString ?: "{}")
                    val errorMessage = jsonObjectError.getString("message")
                    _getUserDetailsData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _getUserDetailsData.postValue(Resource.error("No internet connection", null))
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
                    val jsonObjectError = JSONObject(errorBodyString ?: "{}")
                    val errorMessage = jsonObjectError.getString("message")
                    _updateProfileImageData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _updateProfileImageData.postValue(Resource.error("No internet connection", null))
        }
    }

    fun clearCache() {
        hasLoadedMyRecipesData = false
        hasLoadedSavedData = false
        hasLoadedUserData = false
        currentPage = 1
        _allRecipesData.value = ArrayList()
    }
}