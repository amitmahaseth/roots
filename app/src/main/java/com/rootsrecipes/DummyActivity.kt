package com.rootsrecipes

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.rootsrecipes.database.entity.AppDataBase
import com.rootsrecipes.database.entity.UserNameTable
import com.rootsrecipes.database.entity.reprository.UserRepository
import com.rootsrecipes.database.entity.viewmodel.UserViewmodel

class DummyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dummy)
        val db = AppDataBase.getDatabase(this)
        val repository = UserRepository(db.userDao())
        val vm = UserViewmodel(repository)
        vm.insertUser(UserNameTable(name = "amit", age = 23))


    }


}