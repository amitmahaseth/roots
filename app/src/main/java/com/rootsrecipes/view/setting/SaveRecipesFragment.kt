package com.rootsrecipes.view.setting

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.rootsrecipes.R
import com.rootsrecipes.databinding.FragmentSaveRecipesBinding
import com.rootsrecipes.model.OnItemClickListener
import com.rootsrecipes.utils.BaseFragment
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.Extension
import com.rootsrecipes.utils.SharedPref
import com.rootsrecipes.utils.Status
import com.rootsrecipes.utils.gone
import com.rootsrecipes.utils.makeToast
import com.rootsrecipes.utils.visible
import com.rootsrecipes.view.createAccount.model.RecipeData
import com.rootsrecipes.view.setting.adapter.SavedRecipesAdapter
import com.rootsrecipes.view.setting.viewmodel.SettingVM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel


class SaveRecipesFragment : BaseFragment(), OnItemClickListener {

    private var savedTypeForm: Int = 0
    private lateinit var binding: FragmentSaveRecipesBinding
    private lateinit var savedRecipeAdapter: SavedRecipesAdapter
    private var bundle: Bundle? = null
    private lateinit var bundleFilter: Bundle
    private val pref: SharedPref by inject()
    private val vm: SettingVM by viewModel()
    private var isLoading = false
    private var pageCount = 1
    private var searchKeyword = ""
    private var cuisine = ArrayList<String>()
    private var category = ArrayList<String>()
    private var limit = 10
    private var debounceJob: Job? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentSaveRecipesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (arguments != null) {
            bundle = arguments
            savedTypeForm = bundle!!.getInt(Constants.savedTypeForm)
        }
        bundleFilter = Bundle()
        initUi()
        getFilterItems()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun getFilterItems() {

        if (findNavController().currentBackStackEntry?.savedStateHandle != null) {
            findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Bundle>("requestKey")
                ?.observe(viewLifecycleOwner) { result ->
                    if (result != null) {
                        category = result.getStringArrayList("filteredCategory")!!
                        cuisine = result.getStringArrayList("filteredCuisine")!!
                    }
                }
            updateList()
        }

    }
    private fun updateList() {
        vm.clearCacheSaved()
        vm.clearCacheMySaved()
        pageCount = 1
        isLoading = false
        vm.myRecipesSavedData.removeObservers(this@SaveRecipesFragment)
        vm.userSavedRecipesData.removeObservers(this@SaveRecipesFragment)
        hitApiMyRecipes(true, pageCount)
        if (cuisine.isNotEmpty() || category.isNotEmpty()) {
            binding.ivFilter.setImageDrawable(requireActivity().getDrawable(R.drawable.filter_selected))
        } else {
            binding.ivFilter.setImageDrawable(requireActivity().getDrawable(R.drawable.filter_icon))
        }
    }
    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    private fun initUi() {

        if (savedTypeForm == 0) {
            //save
            binding.apply {
                tvSaveRecipes.text = requireActivity().getString(R.string.save_recipes)
             //   clFilter.visible()
            }
        } else if (savedTypeForm == 1) {
            //search
            binding.apply {
                tvSaveRecipes.text = "My Recipes"
           //     clFilter.gone()
            }
        }

        setupObserver()

        if (Constants.SavedRecipesScroll) {
            Constants.SavedRecipesScroll = false
            pageCount = 1
            isLoading = false
            vm.myRecipesSavedData.removeObservers(this@SaveRecipesFragment)
            vm.userSavedRecipesData.removeObservers(this@SaveRecipesFragment)
            hitApiMyRecipes(true, pageCount)
        } else {
            hitApiMyRecipes(false, pageCount)
        }

        binding.apply {
            clFilter.setOnClickListener {
                if (isAdded) {
                    bundleFilter.putStringArrayList("filteredCategory", category)
                    bundleFilter.putStringArrayList("filteredCuisine", cuisine)
                    //bundleFilter.putInt(Constants.filterTypeForm, if(savedTypeForm == 0) 0 else 2)
                    bundleFilter.putInt(Constants.filterTypeForm,if(savedTypeForm == 0) 0 else 2)
                    findNavController().navigate(
                        R.id.action_saveRecipesFragment_to_filterFragment,
                        bundleFilter
                    )
                }
            }
            ivBackSaveRecipes.setOnClickListener {
                findNavController().popBackStack()
            }
            swipeRefreshLayout.setOnRefreshListener {
                vm.clearCacheSaved()
                vm.clearCacheMySaved()
                pageCount = 1
                isLoading = false
                vm.myRecipesSavedData.removeObservers(this@SaveRecipesFragment)
                vm.userSavedRecipesData.removeObservers(this@SaveRecipesFragment)
                hitApiMyRecipes(true, pageCount)
            }
            val closeButtonImage =
                svSaveRecipes.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
            val searchIcon =
                svSaveRecipes.findViewById<ImageView>(androidx.appcompat.R.id.search_mag_icon)
            searchIcon.setImageDrawable(requireActivity().getDrawable(R.drawable.search_icon))
            closeButtonImage.setOnClickListener {
                svSaveRecipes.setQuery("", false)
                searchKeyword = ""
                vm.clearCacheSaved()
                pageCount = 1
                isLoading = false
                vm.myRecipesSavedData.removeObservers(this@SaveRecipesFragment)
                vm.userSavedRecipesData.removeObservers(this@SaveRecipesFragment)
                hitApiMyRecipes(true, pageCount)
            }
            svSaveRecipes.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {

                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    newText?.let {
                        if (it.startsWith(" ")) {
                            svSaveRecipes.setQuery(it.trimStart(), false)
                        }
                    }
                    // Cancel any ongoing debounce job
                    debounceJob?.cancel()

                    // Start a new debounce job
                    debounceJob = CoroutineScope(Dispatchers.Main).launch {
                        delay(300) // 500ms debounce delay
                        newText?.let {
                            if (it.isNotBlank()) {
                                vm.clearCacheSaved()
                                vm.clearCacheMySaved()
                                searchKeyword = it
                                pageCount = 1
                                isLoading = false
                                vm.userSavedRecipesData.removeObservers(this@SaveRecipesFragment)
                                vm.myRecipesSavedData.removeObservers(this@SaveRecipesFragment)
                                hitApiMyRecipes(true, pageCount)
                            }
                        }
                    }
                    return false
                }


            })

        }
    }


    private fun setSavedRecipeAdapter(list: ArrayList<RecipeData>) {
//        savedRecipeAdapter = SavedRecipesAdapter(requireActivity(), list,this)
//        binding.rvRecipes.adapter = savedRecipeAdapter
        if (!list.isNullOrEmpty()) {
            savedRecipeAdapter = SavedRecipesAdapter(requireActivity(), list, savedTypeForm, this)
            binding.rvRecipes.adapter = savedRecipeAdapter

            binding.rvRecipes.setOnScrollChangeListener { v, _, scrollY, _, _ ->
                val recyclerView = v as RecyclerView
                val lastChild = recyclerView.getChildAt(recyclerView.childCount - 1)
                if (lastChild != null && lastChild.bottom != null) {
                    val diff = lastChild.bottom - (recyclerView.height + scrollY)

                    if (!isLoading && diff <= 0) {
                        loadMoreRecipes()
                    }
                }
            }

        }
    }

    override fun onItemClick(position: Int, type: String) {
        when (type) {
            Constants.recipeInformation -> {

                val bundle = Bundle()
                if (savedRecipeAdapter.getAddList()[position].user_id == pref.getSignInData()!!._id) {
                    bundle.putInt(Constants.typeFrom, 2)
                } else {
                    bundle.putInt(Constants.typeFrom, 1)
                }
                bundle.putParcelable(
                    Constants.recipeInformation,
                    savedRecipeAdapter.getAddList()[position]
                )
                findNavController().navigate(
                    R.id.action_saveRecipesFragment_to_recipeInformationFragment, bundle
                )
            }

            Constants.saveRecipe -> {
                hitApiForUnSaveRecipe(position)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun hitApiForUnSaveRecipe(position: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            vm.unSaveRecipe(savedRecipeAdapter.getAddList()[position]._id!!)
            vm.unSaveRecipeData.observe(this@SaveRecipesFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        if (savedRecipeAdapter.getAddList().size > position) {
                            savedRecipeAdapter.getAddList().removeAt(position)
                            savedRecipeAdapter.notifyDataSetChanged()
                        }

                        if (savedRecipeAdapter.getAddList().isEmpty()) {
                            binding.clNoRecipe.visible()
                        }
                    }

                    Status.ERROR -> {

                    }

                    Status.LOADING -> {

                    }
                }
            }
        }
    }

    private fun loadMoreRecipes() {
        isLoading = true
        pageCount++
        Handler(Looper.getMainLooper()).postDelayed({
            Constants.SavedRecipesScroll = true
            vm.myRecipesSavedData.removeObservers(this@SaveRecipesFragment)
            vm.userSavedRecipesData.removeObservers(this@SaveRecipesFragment)
            hitApiMyRecipes(true, pageCount)
        }, 1000)
    }

    @SuppressLint("FragmentLiveDataObserve", "NotifyDataSetChanged")
    private fun hitApiMyRecipes(forceRefresh: Boolean, pageCount: Int) {
        if (savedTypeForm == 0) {
            this.pageCount = pageCount
            CoroutineScope(Dispatchers.Main).launch {
                vm.userSavedRecipes(
                    pageCount,
                    limit,
                    searchKeyword,
                    Extension.arrayListToCommaSeparatedString(category),
                    Extension.arrayListToCommaSeparatedString(cuisine),
                    forceRefresh
                )
                vm.userSavedRecipesData.observe(this@SaveRecipesFragment) { its ->
                    when (its.status) {
                        Status.SUCCESS -> {
                            Extension.stopProgress()
                            binding.clNoRecipe.gone()
                            binding.swipeRefreshLayout.isRefreshing = false
                            Log.d("ObserverValueHit", pageCount.toString())
                            if (pageCount == 1) {
                                isLoading = false
                                its.data?.let {
                                    if (it.data.isEmpty()) {
                                        binding.clNoRecipe.visible()
                                    } else {
                                        binding.clNoRecipe.gone()
                                        setSavedRecipeAdapter(it.data)
                                    }
                                }
                            } else {
                                if (::savedRecipeAdapter.isInitialized) {
                                    isLoading = false
                                    if (its.data != null) {
                                        savedRecipeAdapter.getAddList().addAll(its.data.data)
                                        savedRecipeAdapter.notifyDataSetChanged()
                                    }
                                } else {
                                    isLoading = false
                                    its.data?.let { setSavedRecipeAdapter(it.data) }
                                }
                            }


                        }

                        Status.ERROR -> {
                            Extension.stopProgress()
                            binding.swipeRefreshLayout.isRefreshing = false
                            its.message?.let { requireActivity().makeToast(it) }
                        }

                        Status.LOADING -> {

                        }
                    }
                }
            }
        } else if (savedTypeForm == 1) {
            this.pageCount = pageCount
            CoroutineScope(Dispatchers.Main).launch {
                vm.myRecipesSaved(
                    searchKeyword,
                    Extension.arrayListToCommaSeparatedString(category),
                    Extension.arrayListToCommaSeparatedString(cuisine),
                    pageCount,
                    limit,
                    forceRefresh
                )
                vm.myRecipesSavedData.observe(this@SaveRecipesFragment) { its ->
                    when (its.status) {
                        Status.SUCCESS -> {
                            Extension.stopProgress()
                            binding.clNoRecipe.gone()
                            binding.swipeRefreshLayout.isRefreshing = false
                            Log.d("ObserverValueHit", pageCount.toString())
                            if (pageCount == 1) {
                                isLoading = false
                                its.data?.let { setSavedRecipeAdapter(it.data) }
                            } else {
                                if (::savedRecipeAdapter.isInitialized) {
                                    isLoading = false
                                    if (its.data != null) {
                                        savedRecipeAdapter.getAddList().addAll(its.data.data)
                                        savedRecipeAdapter.notifyDataSetChanged()
                                    }

                                } else {
                                    isLoading = false
                                    its.data?.let { setSavedRecipeAdapter(it.data) }
                                }
                            }


                        }

                        Status.ERROR -> {
                            Extension.stopProgress()
                            binding.swipeRefreshLayout.isRefreshing = false
                            its.message?.let { requireActivity().makeToast(it) }
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
        /**My recipes data observer**/
        if (savedTypeForm == 0) {
            vm.userSavedRecipesData.observe(this@SaveRecipesFragment) { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        Extension.stopProgress()
                        binding.clNoRecipe.gone()
                        if (this@SaveRecipesFragment.pageCount == 1) {
                            isLoading = false
                            resource.data?.let { setSavedRecipeAdapter(it.data) }
                        } else {
                            if (::savedRecipeAdapter.isInitialized) {
                                isLoading = false
                                if (resource.data != null) {
                                    savedRecipeAdapter.getAddList().addAll(resource.data.data)
                                    savedRecipeAdapter.notifyDataSetChanged()
                                }
                            } else {
                                isLoading = false
                                resource.data?.let { setSavedRecipeAdapter(it.data) }
                            }
                        }

                    }

                    Status.ERROR -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        Extension.stopProgress()
                        resource.message?.let { requireActivity().makeToast(it) }
                    }

                    Status.LOADING -> {
                        // Loading state handled above
                    }
                }
            }
        } else if (savedTypeForm == 1) {
            vm.myRecipesSavedData.observe(this@SaveRecipesFragment) { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        Extension.stopProgress()
                        binding.clNoRecipe.gone()
                        if (this@SaveRecipesFragment.pageCount == 1) {
                            isLoading = false
                            resource.data?.let { setSavedRecipeAdapter(it.data) }
                        } else {
                            if (::savedRecipeAdapter.isInitialized) {
                                isLoading = false
                                if (resource.data != null) {
                                    savedRecipeAdapter.getAddList().addAll(resource.data.data)
                                    savedRecipeAdapter.notifyDataSetChanged()
                                }
                            } else {
                                isLoading = false
                                resource.data?.let { setSavedRecipeAdapter(it.data) }
                            }
                        }

                    }

                    Status.ERROR -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        Extension.stopProgress()
                        resource.message?.let { requireActivity().makeToast(it) }
                    }

                    Status.LOADING -> {
                        // Loading state handled above
                    }
                }
            }
        }

    }
}