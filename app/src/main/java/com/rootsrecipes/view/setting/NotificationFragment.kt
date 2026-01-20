package com.rootsrecipes.view.setting

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.gson.JsonObject
import com.rootsrecipes.databinding.FragmentNotificationBinding
import com.rootsrecipes.utils.BaseFragment
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.NetworkHelper
import com.rootsrecipes.utils.SharedPref
import com.rootsrecipes.utils.Status
import com.rootsrecipes.utils.makeToast
import com.rootsrecipes.view.setting.viewmodel.SettingVM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel


class NotificationFragment : BaseFragment() {

    private lateinit var binding: FragmentNotificationBinding
    private val vm: SettingVM by viewModel()
    private val pref: SharedPref by inject()
    private val networkHelper: NetworkHelper by inject()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setNotificationPref()
        initUi()
    }

    private fun initUi() {
        binding.apply {
            ivBackNotificationPref.setOnClickListener {
                findNavController().popBackStack()
            }
            swRatingNotification.setOnCheckedChangeListener { _, isChecked ->
                if (networkHelper.isNetworkConnected()) {
                    pref.saveBoolean(Constants.RATINGS_NP, isChecked)
                    val jsonObject = JsonObject()
                    jsonObject.addProperty(Constants.RATINGS_NP, isChecked)
                    hitApiUpdateNotification(jsonObject)
                } else {
                    swRatingNotification.isChecked = !isChecked
                    requireActivity().makeToast("Internet not available")
                }
            }
            swComment.setOnCheckedChangeListener { _, isChecked ->
                if (networkHelper.isNetworkConnected()) {
                    pref.saveBoolean(Constants.COMMENTS_NP, isChecked)
                    val jsonObject = JsonObject()
                    jsonObject.addProperty(Constants.COMMENTS_NP, isChecked)
                    hitApiUpdateNotification(jsonObject)
                } else {
                    swComment.isChecked = !isChecked
                    requireActivity().makeToast("Internet not available")
                }
            }
            swNewRecipes.setOnCheckedChangeListener { _, isChecked ->
                if (networkHelper.isNetworkConnected()) {
                    pref.saveBoolean(Constants.RECIPE_NP, isChecked)
                    val jsonObject = JsonObject()
                    jsonObject.addProperty(Constants.RECIPE_NP, isChecked)
                    hitApiUpdateNotification(jsonObject)
                } else {
                    swNewRecipes.isChecked = !isChecked
                    requireActivity().makeToast("Internet not available")
                }
            }


        }
    }

    private fun setNotificationPref() {
        val npRatings = pref.getBoolean(Constants.RATINGS_NP)
        val npComments = pref.getBoolean(Constants.COMMENTS_NP)
        val npNewRecipe = pref.getBoolean(Constants.RECIPE_NP)

        binding.apply {
            swRatingNotification.isChecked = npRatings
            swComment.isChecked = npComments
            swNewRecipes.isChecked = npNewRecipe
        }
    }

    @SuppressLint("FragmentLiveDataObserve")
    private fun hitApiUpdateNotification(jsonObject: JsonObject) {
        CoroutineScope(Dispatchers.Main).launch {
            vm.updateNotificationPreferences(jsonObject)
            vm.updateNotificationData.observe(this@NotificationFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        viewLifecycleOwner.lifecycleScope.launch {
                            // its.data?.message?.let { requireActivity().makeToast(it) }
                            its.data?.data?.let {

                            }
                        }
                    }

                    Status.ERROR -> {
                        its.message?.let { requireActivity().makeToast(it) }
                    }

                    Status.LOADING -> {

                    }
                }


            }
        }
    }

}