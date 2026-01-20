package com.rootsrecipes.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson

class AWSSharedPref(private val context: Context) {

    private val gson = Gson()
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(Constants.AWS_SHARED_PREF_NAME, Context.MODE_PRIVATE)
    }
    fun clearAll(){
        sharedPreferences.edit().clear().commit()
    }

    fun saveString(key:String,value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    fun fetchString(key:String): String? {
        return sharedPreferences.getString(key, "")
    }
}