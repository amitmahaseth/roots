package com.rootsrecipes.view.myRecipes

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Resources
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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.rootsrecipes.BuildConfig
import com.rootsrecipes.MainActivity
import com.rootsrecipes.R
import com.rootsrecipes.databinding.CommentsBottomsheetBinding
import com.rootsrecipes.databinding.DialogDeleteBinding
import com.rootsrecipes.databinding.FragmentRecipeInformationBinding
import com.rootsrecipes.databinding.PopupRvBinding
import com.rootsrecipes.databinding.ProcessDialogBinding
import com.rootsrecipes.databinding.ShareDialogBinding
import com.rootsrecipes.utils.AWSSharedPref
import com.rootsrecipes.utils.BaseFragment
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.Extension
import com.rootsrecipes.utils.Extension.copyUrlMethod
import com.rootsrecipes.utils.Extension.shareViaProfileMethod
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
import com.rootsrecipes.view.myRecipes.adapter.CommentAdapter
import com.rootsrecipes.view.myRecipes.adapter.StepsAdapter
import com.rootsrecipes.view.myRecipes.model.CommentData
import com.rootsrecipes.view.setting.viewmodel.SettingVM
import com.rootsrecipes.viewmodel.CommentVM
import com.rootsrecipes.viewmodel.HomeVM
import com.rootsrecipes.viewmodel.ProfileVM
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

class RecipeInformationFragment : BaseFragment() {
    private var commentAdapter: CommentAdapter? = null
    private var commentsBottomsheet: CommentsBottomsheetBinding? = null
    private var bottomSheetDialog: BottomSheetDialog? = null
    private var recipeDetails: RecipeData? = null
    private lateinit var binding: FragmentRecipeInformationBinding
    private var userType: Int = 1
    private val pref: SharedPref by inject()
    private val vm: SettingVM by viewModel()
    private val commentVM: CommentVM by viewModel()
    private val profileVM: ProfileVM by viewModel()
    private val homeVm: HomeVM by viewModel()
    private var isFollowed = false
    private var myRecipeStatus = ""
    private var photoDialog: Dialog? = null
    private var page = 1
    private var limit = 10
    private var isLoading = false
    private var hasMoreData = true
    private var currentPhotoUri: Uri? = null
    private var galleryPath: File? = null
    private val awsPref: AWSSharedPref by inject()
    private val networkHelper: NetworkHelper by inject()
    private var processDialog: AlertDialog? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentRecipeInformationBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getArgumentsData()
        initUi()
        setupAppBarListener()
    }

    private fun initUi() {
        setCommentItemLayout()
        setOnClickMethod()
        binding.clParentRecipeInformation.gone()
        setUserVisibility(userType)
        // Set toolbar title and ensure toolbar is hidden initially
        binding.tvToolbarTitle.text = recipeDetails?.title ?: ""
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setupAppBarListener() {
        binding.appBarLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            val totalScrollRange = appBarLayout.totalScrollRange
            val isCollapsed = Math.abs(verticalOffset) >= totalScrollRange

            // Show/hide the entire toolbar when collapsed
            binding.toolbar.visibility = if (isCollapsed) View.VISIBLE else View.GONE

            // Update toolbar button visibility based on userType when toolbar is visible
            if (isCollapsed) {
                binding.toolbarBackBtn.visibility = View.VISIBLE
                binding.toolbarMoreIcon.visibility = if (userType != 0) View.VISIBLE else View.GONE
                binding.toolbarSaveIcon.visibility = if (userType == 1) View.VISIBLE else View.GONE
                binding.tvToolbarTitle.visibility = View.VISIBLE

                // Sync save icon state
                if (recipeDetails?.isSaved != null) {
                    binding.toolbarSaveIcon.setImageDrawable(
                        requireActivity().getDrawable(
                            if (recipeDetails!!.isSaved!!) R.drawable.saved_recipe_icon_inside
                            else R.drawable.unsaved_icon
                        )
                    )
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun getArgumentsData() {
        if (arguments != null) {
            val bundle = arguments
            userType = bundle!!.getInt(Constants.typeFrom)
            recipeDetails = bundle.getParcelable(Constants.recipeInformation)!!
            Constants.recipeDetailsEdit = RecipeData()
            Constants.recipeDetailsEdit = recipeDetails!!
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setRecipeDetails() {
        binding.apply {
            clParentRecipeInformation.visible()
            tvRecipeName.text = recipeDetails!!.title
            tvToolbarTitle.text = recipeDetails!!.title // Set toolbar title
            setIngredientsAdapter(recipeDetails!!.ingredients!!)
            setStepsAdapter(recipeDetails!!.steps!!)
            if (recipeDetails!!.min_cooking_time == recipeDetails!!.max_cooking_time) {
                tvRecipeTime.text = recipeDetails!!.min_cooking_time
            } else {
                tvRecipeTime.text =
                    recipeDetails!!.min_cooking_time + " - " + recipeDetails!!.max_cooking_time
            }
            if (recipeDetails!!.min_serving_count == recipeDetails!!.max_serving_count) {
                tvServingsCount.text = recipeDetails!!.min_serving_count + " servings"
            } else {
                tvServingsCount.text =
                    recipeDetails!!.min_serving_count + " - " + recipeDetails!!.max_serving_count + " servings"
            }
            tvCuisineType.text = recipeDetails!!.cuisine
            tvCategoryType.text = recipeDetails!!.category
            if (!recipeDetails!!.sub_category.isNullOrEmpty()) {
                tvSubCategoryType.visible()
                tvSubCategoryType.text = recipeDetails!!.sub_category
            }
            tvRecipeDescription.text = recipeDetails!!.short_description
            if (recipeDetails!!.avg_rating != null) {
                recipeRatingBar.rating = recipeDetails!!.avg_rating!!
                totalRatingRecipe.text = "(${recipeDetails!!.avg_rating!!})"
            } else {
                recipeRatingBar.rating = 0F
                totalRatingRecipe.text = "(0)"
            }
            if (!recipeDetails!!.recipe_image.isNullOrEmpty()) {
                Glide.with(requireActivity())
                    .load(BuildConfig.BASE_MEDIA_URL + recipeDetails!!.recipe_image)
                    .into(ivRecipeImage)
            } else {
                if (userType == 2) {
                    ivRecipeImageEdit.visible()
                }
            }

            if (recipeDetails!!.my_recipe_status == Constants.recipeStatusPrivate) {
                setMyRecipeStatusText(0)
            } else {
                setMyRecipeStatusText(1)
            }

            if (recipeDetails!!.user != null) {
                isFollowed = recipeDetails!!.user!!.followStatus!!
                setFollowBtnView()
                tvFullName.text =
                    recipeDetails!!.user!!.first_name + " " + recipeDetails!!.user!!.last_name
                tvUserName.text = recipeDetails!!.user!!.user_name
                if (!recipeDetails!!.user!!.profile_image.isNullOrEmpty()) {
                    Glide.with(requireActivity())
                        .load(BuildConfig.BASE_MEDIA_URL + recipeDetails!!.user!!.profile_image)
                        .into(ivProfile)
                }
            }
            if (recipeDetails!!.isSaved != null) {
                setSaveIcon()
            }
            //if already rated the recipe
            if (userType == 1) {
                if (recipeDetails!!.hasRated!!) {
                    clRatings.gone()
                } else {
                    clRatings.visible()
                }
            }

            /**Suggestions and Notes*/
            if (!recipeDetails!!.notes.isNullOrEmpty()) {
                tvNotes.visible()
                tvNotesValue.visible()
                tvNotesValue.text = recipeDetails!!.notes
            } else {
                tvNotes.gone()
                tvNotesValue.gone()
            }
            if (!recipeDetails!!.suggestions.isNullOrEmpty()) {
                tvSuggestion.visible()
                tvSuggestionValue.visible()
                tvSuggestionValue.text = recipeDetails!!.suggestions
            } else {
                tvSuggestion.gone()
                tvSuggestionValue.gone()
            }

            addLatestComment()
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
    private fun addLatestComment() {
        binding.apply {
            if (recipeDetails!!.latestComment != null) {
                binding.itemCommentLayout.ivProfile.visible()
                binding.itemCommentLayout.tvComment.visible()
                binding.itemCommentLayout.tvTime.visible()
                binding.itemCommentLayout.tvFullName.visible()
                binding.itemCommentLayout.tvComment.maxLines = 1
                binding.tvShowMoreComments.visible()
                val recComment = recipeDetails!!.latestComment
                itemCommentLayout.tvComment.text = recComment!!.comment_text
                itemCommentLayout.tvTime.text =
                    recipeDetails!!.latestComment!!.createdAt?.let { Extension.formatRelativeTime(it) }
                if (recComment.user != null) {
                    itemCommentLayout.tvFullName.text =
                        recComment.user!!.first_name + " " + recComment.user!!.last_name
                    if (!recComment.user!!.profile_image.isNullOrEmpty()) {
                        Glide.with(requireActivity())
                            .load(BuildConfig.BASE_MEDIA_URL + recComment.user!!.profile_image)
                            .into(itemCommentLayout.ivProfile)
                    } else {
                        Glide.with(requireActivity())
                            .load(requireActivity().getDrawable(R.drawable.profile_icon))
                            .into(itemCommentLayout.ivProfile)
                    }
                }
            } else {
                binding.itemCommentLayout.ivProfile.gone()
                binding.itemCommentLayout.tvComment.gone()
                binding.itemCommentLayout.tvTime.gone()
                binding.itemCommentLayout.tvFullName.gone()
                binding.tvShowMoreComments.gone()
            }
        }
    }

    private fun setMyRecipeStatusText(value: Int) {
        binding.apply {
            if (value == 0) {
                //private
                myRecipeStatus = Constants.recipePublish
                tvRecipeType.text = Constants.recipeStatusPrivate
                btnPublish.text = Constants.recipePublish
                tvRecipeType.setTextColor(requireActivity().getColor(R.color.blue))
                tvRecipeType.backgroundTintList =
                    ColorStateList.valueOf(requireActivity().getColor(R.color.private_bg_color))
            } else if (value == 1) {
                //public
                myRecipeStatus = Constants.recipeUnPublish
                tvRecipeType.text = Constants.recipeStatusPublic
                btnPublish.text = Constants.recipeUnPublish
                tvRecipeType.setTextColor(requireActivity().getColor(R.color.light_green))
                tvRecipeType.backgroundTintList =
                    ColorStateList.valueOf(requireActivity().getColor(R.color.public_bg_color))
            }
        }
    }

    private fun setOnClickMethod() {
        binding.apply {
            // Original action buttons
            ivMoreIcon.setOnClickListener {
                Log.d("RecipeFragment", "More icon clicked")
                createPopupMenu(userType, ivMoreIcon)
            }
            ivBackBtn.setOnClickListener {
                Log.d("RecipeFragment", "Back button clicked")
                findNavController().navigateUp()
            }
            ivRecipeImageEdit.setOnClickListener {
                Log.d("RecipeFragment", "Recipe image edit clicked")
                photoDialog()
            }
            ivSaveIcon.setOnClickListener {
                Log.d("RecipeFragment", "Save icon clicked, isSaved: ${recipeDetails!!.isSaved}")
                if (recipeDetails!!.isSaved!!) {
                    hitApiForUnSaveRecipe(recipeDetails!!._id!!)
                } else {
                    hitApiForSaveRecipe(recipeDetails!!._id!!)
                }
            }

            // Toolbar action buttons
            toolbarMoreIcon.setOnClickListener {
                Log.d("RecipeFragment", "Toolbar more icon clicked")
                createPopupMenu(userType, toolbarMoreIcon)
            }
            toolbarBackBtn.setOnClickListener {
                Log.d("RecipeFragment", "Toolbar back button clicked")
                findNavController().navigateUp()
            }
            toolbarSaveIcon.setOnClickListener {
                Log.d(
                    "RecipeFragment",
                    "Toolbar save icon clicked, isSaved: ${recipeDetails!!.isSaved}"
                )
                if (recipeDetails!!.isSaved!!) {
                    hitApiForUnSaveRecipe(recipeDetails!!._id!!)
                } else {
                    hitApiForSaveRecipe(recipeDetails!!._id!!)
                }
            }

            // Recipe image click
            ivRecipeImage.setOnClickListener {
                Log.d("RecipeFragment", "Recipe image clicked")
                if (!recipeDetails!!.recipe_image.isNullOrEmpty() && !recipeDetails!!._id.isNullOrEmpty()) {
                    Extension.getImageViewMethod(
                        url = recipeDetails!!.recipe_image!!, requireContext()
                    )
                }
            }

            // Other click listeners (unchanged)
            btnSave.setOnClickListener {
                val jsonObject = JsonObject()
                val gson = Gson()
                val ingredientsJsonArray = gson.toJsonTree(recipeDetails!!.ingredients).asJsonArray
                val stepsJsonArray = gson.toJsonTree(recipeDetails!!.steps).asJsonArray

                jsonObject.addProperty("title", recipeDetails!!.title)
                jsonObject.addProperty("cuisine", recipeDetails!!.cuisine)
                jsonObject.addProperty("category", recipeDetails!!.category)
                if (!recipeDetails!!.sub_category.isNullOrEmpty()) {
                    jsonObject.addProperty("sub_category", recipeDetails!!.sub_category)
                }
                jsonObject.addProperty("min_cooking_time", recipeDetails!!.min_cooking_time)
                jsonObject.addProperty("max_cooking_time", recipeDetails!!.max_cooking_time)
                jsonObject.addProperty("short_description", recipeDetails!!.short_description)
                jsonObject.addProperty("min_serving_count", recipeDetails!!.min_serving_count)
                jsonObject.addProperty("max_serving_count", recipeDetails!!.max_serving_count)
                jsonObject.addProperty("recipe_image", recipeDetails!!.recipe_image)
                jsonObject.addProperty("transcribed_text", recipeDetails!!.transcribed_text)
                jsonObject.add("ingredients", ingredientsJsonArray)
                jsonObject.add("steps", stepsJsonArray)
                jsonObject.addProperty("notes", recipeDetails!!.notes)
                jsonObject.addProperty("suggestions", recipeDetails!!.suggestions)
                Constants.EDITRECIPE = false
                Constants.recipeDetailsEdit = RecipeData()
                hitAPiToGeneratedRecipes(jsonObject)
            }
            btnEditRecipes.setOnClickListener {
                Constants.EDITRECIPE = true
                findNavController().navigateUp()
            }
            btnPublish.setOnClickListener {
                hitApiToUpdateRecipeStatus()
            }
            tvFollowBtn.setOnClickListener {
                followLoaderVisibility(1)
                tvFollowBtn.isClickable = false
                if (isFollowed) {
                    hitApiForUnFollow()
                } else {
                    hitApiForFollow()
                }
            }
            btnProceed.setOnClickListener {
                if (leaveRatingBar.rating == 0f) {
                    requireActivity().makeToast("Please select a rating before submitting!")
                } else {
                    hitApiToAddRating()
                }
            }
            ivProfile.setOnClickListener {
                if (recipeDetails!!.user != null) {
                    recipeDetails!!.user!!.followStatus = isFollowed
                    val bundle = Bundle()
                    bundle.putParcelable(Constants.userInformation, recipeDetails!!.user)
                    findNavController().navigate(
                        R.id.action_recipeInformationFragment_to_userProfileFragment, bundle
                    )
                }
            }
            tvComments.setOnClickListener {
                showCommentsBottomSheet()
            }
            tvShowMoreComments.setOnClickListener {
                showCommentsBottomSheet()
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setSaveIcon() {
        if (recipeDetails!!.isSaved!!) {
            binding.ivSaveIcon.setImageDrawable(requireActivity().getDrawable(R.drawable.saved_recipe_icon_inside))
            binding.toolbarSaveIcon.setImageDrawable(requireActivity().getDrawable(R.drawable.saved_recipe_icon_inside))
        } else {
            binding.ivSaveIcon.setImageDrawable(requireActivity().getDrawable(R.drawable.unsaved_icon))
            binding.toolbarSaveIcon.setImageDrawable(requireActivity().getDrawable(R.drawable.unsaved_icon))
        }
    }


    @SuppressLint("FragmentLiveDataObserve")
    private fun hitApiToGetCommentList() {
        CoroutineScope(Dispatchers.Main).launch {
            if (page == 1) {
                commentsBottomsheet!!.pbComments.visible()
                commentsBottomsheet!!.rvComments.gone()
                //  Extension.showProgress(requireActivity())
            } else {
                commentsBottomsheet!!.pbComments.gone()
                commentsBottomsheet!!.rvComments.visible()
            }

            commentVM.commentList(recipeDetails!!._id!!, page, limit)
            commentVM.commentListData.observe(this@RecipeInformationFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        Extension.stopProgress()
                        commentsBottomsheet!!.rvComments.visible()
                        commentsBottomsheet!!.pbComments.gone()
                        isLoading = false
                        val commentList = its.data?.data ?: arrayListOf()
                        setAdapterComments(commentList)
                    }

                    Status.ERROR -> {
                        isLoading = false
                        commentsBottomsheet!!.pbComments.gone()
                        commentsBottomsheet!!.rvComments.visible()
                        if (page > 1) page-- // Only revert if not first page
                        its?.let { requireActivity().makeToast(it.message!!) }
                        Extension.stopProgress()
                    }

                    Status.LOADING -> {}
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun hitApiToAddComment(comment: String) {
        val jsonObject = JsonObject()
        jsonObject.addProperty("recipe_id", recipeDetails!!._id)
        jsonObject.addProperty("comment_text", comment.trim())
        commentsBottomsheet!!.etComment.text?.clear()

        CoroutineScope(Dispatchers.Main).launch {
            commentVM.addComment(jsonObject)
            commentVM.addCommentData.observe(viewLifecycleOwner) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        Extension.stopProgress()
                        val commentData = its.data!!.data
                        commentData.user = pref.getSignInData()

                        recipeDetails!!.latestComment = commentData
                        addLatestComment()

                        // Add comment only at the top
                        commentAdapter?.let {
                            if (it.getCommentList()
                                    .isNotEmpty() && it.getCommentList()[0]._id != commentData._id
                            ) {
                                it.getCommentList().add(0, commentData)
                                it.notifyItemInserted(0)
                                commentsBottomsheet!!.rvComments.scrollToPosition(0)
                            }
                        } ?: run {
                            // If adapter is null
                            setAdapterComments(arrayListOf(commentData))
                        }
                    }

                    Status.ERROR -> {
                        its?.let { requireActivity().makeToast(it.message!!) }
                        Extension.stopProgress()
                    }

                    Status.LOADING -> {}
                }
            }
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun setAdapterComments(commentList: ArrayList<CommentData>) {
        if (commentList.isNotEmpty()) {
            if (commentAdapter != null) {
                // Initialize new adapter
                if (page == 1) {
                    // Replace all data for page 1
                    commentAdapter!!.updateCommentList(commentList)
                    commentAdapter!!.notifyDataSetChanged()
                } else {
                    // Add new data for subsequent pages
                    commentAdapter!!.addComments(commentList)
                }

            } else {
                commentAdapter = CommentAdapter(requireContext(), commentList)
                commentsBottomsheet!!.rvComments.adapter = commentAdapter
                commentsBottomsheet!!.rvComments.layoutManager =
                    LinearLayoutManager(requireContext())
                setupScrollListener()
            }
            updateBottomSheetVisibility(true)
        } else {
            if (page == 1) {
                updateBottomSheetVisibility(false)
            }
            hasMoreData = false
        }
    }

    private fun updateBottomSheetVisibility(hasComments: Boolean) {
        commentsBottomsheet?.apply {
            if (hasComments) {
                llNoComment.gone()
                rvComments.visible()
            } else {
                llNoComment.visible()
                rvComments.gone()
            }
        }
    }

    private fun setupScrollListener() {
        commentsBottomsheet!!.rvComments.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && hasMoreData) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0) {
                        loadMoreComments()
                    }
                }
            }
        })
    }

    private fun loadMoreComments() {
        isLoading = true
        page++
        hitApiToGetCommentList()
    }

    private fun showCommentsBottomSheet() {
        // Create new instances and reset state
        bottomSheetDialog?.dismiss() // Dismiss any existing dialog
        bottomSheetDialog = null
        commentsBottomsheet = null

        resetPaginationState()

        bottomSheetDialog =
            BottomSheetDialog(requireActivity(), R.style.CustomBottomSheetDialogTheme)
        commentsBottomsheet =
            CommentsBottomsheetBinding.inflate(LayoutInflater.from(requireActivity()))

        setupBottomSheetUI()

        bottomSheetDialog!!.setContentView(commentsBottomsheet!!.root)
        bottomSheetDialog!!.setOnDismissListener {
            cleanupBottomSheet()
        }

        // Load comments after setup
        commentAdapter = null
        loadInitialComments()

        bottomSheetDialog!!.show()
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun resetPaginationState() {
        page = 1
        isLoading = false
        hasMoreData = true
        if (commentAdapter != null) {
            commentAdapter!!.getCommentList().clear()
            commentAdapter!!.notifyDataSetChanged()
        }
    }

    private fun setupBottomSheetUI() {
        commentsBottomsheet!!.apply {
            // Clear any existing text
            etComment.text?.clear()
            ivCloseComments.setOnClickListener {
                cleanupBottomSheet()
                bottomSheetDialog!!.dismiss()
            }

            ivSend.setOnClickListener {
                if (etComment.text.toString().isNotBlank()) {
                    hitApiToAddComment(etComment.text.toString())
                }
            }

            val bottomSheetBehavior: BottomSheetBehavior<*> =
                (bottomSheetDialog as BottomSheetDialog).behavior
            bottomSheetBehavior.peekHeight = Resources.getSystem().displayMetrics.heightPixels
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

            Extension.trimEditText(etComment)
        }
    }

    private fun loadInitialComments() {
        commentVM.commentListData.removeObservers(this@RecipeInformationFragment)
        commentVM.addCommentData.removeObservers(this@RecipeInformationFragment)
        page = 1
        hitApiToGetCommentList()
    }

    private fun cleanupBottomSheet() {
        // Remove observers
        commentVM.commentListData.removeObservers(viewLifecycleOwner)
        commentVM.addCommentData.removeObservers(viewLifecycleOwner)

        // Reset state
        resetPaginationState()

        // Clean up views
        commentsBottomsheet = null
    }


    @SuppressLint("FragmentLiveDataObserve")
    private fun hitAPiToGeneratedRecipes(jsonObject: JsonObject) {
        CoroutineScope(Dispatchers.Main).launch {
            Extension.showProgress(requireActivity())
            vm.saveGeneratedRecipes(jsonObject)
            vm.saveGenerateRecipesData.observe(this@RecipeInformationFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        Extension.stopProgress()
                        findNavController().popBackStack(R.id.discoverFragment, false)
                    }

                    Status.ERROR -> {
                        its?.let { requireActivity().makeToast(it.message!!) }
                        Extension.stopProgress()
                    }

                    Status.LOADING -> {

                    }
                }
            }
        }
    }

    @SuppressLint("FragmentLiveDataObserve")
    private fun hitApiToDeleteRecipe() {
        CoroutineScope(Dispatchers.Main).launch {
            Extension.showProgress(requireActivity())
            vm.deleteRecipe(recipeDetails!!._id!!)
            vm.deleteRecipeData.observe(this@RecipeInformationFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        its.data?.let { requireActivity().makeToast(it.message) }
                        Extension.stopProgress()
                        findNavController().navigateUp()
                    }

                    Status.ERROR -> {
                        its?.let { requireActivity().makeToast(it.message!!) }
                        Extension.stopProgress()
                    }

                    Status.LOADING -> {

                    }
                }
            }
        }
    }

    @SuppressLint("FragmentLiveDataObserve")
    private fun hitApiToUpdateRecipeStatus() {
        val jsonObject = JsonObject()
        jsonObject.addProperty("recipe_id", recipeDetails!!._id)

        jsonObject.addProperty(
            "status", when (recipeDetails!!.my_recipe_status) {
                Constants.recipeStatusPublic -> {
                    //un-publish
                    recipeDetails!!.my_recipe_status = Constants.recipeStatusPrivate
                    Constants.recipeStatusPrivate
                }

                Constants.recipeStatusPrivate -> {
                    //publish
                    recipeDetails!!.my_recipe_status = Constants.recipeStatusPublic
                    Constants.recipeStatusPublic
                }

                else -> {
                    ""
                }
            }
        )
        CoroutineScope(Dispatchers.Main).launch {
            Extension.showProgress(requireActivity())
            vm.updateRecipeStatus(jsonObject)
            vm.updateRecipeStatusData.observe(this@RecipeInformationFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        Extension.stopProgress()
                        binding.apply {
                            if (recipeDetails!!.my_recipe_status == Constants.recipeStatusPublic) {
                                setMyRecipeStatusText(1)
                            } else {
                                setMyRecipeStatusText(0)
                            }
                        }
                    }

                    Status.ERROR -> {
                        its.data?.let { requireActivity().makeToast(it.message) }
                        Extension.stopProgress()
                    }

                    Status.LOADING -> {

                    }
                }
            }
        }
    }


    @SuppressLint("FragmentLiveDataObserve")
    private fun hitApiForRecipeData() {
        CoroutineScope(Dispatchers.Main).launch {
            Extension.showProgress(requireActivity())
            vm.getRecipeData(recipeId = recipeDetails!!._id!!)
            vm.getRecipeData.observe(this@RecipeInformationFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        Extension.stopProgress()
                        its.data!!.data.let { recipeDetails = it }
                        setRecipeDetails()
                    }

                    Status.ERROR -> {
                        its?.let { requireActivity().makeToast(it.message!!) }
                        Extension.stopProgress()
                    }

                    Status.LOADING -> {

                    }
                }
            }
        }
    }

    private fun setCommentItemLayout() {
        binding.itemCommentLayout.dividerView.gone()
    }

    private fun setIngredientsAdapter(ingredients: ArrayList<String>) {
        val ingredientsAdapter = StepsAdapter(ingredients, 0)

        binding.rvIngredients.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ingredientsAdapter
        }
    }

    private fun setStepsAdapter(steps: ArrayList<String>) {

        val ingredientsAdapter = StepsAdapter(steps, 1)

        binding.rvSteps.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ingredientsAdapter
        }
    }

    private fun createPopupMenu(value: Int, imageView: ImageView) {
        val inflater =
            requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = PopupRvBinding.inflate(inflater)

        val width = LinearLayout.LayoutParams.WRAP_CONTENT
        val height = LinearLayout.LayoutParams.WRAP_CONTENT
        val focusable = true // if you want the popup to receive focus
        val listMy = arrayListOf(
            PopUpData(myRecipeStatus),
            PopUpData("Share"),
            PopUpData("Edit", R.color.blue),
            PopUpData("Delete", R.color.delete_red)
        )
        val listUser = arrayListOf(
            PopUpData("Share")
        )
        val popupWindow = PopupWindow(popupView.root, width, height, focusable)
        val popupRV = popupView.rvPopUp
        val popupAdapter = if (value == 1) {
            PopUpAdapter(listUser, requireContext(), object : CommonStringListener {
                override fun onEventClick(position: Int) {
                    when (position) {
                        0 -> {

                            showShareDialog()
                            popupWindow.dismiss()
                        }
                    }
                }

            })
        } else {
            PopUpAdapter(listMy, requireContext(), object : CommonStringListener {
                override fun onEventClick(position: Int) {
                    when (position) {
                        0 -> {
                            hitApiToUpdateRecipeStatus()
                            popupWindow.dismiss()
                        }

                        1 -> {

                            showShareDialog()
                            popupWindow.dismiss()
                        }

                        2 -> {
                            editRecipe()
                            popupWindow.dismiss()
                        }

                        3 -> {
                            deleteRecipe()
                            popupWindow.dismiss()
                        }
                    }
                }

            })
        }
        popupRV.apply {
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(
                DividerItemDecoration(
                    requireContext(), LinearLayoutManager.VERTICAL
                )
            )
            adapter = popupAdapter
        }

        val marginEnd = 75
        val density = requireContext().resources.displayMetrics.density
        val marginEndPx = (marginEnd * density).toInt()

        popupWindow.showAsDropDown(imageView, -marginEndPx, 0)
    }

    private fun editRecipe() {/*val bundle = Bundle()
        bundle.putParcelable(
            Constants.recipeInformation, recipeDetails
        )
        bundle.putInt(Constants.typeFrom, 1)
        findNavController().navigate(
            R.id.action_recipeInformationFragment_to_reviewRecordingFragment, bundle
        )*/
        val bundle = Bundle()
        bundle.putParcelable(
            Constants.recipeInformation, recipeDetails
        )
        findNavController().navigate(
            R.id.action_recipeInformationFragment_to_editRecipesFragment, bundle
        )
    }

    @SuppressLint("SetTextI18n")
    private fun deleteRecipe() {
        val dialogLayoutBinding = DialogDeleteBinding.inflate(layoutInflater)
        val dialogBuilder = AlertDialog.Builder(requireContext()).setView(dialogLayoutBinding.root)
            .setCancelable(true)


        val dialog = dialogBuilder.create()
        val params = dialog.window?.attributes
        params?.y = 50
        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes = params
        dialogLayoutBinding.tvDelete.text = "Delete recipe"
        dialogLayoutBinding.tvDeleteDetails.text = "Are you sure you want to delete this recipe?"

        dialogLayoutBinding.tvYesDelete.setOnClickListener {
            hitApiToDeleteRecipe()
            dialog.dismiss()
        }
        dialogLayoutBinding.tvCancelDelete.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()

    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setUserVisibility(value: Int) {
        when (value) {
            0 -> {
                // Create my recipe
                binding.apply {
                    setRecipeDetails()
                    llButtonCreateRecipe.visible()
                    ivSaveIcon.gone()
                    toolbarSaveIcon.gone()
                    btnPublish.gone()
                    clUserInfo.gone()
                    clComments.gone()
                    clRatings.gone()
                    tvRecipeType.gone()
                    ivMoreIcon.gone()
                    toolbarMoreIcon.gone()
                }
            }

            1 -> {
                // Other user recipe
                binding.apply {
                    if (recipeDetails != null) {
                        hitApiForRecipeData()
                    }
                    btnPublish.gone()
                    llButtonCreateRecipe.gone()
                    clUserInfo.visible()
                    clComments.visible()
                    tvRecipeType.gone()
                    ivSaveIcon.visible()
                    if (recipeDetails!!.isSaved!!) {
                        ivSaveIcon.setImageDrawable(requireActivity().getDrawable(R.drawable.saved_recipe_icon_inside))
                        toolbarSaveIcon.setImageDrawable(requireActivity().getDrawable(R.drawable.saved_recipe_icon_inside))
                    } else {
                        ivSaveIcon.setImageDrawable(requireActivity().getDrawable(R.drawable.unsaved_icon))
                        toolbarSaveIcon.setImageDrawable(requireActivity().getDrawable(R.drawable.unsaved_icon))
                    }
                }
            }

            2 -> {
                // My recipe view
                binding.apply {
                    if (recipeDetails != null) {
                        hitApiForRecipeData()
                    }
                    btnPublish.visible()
                    clComments.visible()
                    tvRecipeType.visible()
                    llButtonCreateRecipe.gone()
                    clUserInfo.gone()
                    clRatings.gone()
                    ivSaveIcon.gone()
                    toolbarSaveIcon.gone()
                }
            }
        }
    }

    private fun sharedData(type: Int) {
        val metadata: HashMap<String, String> = HashMap()
        metadata["recipeId"] = recipeDetails!!._id + ""
        metadata["recipeUserId"] = recipeDetails!!.user_id + ""
        val branchUniversalObject: BranchUniversalObject =
            BranchUniversalObject().addContentMetadata(metadata)
        val linkProperties = LinkProperties()
        branchUniversalObject.generateShortUrl(requireActivity(), linkProperties) { url, error ->
            if (error == null) {
                Extension.stopProgress()
                val url11 = "What do you think about this recipe ${recipeDetails!!.title!!}? " + url
                when (type) {
                    0 -> shareViaProfileMethod(url11, requireActivity())
                    1 -> copyUrlMethod(url11, requireActivity())
                }

            } else {
                Extension.stopProgress()
                requireActivity().makeToast(error.toString())
                error.toString()
            }
        }
    }

    private fun showShareDialog() {
        val dialogLayoutBinding = ShareDialogBinding.inflate(layoutInflater)
        val dialogBuilder = AlertDialog.Builder(requireContext()).setView(dialogLayoutBinding.root)
            .setCancelable(true)


        val dialog = dialogBuilder.create()
        val params = dialog.window?.attributes
        params?.y = 50
        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes = params
        dialogLayoutBinding.llShareVia.setOnClickListener {
            Extension.showProgress(requireActivity())
            sharedData(0)
            dialog.dismiss()
        }
        dialogLayoutBinding.llShareWith.setOnClickListener {
//            val bundle = Bundle()
//            bundle.putInt(Constants.typeFrom, 1)
//            findNavController().navigate(
//                R.id.action_recipeInformationFragment_to_selectPersonFragment, bundle
//            )
            val bundle = Bundle()
            bundle.putString("recipe_id", recipeDetails!!._id)
            findNavController().navigate(
                R.id.action_recipeInformationFragment_to_shareRecipeFragment,
                bundle
            )
            dialog.dismiss()
//            requireActivity().makeToast("Not Implemented")
        }
        dialogLayoutBinding.llCopyLink.setOnClickListener {
            Extension.showProgress(requireActivity())
            sharedData(1)
            dialog.dismiss()
        }
        dialogLayoutBinding.tvCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    @SuppressLint("FragmentLiveDataObserve", "SetTextI18n")
    private fun hitApiForFollow() {
        val jsonObject = JsonObject()
        jsonObject.addProperty(Constants.targetUserId, recipeDetails!!.user!!._id)
        CoroutineScope(Dispatchers.Main).launch {
            profileVM.followUser(jsonObject)
            profileVM.followData.observe(this@RecipeInformationFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        isFollowed = true
                        followLoaderVisibility(0)
                        Extension.stopProgress()
                        setFollowBtnView()
                    }

                    Status.ERROR -> {
                        followLoaderVisibility(0)
                        binding.tvFollowBtn.isClickable = true
                        its.data?.let { requireActivity().makeToast(it.message) }
                    }

                    Status.LOADING -> {

                    }
                }
            }
        }
    }

    @SuppressLint("FragmentLiveDataObserve", "SetTextI18n")
    private fun hitApiForUnFollow() {
        val jsonObject = JsonObject()
        jsonObject.addProperty(Constants.targetUserId, recipeDetails!!.user!!._id)
        CoroutineScope(Dispatchers.Main).launch {
            profileVM.unfollowUser(jsonObject)
            profileVM.unfollowData.observe(this@RecipeInformationFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        isFollowed = false
                        followLoaderVisibility(0)
                        Extension.stopProgress()
                        setFollowBtnView()
                    }

                    Status.ERROR -> {
                        followLoaderVisibility(0)
                        binding.tvFollowBtn.isClickable = true
                        its.data?.let { requireActivity().makeToast(it.message) }
                    }

                    Status.LOADING -> {

                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setFollowBtnView() {
        if (isFollowed) {
            binding.tvFollowBtn.apply {
                isClickable = true
                backgroundTintList =
                    ColorStateList.valueOf(requireActivity().getColor(R.color.green))
                setTextColor(requireActivity().getColor(R.color.white))
                text = "Following"
            }
        } else {
            binding.tvFollowBtn.apply {
                isClickable = true
                backgroundTintList =
                    ColorStateList.valueOf(requireActivity().getColor(R.color.category_color))
                setTextColor(requireActivity().getColor(R.color.green))
                text = "Follow"
            }
        }
    }

    private fun followLoaderVisibility(value: Int) {
        binding.apply {
            if (value == 0) {
                followLoader.gone()
            } else if (value == 1) {
                followLoader.visible()
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables", "FragmentLiveDataObserve")
    private fun hitApiForSaveRecipe(recipeId: String) {
        val jsonObject = JsonObject()
        jsonObject.addProperty("recipe_id", recipeId)
        CoroutineScope(Dispatchers.Main).launch {
            if (isAdded) {
                vm.saveRecipe(jsonObject)
                vm.saveRecipeData.observe(this@RecipeInformationFragment) { its ->
                    when (its.status) {
                        Status.SUCCESS -> {
                            recipeDetails!!.isSaved = true
                            setSaveIcon()
                            updateHomeData()
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

    @SuppressLint("FragmentLiveDataObserve")
    private fun hitApiForUnSaveRecipe(recipeId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            vm.unSaveRecipe(recipeId)
            vm.unSaveRecipeData.observe(this@RecipeInformationFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        recipeDetails!!.isSaved = false
                        setSaveIcon()
                        updateHomeData()
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
    private fun updateHomeData() {
        homeVm.homeAllData.observe(this@RecipeInformationFragment) {
            it.data!!.data!!.map { recipes ->
                recipes.recipes!!.map { recipeData ->
                    if (recipeData._id == recipeDetails!!._id) {
                        recipeData.isSaved = recipeDetails!!.isSaved
                        recipeData.user!!.followStatus = recipeDetails!!.user!!.followStatus
                    }
                }
            }
        }
    }


    @SuppressLint("SetTextI18n")
    private fun hitApiToAddRating() {
        val jsonObject = JsonObject()
        jsonObject.addProperty("recipe_id", recipeDetails!!._id)
        jsonObject.addProperty("rating", binding.leaveRatingBar.rating)
        CoroutineScope(Dispatchers.Main).launch {
            commentVM.rateRecipe(jsonObject)
            commentVM.rateRecipeData.observe(viewLifecycleOwner) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        Extension.stopProgress()
                        binding.recipeRatingBar.rating = binding.leaveRatingBar.rating
                        recipeDetails!!.avg_rating = binding.leaveRatingBar.rating
                        binding.totalRatingRecipe.text = "(${recipeDetails!!.avg_rating})"
                        requireActivity().makeToast("Thank you for your rating!")
                        binding.clRateRecipe.gone()
                        binding.tvLeaveRating.gone()

                    }

                    Status.ERROR -> {
                        its?.let { requireActivity().makeToast(it.message!!) }
                        Extension.stopProgress()
                    }

                    Status.LOADING -> {}
                }
            }
        }
    }

    /**type **/
    private fun photoDialog() {
        photoDialog?.dismiss()
        photoDialog = Dialog(requireContext())
        photoDialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val window: Window? = photoDialog!!.window
        val wlp = window!!.attributes
        wlp.gravity = Gravity.BOTTOM
        window.attributes = wlp
        photoDialog!!.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        photoDialog!!.setContentView(R.layout.photo_dialog)
        photoDialog!!.window!!.setLayout(
            ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        photoDialog?.show()

        val llGallery: LinearLayout = photoDialog!!.findViewById(R.id.llGallery)
        val llCamera: LinearLayout = photoDialog!!.findViewById(R.id.llCamera)
        val tvCancel: TextView = photoDialog!!.findViewById(R.id.tvCancel)


        llGallery.setOnClickListener {
            val currentAPIVersion: Int = Build.VERSION.SDK_INT
            if (currentAPIVersion >= Build.VERSION_CODES.TIRAMISU) {
                permissionForHigherVersion()
            } else {
                permissionForLowerVersion()
            }
            photoDialog?.dismiss()
        }
        llCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openCamera()
            } else {
                // Request camera permission
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            photoDialog?.dismiss()
        }
        tvCancel.setOnClickListener {
            photoDialog!!.dismiss()
        }
    }

    private fun permissionForLowerVersion() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            openGallery()
        } else {
            // Request permission
            requestGalleryPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun permissionForHigherVersion() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            openGallery()
        } else {
            // Request permission
            requestGalleryPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
        }
    }


    private val requestGalleryPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openGallery()
            } else {
                requireActivity().makeToast("Permission denied")
            }
        }
    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openCamera()
            } else {
                requireActivity().makeToast("Permission denied")
            }
        }

    private val getImageResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val selectedImageUri: Uri? = data?.data
                if (selectedImageUri != null) {
                    try {
                        binding.apply {
                            Glide.with(requireActivity()).load(selectedImageUri).into(ivRecipeImage)
                        }
                        galleryPath = copyUriToInternalStorage(selectedImageUri)
                        newUploadMethod(galleryPath!!)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e("CatchError", "Error: ${e.message}")
                    }
                }
            }
        }

    private fun openCamera() {
        val photoFile: File?
        try {
            photoFile = createImageFile()
        } catch (ex: IOException) {
            // Error occurred while creating the File
            ex.printStackTrace()
            return
        }

        photoFile.let { file ->
            currentPhotoUri = FileProvider.getUriForFile(
                requireActivity(), "com.rootsrecipes.fileprovider", file
            )

            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri)
            }
            captureImageLauncher.launch(cameraIntent)
        }
    }

    private val captureImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Use the saved URI instead of result.data?.data
                galleryPath = null
                currentPhotoUri?.let { uri ->
                    galleryPath = copyUriToInternalStorage(uri)
                    Glide.with(requireActivity()).load(uri).into(binding.ivRecipeImage)
                    binding.ivRecipeImage.clipToOutline = true
                    newUploadMethod(galleryPath!!)
                }
            }
        }

    private fun copyUriToInternalStorage(uri: Uri): File {
        val inputStream = requireActivity().contentResolver.openInputStream(uri)
        val file =
            File(requireContext().filesDir, "profile_image_${System.currentTimeMillis()}.jpg")
        inputStream?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        return File.createTempFile(
            imageFileName, ".jpg", storageDir
        )
    }

    private fun newUploadMethod(uploadGalleryPath: File) {
        // showProcessDialog()
        Extension.showProgress(requireActivity())
        val transferUtility = TransferUtility.builder().context(requireActivity())
            .defaultBucket(awsPref.fetchString(Constants.aws_bucket_name)).s3Client(
                MainActivity.s3client
            ).build()
        if (networkHelper.isNetworkConnected()) {
            val background = object : Thread() {
                override fun run() {
                    try {
                        val key = "RootsAndRecipes/Media/${System.currentTimeMillis()}/image.jpg"
                        val uploadObserver = transferUtility.upload(key, uploadGalleryPath)

                        uploadObserver.setTransferListener(object : TransferListener {
                            override fun onStateChanged(id: Int, state: TransferState?) {
                                if (state == TransferState.COMPLETED) {
                                    val jsonObject = JsonObject()
                                    jsonObject.addProperty(Constants.recipe_image, key)
                                    jsonObject.addProperty("recipe_id", recipeDetails!!._id)
                                    hitApiUploadImageOnly(jsonObject)
                                } else if (state == TransferState.FAILED) {
                                    Extension.stopProgress()
                                    if (processDialog != null) {
                                        processDialog!!.dismiss()
                                    }
                                    requireActivity().makeToast("Profile picture not uploaded.")
                                }
                            }

                            override fun onProgressChanged(
                                id: Int, bytesCurrent: Long, bytesTotal: Long
                            ) {
                                // Update progress if needed
                            }

                            override fun onError(id: Int, ex: Exception?) {
                                // Handle error
                                Extension.stopProgress()
                            }
                        })
                    } catch (e: AmazonServiceException) {
                        Extension.stopProgress()
                        e.printStackTrace()
                    } catch (e: AmazonClientException) {
                        Extension.stopProgress()
                        e.printStackTrace()
                    } catch (e: Exception) {
                        Extension.stopProgress()
                        e.printStackTrace()
                    }
                }
            }
            background.start()
        } else {
            Extension.stopProgress()
            requireActivity().makeToast("Internet not available")
        }
    }

    private fun hitApiUploadImageOnly(jsonObject: JsonObject) {
        CoroutineScope(Dispatchers.Main).launch {
            vm.saveRecipeImage(jsonObject)
            vm.saveRecipesImageData.observe(this@RecipeInformationFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        if (processDialog != null) {
                            processDialog!!.dismiss()
                        }
                        Extension.stopProgress()
                    }

                    Status.ERROR -> {
                        Extension.stopProgress()
                        if (processDialog != null) {
                            processDialog!!.dismiss()
                        }
                        its.message?.let { requireActivity().makeToast(it) }
                    }

                    Status.LOADING -> {

                    }
                }
            }
        }

    }

    private fun openGallery() {
//        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
//            type = "image/*"
//        }
//        val chooserIntent = Intent.createChooser(intent, getString(R.string.str_select_picture))

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        getImageResultLauncher.launch(intent)
    }

    private fun showProcessDialog() {
        val dialogLayoutBinding = ProcessDialogBinding.inflate(layoutInflater)
        val dialogBuilder = AlertDialog.Builder(requireContext()).setView(dialogLayoutBinding.root)
            .setCancelable(true)


        processDialog = dialogBuilder.create()
        val params = processDialog!!.window?.attributes
        params?.y = 50
        processDialog!!.window?.setGravity(Gravity.BOTTOM)
        processDialog!!.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        processDialog!!.window?.attributes = params
        dialogLayoutBinding.btnCancelProcess.gone()
        dialogLayoutBinding.btnCancelProcess.setOnClickListener {
            processDialog!!.dismiss()
        }
        processDialog!!.show()
    }
}