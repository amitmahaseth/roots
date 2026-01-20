package com.rootsrecipes.view.discover

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.google.gson.JsonObject
import com.rootsrecipes.MainActivity
import com.rootsrecipes.MainActivity.Companion.s3client
import com.rootsrecipes.R
import com.rootsrecipes.databinding.FragmentDiscoverBinding
import com.rootsrecipes.model.AllHomeRecipeData
import com.rootsrecipes.model.OnClickListener
import com.rootsrecipes.model.OnItemClickListener
import com.rootsrecipes.model.User
import com.rootsrecipes.utils.AWSSharedPref
import com.rootsrecipes.utils.BaseFragment
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.Extension
import com.rootsrecipes.utils.SharedPref
import com.rootsrecipes.utils.Status
import com.rootsrecipes.utils.gone
import com.rootsrecipes.utils.makeToast
import com.rootsrecipes.utils.visible
import com.rootsrecipes.view.createAccount.model.RecipeData
import com.rootsrecipes.view.discover.adapter.BreakFastAdapter
import com.rootsrecipes.view.discover.adapter.CategoryAdapter
import com.rootsrecipes.view.discover.adapter.DiscoverRecipesAdapter
import com.rootsrecipes.view.discover.model.CategoryItemData
import com.rootsrecipes.view.setting.viewmodel.SettingVM
import com.rootsrecipes.viewmodel.HomeVM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel


class DiscoverFragment : BaseFragment(), OnClickListener {

    private var allHomeData: ArrayList<AllHomeRecipeData>? = null
    private var categoryDataList: ArrayList<CategoryItemData>? = null
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var discoverRecipesAdapter: DiscoverRecipesAdapter
    private lateinit var breakFastAdapter: BreakFastAdapter
    private lateinit var lunchAdapter: BreakFastAdapter
    private lateinit var dinnerAdapter: BreakFastAdapter
    private lateinit var snacksAdapter: BreakFastAdapter
    private lateinit var dessertAdapter: BreakFastAdapter
    private lateinit var drinksAdapter: BreakFastAdapter
    private lateinit var savedRecipesAdapter: BreakFastAdapter
    private lateinit var binding: FragmentDiscoverBinding
    private lateinit var bundle: Bundle
    private val pref: SharedPref by inject()
    private val awsPref: AWSSharedPref by inject()
    private val homeVm: HomeVM by viewModel()
    private val settingVM: SettingVM by viewModel()
    private lateinit var credentials: BasicAWSCredentials
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun updateOnlineStatus(isLogin : Boolean){
        CoroutineScope(Dispatchers.Main).launch {
            settingVM.updateUserActiveStatus(pref.getSignInData()!!._id!!,isLogin)
        }
    }

    private fun startNotifications(){
        settingVM.startNotifications()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bundle = Bundle()
        initUi()
        fetchToken()
        pref.saveBoolean(Constants.isLogin, true)
    }

    private fun fetchToken(){
        if(pref.getString(Constants.FCM_TOKEN).isNullOrEmpty()) {
            settingVM.fetchTokenMethod { token ->
                if (token != null) {
                    pref.saveString(Constants.FCM_TOKEN, token)
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        if (Constants.recipeShare.isNotEmpty()) {
            gotoRecipeInformation(
                RecipeData(
                    _id = Constants.recipeShare,
                    user_id = Constants.recipeUserIdShare
                )
            )
            Constants.recipeShare = ""
            Constants.recipeUserIdShare = ""
        }

        if (Constants.userIdShare.isNotEmpty()) {
            if (pref.getSignInData()!!._id == Constants.userIdShare) {
                //gotomyrecipe
                findNavController().navigate(
                    R.id.action_discoverFragment_to_myRecipesFragment
                )
            } else {
                val bundle = Bundle()
                bundle.putParcelable(Constants.userInformation, User(_id = Constants.userIdShare))
                findNavController().navigate(
                    R.id.action_discoverFragment_to_userProfileFragment, bundle
                )
            }
            Constants.userIdShare = ""
        }
        startNotifications()
        updateOnlineStatus(true)
    }

    private fun initUi() {
        setupObserver()
        hitApiForHomeRecipes(false)
        hitApiForUserSavedRecipes(false)
        if (awsPref.fetchString(Constants.access_key).isNullOrEmpty() || awsPref.fetchString(
                Constants.aws_bucket_name
            ).isNullOrEmpty() || awsPref.fetchString(Constants.secret_key)
                .isNullOrEmpty() || awsPref.fetchString(Constants.aws_region).isNullOrEmpty()
        ) {
            getAwsCredential()
        } else {
            initializedAws()
        }

    }

    @SuppressLint("FragmentLiveDataObserve")
    private fun setupObserver() {
        homeVm.homeAllData.observe(this@DiscoverFragment) { its ->
            when (its.status) {
                Status.SUCCESS -> {
                    binding.swipeRefreshHome.isRefreshing = false
                    its.data.let {
                        it?.let { it1 ->
                            allHomeData = null
                            allHomeData = it1.data
                            if (allHomeData != null) {
                                setAdapterData()
                            }
                        }
                    }
                }

                Status.ERROR -> {
                    its.message?.let { requireActivity().makeToast(it) }
                    binding.swipeRefreshHome.isRefreshing = false
                    binding.apply {
                        clSaveRecipes.gone()
                        rvSaveRecipes.gone()
                    }
                    Extension.stopProgress()
                }

                Status.LOADING -> {
                    binding.swipeRefreshHome.isRefreshing = false
                    binding.apply {
                        clSaveRecipes.gone()
                        rvSaveRecipes.gone()
                    }
                }
            }
        }
        settingVM.userSavedRecipesData.observe(this@DiscoverFragment) { its ->
            when (its.status) {
                Status.SUCCESS -> {
                    binding.swipeRefreshHome.isRefreshing = false
                    its.data.let {
                        it?.let { it1 ->
                            if (it1.data.isEmpty()) {
                                binding.apply {
                                    clSaveRecipes.gone()
                                    rvSaveRecipes.gone()
                                }
                            } else {
                                saveRecipeAdapter(it1.data)
                            }
                        }
                    }
                }

                Status.ERROR -> {
                    binding.swipeRefreshHome.isRefreshing = false
                    Extension.stopProgress()
                }

                Status.LOADING -> {
                    binding.swipeRefreshHome.isRefreshing = false
                }
            }
        }
        homeVm.navigateToLogin.observe(this@DiscoverFragment) { shouldNavigate ->
            if (shouldNavigate) {
                pref.clearPreference(requireActivity())
                homeVm.navigateToLogin.removeObservers(this@DiscoverFragment)
                val intent = Intent(requireActivity(), MainActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
            }
        }
    }

    private fun setAdapterData() {
        binding.tvNoRecipesFound.gone()
        binding.clDiscoverMain.visible()
        setCategoryAdapter()
        if (!allHomeData.isNullOrEmpty()) {
            for (i in allHomeData!!.indices) {
                when (allHomeData!![i].category) {
                    Constants.DISCOVER -> {
                        setDiscoverRecipesAdapter(allHomeData!![i].recipes!!)
                    }

                    Constants.BREAKFAST -> {
                        setBreakFastAdapter(allHomeData!![i].recipes!!)
                    }

                    Constants.LUNCH -> {
                        setLunchAdapter(allHomeData!![i].recipes!!)
                    }

                    Constants.DINNER -> {
                        setDinnerAdapter(allHomeData!![i].recipes!!)
                    }

                    Constants.SNACKS -> {
                        setSnacksAdapter(allHomeData!![i].recipes!!)
                    }

                    Constants.DESSERTS -> {
                        setDessertAdapter(allHomeData!![i].recipes!!)
                    }

                    Constants.DRINK -> {
                        setDrinkAdapter(allHomeData!![i].recipes!!)
                    }
                }
            }
        }
        setOnClickMethod()
    }

    @SuppressLint("FragmentLiveDataObserve")
    private fun hitApiForHomeRecipes(forceRefresh: Boolean) {
        if (forceRefresh) {
            Extension.showProgress(requireActivity())
        } else if (Constants.onDiscover) {
            if (allHomeData.isNullOrEmpty()) {
                Extension.showProgress(requireActivity())
            }
        }
        CoroutineScope(Dispatchers.Main).launch {
            binding.swipeRefreshHome.isRefreshing = false
            homeVm.getHomeRecipesData(forceRefresh)
            homeVm.homeAllData.observe(this@DiscoverFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        its.data.let {
                            it?.let { it1 ->
                                allHomeData = null
                                allHomeData = it1.data
                                Constants.onDiscover = true
                                if (allHomeData.isNullOrEmpty()) {
                                    noVisibleList()
                                } else {
                                    setAdapterData()
                                }
                            }
                        }
                        Extension.stopProgress()
                    }

                    Status.ERROR -> {
                        its.message?.let { requireActivity().makeToast(it) }
                        Extension.stopProgress()
                    }

                    Status.LOADING -> {}
                }
            }
        }
    }

    private fun noVisibleList() {
        binding.apply {
            clDiscoverRecipes.gone()
            rvDiscoverRecipes.gone()
            clBreakFastRecipes.gone()
            rvBreakFastRecipes.gone()
            clLunchRecipes.gone()
            rvLunchRecipes.gone()
            clDinnerRecipes.gone()
            rvDinnerRecipes.gone()
            clSnacksRecipes.gone()
            rvSnacksRecipes.gone()
            clDessertsRecipes.gone()
            rvDessertsRecipes.gone()
            clDrinksRecipes.gone()
            rvDrinksRecipes.gone()
            tvNoRecipesFound.visible()
        }
    }

    @SuppressLint("FragmentLiveDataObserve")
    private fun hitApiForUserSavedRecipes(forceRefresh: Boolean) {
        CoroutineScope(Dispatchers.Main).launch {
            binding.swipeRefreshHome.isRefreshing = false
            settingVM.userSavedRecipes(1, 4, "", "", "", forceRefresh)
            settingVM.userSavedRecipesData.observe(this@DiscoverFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        its.data.let {
                            it?.let { it1 ->
                                if (it1.data.isEmpty()) {
                                    binding.apply {
                                        clSaveRecipes.gone()
                                        rvSaveRecipes.gone()
                                    }
                                } else {
                                    saveRecipeAdapter(it1.data)
                                }
                            }
                        }
                    }

                    Status.ERROR -> {
                        binding.apply {
                            clSaveRecipes.gone()
                            rvSaveRecipes.gone()
                        }

                    }

                    Status.LOADING -> {
                        binding.apply {
                            clSaveRecipes.gone()
                            rvSaveRecipes.gone()
                        }
                    }
                }
            }
        }
    }


    @SuppressLint("FragmentLiveDataObserve")
    private fun getAwsCredential() {
        Extension.showProgress(requireActivity())
        CoroutineScope(Dispatchers.Main).launch {
            homeVm.getAwsCredential()
            homeVm.awsCredentialData.observe(this@DiscoverFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        Extension.stopProgress()
                        if (its.data?.data != null) {
                            its.data.data.let {
                                it.let { it1 ->
                                    awsPref.saveString(Constants.access_key, it1.access_key)
                                    awsPref.saveString(Constants.secret_key, it1.secret_key)
                                    awsPref.saveString(Constants.aws_region, it1.aws_region)
                                    awsPref.saveString(
                                        Constants.aws_bucket_name, it1.aws_bucket_name
                                    )
                                }
                                initializedAws()
                            }
                        }
                    }

                    Status.ERROR -> {
                        Extension.stopProgress()
                    }

                    Status.LOADING -> {

                    }
                }
            }
        }
    }

    private fun initializedAws() {
        val accessKey = awsPref.fetchString(Constants.access_key)
        val secretKey = awsPref.fetchString(Constants.secret_key)
        /* val bucketName = awsPref.fetchString(Constants.aws_bucket_name)
         val awsRegion = awsPref.fetchString(Constants.aws_region)*/

        Log.d("initializedAWSDebug", "$accessKey")
        Log.d("initializedAWSDebug", "$secretKey")

        credentials = BasicAWSCredentials(accessKey, secretKey)
        s3client = AmazonS3Client(credentials)
    }

    private fun setOnClickMethod() {
        binding.apply {
            ivNotificationRR.setOnClickListener { findNavController().navigate(R.id.notificationsListFragment) }
            swipeRefreshHome.setOnRefreshListener {
                homeVm.clearCache()
                settingVM.clearCache()
                homeVm.homeAllData.removeObservers(this@DiscoverFragment)
                settingVM.userSavedRecipesData.removeObservers(this@DiscoverFragment)
                hitApiForHomeRecipes(true)
                hitApiForUserSavedRecipes(true)
            }

            llDRSeeAll.setOnClickListener { gotoRecipeList(1) }
            llBFSeeAll.setOnClickListener { gotoRecipeList(2) }
            llLunchSeeAll.setOnClickListener { gotoRecipeList(3) }
            llDinnerSeeAll.setOnClickListener { gotoRecipeList(4) }
            llSnacksSeeAll.setOnClickListener { gotoRecipeList(5) }
            llDessertsSeeAll.setOnClickListener { gotoRecipeList(6) }
            llDrinksSeeAll.setOnClickListener { gotoRecipeList(7) }
            llSRSeeAll.setOnClickListener {
                if (isAdded) {
                    bundle.putInt(Constants.savedTypeForm, 0)
                    findNavController().navigate(
                        R.id.action_discoverFragment_to_saveRecipesFragment, bundle
                    )
                }
            }

            svSearchRoots.setOnClickListener { gotoRecipeList(0) }
        }
    }

    private fun gotoRecipeList(value: Int) {
        val bundle = Bundle()
        bundle.putInt(Constants.recipeListTypeFrom, value)
        Constants.DiscoverRecipesScroll = false
        findNavController().navigate(R.id.action_discoverFragment_to_recipesListFragment, bundle)
    }

    private fun saveRecipeAdapter(list: ArrayList<RecipeData>) {
        if (list.isNotEmpty()) {
            savedRecipesAdapter =
                BreakFastAdapter(requireActivity(), list, object : OnItemClickListener {
                    @SuppressLint("NotifyDataSetChanged")
                    override fun onItemClick(position: Int, type: String) {
                        if (type == Constants.recipeInformation) {
                            gotoRecipeInformation(savedRecipesAdapter.getList()[position])
                        } else if (type == Constants.saveRecipe) {
                            if (savedRecipesAdapter.getList()[position].isSaved!!) {
                                savedRecipesAdapter.getList()[position].isSaved = false
                                hitApiForUnSaveRecipe(savedRecipesAdapter.getList()[position])
                                savedRecipesAdapter.notifyDataSetChanged()
                            } else {
                                savedRecipesAdapter.getList()[position].isSaved = true
                                hitApiForSaveRecipe(savedRecipesAdapter.getList()[position])
                                savedRecipesAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                })
            binding.apply {
                rvSaveRecipes.adapter = savedRecipesAdapter
                clSaveRecipes.visible()
                rvSaveRecipes.visible()
                if (list.size >= 4) {
                    llSRSeeAll.visible()
                } else {
                    llSRSeeAll.gone()
                }
            }
        } else {
            binding.apply {
                clSaveRecipes.gone()
                rvSaveRecipes.gone()
            }
        }
    }

    private fun setBreakFastAdapter(list: ArrayList<RecipeData>) {
        if (list.isNotEmpty()) {
            breakFastAdapter =
                BreakFastAdapter(requireActivity(), list, object : OnItemClickListener {
                    @SuppressLint("NotifyDataSetChanged")
                    override fun onItemClick(position: Int, type: String) {
                        if (type == Constants.recipeInformation) {
                            gotoRecipeInformation(breakFastAdapter.getList()[position])
                        } else if (type == Constants.saveRecipe) {
                            if (breakFastAdapter.getList()[position].isSaved!!) {
                                breakFastAdapter.getList()[position].isSaved = false
                                hitApiForUnSaveRecipe(breakFastAdapter.getList()[position])
                                breakFastAdapter.notifyDataSetChanged()
                            } else {
                                breakFastAdapter.getList()[position].isSaved = true
                                hitApiForSaveRecipe(breakFastAdapter.getList()[position])
                                breakFastAdapter.notifyDataSetChanged()
                            }
                        }

                    }

                })
            binding.apply {
                rvBreakFastRecipes.adapter = breakFastAdapter
                clBreakFastRecipes.visible()
                rvBreakFastRecipes.visible()
                if (list.size >= 4) {
                    llBFSeeAll.visible()
                } else {
                    llBFSeeAll.gone()
                }
            }
        } else {
            binding.apply {
                clBreakFastRecipes.gone()
                rvBreakFastRecipes.gone()
            }
        }
    }


    private fun setLunchAdapter(list: ArrayList<RecipeData>) {
        if (list.isNotEmpty()) {
            lunchAdapter = BreakFastAdapter(requireActivity(), list, object : OnItemClickListener {
                @SuppressLint("NotifyDataSetChanged")
                override fun onItemClick(position: Int, type: String) {
                    if (type == Constants.recipeInformation) {
                        gotoRecipeInformation(lunchAdapter.getList()[position])
                    } else if (type == Constants.saveRecipe) {
                        if (lunchAdapter.getList()[position].isSaved!!) {
                            lunchAdapter.getList()[position].isSaved = false
                            hitApiForUnSaveRecipe(lunchAdapter.getList()[position])
                            lunchAdapter.notifyDataSetChanged()
                        } else {
                            lunchAdapter.getList()[position].isSaved = true
                            hitApiForSaveRecipe(lunchAdapter.getList()[position])
                            lunchAdapter.notifyDataSetChanged()
                        }
                    }
                }
            })
            binding.apply {
                rvLunchRecipes.adapter = lunchAdapter
                clLunchRecipes.visible()
                rvLunchRecipes.visible()
                if (list.size >= 4) {
                    llLunchSeeAll.visible()
                } else {
                    llLunchSeeAll.gone()
                }
            }
        } else {
            binding.apply {
                clLunchRecipes.gone()
                rvLunchRecipes.gone()
            }
        }
    }

    private fun setDinnerAdapter(list: ArrayList<RecipeData>) {
        if (list.isNotEmpty()) {
            dinnerAdapter = BreakFastAdapter(requireActivity(), list, object : OnItemClickListener {
                @SuppressLint("NotifyDataSetChanged")
                override fun onItemClick(position: Int, type: String) {
                    if (type == Constants.recipeInformation) {
                        gotoRecipeInformation(dinnerAdapter.getList()[position])
                    } else if (type == Constants.saveRecipe) {
                        if (dinnerAdapter.getList()[position].isSaved!!) {
                            dinnerAdapter.getList()[position].isSaved = false
                            hitApiForUnSaveRecipe(dinnerAdapter.getList()[position])
                            dinnerAdapter.notifyDataSetChanged()
                        } else {
                            dinnerAdapter.getList()[position].isSaved = true
                            hitApiForSaveRecipe(dinnerAdapter.getList()[position])
                            dinnerAdapter.notifyDataSetChanged()
                        }
                    }
                }
            })
            binding.apply {
                rvDinnerRecipes.adapter = dinnerAdapter
                clDinnerRecipes.visible()
                rvDinnerRecipes.visible()
                if (list.size >= 4) {
                    llDinnerSeeAll.visible()
                } else {
                    llDinnerSeeAll.gone()
                }
            }
        } else {
            binding.apply {
                clDinnerRecipes.gone()
                rvDinnerRecipes.gone()
            }
        }
    }

    private fun setSnacksAdapter(list: ArrayList<RecipeData>) {
        if (list.isNotEmpty()) {
            snacksAdapter = BreakFastAdapter(requireActivity(), list, object : OnItemClickListener {
                @SuppressLint("NotifyDataSetChanged")
                override fun onItemClick(position: Int, type: String) {
                    if (type == Constants.recipeInformation) {
                        gotoRecipeInformation(snacksAdapter.getList()[position])
                    } else if (type == Constants.saveRecipe) {
                        if (snacksAdapter.getList()[position].isSaved!!) {
                            snacksAdapter.getList()[position].isSaved = false
                            hitApiForUnSaveRecipe(snacksAdapter.getList()[position])
                            snacksAdapter.notifyDataSetChanged()
                        } else {
                            snacksAdapter.getList()[position].isSaved = true
                            hitApiForSaveRecipe(snacksAdapter.getList()[position])
                            snacksAdapter.notifyDataSetChanged()
                        }
                    }
                }
            })
            binding.apply {
                rvSnacksRecipes.adapter = snacksAdapter
                clSnacksRecipes.visible()
                rvSnacksRecipes.visible()
                if (list.size >= 4) {
                    llSnacksSeeAll.visible()
                } else {
                    llSnacksSeeAll.gone()
                }
            }
        } else {
            binding.apply {
                clSnacksRecipes.gone()
                rvSnacksRecipes.gone()
            }
        }
    }

    private fun setDessertAdapter(list: ArrayList<RecipeData>) {
        if (list.isNotEmpty()) {
            dessertAdapter =
                BreakFastAdapter(requireActivity(), list, object : OnItemClickListener {
                    @SuppressLint("NotifyDataSetChanged")
                    override fun onItemClick(position: Int, type: String) {
                        if (type == Constants.recipeInformation) {
                            gotoRecipeInformation(dessertAdapter.getList()[position])
                        } else if (type == Constants.saveRecipe) {
                            if (dessertAdapter.getList()[position].isSaved!!) {
                                dessertAdapter.getList()[position].isSaved = false
                                hitApiForUnSaveRecipe(dessertAdapter.getList()[position])
                                dessertAdapter.notifyDataSetChanged()
                            } else {
                                dessertAdapter.getList()[position].isSaved = true
                                hitApiForSaveRecipe(dessertAdapter.getList()[position])
                                dessertAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                })
            binding.apply {
                rvDessertsRecipes.adapter = dessertAdapter
                clDessertsRecipes.visible()
                rvDessertsRecipes.visible()
                if (list.size >= 4) {
                    llDessertsSeeAll.visible()
                } else {
                    llDessertsSeeAll.gone()
                }
            }
        } else {
            binding.apply {
                clDessertsRecipes.gone()
                rvDessertsRecipes.gone()
            }
        }
    }

    private fun setDrinkAdapter(list: ArrayList<RecipeData>) {
        if (list.isNotEmpty()) {
            drinksAdapter = BreakFastAdapter(requireActivity(), list, object : OnItemClickListener {
                @SuppressLint("NotifyDataSetChanged")
                override fun onItemClick(position: Int, type: String) {
                    if (type == Constants.recipeInformation) {
                        gotoRecipeInformation(drinksAdapter.getList()[position])
                    } else if (type == Constants.saveRecipe) {
                        if (drinksAdapter.getList()[position].isSaved!!) {
                            drinksAdapter.getList()[position].isSaved = false
                            hitApiForUnSaveRecipe(drinksAdapter.getList()[position])
                            drinksAdapter.notifyDataSetChanged()
                        } else {
                            drinksAdapter.getList()[position].isSaved = true
                            hitApiForSaveRecipe(drinksAdapter.getList()[position])
                            drinksAdapter.notifyDataSetChanged()
                        }
                    }
                }
            })
            binding.apply {
                rvDrinksRecipes.adapter = drinksAdapter
                clDrinksRecipes.visible()
                rvDrinksRecipes.visible()
                if (list.size >= 4) {
                    llDrinksSeeAll.visible()
                } else {
                    llDrinksSeeAll.gone()
                }
            }
        } else {
            binding.apply {
                clDrinksRecipes.gone()
                rvDrinksRecipes.gone()
            }
        }
    }

    private fun setDiscoverRecipesAdapter(list: ArrayList<RecipeData>) {
        if (list.isNotEmpty()) {
            discoverRecipesAdapter =
                DiscoverRecipesAdapter(requireActivity(), list, object : OnItemClickListener {
                    @SuppressLint("NotifyDataSetChanged")
                    override fun onItemClick(position: Int, type: String) {
                        if (type == Constants.recipeInformation) {
                            gotoRecipeInformation(discoverRecipesAdapter.getDiscoverList()[position])
                        } else if (type == Constants.saveRecipe) {
                            if (discoverRecipesAdapter.getDiscoverList()[position].isSaved!!) {
                                discoverRecipesAdapter.getDiscoverList()[position].isSaved = false
                                hitApiForUnSaveRecipe(discoverRecipesAdapter.getDiscoverList()[position])
                                discoverRecipesAdapter.notifyDataSetChanged()
                            } else {
                                discoverRecipesAdapter.getDiscoverList()[position].isSaved = true
                                hitApiForSaveRecipe(discoverRecipesAdapter.getDiscoverList()[position])
                                discoverRecipesAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                })
            binding.apply {
                rvDiscoverRecipes.adapter = discoverRecipesAdapter
                clDiscoverRecipes.visible()
                rvDiscoverRecipes.visible()
                if (list.size >= 4) {
                    llDRSeeAll.visible()
                } else {
                    llDRSeeAll.gone()
                }
            }
        } else {
            binding.apply {
                clDiscoverRecipes.gone()
                rvDiscoverRecipes.gone()
            }
        }

    }

    private fun setCategoryAdapter() {
        categoryDataList = arrayListOf(
            CategoryItemData("All Recipes"),
            CategoryItemData("Breakfast", R.drawable.breakfast),
            CategoryItemData("Lunch", R.drawable.lunch),
            CategoryItemData("Dinner", R.drawable.dinner),
            CategoryItemData("Snacks", R.drawable.snacks),
            CategoryItemData("Dessert", R.drawable.dessert),
            CategoryItemData("Drink", R.drawable.drinks),
        )
        categoryAdapter = CategoryAdapter(requireActivity(), categoryDataList!!, this)
        binding.rvTypeOfCategory.adapter = categoryAdapter
    }

    override fun onClickItem(position: Int) {
        if (isAdded) {
            when (position) {
                0 -> gotoRecipeList(0)
                1 -> gotoRecipeList(2)
                2 -> gotoRecipeList(3)
                3 -> gotoRecipeList(4)
                4 -> gotoRecipeList(5)
                5 -> gotoRecipeList(6)
                6 -> gotoRecipeList(7)
            }

        }
    }

    private fun gotoRecipeInformation(recipeData: RecipeData) {
        val bundle = Bundle()
        if (recipeData.user != null && recipeData.user_id == pref.getSignInData()!!._id) {
            bundle.putInt(Constants.typeFrom, 2)
        } else {
            bundle.putInt(Constants.typeFrom, 1)
        }
        bundle.putParcelable(
            Constants.recipeInformation,
            recipeData
        )
        findNavController().navigate(
            R.id.action_discoverFragment_to_recipeInformationFragment, bundle
        )
    }


    private fun hitApiForSaveRecipe(recipeData: RecipeData) {
        val jsonObject = JsonObject()
        jsonObject.addProperty("recipe_id", recipeData._id)
        CoroutineScope(Dispatchers.Main).launch {
            if (isAdded) {
                settingVM.saveRecipe(jsonObject)
                settingVM.saveRecipeData.observe(this@DiscoverFragment) { its ->
                    when (its.status) {
                        Status.SUCCESS -> {
                            if (!allHomeData.isNullOrEmpty()) {
                                allHomeData!!.map { recipeList ->
                                    recipeList.recipes!!.map {
                                        if (it._id == recipeData._id) {
                                            it.isSaved = true
//                                        updateAdapter(recipeList.category)

                                        }
                                    }
                                }
                            }
                            updateSaveAdapter(recipeData, 1)
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
    private fun updateSaveAdapter(recipe: RecipeData, type: Int) {
        if (::savedRecipesAdapter.isInitialized) {
            if (type == 1) {
                if (savedRecipesAdapter.getList().none { it._id == recipe._id }) {
                    savedRecipesAdapter.getList().add(recipe)
                }
            } else if (type == 0) {
                if (savedRecipesAdapter.getList().any { it._id == recipe._id }) {
                    for (i in savedRecipesAdapter.getList().indices) {
                        if (i < savedRecipesAdapter.getList().size) {
                            if (recipe._id == savedRecipesAdapter.getList()[i]._id) {
                                savedRecipesAdapter.getList().removeAt(i)
                            }
                        }
                    }
                }
            }

            if (savedRecipesAdapter.getList().isNotEmpty()) {
                binding.apply {
                    clSaveRecipes.visible()
                    rvSaveRecipes.visible()
                    if (savedRecipesAdapter.getList().size >= 4) {
                        llSRSeeAll.visible()
                    } else {
                        llSRSeeAll.gone()
                    }
                }
            } else {
                binding.apply {
                    savedRecipesAdapter.getList().clear()
                    clSaveRecipes.gone()
                    rvSaveRecipes.gone()
                }
            }
            savedRecipesAdapter.notifyDataSetChanged()
        } else {
            if (type == 1) {
                val tempArr = ArrayList<RecipeData>()
                tempArr.add(recipe)
                saveRecipeAdapter(tempArr)
            }
        }
        hitApiForUserSavedRecipes(true)
    }

    private fun hitApiForUnSaveRecipe(recipeData: RecipeData) {
        CoroutineScope(Dispatchers.Main).launch {
            settingVM.unSaveRecipe(recipeData._id!!)
            settingVM.unSaveRecipeData.observe(this@DiscoverFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        if (!allHomeData.isNullOrEmpty()) {
                            allHomeData!!.map { recipeList ->
                                recipeList.recipes!!.map {
                                    if (it._id == recipeData._id) {
                                        it.isSaved = false
//                                        updateAdapter(recipeList.category)

                                    }
                                }
                            }
                        }
                        updateSaveAdapter(recipeData, 0)
                    }

                    Status.ERROR -> {

                    }

                    Status.LOADING -> {

                    }
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateAdapter(category: String) {
        when (category) {
            Constants.DISCOVER -> {
                if (::discoverRecipesAdapter.isInitialized) {
                    discoverRecipesAdapter.notifyDataSetChanged()
                }
            }

            Constants.BREAKFAST -> {
                if (::breakFastAdapter.isInitialized) {
                    breakFastAdapter.notifyDataSetChanged()
                }
            }

            Constants.LUNCH -> {
                if (::lunchAdapter.isInitialized) {
                    lunchAdapter.notifyDataSetChanged()
                }
            }

            Constants.DINNER -> {
                if (::dinnerAdapter.isInitialized) {
                    dinnerAdapter.notifyDataSetChanged()
                }
            }

            Constants.SNACKS -> {
                if (::snacksAdapter.isInitialized) {
                    snacksAdapter.notifyDataSetChanged()
                }
            }

            Constants.DESSERTS -> {
                if (::dessertAdapter.isInitialized) {
                    dessertAdapter.notifyDataSetChanged()
                }
            }

            Constants.DRINK -> {
                if (::drinksAdapter.isInitialized) {
                    drinksAdapter.notifyDataSetChanged()
                }
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        Extension.stopProgress()
    }


}