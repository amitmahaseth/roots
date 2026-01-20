package com.rootsrecipes.view.messages

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.ListenerRegistration
import com.rootsrecipes.BuildConfig
import com.rootsrecipes.databinding.FragmentChatBinding
import com.rootsrecipes.model.OnItemClickListener
import com.rootsrecipes.utils.BaseFragment
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.Extension
import com.rootsrecipes.utils.MyApplication
import com.rootsrecipes.utils.SharedPref
import com.rootsrecipes.view.messages.adapter.ChatAdapter
import com.rootsrecipes.view.messages.model.ChatMessages
import com.rootsrecipes.view.messages.model.GetAllMessages
import com.rootsrecipes.view.messages.model.UserDataChat
import com.rootsrecipes.view.messages.viewModel.ChatViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class ChatFragment : BaseFragment(), OnItemClickListener {

    private lateinit var binding: FragmentChatBinding
    private val pref: SharedPref by inject()
    private val chatVM: ChatViewModel by viewModel()
    private var chatAdapter: ChatAdapter? = null
    private var chatRecyclerView: RecyclerView? = null
    private var opponentUserId = ""
    private var opponentName = ""
    private var opponentImage = ""
    private var myUserId = ""
    private var chatId = ""

    var statusListener: ListenerRegistration? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatBinding.inflate(inflater)
        return binding.root
    }

    override fun onStop() {
        super.onStop()
        statusListener?.remove() // Remove the listener to avoid memory leaks
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chatRecyclerView = binding.rvChat
        opponentUserId = arguments?.getString(Constants.OPPONENT_ID).orEmpty()
        opponentName = arguments?.getString(Constants.OPPONENT_NAME).orEmpty()
        opponentImage = arguments?.getString(Constants.OPPONENT_IMAGE).orEmpty()
        initUi()
    }

    private fun initUi() {
        myUserId = pref.getSignInData()?._id.orEmpty()
        chatId = Extension.generateChatID(opponentUserId, myUserId)
        MyApplication.currentChatId = chatId
        setupListeners()
        observeMessages()
        setData()
        chatVM.fetchLoginUserRecentDataMethod(myUserId, opponentUserId, chatId)
        scrollRecyclerViewMethod()

    }

    @SuppressLint("SetTextI18n")
    private fun setData() {
        binding.tvChatFullName.text = opponentName
        if (opponentImage.isNotEmpty()) {
            Glide.with(requireActivity()).load(BuildConfig.BASE_MEDIA_URL + opponentImage)
                .into(binding.ivChatProfile)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onStart() {
        super.onStart()
        statusListener = chatVM.listenToUserOnlineStatus(opponentUserId) { isOnline ->
            Log.d("RealTimeStatus", "User isOnline: $isOnline")
            if (isOnline != null) {
                if (isOnline) {
                    binding.tvActiveStatus.text = "Online"
                } else {
                    binding.tvActiveStatus.text = "Offline"
                }
            } else {
                binding.tvActiveStatus.text = ""
            }
        }
    }

    private fun setupListeners() {
        binding.chatBackBtn.setOnClickListener { findNavController().popBackStack() }

        binding.ibSendMessage.setOnClickListener {
            val message = binding.etMessageText.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessage(message)
                binding.etMessageText.text.clear()
            }
        }
    }


    private fun sendMessage(message: String) {
        val messageData = ChatMessages(
            message = message,
            messageTime = System.currentTimeMillis() / 1000,
            opponentID = opponentUserId,
            senderID = myUserId,
            readStatus = false,
            messageType = 1
        )

        // Get FCM token if available
        val fcmToken = "" // Add logic to get FCM token if needed
        val oppData =
            UserDataChat(userID = opponentUserId, image = opponentImage, name = opponentName)
        chatVM.fetchUnreadCountMethod(chatId, oppData, messageData, fcmToken, true)
        gotoBottomMethod()
    }

    private fun observeMessages() {
        chatVM.chatAllMessagesList.observe(viewLifecycleOwner) { messages ->
            setChatAdapter(messages)
            gotoBottomMethod()
        }
    }

    override fun onPause() {
        super.onPause()
        // Clear current chat ID when leaving
        MyApplication.currentChatId = ""
        chatVM.stopMessageListener()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setChatAdapter(messages: List<GetAllMessages>) {
        if (chatAdapter == null) {
            chatAdapter = ChatAdapter(ArrayList(messages), this, myUserId, requireContext())
            chatRecyclerView!!.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = chatAdapter
            }
        } else {
            chatAdapter?.getList()?.clear()
            chatAdapter?.getList()?.addAll(messages)
            chatAdapter?.notifyDataSetChanged()
        }
    }

    override fun onItemClick(position: Int, type: String) {
        // Handle item clicks if needed
    }

    fun gotoBottomMethod() {
        if (chatAdapter!!.getList().isNotEmpty()) {
            chatRecyclerView!!.scrollToPosition(chatAdapter!!.itemCount - 1)


        }
    }

    private fun scrollRecyclerViewMethod() {

        chatRecyclerView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = LinearLayoutManager::class.java.cast(recyclerView.layoutManager)
                val totalItemCount = layoutManager!!.itemCount

                val lastVisible = layoutManager.findLastVisibleItemPosition()

                val endHasBeenReached = lastVisible + 1 >= totalItemCount
                if ((totalItemCount > 0) && (endHasBeenReached)) {

                    readMessageMethod()

                }
            }
        })
    }

    private fun readMessageMethod() {
        chatVM.clearUnreadCount(chatId, myUserId)
    }
}
