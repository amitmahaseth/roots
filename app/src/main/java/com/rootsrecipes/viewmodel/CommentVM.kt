package com.rootsrecipes.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.JsonObject
import com.rootsrecipes.di.data.repository.MainRepository
import com.rootsrecipes.model.CommonResponse
import com.rootsrecipes.utils.NetworkHelper
import com.rootsrecipes.utils.Resource
import com.rootsrecipes.view.myRecipes.model.AddCommentResponse
import com.rootsrecipes.view.myRecipes.model.CommentListResponse
import com.rootsrecipes.view.myRecipes.model.RateRecipeResponse
import org.json.JSONObject

class CommentVM(
    private val mainRepository: MainRepository, private val networkHelper: NetworkHelper
) : ViewModel() {
    // add Comment
    private val _addCommentData = MutableLiveData<Resource<AddCommentResponse>>()
    val addCommentData: LiveData<Resource<AddCommentResponse>>
        get() = _addCommentData

    // get comment list
    private val _commentListData = MutableLiveData<Resource<CommentListResponse>>()
    val commentListData: LiveData<Resource<CommentListResponse>>
        get() = _commentListData

    //delete comment
    private val _deleteCommentData = MutableLiveData<Resource<CommonResponse>>()
    val deleteCommentData: LiveData<Resource<CommonResponse>>
        get() = _deleteCommentData

    //rate recipe
    private val _rateRecipeData = MutableLiveData<Resource<RateRecipeResponse>>()
    val rateRecipeData: LiveData<Resource<RateRecipeResponse>>
        get() = _rateRecipeData

    suspend fun addComment(jsonObject: JsonObject) {
        _addCommentData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.addComment(jsonObject).let {
                if (it.isSuccessful) {
                    _addCommentData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _addCommentData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _addCommentData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun commentList(recipeId: String, page: Int, limit: Int) {
        _commentListData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.commentList(recipeId, page, limit).let {
                if (it.isSuccessful) {
                    _commentListData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _commentListData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _commentListData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun deleteComment(commentId: String) {
        _deleteCommentData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.deleteComment(commentId).let {
                if (it.isSuccessful) {
                    _deleteCommentData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _deleteCommentData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _deleteCommentData.postValue(Resource.error("No internet connection", null))
        }
    }

    suspend fun rateRecipe(jsonObject: JsonObject) {
        _rateRecipeData.postValue(Resource.loading(null))
        if (networkHelper.isNetworkConnected()) {
            mainRepository.rateRecipe(jsonObject).let {
                if (it.isSuccessful) {
                    _rateRecipeData.postValue(Resource.success(it.body()))
                } else {
                    val errorBodyString = it.errorBody()?.string()
                    val jsonObjectError = JSONObject(errorBodyString!!)
                    val errorMessage = jsonObjectError.getString("message")
                    _rateRecipeData.postValue(Resource.error(errorMessage, null))
                }
            }
        } else {
            _rateRecipeData.postValue(Resource.error("No internet connection", null))
        }
    }
}