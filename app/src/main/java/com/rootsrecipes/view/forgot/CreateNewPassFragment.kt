package com.rootsrecipes.view.forgot

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.gson.JsonObject
import com.rootsrecipes.R
import com.rootsrecipes.databinding.FragmentCreateNewPassBinding
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.Extension
import com.rootsrecipes.utils.Status
import com.rootsrecipes.utils.makeToast
import com.rootsrecipes.view.forgot.viewmodel.ForgotVM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel


class CreateNewPassFragment : Fragment() {
    private var userEmail: String? = null
    private lateinit var binding: FragmentCreateNewPassBinding
    private lateinit var bundle: Bundle
    private val vm: ForgotVM by viewModel()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentCreateNewPassBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bundle = requireArguments()
        initUi()
    }

    private fun initUi() {
        userEmail = bundle.getString(Constants.email)
        binding.apply {
            etNewPassword.addTextChangedListener(object : TextWatcher {
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

            etConfirmPass.addTextChangedListener(object : TextWatcher {
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
                        binding.tiConfirmPass.error = null
                    } else if (validationMessage != "True") {
                        binding.tiConfirmPass.error = validationMessage
                    } else {
                        binding.tiConfirmPass.error = null
                    }
                    binding.tiConfirmPass.errorIconDrawable = null
                }
            })

            btnSubmitPass.setOnClickListener {
                val etNewPassword = etNewPassword.text.toString()
                val etConfirmPass = etConfirmPass.text.toString()
                when {
                    etNewPassword.isEmpty() -> requireActivity().makeToast("Please enter your new password!")
                    etConfirmPass.isEmpty() -> requireActivity().makeToast("Please enter your confirm password!")
                    else -> {
                        val jsonObject = JsonObject()
                        jsonObject.addProperty(Constants.email, userEmail)
                        jsonObject.addProperty(Constants.newPassword, etNewPassword)
                        jsonObject.addProperty(Constants.confirmPassword, etConfirmPass)
                        hitApiPasswordChange(jsonObject)
                    }
                }
            }
        }
    }

    @SuppressLint("FragmentLiveDataObserve")
    private fun hitApiPasswordChange(jsonObject: JsonObject) {
        CoroutineScope(Dispatchers.Main).launch {
            Extension.showProgress(requireActivity())
            vm.resetPasswordUser(jsonObject)
            vm.updatePasswordData.observe(this@CreateNewPassFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        Extension.stopProgress()
                        its.message?.let { requireActivity().makeToast(it) }
                        viewLifecycleOwner.lifecycleScope.launch {
                            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                                val currentDestination = findNavController().currentDestination
                                if (currentDestination?.id == R.id.createNewPassFragment) {
                                    bundle.putString(Constants.email, userEmail)
                                    findNavController().navigate(R.id.action_createNewPassFragment_to_signInFragment)
                                }
                            }
                        }
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