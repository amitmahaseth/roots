package com.rootsrecipes.model

data class CuisinResponse(
    val `data`: CuisineAndCategoryData,
    val statusCode: Int
)

data class CuisineAndCategoryData(
    val categories: ArrayList<Category>,
    val cuisines: ArrayList<String>
)

data class Category(
    val _id: String,
    val name: String,
    val sub_categories: ArrayList<String>
)