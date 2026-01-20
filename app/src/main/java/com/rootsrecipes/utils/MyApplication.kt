package com.rootsrecipes.utils

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.rootsrecipes.di.module.appModule
import com.rootsrecipes.model.ExceptionListener
import io.branch.referral.Branch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MyApplication : Application(), ExceptionListener {
    companion object{
        var currentChatId=""
    }
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext((this@MyApplication))
            modules(listOf(appModule))
        }
        Branch.getAutoInstance(this)
//        setupExceptionHandler()
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        Log.d("RootsRecipes", throwable.message!!)
        Log.d("RootsRecipes", thread.name)
        Extension.stopProgress()
        this.makeToast(throwable.message!!)
    }

    private fun setupExceptionHandler() {
        Handler(Looper.getMainLooper()).post {
            while (true) {
                try {
                    Looper.loop()
                } catch (e: Throwable) {
                    uncaughtException(Looper.getMainLooper().thread, e)
                }
            }
        }
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            uncaughtException(t, e)
        }
    }
}