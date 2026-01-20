package com.rootsrecipes.view.notification.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rootsrecipes.di.data.repository.MainRepository
import com.rootsrecipes.model.NotificationListResponse
import com.rootsrecipes.utils.NetworkHelper
import com.rootsrecipes.utils.Resource
import org.json.JSONObject

class NotificationsListVM(
    private val mainRepository: MainRepository,
    private val networkHelper: NetworkHelper
) :
    ViewModel() {
    private val _notificationListData = MutableLiveData<Resource<NotificationListResponse>>()
    val notificationListData: LiveData<Resource<NotificationListResponse>>
        get() = _notificationListData


    suspend fun getNotifications(page: Int, limit: Int) {
        _notificationListData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.getNotifications(page, limit).let {
                if (it.isSuccessful) {
                    _notificationListData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    // Parse the JSON content to extract the error message
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _notificationListData.postValue(Resource.error(errorMessage, null))
                }
            }


        } else {
            _notificationListData.postValue(Resource.error("No internet connection", null))
        }
    }

}