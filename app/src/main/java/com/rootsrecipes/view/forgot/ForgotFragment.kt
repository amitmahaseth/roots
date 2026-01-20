package com.rootsrecipes.view.forgot

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.gson.JsonObject
import com.rootsrecipes.R
import com.rootsrecipes.databinding.FragmentForgotBinding
import com.rootsrecipes.utils.BaseFragment
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.Status
import com.rootsrecipes.utils.makeToast
import com.rootsrecipes.view.forgot.viewmodel.ForgotVM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel


class ForgotFragment : BaseFragment() {
    private lateinit var binding: FragmentForgotBinding
    private lateinit var bundle: Bundle
    private val vm: ForgotVM by viewModel()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentForgotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bundle = Bundle()
        initUi()
    }

    private fun initUi() {
        binding.apply {
            btnSendOtp.setOnClickListener {
                if (etEmailForgot.text.toString().trim().isEmpty()) {
                    requireActivity().makeToast("Please enter your email!")
                } else {
                    val jsonObject = JsonObject()
                    jsonObject.addProperty(Constants.email, etEmailForgot.text.toString().trim())
                    hitApiForgotPass(jsonObject)
                }

            }
        }
    }

    @SuppressLint("FragmentLiveDataObserve")
    private fun hitApiForgotPass(jsonObject: JsonObject) {
        CoroutineScope(Dispatchers.Main).launch {
            vm.forgotEmail(jsonObject)
            vm.forgotEmailData.observe(this@ForgotFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        viewLifecycleOwner.lifecycleScope.launch {
                            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                                val currentDestination = findNavController().currentDestination
                                if (currentDestination?.id == R.id.forgotFragment) {
                                    bundle.putInt(Constants.typeFrom, 1)
                                    bundle.putString(
                                        Constants.email,
                                        binding.etEmailForgot.text.toString().trim()
                                    )
                                    findNavController().navigate(
                                        R.id.action_forgotFragment_to_otpFragment, bundle
                                    )
                                }
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