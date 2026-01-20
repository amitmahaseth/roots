package com.rootsrecipes.view.myRecipes

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.gson.JsonObject
import com.rootsrecipes.BuildConfig
import com.rootsrecipes.R
import com.rootsrecipes.databinding.FragmentUserProfileBinding
import com.rootsrecipes.databinding.PopupRvBinding
import com.rootsrecipes.databinding.ShareDialogBinding
import com.rootsrecipes.model.OnItemClickListener
import com.rootsrecipes.model.User
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.Extension
import com.rootsrecipes.utils.Status
import com.rootsrecipes.utils.gone
import com.rootsrecipes.utils.makeToast
import com.rootsrecipes.utils.popUpMenu.CommonStringListener
import com.rootsrecipes.utils.popUpMenu.PopUpAdapter
import com.rootsrecipes.utils.popUpMenu.PopUpData
import com.rootsrecipes.utils.visible
import com.rootsrecipes.view.createAccount.model.RecipeData
import com.rootsrecipes.view.myRecipes.adapter.MyRecipeAdapter
import com.rootsrecipes.viewmodel.ProfileVM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.util.LinkProperties

class UserProfileFragment : Fragment() {
    private lateinit var binding: FragmentUserProfileBinding
    private var bundle: Bundle? = null
    private var userDetails: User? = null
    private val profileVM: ProfileVM by viewModel()
    private var isFollowed = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentUserProfileBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUi()
    }

    private fun initUi() {
        if (arguments != null) {
            bundle = arguments
            userDetails = bundle!!.getParcelable(Constants.userInformation)!!

        }
        if (userDetails != null) {
            if(userDetails!!.user_name.isNullOrEmpty()){
                binding.swipeRefreshLayout.gone()
                hitApiUserDetails(userDetails!!._id!!)
            }else {
                setUserDetails()
            }
        }
        setOnClickMethod()
        setupPullToRefresh()
    }
    private fun setupPullToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
          hitApiUserDetails(userDetails!!._id!!)
        }
    }

    private fun setOnClickMethod() {
        binding.apply {
            btnFollow.setOnClickListener {
                /**Commented for beta testing**/
                btnFollow.isClickable = false
                if (isFollowed) {
                    hitApiForUnFollow()
                } else {
                    hitApiForFollow()
                }
            }

            ivBackUserProfile.setOnClickListener {
                findNavController().navigateUp()
            }

            btnShowAllRecipes.setOnClickListener {
                val bundle = Bundle()
                bundle.putInt(Constants.recipeListTypeFrom, 11)
                bundle.putParcelable(Constants.userInformation, userDetails)
                Constants.DiscoverRecipesScroll = false
                findNavController().navigate(
                    R.id.action_userProfileFragment_to_recipesListFragment,
                    bundle
                )
            }
            ivMore.setOnClickListener {
                createPopupMenu()
            }

            ivUserPfRecipes.setOnClickListener{
                if(!userDetails!!.profile_image!!.isNullOrEmpty()) {
                    Extension.getImageViewMethod(
                        url = userDetails!!.profile_image!!,
                        requireContext()
                    )
                }
            }

            llFollowers.setOnClickListener {
                val bundle = Bundle()
                bundle.putInt(Constants.typeFrom, 0)
                bundle.putInt(Constants.userTypeFrom , 1)
                bundle.putString(Constants.userId , userDetails!!._id)
                bundle.putString(Constants.user_name , userDetails!!.user_name)
                findNavController().navigate(
                    R.id.action_userProfileFragment_to_followersFragment, bundle
                )
            }
            llFollowing.setOnClickListener {
                val bundle = Bundle()
                bundle.putInt(Constants.typeFrom, 1)
                bundle.putInt(Constants.userTypeFrom , 1)
                bundle.putString(Constants.userId , userDetails!!._id)
                bundle.putString(Constants.user_name , userDetails!!.user_name)
                findNavController().navigate(
                    R.id.action_userProfileFragment_to_followersFragment, bundle
                )
            }

        }
    }
    private fun sharedData(type:Int) {
        val metadata: HashMap<String, String> = HashMap()
        metadata["userId"] = userDetails!!._id + ""
        val branchUniversalObject: BranchUniversalObject =
            BranchUniversalObject().addContentMetadata(metadata)
        val linkProperties = LinkProperties()
        branchUniversalObject.generateShortUrl(requireActivity(), linkProperties) { url, error ->
            if (error == null) {
                Extension.stopProgress()
                val url11 = "What do you think about ${userDetails!!.first_name!!} ${userDetails!!.last_name!!}? " + url
                when (type) {
                    0 -> Extension.shareViaProfileMethod(url11, requireActivity())
                    1 -> Extension.copyUrlMethod(url11, requireActivity())
                }
            } else {
                Extension.stopProgress()
                requireActivity().makeToast(error.toString())
                error.toString()
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
    private fun setUserDetails() {
        binding.apply {
            swipeRefreshLayout.visible()
            isFollowed = userDetails!!.followStatus!!
            setFollowBtnView()
            tvUserName.text = userDetails!!.user_name
            tvUserBio.text = userDetails!!.about_me
            tvFullName.text = userDetails!!.first_name + " " + userDetails!!.last_name
            tvTotalFollowers.text = userDetails!!.followers_count.toString()
            tvTotalFollowing.text = userDetails!!.following_count.toString()
            if (!userDetails!!.profile_image.isNullOrEmpty()) {
                Glide.with(requireActivity())
                    .load(BuildConfig.BASE_MEDIA_URL + userDetails!!.profile_image)
                    .placeholder(requireActivity().getDrawable(R.drawable.profile_icon))
                    .into(binding.ivUserPfRecipes)
            }
/**Commented for beta testing**/
          //  hitApiToGetUserRecipes()
        }
    }

    @SuppressLint("FragmentLiveDataObserve", "SetTextI18n")
    private fun hitApiForFollow() {
        val jsonObject = JsonObject()
        jsonObject.addProperty(Constants.targetUserId, userDetails!!._id)
        CoroutineScope(Dispatchers.Main).launch {
            profileVM.followUser(jsonObject)
            profileVM.followData.observe(this@UserProfileFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        isFollowed = true
                        userDetails!!.followers_count = userDetails!!.followers_count?.plus(1)
                        binding.tvTotalFollowers.text = "${userDetails!!.followers_count!!}"
                        Extension.stopProgress()
                        setFollowBtnView()
                    }

                    Status.ERROR -> {
                        binding.btnFollow.isClickable = true
                        its?.let { requireActivity().makeToast(it.message!!) }
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
        jsonObject.addProperty(Constants.targetUserId, userDetails!!._id)
        CoroutineScope(Dispatchers.Main).launch {
            profileVM.unfollowUser(jsonObject)
            profileVM.unfollowData.observe(this@UserProfileFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        isFollowed = false
                        userDetails!!.followers_count = userDetails!!.followers_count?.minus(1)
                        if (userDetails!!.followers_count!! >= 0) {
                            binding.tvTotalFollowers.text = "${userDetails!!.followers_count}"
                        }
                        Extension.stopProgress()
                        setFollowBtnView()
                    }

                    Status.ERROR -> {
                        binding.btnFollow.isClickable = true
                        its?.let { requireActivity().makeToast(it.message!!) }
                    }

                    Status.LOADING -> {

                    }
                }
            }
        }
    }

    @SuppressLint("FragmentLiveDataObserve")
    private fun hitApiToGetUserRecipes() {
        CoroutineScope(Dispatchers.Main).launch {
            profileVM.userRecipes(userDetails!!._id!!, 1, 4)
            profileVM.userRecipesData.observe(this@UserProfileFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        Extension.stopProgress()
                        binding.apply {
                            if (its.data!!.data.recipes.isEmpty()) {
                                tvLatestRecipes.gone()
                                rvUserRecipes.gone()
                                btnShowAllRecipes.gone()
                            } else {
                                tvLatestRecipes.visible()
                                rvUserRecipes.visible()
                                if (its.data.data.recipes.size >= 4) {
                                    btnShowAllRecipes.visible()
                                } else {
                                    btnShowAllRecipes.gone()
                                }
                            }
                        }

                        setRecipeAdapter(its.data!!.data.recipes)
                    }

                    Status.ERROR -> {
                        its?.let { requireActivity().makeToast(it.message!!) }
                    }

                    Status.LOADING -> {

                    }
                }
            }
        }

    }

    private fun setRecipeAdapter(data: ArrayList<RecipeData>) {
        binding.rvUserRecipes.apply {
            layoutManager = GridLayoutManager(requireActivity(), 2)
            adapter = MyRecipeAdapter(requireActivity(), data, object : OnItemClickListener {
                override fun onItemClick(position: Int, type: String) {
                    if (type == Constants.recipeInformation) {
                        val bundle = Bundle()
                        val recipeInfo = data[position]
                        recipeInfo.user = userDetails
                        bundle.putInt(Constants.typeFrom, 1)
                        bundle.putParcelable(Constants.recipeInformation, recipeInfo)
                        findNavController().navigate(
                            R.id.action_userProfileFragment_to_recipeInformationFragment,
                            bundle
                        )
                    }
                }
            } , userTypeFrom = 1)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setFollowBtnView() {
        if (isFollowed) {
            binding.btnFollow.apply {
                isClickable = true
                backgroundTintList =
                    ColorStateList.valueOf(requireActivity().getColor(R.color.green))
                setTextColor(requireActivity().getColor(R.color.white))
                text = "Following"
            }
        } else {
            binding.btnFollow.apply {
                isClickable = true
                backgroundTintList =
                    ColorStateList.valueOf(requireActivity().getColor(R.color.category_color))
                setTextColor(requireActivity().getColor(R.color.green))
                text = "Follow"
            }
        }
    }

    private fun createPopupMenu() {
        val inflater =
            requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = PopupRvBinding.inflate(inflater)

        val width = LinearLayout.LayoutParams.WRAP_CONTENT
        val height = LinearLayout.LayoutParams.WRAP_CONTENT
        val focusable = true // if you want the popup to receive focus
        val listUser = arrayListOf(
            PopUpData("Share Profile")
        )
        val popupWindow = PopupWindow(popupView.root, width, height, focusable)
        val popupRV = popupView.rvPopUp
        val popupAdapter =
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

        popupWindow.showAsDropDown(binding.ivMore, -marginEndPx, 0)
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
           /* val bundle = Bundle()
            bundle.putInt(Constants.typeFrom, 1)
            findNavController().navigate(
                R.id.action_recipeInformationFragment_to_selectPersonFragment, bundle
            )
            dialog.dismiss()*/
            requireActivity().makeToast("Not Implemented")
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

    @SuppressLint("FragmentLiveDataObserve")
    private fun hitApiUserDetails(userId:String) {
        CoroutineScope(Dispatchers.Main).launch {
           profileVM.getUserDetails(userId)
            profileVM.getUserDetailsData.observe(this@UserProfileFragment) { userData ->
                when (userData.status) {
                    Status.SUCCESS -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        Extension.stopProgress()
                        userDetails = userData.data!!.data
                        setUserDetails()

                    }

                    Status.ERROR -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        Extension.stopProgress()
                        userData.message?.let { requireActivity().makeToast(it) }
                    }

                    Status.LOADING -> {
                        // Loading state handled above
                    }
                }
            }
        }
    }
}