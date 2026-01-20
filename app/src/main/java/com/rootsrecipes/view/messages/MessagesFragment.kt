package com.rootsrecipes.view.messages

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonObject
import com.rootsrecipes.R
import com.rootsrecipes.databinding.DialogRemoveFollowerBinding
import com.rootsrecipes.databinding.FragmentMessagesBinding
import com.rootsrecipes.model.OnItemClickListener
import com.rootsrecipes.utils.BaseFragment
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.Extension
import com.rootsrecipes.utils.SharedPref
import com.rootsrecipes.utils.Status
import com.rootsrecipes.utils.gone
import com.rootsrecipes.utils.makeToast
import com.rootsrecipes.utils.visible
import com.rootsrecipes.view.messages.adapter.MessagesAdapter
import com.rootsrecipes.view.messages.adapter.SearchAdapter
import com.rootsrecipes.view.messages.model.ChatData
import com.rootsrecipes.view.messages.model.Data
import com.rootsrecipes.view.messages.viewModel.MessageViewModel
import com.rootsrecipes.viewmodel.ProfileVM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MessagesFragment : BaseFragment(), OnItemClickListener {

    private lateinit var followerAdapter: SearchAdapter
    private lateinit var binding: FragmentMessagesBinding
    private lateinit var messagesAdapter: MessagesAdapter
    private lateinit var messageRV: RecyclerView
    private val viewModel: MessageViewModel by viewModel()
    private var chatDataArrayList: ArrayList<ChatData>? = null
    private var filteredChatList: ArrayList<ChatData>? = null
    private var searchQuery = ""
    private val pref: SharedPref by inject()
    private val vm: ProfileVM by viewModel()
    private var removeFollowerDialog: Dialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        messageRV = binding.rvMessages
        initUi()

        // Fetch chats for current user
        val userId = SharedPref(requireContext()).getSignInData()?._id
        userId?.let {
            viewModel.fetchChatsForUser(it)
            observeChatList()
        }

        // Setup search functionality
        setupSearch()
    }

    private fun observeChatList() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.chatList.observe(viewLifecycleOwner) { chatList ->
                    chatDataArrayList = ArrayList(chatList)
                    filteredChatList = ArrayList(chatList) // Initialize filtered list
                    updateAdapter(filteredChatList!!)
                    updateEmptyState()
                }
            }
        }
    }

    private fun hideKeyboard() {
        val inputMethodManager =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = requireActivity().currentFocus ?: View(requireActivity())
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun setupSearch() {
        binding.apply {
            Extension.trimEditText(searchEditText)
            searchEditText.setOnEditorActionListener(object : OnEditorActionListener {
                override fun onEditorAction(
                    v: TextView?, actionId: Int, event: KeyEvent?
                ): Boolean {
                    hideKeyboard()
                    return false
                }

            })
            searchEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    searchQuery = s.toString().trim()
                    if (searchQuery.isEmpty()) {
                        rvFollowersList.gone()
                        tvUsers.gone()
                        ivClearSearch.gone()
                    } else {
                        rvFollowersList.visible()
                        tvUsers.visible()
                        ivClearSearch.visible()
                    }
                    hitApiForChatSearch(searchQuery)
//                    filterChats(searchQuery)
                }
            })

            ivClearSearch.setOnClickListener {
                searchQuery = ""
                searchEditText.text.clear()
                rvFollowersList.gone()
                tvUsers.gone()
            }
        }
    }

    //amit
    @SuppressLint("FragmentLiveDataObserve")
    private fun hitApiForChatSearch(searchQuery: String) {
        CoroutineScope(Dispatchers.Main).launch {
            vm.searchUserChat(searchQuery)
            vm.searchUserData.observe(this@MessagesFragment) {
                when (it.status) {
                    Status.SUCCESS -> {
                        it.data?.let { it1 -> setAdapterFollowers(ArrayList(it1.data)) }
                    }

                    Status.ERROR -> {

                    }

                    Status.LOADING -> {

                    }
                }
            }
        }
    }

    //amit
    private fun setAdapterFollowers(followList: ArrayList<Data>) {
        followerAdapter = SearchAdapter(requireContext(),
            loginId = pref.getSignInData()!!._id!!,
            followList,
            object : OnItemClickListener {
                override fun onItemClick(position: Int, type: String) {
                    when (type) {
                        "unfollow" -> unfollowFollowers(position)
                        "followBack" -> hitApiForFollow(position)
                        "chat" -> gotoChatMethod(position)
                        "follow" -> hitApiForFollow(position)
                    }
                }
            })
        binding.rvFollowersList.adapter = followerAdapter
    }

    //amit
    private fun gotoChatMethod(position: Int) {
        val userId = followerAdapter.getFollowList()[position]._id
        val name =
            followerAdapter.getFollowList()[position].first_name + " " + followerAdapter.getFollowList()[position].last_name
        val image = followerAdapter.getFollowList()[position].profile_image
        val bundle = Bundle()
        bundle.putString(Constants.OPPONENT_ID, userId)
        bundle.putString(Constants.OPPONENT_NAME, name)
        bundle.putString(Constants.OPPONENT_IMAGE, image)
        findNavController().navigate(R.id.action_messagesFragment_to_chatFragment, bundle)
    }

    //amit
    @SuppressLint("FragmentLiveDataObserve", "SetTextI18n", "NotifyDataSetChanged")
    private fun hitApiForFollow(position: Int) {
        Extension.showProgress(requireActivity())
        val jsonObject = JsonObject()
        jsonObject.addProperty(
            Constants.targetUserId, followerAdapter.getFollowList()[position]._id
        )
        CoroutineScope(Dispatchers.Main).launch {
            vm.followUser(jsonObject)
            vm.followData.observe(this@MessagesFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        Extension.stopProgress()
                        if (followerAdapter.itemCount > position) {
                            followerAdapter.getFollowList()[position].is_following = true
                            followerAdapter.notifyDataSetChanged()
                        }
                    }

                    Status.ERROR -> {
                        Extension.stopProgress()
                        its?.let { requireActivity().makeToast(it.message!!) }
                    }

                    Status.LOADING -> {

                    }
                }
            }
        }
    }

    //amit
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
                "Unfollow ${followerAdapter.getFollowList()[position].first_name + " " + followerAdapter.getFollowList()[position].last_name}"
            dialogBinding.tvFollowerDetails.text =
                "We won't tell ${followerAdapter.getFollowList()[position].first_name + " " + followerAdapter.getFollowList()[position].last_name} that they were unfollowed from your followings."
            dialogBinding.tvCancelFollower.setOnClickListener { dismiss() }
            dialogBinding.tvRemove.setOnClickListener {
                hitApiForUnFollow(followerAdapter.getFollowList()[position]._id, position)
                dismiss()
            }

            show()
        }
    }

    //amit
    @SuppressLint("FragmentLiveDataObserve", "SetTextI18n", "NotifyDataSetChanged")
    private fun hitApiForUnFollow(userId: String, position: Int) {
        Extension.showProgress(requireActivity())
        val jsonObject = JsonObject()
        jsonObject.addProperty(Constants.targetUserId, userId)
        CoroutineScope(Dispatchers.Main).launch {
            vm.unfollowUser(jsonObject)
            vm.unfollowData.observe(this@MessagesFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        Extension.stopProgress()
                        followerAdapter.getFollowList()[position].is_following = false
                        followerAdapter.notifyDataSetChanged()
                    }

                    Status.ERROR -> {
                        Extension.stopProgress()
                        its.data?.let { requireActivity().makeToast(it.message) }
                    }

                    Status.LOADING -> {

                    }
                }
            }
        }
    }


    private fun filterChats(query: String) {
        filteredChatList = if (query.isEmpty()) {
            ArrayList(chatDataArrayList ?: emptyList()) // Show all chats if query is empty
        } else {
            ArrayList(chatDataArrayList?.filter {
                it.opponentUser.name.contains(query, ignoreCase = true)
            } ?: emptyList())
        }
        updateAdapter(filteredChatList!!)
        updateEmptyState()
    }

    private fun updateAdapter(chatList: ArrayList<ChatData>) {
        messagesAdapter = MessagesAdapter(
            requireContext(), chatList, this@MessagesFragment
        )
        messageRV.layoutManager = LinearLayoutManager(requireContext())
        messageRV.adapter = messagesAdapter
        messagesAdapter.submitList(chatList)
    }

    private fun updateEmptyState() {
        if (filteredChatList.isNullOrEmpty() && searchQuery.isEmpty()) {
            binding.emptyMessagesLayout.visibility = View.VISIBLE
            binding.messagesLayout.visibility = View.GONE
        } else {
            binding.emptyMessagesLayout.visibility = View.GONE
            binding.messagesLayout.visibility = View.VISIBLE
        }
    }

    private fun initUi() {
        setOnClickMethod()
    }

    private fun setOnClickMethod() {
        binding.apply {
            notificationBtn.setOnClickListener {
                findNavController().navigate(R.id.notificationsListFragment)
            }
            addNewMessageBtn.setOnClickListener {
                val bundle = Bundle()
                bundle.putInt(Constants.typeFrom, 0)
                findNavController().navigate(R.id.selectPersonFragment, bundle)
            }
            chatWithOtherBtn.setOnClickListener {
                val bundle = Bundle()
                bundle.putInt(Constants.typeFrom, 0)
                findNavController().navigate(R.id.selectPersonFragment, bundle)
            }
        }
    }

    override fun onItemClick(position: Int, type: String) {
        when (type) {
            "chat" -> {
                gotoChatScreenMethod(position)
            }
        }
    }

    private fun gotoChatScreenMethod(position: Int) {
        val userId = messagesAdapter.getItem(position).opponentUser.userID
        val name = messagesAdapter.getItem(position).opponentUser.name
        val image = messagesAdapter.getItem(position).opponentUser.image
        val bundle = Bundle()
        bundle.putString(Constants.OPPONENT_ID, userId)
        bundle.putString(Constants.OPPONENT_NAME, name)
        bundle.putString(Constants.OPPONENT_IMAGE, image)
        findNavController().navigate(R.id.action_messagesFragment_to_chatFragment, bundle)
    }
}