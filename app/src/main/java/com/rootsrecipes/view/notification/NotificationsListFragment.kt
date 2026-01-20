package com.rootsrecipes.view.notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rootsrecipes.R
import com.rootsrecipes.databinding.FragmentNotificationsListBinding
import com.rootsrecipes.model.Notification
import com.rootsrecipes.model.OnItemClickListener
import com.rootsrecipes.model.User
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.Extension
import com.rootsrecipes.utils.Status
import com.rootsrecipes.utils.gone
import com.rootsrecipes.utils.makeToast
import com.rootsrecipes.utils.visible
import com.rootsrecipes.view.createAccount.model.RecipeData
import com.rootsrecipes.view.notification.adapter.NotificationAdapter
import com.rootsrecipes.view.notification.viewModel.NotificationsListVM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class NotificationsListFragment : Fragment(), OnItemClickListener {

    private lateinit var binding: FragmentNotificationsListBinding
    private val viewModel: NotificationsListVM by viewModel()
    private var notificationAdapter: NotificationAdapter? = null
    private var notificationsList = ArrayList<Notification>()
    private var page = 1
    private var limit = 10
    private var isLoading = false
    private var isLastPage = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNotificationsListBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUi()
    }

    private fun initUi() {
        setNotificationAdapter()
        setOnClickMethod()
        observeNotifications()
        fetchNotifications()
    }

    private fun fetchNotifications() {
        if (!isLastPage && !isLoading) {
            isLoading = true
            if(page == 1){
              binding.progressBar.visible()
            }
            CoroutineScope(Dispatchers.Main).launch {
                viewModel.getNotifications(page, limit)
            }
        }
    }

    private fun observeNotifications() {
        viewModel.notificationListData.observe(viewLifecycleOwner) { its ->
            when (its.status) {
                Status.SUCCESS -> {
                    isLoading = false
                    binding.progressBar.gone()
                    Extension.stopProgress()
                    its.data?.data?.notifications?.let { newNotifications ->
                        if (page == 1) {
                            if(newNotifications.isEmpty()){
                                binding.tvNoNotification.visible()
                            }else{
                                binding.tvNoNotification.gone()
                            }
                            notificationsList.clear()
                        }
                        notificationsList.addAll(newNotifications)
                        notificationAdapter?.notifyDataSetChanged()

                        if (newNotifications.size < limit) {
                            isLastPage = true
                        }
                    }
                }

                Status.ERROR -> {
                    isLoading = false
                    binding.progressBar.gone()
                   /*
                    if(page == 1 && notificationsList.isEmpty()){
                        binding.tvNoNotification.visible()
                    }*/
                    Extension.stopProgress()
                    its.message?.let { requireActivity().makeToast(it) }
                }

                Status.LOADING -> {
                    // Optional: show progress indicator
                }
            }
        }
    }

    private fun setOnClickMethod() {
        binding.apply {
            ivBackNotificationsList.setOnClickListener {
                findNavController().popBackStack()
            }
        }
    }

    private fun setNotificationAdapter() {
        notificationAdapter = NotificationAdapter(requireContext(), notificationsList, this)
        val layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotifications.layoutManager = layoutManager
        binding.rvNotifications.adapter = notificationAdapter

        binding.rvNotifications.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                if (!isLoading && !isLastPage && totalItemCount <= lastVisibleItem + 2) {
                    page++
                    fetchNotifications()
                }
            }
        })
    }

    override fun onItemClick(position: Int, type: String) {
        when (type) {
            Constants.targetUserId -> {
                val opponentUserId = notificationsList[position].user._id
                val bundle = Bundle()
                bundle.putParcelable(Constants.userInformation, User(_id = opponentUserId))
                findNavController().navigate(
                    R.id.action_notificationsListFragment_to_userProfileFragment, bundle
                )
            }

            Constants.GET_RECIPE -> {
                val recipeId = notificationsList[position].recipeData!!._id
                val bundle = Bundle()
                bundle.putInt(Constants.typeFrom, 2)

                bundle.putParcelable(
                    Constants.recipeInformation,
                    RecipeData(_id = recipeId)
                )
                findNavController().navigate(
                    R.id.action_notificationsListFragment_to_recipeInformationFragment,
                    bundle
                )
            }

        }
    }
}
