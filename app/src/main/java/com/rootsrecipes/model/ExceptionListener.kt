package com.rootsrecipes.model

interface ExceptionListener {
    fun uncaughtException(thread: Thread, throwable: Throwable)
}