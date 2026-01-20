package com.rootsrecipes.view.recipeRecording.model

import androidx.versionedparcelable.ParcelField
import com.rootsrecipes.view.createAccount.model.RecipeData
import kotlinx.android.parcel.Parcelize

data class ChipData(
    var text: String,
    var isChecked: Boolean = false,
    val subCategories: ArrayList<String> = ArrayList()
)
data class ExtractRecipeResponse(
     val `data`: RecipeData,
    val message: String,
    val statusCode: Int
)

data class CountRecipeResponse(
    val `data`: CountRecipe,
    val message: String,
    val statusCode: Int
)

data class CountRecipe(
    val count: Int
)
