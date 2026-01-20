package com.rootsrecipes.view.loginAccount.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.JsonObject
import com.rootsrecipes.di.data.repository.MainRepository
import com.rootsrecipes.model.CommonResponse
import com.rootsrecipes.model.SignInResponse
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.NetworkHelper
import com.rootsrecipes.utils.Resource
import com.rootsrecipes.utils.SingleLiveEvent
import com.rootsrecipes.view.messages.model.UserDataChat
import com.rootsrecipes.view.messages.viewModel.ChatViewModel
import org.json.JSONObject

class SignInVM(
    private val mainRepository: MainRepository, private val networkHelper: NetworkHelper
) : ViewModel() {
    //create user account
    private val _signInData = SingleLiveEvent<Resource<SignInResponse>>()
    val signInData: LiveData<Resource<SignInResponse>>
        get() = _signInData

    private val firestore = FirebaseFirestore.getInstance()
    suspend fun loginUser(jsonObject: JsonObject) {
        _signInData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.signInUser(jsonObject).let {
                if (it.isSuccessful) {
                    _signInData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _signInData.postValue(Resource.error(errorMessage, null))
                }
            }

        } else {
            _signInData.postValue(Resource.error("No internet connection", null))
        }
    }

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