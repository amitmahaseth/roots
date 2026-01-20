package com.rootsrecipes.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rootsrecipes.di.data.repository.MainRepository
import com.rootsrecipes.model.AWSResponse
import com.rootsrecipes.model.AllHomeRecipeResponse
import com.rootsrecipes.utils.NetworkHelper
import com.rootsrecipes.utils.Resource
import org.json.JSONObject

class HomeVM(
    private val mainRepository: MainRepository, private val networkHelper: NetworkHelper
) : ViewModel() {

    //AWS Credential
    private val _awsCredentialData = MutableLiveData<Resource<AWSResponse>>()
    val awsCredentialData: LiveData<Resource<AWSResponse>>
        get() = _awsCredentialData

    //home recipes
    private val _homeAllData = MutableLiveData<Resource<AllHomeRecipeResponse>>()
    val homeAllData: LiveData<Resource<AllHomeRecipeResponse>>
        get() = _homeAllData

    private val _navigateToLogin = MutableLiveData<Boolean>()
    val navigateToLogin: LiveData<Boolean>
        get() = _navigateToLogin

    private var hasLoadedHomeData = false
    suspend fun getHomeRecipesData(forceRefresh: Boolean = false) {
        if (hasLoadedHomeData && !forceRefresh) {
            return
        }
        _homeAllData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.getAllHomeRecipes().let { its ->
                if (its.isSuccessful) {
                    hasLoadedHomeData = true
                    _homeAllData.postValue(Resource.success(its.body()))
                } else if(its.code() == 401){
                    _navigateToLogin.postValue(true)
                } else {
                    hasLoadedHomeData = false
                    val errorBodyString = its.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _homeAllData.postValue(Resource.error(errorMessage, null))
                }

            }
        } else {
            hasLoadedHomeData = false
            _homeAllData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun getAwsCredential() {
        _awsCredentialData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.awsCredential().let {
                if (it.isSuccessful) {
                    _awsCredentialData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _awsCredentialData.postValue(Resource.error(errorMessage, null))
                }
            }


        } else {
            _awsCredentialData.postValue(Resource.error("No internet connection", null))
        }
    }

    fun clearCache() {
        hasLoadedHomeData = false
    }
}