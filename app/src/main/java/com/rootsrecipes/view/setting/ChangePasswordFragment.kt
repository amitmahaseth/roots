package com.rootsrecipes.view.setting

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.gson.JsonObject
import com.rootsrecipes.databinding.FragmentChangePasswordBinding
import com.rootsrecipes.utils.BaseFragment
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.Extension
import com.rootsrecipes.utils.SharedPref
import com.rootsrecipes.utils.Status
import com.rootsrecipes.utils.makeToast
import com.rootsrecipes.viewmodel.ProfileVM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel


class ChangePasswordFragment : BaseFragment() {

    private lateinit var binding: FragmentChangePasswordBinding
    private val pref: SharedPref by inject()
    private val vm: ProfileVM by viewModel()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUi()
    }

    private fun initUi() {
        val oldPassWord = pref.getString(Constants.USER_PASSWORD)
        binding.apply {
            ivBackChangePass.setOnClickListener {
                findNavController().navigateUp()
            }
            btnChangePass.setOnClickListener {
                val etOldPass = etOldPass.text.toString()
                val etNewPassChange = etNewPassChange.text.toString()
                val etReNewPass = etReNewPass.text.toString()

                when {
                    etOldPass.isEmpty() -> requireActivity().makeToast("Please enter your old password!")
                    etNewPassChange.isEmpty() -> requireActivity().makeToast("Please enter your new password!")
                    etReNewPass.isEmpty() -> requireActivity().makeToast("Your Re-enter password not matched with new password!")
                    oldPassWord == etNewPassChange -> requireActivity().makeToast("Your new password and old password are same!")
                    else -> {
                        val jsonObject = JsonObject()
                        jsonObject.addProperty(Constants.newPassword, etNewPassChange)
                        jsonObject.addProperty(Constants.confirmPassword, etReNewPass)
                        jsonObject.addProperty(Constants.oldPassword, oldPassWord)
                        hitApiForChangePassword(jsonObject)
                    }

                }
            }

            etNewPassChange.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?, start: Int, before: Int, count: Int
                ) {
                }

                override fun afterTextChanged(s: Editable?) {
                    val validationMessage = Extension.getPasswordValidationMessage(s.toString())
                    if (s.toString().isEmpty()) {
                        binding.tiNewPass.error = null
                    } else if (validationMessage != "True") {
                        binding.tiNewPass.error = validationMessage
                    } else {
                        binding.tiNewPass.error = null
                    }
                    binding.tiNewPass.errorIconDrawable = null
                }
            })
            etReNewPass.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?, start: Int, before: Int, count: Int
                ) {
                }

                override fun afterTextChanged(s: Editable?) {
                    val validationMessage = Extension.getPasswordValidationMessage(s.toString())
                    if (s.toString().isEmpty()) {
                        binding.tiReNewPass.error = null
                    } else if (validationMessage != "True") {
                        binding.tiReNewPass.error = validationMessage
                    } else {
                        binding.tiReNewPass.error = null
                    }
                    binding.tiReNewPass.errorIconDrawable = null
                }
            })

            etOldPass.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?, start: Int, before: Int, count: Int
                ) {
                }

                override fun afterTextChanged(s: Editable?) {
                    val validationMessage = Extension.getPasswordValidationMessage(s.toString())
                    if (s.toString().isEmpty()) {
                        binding.tiOldPass.error = null
                    } else if (validationMessage != "True") {
                        binding.tiOldPass.error = validationMessage
                    } else {
                        binding.tiOldPass.error = null
                    }
                    binding.tiOldPass.errorIconDrawable = null
                }
            })
        }
    }

    @SuppressLint("FragmentLiveDataObserve")
    private fun hitApiForChangePassword(jsonObject: JsonObject) {
        CoroutineScope(Dispatchers.Main).launch {
            Extension.showProgress(requireActivity())
            vm.userProfileData(jsonObject)
            vm.userProfileData.observe(this@ChangePasswordFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        Extension.stopProgress()
                        its.message?.let { requireActivity().makeToast(it) }
                        findNavController().navigateUp()
                    }

                    Status.ERROR -> {
                        Extension.stopProgress()
                        its.message?.let { requireActivity().makeToast(it) }
                    }

                    Status.LOADING -> {

                    }
                }
            }
        }

    }


}