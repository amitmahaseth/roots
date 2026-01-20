package com.rootsrecipes.view.discover.model

import com.rootsrecipes.R

data class FilterCategory(
    var filterName : String ,
    var isChecked : Boolean = false,
    var totalFilter: Int = 0,
    var profileImage : String = "",
    var userInfo: Pair<String, String> = Pair("", "")
)

data class CategoryItemData(
    val categoryName :String,
    val categoryIcon : Int = R.drawable.all_recipes_icon
)
