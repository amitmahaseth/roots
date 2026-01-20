package com.rootsrecipes.view.recipeRecording.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.JsonObject
import com.rootsrecipes.di.data.repository.MainRepository
import com.rootsrecipes.model.CuisinResponse
import com.rootsrecipes.model.CuisinesAddResponse
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.NetworkHelper
import com.rootsrecipes.utils.Resource
import com.rootsrecipes.view.myRecipes.model.ConnectionResponse
import com.rootsrecipes.view.recipeRecording.model.CountRecipeResponse
import com.rootsrecipes.view.recipeRecording.model.ExtractRecipeResponse
import org.json.JSONObject

class RecipeCreateViewModel(
    private val mainRepository: MainRepository, private val networkHelper: NetworkHelper
) : ViewModel() {
    //extract details
    private val _extractDetailsData = MutableLiveData<Resource<ExtractRecipeResponse>>()
    val extractDetailsData: LiveData<Resource<ExtractRecipeResponse>>
        get() = _extractDetailsData

    //update Recipe
    private val _updateRecipeData = MutableLiveData<Resource<ExtractRecipeResponse>>()
    val updateRecipeData: LiveData<Resource<ExtractRecipeResponse>>
        get() = _updateRecipeData

    //get Cuisines
    private val _cuisinesData = MutableLiveData<Resource<CuisinResponse>>()
    val cuisinesData: LiveData<Resource<CuisinResponse>>
        get() = _cuisinesData

    //add Cuisines
    private val _addCuisinesData = MutableLiveData<Resource<CuisinesAddResponse>>()
    val addCuisinesData: LiveData<Resource<CuisinesAddResponse>>
        get() = _addCuisinesData

    //count my recipes
    private val _countRecipeData = MutableLiveData<Resource<CountRecipeResponse>>()
    val countRecipeData: LiveData<Resource<CountRecipeResponse>>
        get() = _countRecipeData

    //count public recipes
    private val _countPublicRecipeData = MutableLiveData<Resource<CountRecipeResponse>>()
    val countPublicRecipeData: LiveData<Resource<CountRecipeResponse>>
        get() = _countPublicRecipeData

    //count saved recipes
    //count public recipes
    private val _countSavedRecipeData = MutableLiveData<Resource<CountRecipeResponse>>()
    val countSavedRecipeData: LiveData<Resource<CountRecipeResponse>>
        get() = _countSavedRecipeData

    //get people for filter

    private val _peopleFilterData = MutableLiveData<Resource<ConnectionResponse>>()
    val peopleFilterData: LiveData<Resource<ConnectionResponse>>
        get() = _peopleFilterData


    suspend fun extractDetails(jsonObject: JsonObject) {
        _extractDetailsData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.extractDetails(jsonObject).let {
                if (it.isSuccessful) {
                    _extractDetailsData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _extractDetailsData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _extractDetailsData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun updateRecipe(jsonObject: JsonObject) {
        _updateRecipeData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.updateRecipe(jsonObject).let {
                if (it.isSuccessful) {
                    _updateRecipeData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _updateRecipeData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _updateRecipeData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun getCuisinesData() {
        _cuisinesData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.getCuisines().let {
                if (it.isSuccessful) {
                    _cuisinesData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _cuisinesData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _cuisinesData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun addCuisinesData(jsonObject: JsonObject) {
        _addCuisinesData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.addCuisines(jsonObject).let {
                if (it.isSuccessful) {
                    _addCuisinesData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _addCuisinesData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _addCuisinesData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun myCountRecipes(
        category: ArrayList<String>,
        cuisine: ArrayList<String>,
    ) {
        _countRecipeData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.myCountRecipes(category, cuisine).let {
                if (it.isSuccessful) {
                    _countRecipeData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _countRecipeData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _countRecipeData.postValue(Resource.error("No internet connection", null))
        }
    }


    suspend fun countPublicRecipes(
        users: String,
        category: String,
        cuisine: String
    ) {
        _countPublicRecipeData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.countPublicRecipes(users, category, cuisine).let {
                if (it.isSuccessful) {
                    _countPublicRecipeData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _countPublicRecipeData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _countPublicRecipeData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun countSaveRecipes(
        category: String,
        cuisine: String
    ) {
        _countSavedRecipeData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.countSaveRecipes(category, cuisine).let {
                if (it.isSuccessful) {
                    _countSavedRecipeData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _countSavedRecipeData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _countSavedRecipeData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun getPeopleFilters(
        page: Int,
        limit: Int
    ) {
        _peopleFilterData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.myConnections(Constants.FOLLOWING_KEY, "", page, limit).let {
                if (it.isSuccessful) {
                    _peopleFilterData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _peopleFilterData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _peopleFilterData.postValue(Resource.error("No internet connection", null))
        }
    }
}