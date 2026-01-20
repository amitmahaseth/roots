package com.rootsrecipes.view.createAccount.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.JsonObject
import com.rootsrecipes.di.data.repository.MainRepository
import com.rootsrecipes.model.CommonResponse
import com.rootsrecipes.model.OTPResponse
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.NetworkHelper
import com.rootsrecipes.utils.Resource
import com.rootsrecipes.utils.SingleLiveEvent
import com.rootsrecipes.view.messages.model.UserDataChat
import com.rootsrecipes.view.messages.viewModel.ChatViewModel
import org.json.JSONObject

class SignUpVM(
    private val mainRepository: MainRepository, private val networkHelper: NetworkHelper
) : ViewModel() {

    //create user account
    private val _createData = MutableLiveData<Resource<CommonResponse>>()
    val createData: LiveData<Resource<CommonResponse>>
        get() = _createData

    //create resend otp
    private val _resendOtpData = MutableLiveData<Resource<CommonResponse>>()
    val resendOtpData: LiveData<Resource<CommonResponse>>
        get() = _resendOtpData

    //verify otp
    private val _otpData = MutableLiveData<Resource<OTPResponse>>()
    val otpData: LiveData<Resource<OTPResponse>>
        get() = _otpData

    //verify otp password change
    private val _otpPasswordData = MutableLiveData<Resource<CommonResponse>>()
    val otpPasswordData: LiveData<Resource<CommonResponse>>
        get() = _otpPasswordData

    //verify otp phone number
    private val _otpPhoneNumberData = SingleLiveEvent<Resource<OTPResponse>>()
    val otpPhoneNumberData: LiveData<Resource<OTPResponse>>
        get() = _otpPhoneNumberData

    private val _checkUserData = MutableLiveData<Resource<CommonResponse>>()
    val checkUserData: LiveData<Resource<CommonResponse>>
        get() = _checkUserData
    suspend fun createUser(jsonObject: JsonObject) {
        _createData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.createUserAccount(jsonObject).let {
                if (it.isSuccessful) {
                    _createData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _createData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _createData.postValue(Resource.error("No internet connection", null))
        }
    }
    suspend fun resendOtp(jsonObject: JsonObject) {
        _resendOtpData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.resendOtp(jsonObject).let {
                if (it.isSuccessful) {
                    _resendOtpData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _resendOtpData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _resendOtpData.postValue(Resource.error("No internet connection", null))
        }
    }
    suspend fun otpVerify(jsonObject: JsonObject) {
        _otpData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.otpVerify(jsonObject).let {
                if (it.isSuccessful) {
                    _otpData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _otpData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _otpData.postValue(Resource.error("No internet connection", null))
        }
    }
    suspend fun verifyOtpPasswordChange(jsonObject:JsonObject) {
        _otpPasswordData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.verifyOtpPasswordChange(jsonObject).let {
                if (it.isSuccessful) {
                    _otpPasswordData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _otpPasswordData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _otpPasswordData.postValue(Resource.error("No internet connection", null))
        }
    }
    suspend fun checkUsername(username: String) {
        _checkUserData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.checkUsername(username).let {
                if (it.isSuccessful) {
                    _checkUserData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _checkUserData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _checkUserData.postValue(Resource.error("No internet connection", null))
        }
    }
    suspend fun verifyOtpPhoneNumber(jsonObject: JsonObject) {
        _otpPhoneNumberData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.mobileOtpVerify(jsonObject).let {
                if (it.isSuccessful) {
                    _otpPhoneNumberData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _otpPhoneNumberData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _otpPhoneNumberData.postValue(Resource.error("No internet connection", null))
        }
    }
    private val firestore = FirebaseFirestore.getInstance()
    fun storeUserInChatUsers(userData: UserDataChat) {
        // First check if userID is valid
        if (userData.userID.isNullOrEmpty()) {
            Log.e("ChatUsers", "Cannot store user: userID is empty")
            return
        }

        val userMap = hashMapOf(
            "userId" to userData.userID,
            "name" to userData.name,
            "image" to userData.image,
        )

        // Directly set with merge option
        try {
            firestore.collection(Constants.USERS_COLLECTION).document(userData.userID)
                .set(userMap, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("ChatUsers", "User ${userData.userID} updated successfully")
                }
                .addOnFailureListener { e ->
                    Log.e("ChatUsers", "Error storing user: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e("ChatUsers", "Exception when storing user: ${e.message}")
            e.printStackTrace()
        }
    }

}