package com.rootsrecipes.view.forgot.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.JsonObject
import com.rootsrecipes.di.data.repository.MainRepository
import com.rootsrecipes.model.CommonResponse
import com.rootsrecipes.utils.NetworkHelper
import com.rootsrecipes.utils.Resource
import org.json.JSONObject

class ForgotVM(
    private val mainRepository: MainRepository, private val networkHelper: NetworkHelper
) : ViewModel() {

    //forgot user account
    private val _forgotEmailData = MutableLiveData<Resource<CommonResponse>>()
    val forgotEmailData: LiveData<Resource<CommonResponse>>
        get() = _forgotEmailData

    //updatePassword User account
    private val _updatePasswordData = MutableLiveData<Resource<CommonResponse>>()
    val updatePasswordData: LiveData<Resource<CommonResponse>>
        get() = _updatePasswordData

    suspend fun forgotEmail(jsonObject: JsonObject) {
        _forgotEmailData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.sendOtpOnEmail(jsonObject).let {
                if (it.isSuccessful) {
                    _forgotEmailData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _forgotEmailData.postValue(Resource.error(errorMessage, null))
                }
            }

        } else {
            _forgotEmailData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun resetPasswordUser(jsonObject: JsonObject) {
        _updatePasswordData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.resetPasswordUser(jsonObject).let {
                if (it.isSuccessful) {
                    _updatePasswordData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _updatePasswordData.postValue(Resource.error(errorMessage, null))
                }
            }

        } else {
            _updatePasswordData.postValue(Resource.error("No internet connection", null))
        }
    }

}