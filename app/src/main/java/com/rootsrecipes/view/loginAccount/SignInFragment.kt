package com.rootsrecipes.view.loginAccount

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.gson.JsonObject
import com.rootsrecipes.R
import com.rootsrecipes.databinding.FragmentSignInBinding
import com.rootsrecipes.utils.BaseFragment
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.Extension
import com.rootsrecipes.utils.Extension.getPasswordValidationMessage
import com.rootsrecipes.utils.SharedPref
import com.rootsrecipes.utils.Status
import com.rootsrecipes.utils.makeToast
import com.rootsrecipes.view.loginAccount.viewModel.SignInVM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel


class SignInFragment : BaseFragment() {
    private lateinit var binding: FragmentSignInBinding
    private val vm: SignInVM by viewModel()
    private val pref: SharedPref by inject()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        // Inflate the layout for this fragment
        binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUi()
        setHeight()
    }

    private fun setHeight() {
        // Use ViewTreeObserver to ensure views are laid out
        binding.root.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // Remove the listener to prevent multiple calls
                binding.root.viewTreeObserver.removeOnGlobalLayoutListener(this)

                // Get screen height in pixels
                val displayMetrics = DisplayMetrics()
                requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
                val totalHeightPixels = displayMetrics.heightPixels

                // Get margins from layout params
                val btnLoginParams = binding.btnLogin.layoutParams as ViewGroup.MarginLayoutParams
                val tvDontHaveAccParams =
                    binding.tvDontHaveAcc.layoutParams as ViewGroup.MarginLayoutParams

                // Calculate used height in pixels
                val usedHeight =
                    (binding.btnLogin.height + binding.btnLogin.paddingTop + binding.btnLogin.paddingBottom +
                            btnLoginParams.bottomMargin + btnLoginParams.topMargin) +
                            (binding.tvDontHaveAcc.height + binding.tvDontHaveAcc.paddingTop + binding.tvDontHaveAcc.paddingBottom +
                                    tvDontHaveAccParams.bottomMargin + tvDontHaveAccParams.topMargin)

                // Calculate available height
                val availableHeight = totalHeightPixels - usedHeight

                // Set the height of content layout
                val params = binding.clLoginCred.layoutParams
                params.height = availableHeight
                binding.clLoginCred.layoutParams = params

                Log.d("setHeightDebugs", "totalHeightPixels: $totalHeightPixels")
                Log.d("setHeightDebugs", "usedHeight: $usedHeight")
                Log.d("setHeightDebugs", "availableHeight: $availableHeight")
                Log.d("setHeightDebugs", "final content height: ${binding.clLoginCred.height}")
            }
        })
    }

    private fun initUi() {

        binding.apply {
            tvDontHaveAcc.setOnClickListener {
                if (isAdded) {
                    findNavController().navigate(R.id.action_signInFragment_to_signupFragment)
                }
            }
            tvForgot.setOnClickListener {
                if (isAdded) {
                    findNavController().navigate(R.id.action_signInFragment_to_forgotFragment)
                }
            }
            etPasswordLogin.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?, start: Int, before: Int, count: Int
                ) {
                    binding.tiPasswordLogin.apply {
                        error = null
                        errorIconDrawable = null
                    }
                }

                override fun afterTextChanged(s: Editable?) {
                    val validationMessage = getPasswordValidationMessage(s.toString())
                    if (s.toString().isEmpty()) {
                        binding.tiPasswordLogin.error = null
                    } else if (validationMessage != "True") {
                        binding.tiPasswordLogin.error = validationMessage
                    } else {
                        binding.tiPasswordLogin.error = null
                    }
                    binding.tiPasswordLogin.errorIconDrawable = null
                }
            })

            btnLogin.setOnClickListener {
//                throw RuntimeException("Test Crash") // Force a crash
                val etUserNameLogin = binding.etUserNameLogin.text.toString().trim()
                val etPasswordLogin = binding.etPasswordLogin.text.toString()
                val validationMessage = getPasswordValidationMessage(etPasswordLogin)
                when {
                    etUserNameLogin.isEmpty() -> requireActivity().makeToast("Please enter your username!")
                    etPasswordLogin.isEmpty() -> requireActivity().makeToast("Please enter your password!")
                    getPasswordValidationMessage(etPasswordLogin) != "True" -> {
                        requireActivity().makeToast(getPasswordValidationMessage(etPasswordLogin))
                        binding.tiPasswordLogin.error =
                            getPasswordValidationMessage(etPasswordLogin)
                        binding.tiPasswordLogin.errorIconDrawable = null
                    }

                    validationMessage != "True" -> {}
                    else -> {
                        val jsonObject = JsonObject()
                        jsonObject.addProperty(Constants.personal_info, etUserNameLogin)
                        jsonObject.addProperty(Constants.password, etPasswordLogin)
                        hitApiForLogin(jsonObject)
                    }

                }
            }
        }
    }

    private fun hitApiForLogin(jsonObject: JsonObject) {
        pref.clearPreference(requireActivity())
        CoroutineScope(Dispatchers.Main).launch {
            Extension.showProgress(requireActivity())
            vm.loginUser(jsonObject)
            vm.signInData.observe(requireActivity()) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        Extension.stopProgress()
                        its.data?.let {
                            if (it.data.is_email_verified!!) {
                                pref.saveString(Constants.TOKEN, it.token)
                                pref.saveSignInData(it.data)
                                pref.saveBoolean(
                                    Constants.RATINGS_NP,
                                    it.data.notificationPreferenceData!!.rating_notification
                                )
                                pref.saveBoolean(
                                    Constants.COMMENTS_NP,
                                    it.data.notificationPreferenceData!!.new_comment
                                )
                                pref.saveBoolean(
                                    Constants.RECIPE_NP,
                                    it.data.notificationPreferenceData!!.add_new_recipe
                                )
                                Log.d("getSignInDataDebugSign", "${pref.getSignInData()}")
                            } else {
                                requireActivity().makeToast(it.message)
                                val bundle = Bundle()
                                bundle.putInt(Constants.typeFrom, 0)
                                bundle.putString(
                                    Constants.email,
                                    it.data.email
                                )
                                findNavController().navigate(
                                    R.id.action_signInFragment_to_otpFragment,
                                    bundle
                                )
                            }
                        }
                        pref.saveString(
                            Constants.USER_PASSWORD,
                            binding.etPasswordLogin.text.toString()
                        )
                        its.message?.let { requireActivity().makeToast(it) }
                        viewLifecycleOwner.lifecycleScope.launch {
                            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                                val currentDestination = findNavController().currentDestination
                                if (currentDestination?.id == R.id.signInFragment) {
                                    findNavController().navigate(R.id.action_signInFragment_to_discoverFragment)
                                }
                            }
                        }
                    }

                    Status.ERROR -> {
                        Extension.stopProgress()
                        its.message?.let { requireActivity().makeToast(it) }
                    }

                    Status.LOADING -> {
//                        Extension.showProgress(requireActivity())
                    }
                }
            }
        }
    }

}