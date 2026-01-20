package com.rootsrecipes.view.discover

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.content.res.ResourcesCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.google.gson.JsonObject
import com.rootsrecipes.R
import com.rootsrecipes.databinding.FragmentRecipesListBinding
import com.rootsrecipes.model.OnItemClickListener
import com.rootsrecipes.model.User
import com.rootsrecipes.utils.BaseFragment
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.Extension
import com.rootsrecipes.utils.SharedPref
import com.rootsrecipes.utils.Status
import com.rootsrecipes.utils.gone
import com.rootsrecipes.utils.visible
import com.rootsrecipes.view.createAccount.model.RecipeData
import com.rootsrecipes.view.setting.adapter.SavedRecipesAdapter
import com.rootsrecipes.view.setting.viewmodel.SettingVM
import com.rootsrecipes.viewmodel.HomeVM
import com.rootsrecipes.viewmodel.ProfileVM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class RecipesListFragment : BaseFragment(), OnItemClickListener {

    private lateinit var binding: FragmentRecipesListBinding
    private var recipeListTypeFrom: Int = 0
    private lateinit var recipeAdapter: SavedRecipesAdapter
    private var bundle: Bundle? = null
    private lateinit var bundleFilter: Bundle
    private val pref: SharedPref by inject()
    private val vm: SettingVM by viewModel()
    private val homeVm: HomeVM by viewModel()
    private val profileVM: ProfileVM by viewModel()
    private var isLoading = false
    private var pageCount = 1
    private var searchKeyword = ""
    private var cuisine = ArrayList<String>()
    private var category = ArrayList<String>()
    private var users = ArrayList<Pair<String, String>>()
    private var limit = 10
    private var debounceJob: Job? = null

    private var userId = ""
    private var userDetails: User? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentRecipesListBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUi()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initUi() {
        if (arguments != null) {
            bundle = arguments
            recipeListTypeFrom = bundle!!.getInt(Constants.recipeListTypeFrom)
            if (recipeListTypeFrom == 11) {
                userDetails = bundle!!.getParcelable(Constants.userInformation)!!
            }
        }

        if (userDetails != null) {
            userId = userDetails!!._id!!
        }

        if (userId.isNotEmpty()) {
            binding.apply {
                clSearch.gone()
                clFilter.gone()
            }
        } else {
            binding.apply {
                clSearch.visible()
                clFilter.visible()
            }
        }

        bundleFilter = Bundle()
        setHeaderMethod()
        setupObserver()
        vm.clearCacheHomePage()
        hitApiHomePageRecipes(false, pageCount)
        setOnClickMethod()
        getFilterItems()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setOnClickMethod() {
        binding.apply {
            ivBackRecipeList.setOnClickListener { findNavController().navigateUp() }
            ivFilterRecipeList.setOnClickListener {
                if (isAdded) {
                    bundleFilter.putStringArrayList("filteredCategory", category)
                    bundleFilter.putStringArrayList("filteredCuisine", cuisine)
                    bundleFilter.putSerializable("filteredPeople", users) // Use Serializable
                    bundleFilter.putInt(Constants.filterTypeForm, 1)
                    findNavController().navigate(
                        R.id.action_recipesListFragment_to_filterFragment, bundleFilter
                    )
                }
            }
            swipeRefreshLayout.setOnRefreshListener {
                vm.clearCacheHomePage()
                pageCount = 1
                isLoading = false
                if (userId.isEmpty()) {
                    vm.homePageRecipesData.removeObservers(this@RecipesListFragment)
                } else {
                    profileVM.userRecipesData.removeObservers(this@RecipesListFragment)
                }
                hitApiHomePageRecipes(true, pageCount)
            }
            val closeButtonImage =
                svRecipesList.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
            val searchIcon =
                svRecipesList.findViewById<ImageView>(androidx.appcompat.R.id.search_mag_icon)
            searchIcon.setImageDrawable(requireActivity().getDrawable(R.drawable.search_icon))
            closeButtonImage.setOnClickListener {
                svRecipesList.setQuery("", false)
                searchKeyword = ""
                vm.clearCacheHomePage()
                pageCount = 1
                isLoading = false
                if (userId.isEmpty()) {
                    vm.homePageRecipesData.removeObservers(this@RecipesListFragment)
                } else {
                    profileVM.userRecipesData.removeObservers(this@RecipesListFragment)
                }
                hitApiHomePageRecipes(true, pageCount)
            }
            svRecipesList.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {

                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    newText?.let {
                        if (it.startsWith(" ")) {
                            binding.svRecipesList.setQuery(it.trimStart(), false)
                        }
                    }
                    // Cancel any ongoing debounce job
                    debounceJob?.cancel()

                    // Start a new debounce job
                    debounceJob = CoroutineScope(Dispatchers.Main).launch {
                        delay(500) // 500ms debounce delay
                        newText?.let {
                            if (it.isNotBlank()) {
                                vm.clearCacheHomePage()
                                searchKeyword = it
                                pageCount = 1
                                if (userId.isEmpty()) {
                                    vm.homePageRecipesData.removeObservers(this@RecipesListFragment)
                                } else {
                                    profileVM.userRecipesData.removeObservers(this@RecipesListFragment)
                                }
                                hitApiHomePageRecipes(true, pageCount)
                            }
                        }
                    }
                    return true
                }


            })
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setHeaderMethod() {
        binding.apply {
            when (recipeListTypeFrom) {
                0 -> {
                    tvHeaderText.text = "All Recipes"
                }

                1 -> {
                    tvHeaderText.text = "Discover Recipes"
                }

                2 -> {
                    tvHeaderText.text = "Breakfast Recipes"
                    category.add("Breakfast")
                }

                3 -> {
                    tvHeaderText.text = "Lunch Recipes"
                    category.add("Lunch")
                }

                4 -> {
                    tvHeaderText.text = "Dinner Recipes"
                    category.add("Dinner")
                }

                5 -> {
                    tvHeaderText.text = "Snacks Recipes"
                    category.add("Snacks")
                }

                6 -> {
                    tvHeaderText.text = "Dessert Recipes"
                    category.add("Dessert")
                }

                7 -> {
                    tvHeaderText.text = "Drink Recipes"
                    category.add("Drink")
                }

                8 -> {
                    tvHeaderText.text = "Saved Recipes"
                }

            }
        }
    }

    private fun setHomePageRecipeAdapter(list: ArrayList<RecipeData>) {
        if (!list.isNullOrEmpty()) {
            recipeAdapter = SavedRecipesAdapter(requireActivity(), list, 0,this)
            binding.rvRecipes.adapter = recipeAdapter

            binding.rvRecipes.setOnScrollChangeListener { v, _, scrollY, _, _ ->
                val recyclerView = v as RecyclerView
                val lastChild = recyclerView.getChildAt(recyclerView.childCount - 1)
                if (lastChild != null) {
                    val diff = lastChild.bottom - (recyclerView.height + scrollY)

                    if (!isLoading && diff <= 0) {
                        loadMoreTransactions()
                    }
                }
            }
        } else {
            binding.tvNoRecipe.visible()
        }
    }

    private fun loadMoreTransactions() {
        isLoading = true
        pageCount++
        Handler(Looper.getMainLooper()).postDelayed({
            Constants.DiscoverRecipesScroll = true
            if (userId.isEmpty()) {
                vm.homePageRecipesData.removeObservers(this@RecipesListFragment)
            } else {
                profileVM.userRecipesData.removeObservers(this@RecipesListFragment)
            }
            hitApiHomePageRecipes(true, pageCount)
        }, 1000)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun getFilterItems() {
        if (findNavController().currentBackStackEntry?.savedStateHandle != null) {
            findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Bundle>("requestKey")
                ?.observe(viewLifecycleOwner) { result ->
                    if (result != null) {
                        category = result.getStringArrayList("filteredCategory")!!
                        cuisine = result.getStringArrayList("filteredCuisine")!!
                        users = result.getSerializable("filteredPeople") as? ArrayList<Pair<String, String>> ?: ArrayList()

                        val newList = (category + cuisine + users.map { it.second }) as ArrayList // Use names for display
                        addChips(newList, binding.cgFilter)
                    }
                }
            updateList()
        }
    }
    @SuppressLint("FragmentLiveDataObserve", "NotifyDataSetChanged")
    private fun hitApiHomePageRecipes(forceRefresh: Boolean, pageCount: Int) {
        if (pageCount == 1) {
            setTopLoaderVisibility(1)
        }
        this.pageCount = pageCount

        if (userId.isEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                vm.homePageRecipes(
                    Extension.arrayListToCommaSeparatedString(users.map { it.first } as ArrayList), // Use IDs for API
                    searchKeyword,
                    Extension.arrayListToCommaSeparatedString(category),
                    Extension.arrayListToCommaSeparatedString(cuisine),
                    pageCount,
                    limit,
                    forceRefresh
                )
                vm.homePageRecipesData.observe(this@RecipesListFragment) { its ->
                    when (its.status) {
                        Status.SUCCESS -> {
                            Extension.stopProgress()
                            setTopLoaderVisibility(0)
                            binding.swipeRefreshLayout.isRefreshing = false
                            Log.d("ObserverValueHit", pageCount.toString())
                            if (pageCount == 1) {
                                isLoading = false
                                if (its.data!!.data.isEmpty()) {
                                    if (::recipeAdapter.isInitialized) {
                                        recipeAdapter.getAddList().clear()
                                        recipeAdapter.notifyDataSetChanged()
                                    }
                                    binding.tvNoRecipe.visible()
                                } else {
                                    its.data.let { setHomePageRecipeAdapter(it.data) }
                                    binding.tvNoRecipe.gone()
                                }

                            } else {
                                if (::recipeAdapter.isInitialized) {
                                    isLoading = false
                                    if (its.data != null) {
                                        recipeAdapter.getAddList().addAll(its.data.data)
                                        recipeAdapter.notifyDataSetChanged()
                                        binding.tvNoRecipe.gone()
                                    }

                                } else {
                                    isLoading = false
                                    its.data?.let { setHomePageRecipeAdapter(it.data) }
                                    binding.tvNoRecipe.gone()

                                }
                            }
                            if(cuisine.isEmpty() && category.isEmpty() && users.isEmpty()){
                                binding.ivFilterRecipeList.setImageDrawable(requireActivity().getDrawable(R.drawable.filter_icon))
                            }else {
                                binding.ivFilterRecipeList.setImageDrawable(requireActivity().getDrawable(R.drawable.filter_selected))
                            }

                        }

                        Status.ERROR -> {
                            Extension.stopProgress()
                            setTopLoaderVisibility(0)
                            binding.swipeRefreshLayout.isRefreshing = false
                        }

                        Status.LOADING -> {

                        }
                    }
                }
            }
        } else {
            CoroutineScope(Dispatchers.Main).launch {
                profileVM.userRecipes(
                    userId, pageCount, limit
                )
                profileVM.userRecipesData.observe(this@RecipesListFragment) { its ->
                    when (its.status) {
                        Status.SUCCESS -> {
                            val recipesTemp = ArrayList<RecipeData>()
                            userDetails = its.data!!.data.user
                            for (i in its.data.data.recipes) {
                                i.user = userDetails
                                recipesTemp.add(i)
                            }

                            Extension.stopProgress()
                            setTopLoaderVisibility(0)
                            binding.swipeRefreshLayout.isRefreshing = false
                            Log.d("ObserverValueHit", pageCount.toString())
                            if (pageCount == 1) {
                                isLoading = false
                                if (its.data.data.recipes.isEmpty()) {
                                    if (::recipeAdapter.isInitialized) {
                                        recipeAdapter.getAddList().clear()
                                        recipeAdapter.notifyDataSetChanged()
                                    }
                                    binding.tvNoRecipe.visible()
                                } else {
                                    its.data.let { setHomePageRecipeAdapter(recipesTemp) }
                                    binding.tvNoRecipe.gone()
                                }

                            } else {
                                if (::recipeAdapter.isInitialized) {
                                    isLoading = false
                                    if (its.data != null) {
                                        recipeAdapter.getAddList().addAll(recipesTemp)
                                        recipeAdapter.notifyDataSetChanged()
                                        binding.tvNoRecipe.gone()
                                    }

                                } else {
                                    isLoading = false
                                    its.data.let { setHomePageRecipeAdapter(recipesTemp) }
                                    binding.tvNoRecipe.gone()

                                }
                            }


                        }

                        Status.ERROR -> {
                            Extension.stopProgress()
                            setTopLoaderVisibility(0)
                            binding.swipeRefreshLayout.isRefreshing = false
                        }

                        Status.LOADING -> {

                        }
                    }
                }
            }
        }
    }

    @SuppressLint("FragmentLiveDataObserve", "NotifyDataSetChanged")
    private fun setupObserver() {

        if (userId.isEmpty()) {
            vm.homePageRecipesData.observe(this@RecipesListFragment) { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        Extension.stopProgress()
                        setTopLoaderVisibility(0)
                        if (this@RecipesListFragment.pageCount == 1) {
                            isLoading = false
                            if (resource.data!!.data.isEmpty()) {
                                if (::recipeAdapter.isInitialized) {
                                    recipeAdapter.getAddList().clear()
                                    recipeAdapter.notifyDataSetChanged()
                                }
                                binding.tvNoRecipe.visible()
                            } else {
                                setHomePageRecipeAdapter(resource.data.data)
                                binding.tvNoRecipe.gone()
                            }

                        } else {
                            if (::recipeAdapter.isInitialized) {
                                isLoading = false
                                if (resource.data != null) {
                                    recipeAdapter.getAddList().addAll(resource.data.data)
                                    recipeAdapter.notifyDataSetChanged()
                                    binding.tvNoRecipe.gone()
                                }
                            } else {
                                isLoading = false
                                resource.data?.let { setHomePageRecipeAdapter(it.data) }
                                binding.tvNoRecipe.gone()

                            }
                        }

                    }

                    Status.ERROR -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        Extension.stopProgress()
                        setTopLoaderVisibility(0)
                    }

                    Status.LOADING -> {
                        // Loading state handled above
                    }
                }
            }
        } else {
            profileVM.userRecipesData.observe(this@RecipesListFragment) { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        val recipesTemp = ArrayList<RecipeData>()
                        userDetails = resource.data!!.data.user
                        for (i in resource.data.data.recipes) {
                            i.user = userDetails
                            recipesTemp.add(i)
                        }

                        binding.swipeRefreshLayout.isRefreshing = false
                        Extension.stopProgress()
                        setTopLoaderVisibility(0)
                        if (this@RecipesListFragment.pageCount == 1) {
                            isLoading = false
                            if (resource.data.data.recipes.isEmpty()) {
                                if (::recipeAdapter.isInitialized) {
                                    recipeAdapter.getAddList().clear()
                                    recipeAdapter.notifyDataSetChanged()
                                }
                                binding.tvNoRecipe.visible()
                            } else {
                                setHomePageRecipeAdapter(recipesTemp)
                                binding.tvNoRecipe.gone()
                            }

                        } else {
                            if (::recipeAdapter.isInitialized) {
                                isLoading = false
                                if (resource.data != null) {
                                    recipeAdapter.getAddList().addAll(recipesTemp)
                                    recipeAdapter.notifyDataSetChanged()
                                    binding.tvNoRecipe.gone()
                                }
                            } else {
                                isLoading = false
                                setHomePageRecipeAdapter(recipesTemp)
                                binding.tvNoRecipe.gone()

                            }
                        }

                    }

                    Status.ERROR -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        Extension.stopProgress()
                        setTopLoaderVisibility(0)
                    }

                    Status.LOADING -> {
                        // Loading state handled above
                    }
                }
            }
        }

    }

    // Update addChips to handle Pair removal
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun addChips(list: ArrayList<String>, chipGroup: ChipGroup) {
        chipGroup.removeAllViews()
        chipGroup.requestLayout()

        if (list.isNotEmpty()) {
            for (i in list.indices) {
                val linearLayout = LinearLayout(requireActivity())
                linearLayout.gravity = Gravity.CENTER_VERTICAL

                val textview = TextView(requireActivity())
                val imageView = ImageView(requireActivity())
                textview.text = list[i]
                // ... chip styling ...
                linearLayout.setBackgroundResource(R.drawable.chip_bg)
                linearLayout.backgroundTintList =
                    ColorStateList.valueOf(requireActivity().getColor(R.color.category_color))
                textview.setTextColor(requireActivity().getColor(R.color.black))
                textview.textSize = 12f
                val typeface = ResourcesCompat.getFont(requireContext(), R.font.work_sans)
                textview.typeface = typeface
                textview.setPadding(20, 15, 18, 17)
                linearLayout.setPadding(20, 0, 20, 0)

                imageView.setImageDrawable(requireContext().getDrawable(R.drawable.close_button_white))

                linearLayout.addView(textview)
                linearLayout.addView(imageView)
                chipGroup.addView(linearLayout)

                imageView.setOnClickListener {
                    val textToRemove = textview.text.toString()
                    category.removeAll { it == textToRemove }
                    cuisine.removeAll { it == textToRemove }
                    users.removeAll { it.second == textToRemove } // Remove based on name
                    chipGroup.removeView(linearLayout)
                    updateList()
                }
            }
            chipGroup.chipSpacingVertical = 40
            chipGroup.chipSpacingHorizontal = 30
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun updateList() {
        vm.clearCacheHomePage()
        pageCount = 1
        isLoading = false
        if (userId.isEmpty()) {
            vm.homePageRecipesData.removeObservers(this@RecipesListFragment)
        } else {
            profileVM.userRecipesData.removeObservers(this@RecipesListFragment)
        }



        hitApiHomePageRecipes(false, pageCount)
    }

    private fun setTopLoaderVisibility(value: Int) {
        binding.topProgressBar.apply {
            if (value == 0) {
                gone()
            } else if (value == 1) {
                visible()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onItemClick(position: Int, type: String) {
        when (type) {
            Constants.recipeInformation -> {
                val bundle = Bundle()
                if (recipeAdapter.getAddList()[position].user != null && recipeAdapter.getAddList()[position].user!!._id == pref.getSignInData()!!._id) {
                    bundle.putInt(Constants.typeFrom, 2)
                } else {
                    bundle.putInt(Constants.typeFrom, 1)
                }
                bundle.putParcelable(
                    Constants.recipeInformation, recipeAdapter.getAddList()[position]
                )
                findNavController().navigate(
                    R.id.action_recipesListFragment_to_recipeInformationFragment, bundle
                )
            }

            Constants.saveRecipe -> {
                if (recipeAdapter.getAddList()[position].isSaved!!) {
                    recipeAdapter.getAddList()[position].isSaved = false
                    hitApiForUnSaveRecipe(recipeAdapter.getAddList()[position]._id!!, position)
                    recipeAdapter.notifyItemChanged(position)
                } else {
                    recipeAdapter.getAddList()[position].isSaved = true
                    hitApiForSaveRecipe(recipeAdapter.getAddList()[position]._id!!, position)
                    recipeAdapter.notifyItemChanged(position)
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun hitApiForSaveRecipe(recipeId: String, position: Int) {
        val jsonObject = JsonObject()
        jsonObject.addProperty("recipe_id", recipeId)
        CoroutineScope(Dispatchers.Main).launch {
            if (isAdded){
                vm.saveRecipe(jsonObject)
                vm.saveRecipeData.observe(this@RecipesListFragment) { its ->
                    when (its.status) {
                        Status.SUCCESS -> {
                            recipeAdapter.getAddList()[position].isSaved = true
                            recipeAdapter.notifyItemChanged(position)
                            updateHomeData(position)
                        }

                        Status.ERROR -> {

                        }

                        Status.LOADING -> {

                        }
                    }
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun hitApiForUnSaveRecipe(recipeId: String, position: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            vm.unSaveRecipe(recipeId)
            vm.unSaveRecipeData.observe(this@RecipesListFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        recipeAdapter.getAddList()[position].isSaved = false
                        recipeAdapter.notifyItemChanged(position)
                        updateHomeData(position)
                    }

                    Status.ERROR -> {

                    }

                    Status.LOADING -> {

                    }
                }
            }
        }
    }

    @SuppressLint("FragmentLiveDataObserve")
    private fun updateHomeData(position: Int) {
        homeVm.homeAllData.observe(this@RecipesListFragment) {
            it.data!!.data!!.map { recipes ->
                recipes.recipes!!.map { recipeData ->
                    if (recipeData._id == recipeAdapter.getAddList()[position]._id) {
                        recipeData.isSaved = recipeAdapter.getAddList()[position].isSaved
                        recipeData.user!!.followStatus =
                            recipeAdapter.getAddList()[position].user!!.followStatus
                    }
                }
            }
        }
    }
}