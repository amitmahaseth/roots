package com.rootsrecipes.view.setting

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonObject
import com.rootsrecipes.R
import com.rootsrecipes.databinding.DialogRemoveFollowerBinding
import com.rootsrecipes.databinding.FragmentFollowersBinding
import com.rootsrecipes.model.OnItemClickListener
import com.rootsrecipes.utils.BaseFragment
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.Extension
import com.rootsrecipes.utils.SharedPref
import com.rootsrecipes.utils.Status
import com.rootsrecipes.utils.gone
import com.rootsrecipes.utils.makeToast
import com.rootsrecipes.utils.visible
import com.rootsrecipes.view.myRecipes.model.ConnectionUserData
import com.rootsrecipes.view.setting.adapter.FollowerAdapter
import com.rootsrecipes.viewmodel.ProfileVM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel


class FollowersFragment : BaseFragment() {

    private lateinit var binding: FragmentFollowersBinding
    private var followerAdapter: FollowerAdapter? = null
    private val pref: SharedPref by inject()
    private var removeFollowerDialog: Dialog? = null
    private val vm: ProfileVM by viewModel()

    private var page = 1
    private var limit = 10
    private var searchKeyword = ""

    private var isLoading = false
    private var hasMoreData = true
    private var currentList = mutableListOf<ConnectionUserData>()
    private var debounceJob: Job? = null

    private var userTypeFrom = 0
    private var userId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("sdfjfsflks","onCreate")
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentFollowersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        followerAdapter = null
        initUi()
    }


    private fun initUi() {
        setDataForUser()
        setOnClickMethod()
        setupPagination()
        resetPaginationState()
        setupPullToRefresh()
    }

    private fun setDataForUser() {
        val bundle = this.arguments
        var userName = ""
        if (bundle != null) {
            if (bundle.getInt(Constants.typeFrom) == 0) {
                changeColorForFollower()
                apiForFollowers()
                binding.tvFollowerFollowing.text = getString(R.string.all_followers)
            } else if (bundle.getInt(Constants.typeFrom) == 1) {
                changeColorForFollowing()
                apiForFollowings()
                binding.tvFollowerFollowing.text = getString(R.string.all_followings)
            }

            userTypeFrom = bundle.getInt(Constants.userTypeFrom)

            if (userTypeFrom == 1) {
                userId = bundle.getString(Constants.userId)!!
                userName = bundle.getString(Constants.user_name)!!
            }
        }

        binding.apply {
            if (userTypeFrom == 0) {
                tvUserNameFollow.text = pref.getSignInData()?.user_name
            } else {
                tvUserNameFollow.text = userName
            }
        }
    }

    private fun showLoadingState() {
        binding.progressBar.visible()
        binding.rvFollowersList.gone()
    }

    private fun hideLoadingState() {
        binding.progressBar.gone()
        binding.rvFollowersList.visible()
    }

    private fun showLoadMoreProgress() {
        binding.loadMoreProgress.visible()
    }

    private fun hideLoadMoreProgress() {
        binding.loadMoreProgress.gone()
    }

    private fun setupPullToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            currentList.clear()
            resetPaginationState()
            when (binding.tvFollowerFollowing.text) {
                getString(R.string.all_followers) -> apiForFollowers()
                getString(R.string.all_followings) -> apiForFollowings()
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setOnClickMethod() {
        binding.apply {
            tvFollowersClick.setOnClickListener {
                if (tvFollowerFollowing.text != getString(R.string.all_followers)) {
                    currentList.clear()
                    if (followerAdapter != null) {
                        followerAdapter!!.updateList(ArrayList())
                    }
                    resetPaginationState()
                    changeColorForFollower()
                    tvFollowerFollowing.text = getString(R.string.all_followers)
                    apiForFollowers()
                }
            }

            tvFollowingClick.setOnClickListener {
                if (tvFollowerFollowing.text != getString(R.string.all_followings)) {
                    currentList.clear()
                    if (followerAdapter != null) {
                        followerAdapter!!.updateList(ArrayList())
                    }
                    resetPaginationState()
                    changeColorForFollowing()
                    tvFollowerFollowing.text = getString(R.string.all_followings)
                    apiForFollowings()
                }
            }

            val closeButtonImage =
                svSearchFollowers.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
            val searchIcon =
                svSearchFollowers.findViewById<ImageView>(androidx.appcompat.R.id.search_mag_icon)
            searchIcon.setImageDrawable(requireActivity().getDrawable(R.drawable.search_icon))
            closeButtonImage.setOnClickListener {
                svSearchFollowers.setQuery("", false)
                searchKeyword = ""
                debounceJob?.cancel() // Cancel any pending debounce job
                currentList.clear()
                if (followerAdapter != null) {
                    followerAdapter!!.updateList(ArrayList()) // Clear adapter list
                }
                resetPaginationState()
                when (binding.tvFollowerFollowing.text) {
                    getString(R.string.all_followers) -> apiForFollowers()
                    getString(R.string.all_followings) -> apiForFollowings()
                }
            }
            svSearchFollowers.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    newText?.let {
                        if (it.startsWith(" ")) {
                            svSearchFollowers.setQuery(it.trimStart(), false)
                        }
                    }
                    debounceJob?.cancel() // Cancel previous debounce job
                    debounceJob = CoroutineScope(Dispatchers.Main).launch {
                        delay(300)
                        newText?.let {
                            searchKeyword = it.trim() // Trim to handle spaces
                            currentList.clear()
                            if (followerAdapter != null) {
                                followerAdapter!!.updateList(ArrayList()) // Clear adapter list
                            }
                            resetPaginationState()
                            if (isAdded) {
                                when (binding.tvFollowerFollowing.text) {
                                    getString(R.string.all_followers) -> apiForFollowers()
                                    getString(R.string.all_followings) -> apiForFollowings()
                                }
                            }
                        }
                    }
                    return true
                }
            })
            ivBackFollowers.setOnClickListener {
                findNavController().popBackStack()
            }
        }
    }

    @SuppressLint("FragmentLiveDataObserve")
    private fun apiForFollowers(isRefreshing: Boolean = false) {
        CoroutineScope(Dispatchers.Main).launch {
            if (page == 1) {
                if (!isRefreshing) {
                    showLoadingState()
                }
                currentList.clear()
                if (followerAdapter != null) {
                    followerAdapter!!.updateList(ArrayList()) // Clear adapter list
                }
            } else {
                showLoadMoreProgress()
            }
            if (userTypeFrom == 0) {
                vm.myConnections(Constants.FOLLOWER_KEY, searchKeyword, page, limit)
            } else {
                vm.userConnections(Constants.FOLLOWER_KEY, userId, searchKeyword, page, limit)
            }
            // Remove previous observers to prevent multiple triggers
            vm.connectionUserData.removeObservers(this@FollowersFragment)
            vm.connectionUserData.observe(this@FollowersFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        if (page == 1) {
                            hideLoadingState()
                            binding.swipeRefreshLayout.isRefreshing = false
                        } else {
                            hideLoadMoreProgress()
                        }
                        its.data?.let {
                            if (it.data.isEmpty()) {
                                hasMoreData = false
                                if (page == 1) {
                                    setNoDataVisibility(1)
                                }
                            } else {
                                setNoDataVisibility(0)
                                if (binding.tvFollowerFollowing.text == getString(R.string.all_followers)) {
                                    currentList.clear() // Clear before adding to avoid duplicates
                                    currentList.addAll(it.data)
                                    if (followerAdapter == null) {
                                        setAdapterFollowers(ArrayList(currentList), 0)
                                    } else {
                                        followerAdapter!!.updateListType(0)
                                        followerAdapter!!.updateList(ArrayList(currentList)) // Update existing adapter
                                        followerAdapter!!.notifyDataSetChanged()
                                    }
                                }
                                if (it.data.size < limit) {
                                    hasMoreData = false
                                }
                            }
                        }
                        isLoading = false
                    }

                    Status.ERROR -> {
                        if (page == 1) {
                            hideLoadingState()
                            binding.swipeRefreshLayout.isRefreshing = false
                            setNoDataVisibility(1)
                        } else {
                            hideLoadMoreProgress()
                            page--
                        }
                        isLoading = false
                        its?.let { requireActivity().makeToast(it.message!!) }
                    }

                    Status.LOADING -> {
                        if (page == 1 && !binding.swipeRefreshLayout.isRefreshing) {
                            showLoadingState()
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("FragmentLiveDataObserve")
    private fun apiForFollowings(isRefreshing: Boolean = false) {
        CoroutineScope(Dispatchers.Main).launch {
            if (page == 1) {
                if (!isRefreshing) {
                    showLoadingState()
                }
                currentList.clear()
                if (followerAdapter != null) {
                    followerAdapter!!.updateList(ArrayList()) // Clear adapter list
                }
            } else {
                showLoadMoreProgress()
            }
            if (userTypeFrom == 0) {
                vm.myConnections(Constants.FOLLOWING_KEY, searchKeyword, page, limit)
            } else {
                vm.userConnections(Constants.FOLLOWING_KEY, userId, searchKeyword, page, limit)
            }
            // Remove previous observers to prevent multiple triggers
            vm.connectionUserData.removeObservers(this@FollowersFragment)
            vm.connectionUserData.observe(this@FollowersFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        if (page == 1) {
                            hideLoadingState()
                            binding.swipeRefreshLayout.isRefreshing = false
                        } else {
                            hideLoadMoreProgress()
                        }
                        its.data?.let {
                            if (it.data.isEmpty()) {
                                hasMoreData = false
                                if (page == 1) {
                                    setNoDataVisibility(1)
                                }
                            } else {
                                setNoDataVisibility(0)
                                if (binding.tvFollowerFollowing.text == getString(R.string.all_followings)) {
                                    currentList.clear() // Clear before adding to avoid duplicates
                                    currentList.addAll(it.data)
                                    if (followerAdapter == null) {
                                        setAdapterFollowers(ArrayList(currentList), 1)
                                    } else {
                                        followerAdapter!!.updateListType(1)
                                        followerAdapter!!.updateList(ArrayList(currentList))
                                        followerAdapter!!.notifyDataSetChanged()
                                    }
                                }
                                if (it.data.size < limit) {
                                    hasMoreData = false
                                }
                            }
                        }
                        isLoading = false
                    }

                    Status.ERROR -> {
                        if (page == 1) {
                            hideLoadingState()
                            binding.swipeRefreshLayout.isRefreshing = false
                            setNoDataVisibility(1)
                        } else {
                            hideLoadMoreProgress()
                            page--
                        }
                        isLoading = false
                        its?.let { requireActivity().makeToast(it.message!!) }
                    }

                    Status.LOADING -> {
                        if (page == 1 && !binding.swipeRefreshLayout.isRefreshing) {
                            showLoadingState()
                        }
                    }
                }
            }
        }
    }

    private fun resetPaginationState() {
        page = 1
        hasMoreData = true
        isLoading = false
        currentList.clear()
        if (followerAdapter != null) {
            followerAdapter!!.updateList(ArrayList())
        }
        hideLoadMoreProgress()
    }

    private fun setNoDataVisibility(value: Int) {
        binding.apply {
            if (value == 0) {
                clSearchFollowers.visible()
                tvFollowerFollowing.visible()
                rvFollowersList.visible()
                clNoUserFound.gone()
            } else if (value == 1) {
                if (searchKeyword.isEmpty()) {
                    clSearchFollowers.gone()
                    tvFollowerFollowing.gone()
                    rvFollowersList.gone()
                }
                clNoUserFound.visible()
            }

        }
    }

    private fun setAdapterFollowers(followList: ArrayList<ConnectionUserData>, listType: Int) {
//        if (followerAdapter == null) {
        followerAdapter = FollowerAdapter(
            requireContext(),
            loginId = pref.getSignInData()!!._id!!,
            followList,
            listType,
            userTypeFrom,
            object : OnItemClickListener {
                override fun onItemClick(position: Int, type: String) {
                    when (type) {
                        "remove" -> removeFollowers(position)
                        "unfollow" -> unfollowFollowers(position)
                        "followBack" -> hitApiForFollow(position)
                        "chat" -> gotoChatMethod(position)
                        "follow" -> hitApiForFollow(position)
                    }
                }
            }
        )
        binding.rvFollowersList.adapter = followerAdapter
//        } else {
//            followerAdapter!!.updateListType(listType)
//            followerAdapter!!.updateList(followList) // Update existing adapter
//            followerAdapter!!.notifyDataSetChanged()
//        }
    }

    private fun gotoChatMethod(position: Int) {
        val userId = followerAdapter!!.getFollowList()[position].user_id
        val name =
            followerAdapter!!.getFollowList()[position].firstName + " " + followerAdapter!!.getFollowList()[position].lastName
        val image = followerAdapter!!.getFollowList()[position].profileImage
        val bundle = Bundle()
        bundle.putString(Constants.OPPONENT_ID, userId)
        bundle.putString(Constants.OPPONENT_NAME, name)
        bundle.putString(Constants.OPPONENT_IMAGE, image)
        findNavController().navigate(R.id.action_followersFragment_to_chatFragment, bundle)
    }

    private fun changeColorForFollowing() {
        binding.tvFollowingClick.background = (ContextCompat.getDrawable(
            requireActivity(), R.drawable.btn_corner
        ))
        binding.tvFollowingClick.setTextColor(
            ContextCompat.getColor(
                requireActivity(), R.color.white
            )
        )
        binding.tvFollowersClick.setTextColor(
            ContextCompat.getColor(
                requireActivity(), R.color.black
            )
        )
        binding.tvFollowersClick.setBackgroundColor(
            ContextCompat.getColor(
                requireActivity(), R.color.white
            )
        )
    }

    private fun changeColorForFollower() {
        binding.tvFollowersClick.background = (ContextCompat.getDrawable(
            requireActivity(), R.drawable.btn_corner
        ))
        binding.tvFollowersClick.setTextColor(
            ContextCompat.getColor(
                requireActivity(), R.color.white
            )
        )
        binding.tvFollowingClick.setTextColor(
            ContextCompat.getColor(
                requireActivity(), R.color.black
            )
        )
        binding.tvFollowingClick.setBackgroundColor(
            ContextCompat.getColor(
                requireActivity(), R.color.white
            )
        )
    }

    @SuppressLint("SetTextI18n")
    private fun removeFollowers(position: Int) {
        removeFollowerDialog?.dismiss()

        val dialogBinding = DialogRemoveFollowerBinding.inflate(layoutInflater)
        removeFollowerDialog = Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(dialogBinding.root)
            window?.apply {
                setBackgroundDrawableResource(android.R.color.transparent)
                attributes = attributes.apply {
                    gravity = Gravity.BOTTOM
                }
                setLayout(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                )
            }

            dialogBinding.tvFollowerDetails.text =
                "We won't tell ${followerAdapter!!.getFollowList()[position].firstName + " " + followerAdapter!!.getFollowList()[position].lastName} that they were removed from your followers."
            dialogBinding.tvCancelFollower.setOnClickListener { dismiss() }
            dialogBinding.tvRemove.setOnClickListener {
                hitApiToRemoveFollower(
                    followerAdapter!!.getFollowList()[position].user_id,
                    position
                )
                dismiss()
            }

            show()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun unfollowFollowers(position: Int) {
        removeFollowerDialog?.dismiss()

        val dialogBinding = DialogRemoveFollowerBinding.inflate(layoutInflater)
        removeFollowerDialog = Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(dialogBinding.root)
            window?.apply {
                setBackgroundDrawableResource(android.R.color.transparent)
                attributes = attributes.apply {
                    gravity = Gravity.BOTTOM
                }
                setLayout(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                )
            }
            dialogBinding.tvRemove.text = "Unfollow"
            dialogBinding.tvRemoveFollower.text =
                "Unfollow ${followerAdapter!!.getFollowList()[position].firstName + " " + followerAdapter!!.getFollowList()[position].lastName}"
            dialogBinding.tvFollowerDetails.text =
                "We won't tell ${followerAdapter!!.getFollowList()[position].firstName + " " + followerAdapter!!.getFollowList()[position].lastName} that they were unfollowed from your followings."
            dialogBinding.tvCancelFollower.setOnClickListener { dismiss() }
            dialogBinding.tvRemove.setOnClickListener {
                hitApiForUnFollow(followerAdapter!!.getFollowList()[position].user_id, position)
                dismiss()
            }

            show()
        }
    }

    private fun setupPagination() {
        binding.rvFollowersList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && hasMoreData) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0
                    ) {
                        loadMoreItems()
                    }
                }
            }
        })
    }

    private fun loadMoreItems() {
        isLoading = true
        page++
        when (binding.tvFollowerFollowing.text) {
            getString(R.string.all_followers) -> apiForFollowers()
            getString(R.string.all_followings) -> apiForFollowings()
        }
    }

    @SuppressLint("FragmentLiveDataObserve", "SetTextI18n", "NotifyDataSetChanged")
    private fun hitApiToRemoveFollower(userId: String, position: Int) {
        val jsonObject = JsonObject()
        jsonObject.addProperty(Constants.targetUserId, userId)
        CoroutineScope(Dispatchers.Main).launch {
            vm.removeFollower(jsonObject)
            vm.removeFollowerData.observe(this@FollowersFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        Extension.stopProgress()
                        followerAdapter!!.getFollowList().removeAt(position)
                        followerAdapter!!.notifyDataSetChanged()
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

    @SuppressLint("FragmentLiveDataObserve", "SetTextI18n", "NotifyDataSetChanged")
    private fun hitApiForUnFollow(userId: String, position: Int) {
        val jsonObject = JsonObject()
        jsonObject.addProperty(Constants.targetUserId, userId)
        CoroutineScope(Dispatchers.Main).launch {
            vm.unfollowUser(jsonObject)
            vm.unfollowData.observe(this@FollowersFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        Extension.stopProgress()
                        if (userTypeFrom == 0) {
                            followerAdapter!!.getFollowList().removeAt(position)
                        } else {
                            followerAdapter!!.getFollowList()[position].isFollow = false
                        }
                        followerAdapter!!.notifyDataSetChanged()
                    }

                    Status.ERROR -> {
                        its.data?.let { requireActivity().makeToast(it.message) }
                    }

                    Status.LOADING -> {

                    }
                }
            }
        }
    }


    @SuppressLint("FragmentLiveDataObserve", "SetTextI18n", "NotifyDataSetChanged")
    private fun hitApiForFollow(position: Int) {
        val jsonObject = JsonObject()
        jsonObject.addProperty(
            Constants.targetUserId,
            followerAdapter!!.getFollowList()[position].user_id
        )
        CoroutineScope(Dispatchers.Main).launch {
            vm.followUser(jsonObject)
            vm.followData.observe(this@FollowersFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        Extension.stopProgress()
                        if (followerAdapter!!.itemCount > position) {
                            followerAdapter!!.getFollowList()[position].isFollow = true
                            followerAdapter!!.notifyDataSetChanged()
                        }
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

}