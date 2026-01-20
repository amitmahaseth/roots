package com.rootsrecipes.view.createAccount

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.gson.JsonObject
import com.rootsrecipes.R
import com.rootsrecipes.databinding.FragmentOtpBinding
import com.rootsrecipes.utils.BaseFragment
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.Extension
import com.rootsrecipes.utils.SharedPref
import com.rootsrecipes.utils.Status
import com.rootsrecipes.utils.gone
import com.rootsrecipes.utils.makeToast
import com.rootsrecipes.utils.visible
import com.rootsrecipes.view.createAccount.viewModel.SignUpVM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel


class OtpFragment : BaseFragment() {

    private var typeFrom: Int = 0
    private var userEmail: String? = null
    private var userPhoneNumber: String? = null
    private lateinit var binding: FragmentOtpBinding
    private lateinit var bundle: Bundle
    private val vm: SignUpVM by viewModel()
    private val pref: SharedPref by inject()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentOtpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bundle = requireArguments()
        initUi()
    }

    @SuppressLint("SetTextI18n")
    private fun initUi() {
        typeFrom = bundle.getInt(Constants.typeFrom)
        userEmail = bundle.getString(Constants.email)
        when (typeFrom) {
            1 -> {
                binding.tvVerification.text = "Email Verification"
                binding.tvVerificationSent.text = "Please enter the 4 digit code sent to"
                binding.tvVerificationValue.text = userEmail
            }

            2 -> {
                userPhoneNumber = bundle.getString(Constants.phone_number)
                binding.tvVerification.text = "Phone Number Verification"
                binding.tvVerificationSent.text = "Please Enter The 4 Digit Code Sent To"
                binding.tvVerificationValue.text = userPhoneNumber
                //  binding.tvVerificationValue.text = userEmail
            }

            else -> {
                Constants.otpBackHandle = true
                binding.tvVerificationValue.text = userEmail
            }
        }
        binding.tvResendOtpTimer.visible()
        binding.tvResendOtp.gone()
        timer()
        binding.apply {
            ivBackOtp.setOnClickListener {
                findNavController().navigateUp()
            }
            tvResendOtp.setOnClickListener {
                if (typeFrom == 1) {
                    hitResendApi()
                } else {
                    hitResendApi()
                }
            }

            btnSubmit.setOnClickListener {
                when (typeFrom) {
                    1 -> {
                        when {
                            otpView.text.toString()
                                .isEmpty() -> requireActivity().makeToast("Please enter your otp!")

                            otpView.text.toString()
                                .toInt() < 4 -> requireActivity().makeToast("Please enter 4 digits otp!")

                            else -> {
                                hitApiForOtpVerifyPassChange()
                            }
                        }

                    }

                    2 -> {
                        when {
                            otpView.text.toString()
                                .isEmpty() -> requireActivity().makeToast("Please enter your otp!")

                            otpView.text.toString()
                                .toInt() < 4 -> requireActivity().makeToast("Please enter 4 digits otp!")

                            else -> {
                                hitApiForOtpVerifyPhoneNumber()
                            }
                        }
                    }

                    else -> {
                        when {
                            otpView.text.toString()
                                .isEmpty() -> requireActivity().makeToast("Please enter your otp!")

                            otpView.text.toString()
                                .toInt() < 4 -> requireActivity().makeToast("Please enter 4 digits otp!")

                            else -> {
                                hitApiForOtpVerify()
                            }
                        }
                    }
                }
            }
        }

    }

    @SuppressLint("FragmentLiveDataObserve")
    private fun hitApiForOtpVerifyPhoneNumber() {
        CoroutineScope(Dispatchers.Main).launch {
            if (userPhoneNumber != null) {
                val jsonObject = JsonObject()
                jsonObject.addProperty(Constants.mobile_otp, binding.otpView.text.toString().trim())
                vm.verifyOtpPhoneNumber(jsonObject)
                vm.otpPhoneNumberData.observe(this@OtpFragment) { its ->
                    when (its.status) {
                        Status.SUCCESS -> {
                            val updatedData = pref.getSignInData()!!
                            updatedData.is_phone_number_verified = true
                            pref.saveSignInData(updatedData)
                            its.data?.message?.let { requireActivity().makeToast(it) }
                            findNavController().navigateUp()
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

    private fun timer() {
        object : CountDownTimer(120000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                val minutes = secondsRemaining / 60
                val seconds = secondsRemaining % 60
                // Format the timer text as MM:SS
                binding.tvResendOtpTimer.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                binding.tvResendOtpTimer.gone()
                binding.tvResendOtp.visible()
            }
        }.start()
    }

    @SuppressLint("FragmentLiveDataObserve")
    private fun hitApiForOtpVerifyPassChange() {
        CoroutineScope(Dispatchers.Main).launch {
            if (userEmail != null) {
                Extension.showProgress(requireActivity())
                val jsonObject = JsonObject()
                jsonObject.addProperty(Constants.email, userEmail)
                jsonObject.addProperty(Constants.otp, binding.otpView.text.toString().trim())
                vm.verifyOtpPasswordChange(jsonObject)
                vm.otpPasswordData.observe(this@OtpFragment) { its ->
                    when (its.status) {
                        Status.SUCCESS -> {
                            Extension.stopProgress()
                            viewLifecycleOwner.lifecycleScope.launch {
                                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                                    val currentDestination = findNavController().currentDestination
                                    if (currentDestination?.id == R.id.otpFragment) {
                                        bundle.putString(Constants.email, userEmail)
                                        findNavController().navigate(
                                            R.id.action_otpFragment_to_createNewPassFragment, bundle
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

    @SuppressLint("FragmentLiveDataObserve")
    private fun hitResendApi() {
        CoroutineScope(Dispatchers.Main).launch {
            Extension.showProgress(requireActivity())
            val jsonObject = JsonObject()
            jsonObject.addProperty(Constants.email, userEmail)
            vm.resendOtp(jsonObject)
            vm.resendOtpData.observe(this@OtpFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        binding.tvResendOtpTimer.visible()
                        binding.tvResendOtp.gone()
                        timer()
                        Extension.stopProgress()
                        its.message?.let { requireActivity().makeToast(it) }
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

    @SuppressLint("FragmentLiveDataObserve")
    private fun hitApiForOtpVerify() {
        CoroutineScope(Dispatchers.Main).launch {
            if (userEmail != null) {
                Extension.showProgress(requireActivity())
                val jsonObject = JsonObject()
                jsonObject.addProperty(Constants.email, userEmail)
                jsonObject.addProperty(Constants.otp, binding.otpView.text.toString().trim())
                vm.otpVerify(jsonObject)
                vm.otpData.observe(this@OtpFragment) { its ->
                    when (its.status) {
                        Status.SUCCESS -> {
                            Constants.otpBackHandle = false
                            Extension.stopProgress()
                            its.data?.let { pref.saveString(Constants.TOKEN, it.token) }
                            its.data?.data?.let {
                                pref.saveSignInData(it)
                                pref.saveBoolean(
                                    Constants.RATINGS_NP,
                                    it.notificationPreferenceData!!.rating_notification
                                )
                                pref.saveBoolean(
                                    Constants.COMMENTS_NP,
                                    it.notificationPreferenceData!!.new_comment
                                )
                                pref.saveBoolean(
                                    Constants.RECIPE_NP,
                                    it.notificationPreferenceData!!.add_new_recipe
                                )
                            }

                            viewLifecycleOwner.lifecycleScope.launch {
                                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                                    val currentDestination = findNavController().currentDestination
                                    if (currentDestination?.id == R.id.otpFragment) {
                                        findNavController().navigate(R.id.action_otpFragment_to_walkThroughFragment)

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
}