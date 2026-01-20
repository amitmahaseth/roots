package com.rootsrecipes.view.myRecipes.model

data class PaginationState<T>(
    val items: List<T> = emptyList(),
    val currentPage: Int = 1,
    val isLoading: Boolean = false,
    val hasMoreData: Boolean = true,
    val error: String? = null
)