package com.rootsrecipes.view.myRecipes

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.services.s3.AmazonS3Client
import com.bumptech.glide.Glide
import com.google.android.material.chip.ChipGroup
import com.google.gson.JsonObject
import com.rootsrecipes.BuildConfig
import com.rootsrecipes.MainActivity
import com.rootsrecipes.R
import com.rootsrecipes.databinding.FragmentMyRecipesBinding
import com.rootsrecipes.databinding.PopupRvBinding
import com.rootsrecipes.databinding.ShareDialogBinding
import com.rootsrecipes.databinding.TutorialDialogLayoutBinding
import com.rootsrecipes.model.OnItemClickListener
import com.rootsrecipes.model.User
import com.rootsrecipes.utils.AWSSharedPref
import com.rootsrecipes.utils.BaseFragment
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.Extension
import com.rootsrecipes.utils.NetworkHelper
import com.rootsrecipes.utils.SharedPref
import com.rootsrecipes.utils.Status
import com.rootsrecipes.utils.gone
import com.rootsrecipes.utils.makeToast
import com.rootsrecipes.utils.popUpMenu.CommonStringListener
import com.rootsrecipes.utils.popUpMenu.PopUpAdapter
import com.rootsrecipes.utils.popUpMenu.PopUpData
import com.rootsrecipes.utils.visible
import com.rootsrecipes.view.createAccount.model.RecipeData
import com.rootsrecipes.view.myRecipes.adapter.MyRecipeAdapter
import com.rootsrecipes.view.myRecipes.viewModel.MyRecipesVM
import com.rootsrecipes.viewmodel.HomeVM
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.util.LinkProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyRecipesFragment : BaseFragment(), OnItemClickListener {

    private var userDataInfo: User? = null
    private lateinit var binding: FragmentMyRecipesBinding
    private lateinit var adapterMyRecipe: MyRecipeAdapter
    private lateinit var bundle: Bundle
    private var photoDialog: Dialog? = null
    private var currentPhotoUri: Uri? = null
    private val pref: SharedPref by inject()
    private val awsPref: AWSSharedPref by inject()
    private var galleryPath: File? = null
    private val vm: MyRecipesVM by viewModel()
    private val networkHelper: NetworkHelper by inject()
    private var searchKeyword = ""
    private var cuisine = ArrayList<String>()
    private var category = ArrayList<String>()
    private var limit = 10
    private val homeVm: HomeVM by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentMyRecipesBinding.inflate(inflater, container, false)
        userDataInfo = pref.getSignInData()

        // Set initial visibility: main search visible, fixed search hidden, no recipe hidden
        binding.clSearchAndFilterMyRecipes.visibility = View.VISIBLE
        binding.clSearchAndFilterMyRecipesHide.visibility = vm.fixedSearchBarVisibility // Default GONE from VM
        binding.clNoRecipe.visibility = View.GONE // Ensure "no recipe" is hidden initially

        // Observe isShowingMyRecipes to update UI and load data
        vm.isShowingMyRecipes.observe(viewLifecycleOwner) { isShowing ->
            updateUIForRecipeType(isShowing)
            if (isShowing) loadInitialData() else loadSavedRecipes()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (awsPref.fetchString(Constants.access_key).isNullOrEmpty() ||
            awsPref.fetchString(Constants.aws_bucket_name).isNullOrEmpty() ||
            awsPref.fetchString(Constants.secret_key).isNullOrEmpty() ||
            awsPref.fetchString(Constants.aws_region).isNullOrEmpty()) {
            getAwsCredential()
        } else {
            initializedAws()
        }
        bundle = Bundle()
        initUi()
    }

    private fun initializedAws() {
        val accessKey = awsPref.fetchString(Constants.access_key)
        val secretKey = awsPref.fetchString(Constants.secret_key)
        MainActivity.s3client = AmazonS3Client(BasicAWSCredentials(accessKey, secretKey))
    }

    @SuppressLint("FragmentLiveDataObserve")
    private fun getAwsCredential() {
        Extension.showProgress(requireActivity())
        CoroutineScope(Dispatchers.Main).launch {
            homeVm.getAwsCredential()
            homeVm.awsCredentialData.observe(this@MyRecipesFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        Extension.stopProgress()
                        its.data?.data?.let { data ->
                            awsPref.saveString(Constants.access_key, data.access_key)
                            awsPref.saveString(Constants.secret_key, data.secret_key)
                            awsPref.saveString(Constants.aws_region, data.aws_region)
                            awsPref.saveString(Constants.aws_bucket_name, data.aws_bucket_name)
                            initializedAws()
                        }
                    }
                    Status.ERROR -> Extension.stopProgress()
                    Status.LOADING -> {}
                }
            }
        }
    }

    private fun initUi() {
        val savedScrollPosition = pref.getInt(Constants.NESTEDSCROLL_POSITION)
        binding.nestedMyRecipes.post { binding.nestedMyRecipes.scrollTo(0, savedScrollPosition) }
        setNestedData()
        setupObservers()
        setOnClickMethod()
        setUserData()
        hitApiUserDetails(false)
        getFilterItems()
    }

    private fun loadSavedRecipes() {
        viewLifecycleOwner.lifecycleScope.launch {
            vm.savedRecipes(
                search = searchKeyword,
                category = Extension.arrayListToCommaSeparatedString(category),
                cuisine = Extension.arrayListToCommaSeparatedString(cuisine),
                page = 1,
                limit = limit,
                forceRefresh = true
            )
        }
    }

    private fun loadInitialData() {
        Constants.loadInitialData = true
        viewLifecycleOwner.lifecycleScope.launch {
            vm.myRecipes(
                searchKeyword = searchKeyword,
                category = Extension.arrayListToCommaSeparatedString(category),
                cuisine = Extension.arrayListToCommaSeparatedString(cuisine),
                pageNumber = 1,
                limit = limit,
                forceRefresh = true
            )
        }
    }

    private fun loadMoreRecipes() {
        viewLifecycleOwner.lifecycleScope.launch {

            vm.updateCurrentPage()
            if (vm.isShowingMyRecipes.value == true) {
                vm.myRecipes(
                    searchKeyword = searchKeyword,
                    category = Extension.arrayListToCommaSeparatedString(category),
                    cuisine = Extension.arrayListToCommaSeparatedString(cuisine),
                    pageNumber = vm.getCurrentPage(),
                    limit = limit,
                    forceRefresh = false
                )
            } else {
                vm.savedRecipes(
                    search = searchKeyword,
                    category = Extension.arrayListToCommaSeparatedString(category),
                    cuisine = Extension.arrayListToCommaSeparatedString(cuisine),
                    page = vm.getCurrentPage(),
                    limit = limit,
                    forceRefresh = false
                )
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setAdapterMyRecipe(data: ArrayList<RecipeData>, isMyRecipes: Boolean) {
        val userType = if (vm.isShowingMyRecipes.value == true) 0 else 2
        adapterMyRecipe = MyRecipeAdapter(requireContext(), data, this, userType)
        binding.rvMyRecipes.adapter = adapterMyRecipe

        // Only show clNoRecipe after API response if data is empty
        if (data.isEmpty()) {
            binding.clNoRecipe.visible()
            if (category.isNotEmpty() || cuisine.isNotEmpty()) {
                binding.noRecipeSubText.text = "No recipes match the active filters. Please clear the filters to continue."
                binding.btnAddRecipe.text = "Clear Filters"
                binding.btnAddRecipe.setOnClickListener { clearFilters() }
            } else {
//                binding.clSearchAndFilterMyRecipes.gone()
                binding.noRecipeSubText.text = if (isMyRecipes) {
                    "You currently have no recipes. Start adding your favorite recipes to get started!"
                } else {
                    "You haven't saved any recipes yet. Start saving your favorite recipes!"
                }
                binding.btnAddRecipe.text = if (isMyRecipes) "Add Recipe" else "Browse Recipes"
                if(isMyRecipes) binding.btnAddRecipe.visible() else binding.btnAddRecipe.gone()
                binding.btnAddRecipe.setOnClickListener {
                    if (isMyRecipes) handleAddRecipeClick()
                }
            }
        } else {
            binding.clNoRecipe.gone()
            binding.clSearchAndFilterMyRecipes.visible() // Ensure search stays visible with data
        }
    }

    private fun clearFilters() {
        cuisine.clear()
        category.clear()
        binding.cgFilter.removeAllViews()
        updateList()
    }

    private fun handleAddRecipeClick() {
        val tutorialDialogShown = pref.getBoolean(Constants.TUTORIAL_DIALOG)
        if (tutorialDialogShown) {
            findNavController().navigate(R.id.action_myRecipesFragment_to_recordRecipeFragment)
        } else {
            showTutorialDialog()
        }
    }

    private fun setNestedData() {
        binding.nestedMyRecipes.setOnScrollChangeListener { v, _, scrollY, _, _ ->
            val nestedScrollView = v as NestedScrollView
            val lastChild = nestedScrollView.getChildAt(nestedScrollView.childCount - 1)
            val diff = lastChild.bottom - (nestedScrollView.height + scrollY)

            if (diff <= 0 && !vm.isLoading() && vm.hasMoreData()){
                vm.setLoading(true)
                loadMoreRecipes()
            }
            pref.saveInt(Constants.NESTEDSCROLL_POSITION, scrollY)
            binding.root.post { handleStickySearch() }
        }
    }

    @SuppressLint("FragmentLiveDataObserve", "NotifyDataSetChanged")
    private fun setupObservers() {
        if (vm.getCurrentPage() == 1) setTopLoaderVisibility(1)

        vm.savedRecipesData.observe(this@MyRecipesFragment) { resource ->
            binding.swipeRefreshLayout.isRefreshing = false
            when (resource.status) {
                Status.SUCCESS -> {
                    Extension.stopProgress()
                    setTopLoaderVisibility(0)
                    vm.setLoading(false)
                    resource.data?.let { data ->
                        vm.setHasMoreData(data.data.size >= limit)
                        if (vm.isShowingMyRecipes.value == false) {
                            if(data.data.isEmpty()){
                                binding.clSearchAndFilterMyRecipes.gone()
                            }else{
                                binding.clSearchAndFilterMyRecipes.visible()
                            }

                            if (vm.getCurrentPage() == 1) setAdapterMyRecipe(data.data, false)
                            else {
                                adapterMyRecipe.addItems(data.data)
                                adapterMyRecipe.notifyDataSetChanged()
                            }
                        }
                    }
                }
                Status.ERROR -> {
                    Extension.stopProgress()
                    setTopLoaderVisibility(0)
                    vm.setLoading(false)
                    resource.message?.let { requireContext().makeToast(it) }
                    // Show "no recipe" only if explicitly empty after error (e.g., no data cached)
                    if (vm.allRecipesData.value.isNullOrEmpty()) setAdapterMyRecipe(ArrayList(), false)
                }
                Status.LOADING -> {
                    setTopLoaderVisibility(1)
                    binding.clNoRecipe.gone() // Explicitly hide during loading
                }
            }
        }

        vm.allRecipesData.observe(this@MyRecipesFragment) { recipes ->
            binding.swipeRefreshLayout.isRefreshing = false
            if (vm.isShowingMyRecipes.value == true) {
                if (vm.getCurrentPage() == 1) setAdapterMyRecipe(recipes, true)
                else if (::adapterMyRecipe.isInitialized) {
                    adapterMyRecipe.getAddList().clear()
                    adapterMyRecipe.getAddList().addAll(recipes)
                    adapterMyRecipe.notifyDataSetChanged()
                } else setAdapterMyRecipe(recipes, true)
            }
        }

        vm.myRecipesData.observe(this@MyRecipesFragment) { resource ->
            binding.swipeRefreshLayout.isRefreshing = false
            when (resource.status) {
                Status.SUCCESS -> {
                    Extension.stopProgress()
                    setTopLoaderVisibility(0)
                    vm.setLoading(false)
                    resource.data?.let { data ->
                        vm.setHasMoreData(data.data.size >= limit)
                        if (vm.isShowingMyRecipes.value == true) {
                            Log.d("pageNumberDebug","${vm.getCurrentPage()}")
                            if(data.data.isEmpty()){
                                binding.clSearchAndFilterMyRecipes.gone()
                            }else{
                                binding.clSearchAndFilterMyRecipes.visible()
                            }


                            if (vm.getCurrentPage() == 1) setAdapterMyRecipe(data.data, true)
                            else {
                                adapterMyRecipe.addItems(data.data)
                                adapterMyRecipe.notifyDataSetChanged()
                            }
                        }
                    }
                }
                Status.ERROR -> {
                    Extension.stopProgress()
                    setTopLoaderVisibility(0)
                    vm.setLoading(false)
                    resource.message?.let { requireContext().makeToast(it) }
                    // Show "no recipe" only if explicitly empty after error
                    if (vm.allRecipesData.value.isNullOrEmpty()) setAdapterMyRecipe(ArrayList(), true)
                }
                Status.LOADING -> {
                    setTopLoaderVisibility(1)
                    binding.clNoRecipe.gone() // Explicitly hide during loading
                }
            }
        }

        vm.getUserDetailsData.observe(this@MyRecipesFragment) { userData ->
            when (userData.status) {
                Status.SUCCESS -> {
                    Extension.stopProgress()
                    userData.data?.data?.let { pref.saveSignInData(it) }
                    userDataInfo = pref.getSignInData()
                    setUserData()
                }
                Status.ERROR -> {
                    Extension.stopProgress()
                    userData.message?.let { requireActivity().makeToast(it) }
                }
                Status.LOADING -> {}
            }
        }

        vm.updateProfileImageData.observe(this@MyRecipesFragment) { its ->
            when (its.status) {
                Status.SUCCESS -> {
                    Extension.stopProgress()
                    its.data?.data?.let { pref.saveSignInData(it) }
                    setUserData()
                }
                Status.ERROR -> {
                    Extension.stopProgress()
                    its.message?.let { requireActivity().makeToast(it) }
                }
                Status.LOADING -> {}
            }
        }
    }

    private fun hitApiUserDetails(forceRefresh: Boolean = false) {
        userDataInfo?._id?.let { userId ->
            CoroutineScope(Dispatchers.Main).launch {
                vm.getUserDetails(forceRefresh, userId)
            }
        } ?: requireActivity().makeToast("User data not available")
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun getFilterItems() {
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Bundle>("requestKey")
            ?.observe(viewLifecycleOwner) { result ->
                category = result.getStringArrayList("filteredCategory") ?: arrayListOf()
                cuisine = result.getStringArrayList("filteredCuisine") ?: arrayListOf()
                val newList = (category + cuisine) as ArrayList
                addChips(newList, binding.cgFilter)
                updateList()
            }
        binding.ivFilterMyRecipes.setImageDrawable(
            if (cuisine.isNotEmpty() || category.isNotEmpty()) requireActivity().getDrawable(R.drawable.filter_selected)
            else requireActivity().getDrawable(R.drawable.filter_icon)
        )
    }

    private fun updateList() {
        vm.clearCache()
        if (vm.isShowingMyRecipes.value == true) loadInitialData() else loadSavedRecipes()
    }

    private fun setOnClickMethod() {
        binding.apply {
            tvMyRecipesClick.setOnClickListener {
                vm.setIsShowingMyRecipes(true)
                updateList()
                clSearchAndFilterMyRecipes.visibility = View.VISIBLE // Keep search visible
                clNoRecipe.gone() // Hide "no recipe" until API responds
            }
            tvFavouriteClick.setOnClickListener {
                vm.setIsShowingMyRecipes(false)
                updateList()
                clSearchAndFilterMyRecipes.visibility = View.VISIBLE // Keep search visible
                clNoRecipe.gone() // Hide "no recipe" until API responds
            }
            ivMore.setOnClickListener {
                Constants.MyRecipesScroll = true
                createPopupMenu()
            }
            ivEditPfRecipes.setOnClickListener { photoDialog() }
            swipeRefreshLayout.setOnRefreshListener {
                vm.clearCache()
                if (vm.isShowingMyRecipes.value == true) {
                    loadInitialData()
                    hitApiUserDetails(true)
                } else loadSavedRecipes()
            }
        /*    ivFilterMyRecipes.setOnClickListener {
                if (isAdded) {
                    bundle.putStringArrayList("filteredCategory", category)
                    bundle.putStringArrayList("filteredCuisine", cuisine)
                    bundle.putInt(Constants.filterTypeForm, 2)
                    findNavController().navigate(R.id.action_myRecipesFragment_to_filterFragment, bundle)
                }
            }*/
          /*  ivFilterMyRecipesHide.setOnClickListener {
                if (isAdded) {
                    bundle.putInt(Constants.filterTypeForm, 1)
                    findNavController().navigate(R.id.action_myRecipesFragment_to_filterFragment, bundle)
                }
            }*/
            llFollowers.setOnClickListener {
                val bundle = Bundle().apply {
                    putInt(Constants.typeFrom, 0)
                    putInt(Constants.userTypeFrom, 0)
                }
                findNavController().navigate(R.id.action_myRecipesFragment_to_followersFragment, bundle)
            }
            llFollowing.setOnClickListener {
                val bundle = Bundle().apply {
                    putInt(Constants.typeFrom, 1)
                    putInt(Constants.userTypeFrom, 0)
                }
                findNavController().navigate(R.id.action_myRecipesFragment_to_followersFragment, bundle)
            }
//            clSearchMyRecipe.setOnClickListener { goToSearchScreen() }
            clSearchAndFilterMyRecipes.setOnClickListener { goToSearchScreen() }
            svMyRecipes.setOnClickListener { goToSearchScreen() }
            clSearchMyRecipeHide.setOnClickListener { goToSearchScreen() }
            clSearchAndFilterMyRecipesHide.setOnClickListener { goToSearchScreen() }
            ivUserPfRecipes.setOnClickListener {
                userDataInfo?.profile_image?.takeIf { it.isNotEmpty() }?.let {
                    Extension.getImageViewMethod(it, requireContext())
                }
            }
        }
    }

    private fun updateUIForRecipeType(isShowingMyRecipes: Boolean) {
        binding.apply {
            if (isShowingMyRecipes) {
                tvMyRecipesClick.background = ContextCompat.getDrawable(requireActivity(), R.drawable.btn_corner)
                tvMyRecipesClick.setTextColor(ContextCompat.getColor(requireActivity(), R.color.white))
                tvFavouriteClick.setTextColor(ContextCompat.getColor(requireActivity(), R.color.black))
                tvFavouriteClick.setBackgroundColor(ContextCompat.getColor(requireActivity(), R.color.white))
            } else {
                tvFavouriteClick.background = ContextCompat.getDrawable(requireActivity(), R.drawable.btn_corner)
                tvFavouriteClick.setTextColor(ContextCompat.getColor(requireActivity(), R.color.white))
                tvMyRecipesClick.setTextColor(ContextCompat.getColor(requireActivity(), R.color.black))
                tvMyRecipesClick.setBackgroundColor(ContextCompat.getColor(requireActivity(), R.color.white))
            }
        }
    }

    private fun showTutorialDialog() {
        val dialogLayoutBinding = TutorialDialogLayoutBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireActivity())
            .setView(dialogLayoutBinding.root)
            .setCancelable(true)
            .create()
        dialog.window?.apply {
            attributes.y = 50
            setGravity(Gravity.BOTTOM)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialogLayoutBinding.btnOk.setOnClickListener {
            pref.saveBoolean(Constants.TUTORIAL_DIALOG, true)
            findNavController().navigate(R.id.action_myRecipesFragment_to_recordRecipeFragment)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun goToSearchScreen() {
        val bundle = Bundle().apply {
            putInt(Constants.savedTypeForm, if (vm.isShowingMyRecipes.value == true) 1 else 0)
        }
        findNavController().navigate(R.id.action_myRecipesFragment_to_saveRecipesFragment, bundle)
    }

    @SuppressLint("SetTextI18n")
    private fun setUserData() {
        userDataInfo?.let {
            binding.tvUserNameRecipes.text = "${it.first_name} ${it.last_name}"
            binding.tvUserBio.text = it.about_me
            Glide.with(requireActivity())
                .load(BuildConfig.BASE_MEDIA_URL + it.profile_image)
                .placeholder(R.drawable.profile_icon)
                .into(binding.ivUserPfRecipes)
            binding.tvTotalFollowers.text = it.followers_count.toString()
            binding.tvTotalFollowing.text = it.following_count.toString()
        } ?: run {
            binding.tvUserNameRecipes.text = "User"
            binding.tvUserBio.text = ""
            binding.tvTotalFollowers.text = "0"
            binding.tvTotalFollowing.text = "0"
        }
    }

    private fun photoDialog() {
        photoDialog?.dismiss()
        photoDialog = Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setGravity(Gravity.BOTTOM)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setContentView(R.layout.photo_dialog)
            window?.setLayout(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT)
            show()
        }

        photoDialog?.findViewById<LinearLayout>(R.id.llGallery)?.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) openGallery()
                else requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
            } else {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) openGallery()
                else requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
            }
            photoDialog?.dismiss()
        }

        photoDialog?.findViewById<LinearLayout>(R.id.llCamera)?.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) openCamera()
            else requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            photoDialog?.dismiss()
        }

        photoDialog?.findViewById<TextView>(R.id.tvCancel)?.setOnClickListener { photoDialog?.dismiss() }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val granted = permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
        if (granted) openGallery() else requireActivity().makeToast("Permission denied")
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) openCamera() else requireActivity().makeToast("Permission denied")
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        getImageResultLauncher.launch(intent)
    }

    private fun openCamera() {
        try {
            val photoFile = createImageFile()
            currentPhotoUri = FileProvider.getUriForFile(requireActivity(), "com.rootsrecipes.fileprovider", photoFile)
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri)
            }
            captureImageLauncher.launch(cameraIntent)
        } catch (ex: IOException) {
            ex.printStackTrace()
            requireActivity().makeToast("Failed to open camera")
        }
    }

    private val captureImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            currentPhotoUri?.let { uri ->
                galleryPath = copyUriToInternalStorage(uri)
                Glide.with(requireActivity()).load(uri).into(binding.ivUserPfRecipes)
                galleryPath?.let { newUploadMethod(it) }
            }
        }
    }

    private fun copyUriToInternalStorage(uri: Uri): File {
        val inputStream = requireActivity().contentResolver.openInputStream(uri)
        val file = File(requireContext().filesDir, "profile_image_${System.currentTimeMillis()}.jpg")
        inputStream?.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
        return file
    }

    private fun newUploadMethod(uploadGalleryPath: File) {
        Extension.showProgress(requireContext())
        val transferUtility = TransferUtility.builder()
            .context(requireActivity())
            .defaultBucket(awsPref.fetchString(Constants.aws_bucket_name))
            .s3Client(MainActivity.s3client)
            .build()
        if (networkHelper.isNetworkConnected()) {
            Thread {
                try {
                    val key = "RootsAndRecipes/Media/${System.currentTimeMillis()}/image.jpg"
                    val uploadObserver = transferUtility.upload(key, uploadGalleryPath)
                    uploadObserver.setTransferListener(object : TransferListener {
                        override fun onStateChanged(id: Int, state: TransferState?) {
                            if (state == TransferState.COMPLETED) {
                                val jsonObject = JsonObject().apply { addProperty(Constants.profile_image, key) }
                                hitApiUpdateProfileImage(jsonObject)
                            } else if (state == TransferState.FAILED) {
                                Extension.stopProgress()
                                requireActivity().makeToast("Profile picture upload failed")
                            }
                        }
                        override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {}
                        override fun onError(id: Int, ex: Exception?) {
                            Extension.stopProgress()
                            requireActivity().makeToast("Upload error: ${ex?.message}")
                        }
                    })
                } catch (e: Exception) {
                    Extension.stopProgress()
                    requireActivity().makeToast("Upload error: ${e.message}")
                    e.printStackTrace()
                }
            }.start()
        } else {
            Extension.stopProgress()
            requireActivity().makeToast("No internet connection")
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    private val getImageResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    Glide.with(requireActivity()).load(uri).into(binding.ivUserPfRecipes)
                    galleryPath = copyUriToInternalStorage(uri)
                    galleryPath?.let { newUploadMethod(it) }
                } catch (e: Exception) {
                    e.printStackTrace()
                    requireActivity().makeToast("Error loading image: ${e.message}")
                }
            }
        }
    }

    private fun handleStickySearch() {
        val mainSearchBar = binding.clSearchAndFilterMyRecipes
        val fixedSearchBar = binding.clSearchAndFilterMyRecipesHide
        val mainSearchLocation = IntArray(2)
        mainSearchBar.getLocationInWindow(mainSearchLocation)
        val mainSearchY = mainSearchLocation[1]
        val threshold = resources.getDimensionPixelSize(R.dimen.dimen_40dp)

        if (mainSearchY <= threshold && fixedSearchBar.visibility != View.VISIBLE) {
            fixedSearchBar.visibility = View.VISIBLE
            vm.fixedSearchBarVisibility = View.VISIBLE
        } else if (mainSearchY > threshold && fixedSearchBar.visibility != View.GONE) {
            fixedSearchBar.visibility = View.GONE
            vm.fixedSearchBarVisibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        photoDialog?.dismiss()
    }

    private fun createPopupMenu() {
        val inflater = requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = PopupRvBinding.inflate(inflater)
        val popupWindow = PopupWindow(popupView.root, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true)
        val list = arrayListOf(PopUpData("Share Profile"), PopUpData("Edit Profile", R.color.blue))
        popupView.rvPopUp.apply {
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL))
            adapter = PopUpAdapter(list, requireContext(), object : CommonStringListener {
                override fun onEventClick(position: Int) {
                    when (position) {
                        0 -> showShareDialog()
                        1 -> gotoEditScreenMethod()
                    }
                    popupWindow.dismiss()
                }
            })
        }
        val marginEndPx = (75 * requireContext().resources.displayMetrics.density).toInt()
        popupWindow.showAsDropDown(binding.ivMore, -marginEndPx, 0)
    }

    private fun showShareDialog() {
        val dialogLayoutBinding = ShareDialogBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogLayoutBinding.root)
            .setCancelable(true)
            .create()
        dialog.window?.apply {
            attributes.y = 50
            setGravity(Gravity.BOTTOM)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialogLayoutBinding.llShareVia.setOnClickListener {
            Extension.showProgress(requireActivity())
            sharedData(0)
            dialog.dismiss()
        }
        dialogLayoutBinding.llCopyLink.setOnClickListener {
            Extension.showProgress(requireActivity())
            sharedData(1)
            dialog.dismiss()
        }
        dialogLayoutBinding.tvCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun sharedData(type: Int) {
        val metadata = hashMapOf("userId" to (userDataInfo?._id ?: ""))
        val branchUniversalObject = BranchUniversalObject().addContentMetadata(metadata)
        val linkProperties = LinkProperties()
        branchUniversalObject.generateShortUrl(requireActivity(), linkProperties) { url, error ->
            Extension.stopProgress()
            if (error == null) {
                val shareText = "What do you think about ${userDataInfo?.first_name ?: ""} ${userDataInfo?.last_name ?: ""}? $url"
                when (type) {
                    0 -> Extension.shareViaProfileMethod(shareText, requireActivity())
                    1 -> Extension.copyUrlMethod(shareText, requireActivity())
                }
            } else requireActivity().makeToast("Error generating share link: ${error.message}")
        }
    }

    private fun gotoEditScreenMethod() {
        findNavController().navigate(R.id.action_myRecipesFragment_to_profileInfoFragment)
    }

    private fun hitApiUpdateProfileImage(jsonObject: JsonObject) {
        CoroutineScope(Dispatchers.Main).launch {
            vm.updateProfileImageData(jsonObject)
        }
    }

    override fun onItemClick(position: Int, type: String) {
        when (type) {
            Constants.recipeInformation -> {
                vm.fixedSearchBarVisibility = binding.clSearchAndFilterMyRecipesHide.visibility

                val bundle = Bundle().apply {
                    putParcelable(Constants.recipeInformation, adapterMyRecipe.mList[position])
                    if (adapterMyRecipe.mList[position].user_id == pref.getSignInData()!!._id) {
                        putInt(Constants.typeFrom, 2)
                    } else {
                        putInt(Constants.typeFrom, 1)
                    }
                }
                findNavController().navigate(R.id.action_myRecipesFragment_to_recipeInformationFragment, bundle)
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun addChips(list: ArrayList<String>, chipGroup: ChipGroup) {
        chipGroup.removeAllViews()
        if (list.isNotEmpty()) {
            list.forEach { item ->
                val linearLayout = LinearLayout(requireActivity()).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    setBackgroundResource(R.drawable.chip_bg)
                    backgroundTintList = ColorStateList.valueOf(requireActivity().getColor(R.color.category_color))
                    setPadding(20, 0, 20, 0)
                }
                val textView = TextView(requireActivity()).apply {
                    text = item
                    setTextColor(requireActivity().getColor(R.color.black))
                    textSize = 12f
                    typeface = ResourcesCompat.getFont(requireContext(), R.font.work_sans)
                    setPadding(20, 15, 18, 17)
                }
                val imageView = ImageView(requireActivity()).apply {
                    setImageDrawable(requireContext().getDrawable(R.drawable.close_button_white))
                    setOnClickListener {
                        category.remove(item)
                        cuisine.remove(item)
                        chipGroup.removeView(linearLayout)
                        updateList()
                    }
                }
                linearLayout.addView(textView)
                linearLayout.addView(imageView)
                chipGroup.addView(linearLayout)
            }
            chipGroup.chipSpacingVertical = 40
            chipGroup.chipSpacingHorizontal = 30
        }
    }

    private fun setTopLoaderVisibility(value: Int) {
        binding.topProgressBar.visibility = if (value == 0) View.GONE else View.VISIBLE
    }
}