package com.rootsrecipes.view

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.JsonObject
import com.rootsrecipes.MainActivity
import com.rootsrecipes.databinding.ActivityLaunchBinding
import com.rootsrecipes.utils.Constants
import io.branch.referral.Branch
import org.json.JSONObject

@SuppressLint("CustomSplashScreen")
class LaunchActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLaunchBinding
    private var jsonObject:String ?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLaunchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if(intent.extras!=null && intent.extras!!.get("data") !=null ) {
            Log.d("intentDebug", intent.extras!!.get("data").toString() + intent.data.toString())
           jsonObject =  intent.extras!!.get("data").toString()
        }
    }
    override fun onStart() {
        super.onStart()
        Branch.sessionBuilder(this)
            .withCallback { referringParams, error ->
                if (error == null) {
                    Log.d("Branch", referringParams.toString())
                    if (referringParams?.has("recipeId") == true && referringParams?.has("recipeUserId") == true) {
                        Constants.recipeShare = referringParams.getString("recipeId")
                        Constants.recipeUserIdShare = referringParams.getString("recipeUserId")
                    }else if (referringParams?.has("userId") == true){
                        Constants.userIdShare = referringParams.getString("userId")
                    }
                } else {
                    Log.e("BranchError", error.message ?: "Unknown error")
                }
            }
            .withData(this.intent?.data)
            .init()
    }

    override fun onResume() {
        super.onResume()
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            if(jsonObject!=null) {
                intent.putExtra("data", jsonObject)
            }
            startActivity(intent)
            finish()
        }, 2000)
    }
}