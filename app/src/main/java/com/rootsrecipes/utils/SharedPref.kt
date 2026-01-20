package com.rootsrecipes.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.rootsrecipes.model.SignInData
import com.rootsrecipes.model.User

class SharedPref(private var context: Context) {
    private var pref: SharedPreferences = context.getSharedPreferences("pref", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveString(key: String, value: String) {
        pref.edit().putString(key, value).apply()
    }


    fun getString(key: String): String {
        return pref.getString(key, "").toString()
    }

    fun saveInt(key: String, value: Int) {
        pref.edit().putInt(key, value).apply()
    }

    fun saveLong(key: String, value: Long) {
        pref.edit().putLong(key, value).apply()
    }

    fun saveFloat(key: String, value: Float) {
        pref.edit().putFloat(key, value).apply()
    }

    fun getFloat(key: String): Float {
        return pref.getFloat(key, 0.00f)
    }


    fun getLong(key: String): Long {
        return pref.getLong(key, 0)
    }

    fun getInt(key: String): Int {
        return pref.getInt(key, 0)
    }

    fun getBoolean(key: String): Boolean {
        return pref.getBoolean(key, false)
    }

    fun saveBoolean(key: String, value: Boolean) {
        pref.edit().putBoolean(key, value).apply()
    }


    fun clearPreference(context: Context) {
        pref = context.getSharedPreferences("pref", Context.MODE_PRIVATE)
        val prefsEditor: SharedPreferences.Editor = pref.edit()
        prefsEditor.clear()
        prefsEditor.apply()
    }
    fun saveSignInData(response: User) {
        val json = gson.toJson(response)
        pref.edit().putString("sign_in_data", json).apply()
    }

    fun getSignInData(): User? {
        val json = pref.getString("sign_in_data", null)
        return if (json != null) {
            gson.fromJson(json, User::class.java)
        } else {
            null
        }
    }

}