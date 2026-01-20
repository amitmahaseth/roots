package com.rootsrecipes.view.createAccount

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.gson.JsonObject
import com.rootsrecipes.BuildConfig
import com.rootsrecipes.R
import com.rootsrecipes.databinding.FragmentSignupBinding
import com.rootsrecipes.utils.BaseFragment
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.Extension
import com.rootsrecipes.utils.Extension.getPasswordValidationMessage
import com.rootsrecipes.utils.Extension.isValidEmail
import com.rootsrecipes.utils.SharedPref
import com.rootsrecipes.utils.Status
import com.rootsrecipes.utils.makeToast
import com.rootsrecipes.view.createAccount.viewModel.SignUpVM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel


class SignupFragment : BaseFragment() {
    private lateinit var binding: FragmentSignupBinding
    private lateinit var bundle: Bundle
    private val vm: SignUpVM by viewModel()
    private var debounceJob: Job? = null
    private val debounceDelay: Long = 300
    private val pref: SharedPref by inject()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bundle = Bundle()
        initUi()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initUi() {
        clickSpannableMethod()

        binding.apply {
            btnNext.setOnClickListener {
                val etNameOfUser = binding.etNameOfUser.text.toString()
                val etLastName = binding.etLastName.text.toString()
                val etUSerName = binding.etUSerName.text.toString().trim()
                val etEmailCreate = binding.etEmailCreate.text.toString().trim()
                val etPhoneNumber = binding.etPhoneNumber.text.toString().trim()
                val codeCp = binding.codeCp.selectedCountryCode
                val etPasswordCreate = binding.etPasswordCreate.text.toString()
                val cvTermServices = binding.cvTermServices.isChecked

                when {
                    etNameOfUser.isEmpty() -> requireActivity().makeToast("Please enter first name!")
//                    etLastName.isEmpty() -> requireActivity().makeToast("Please enter last name!")
                    etUSerName.isEmpty() -> requireActivity().makeToast("Please enter your username!")
                    etEmailCreate.isEmpty() -> requireActivity().makeToast("Please enter your email!")
                    etPasswordCreate.isEmpty() -> requireActivity().makeToast("Please enter your password!")
                    !isValidEmail(etEmailCreate) -> requireActivity().makeToast("Please enter valid email address!")
                    getPasswordValidationMessage(etPasswordCreate) != "True" -> {
                        requireActivity().makeToast(getPasswordValidationMessage(etPasswordCreate))
                        binding.tiPassword.error = getPasswordValidationMessage(etPasswordCreate)
                        binding.tiPassword.errorIconDrawable = null
                    }

                    !cvTermServices -> requireActivity().makeToast("Please agree Term of service and Privacy Policy!")

                    else -> {
                        val jsonObject = JsonObject()
                        jsonObject.addProperty(Constants.first_name, etNameOfUser)
                        jsonObject.addProperty(Constants.last_name, etLastName)
                        jsonObject.addProperty(Constants.user_name, etUSerName)
                        jsonObject.addProperty(Constants.email, etEmailCreate)
                        jsonObject.addProperty(Constants.password, etPasswordCreate)
                        if (etPhoneNumber.isNotEmpty()) {
                            jsonObject.addProperty(Constants.phone_number, etPhoneNumber)
                            jsonObject.addProperty(Constants.country_code, "+$codeCp")
                        }
                        hitApiCreateAccount(jsonObject, etEmailCreate)
                    }
                }
            }
            etPasswordCreate.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?, start: Int, before: Int, count: Int
                ) {
                    binding.tiPassword.error = null
                    binding.tiPassword.errorIconDrawable = null
                }

                override fun afterTextChanged(s: Editable?) {
                    val password = s.toString()
                    val specialChars = "!@#\$%^&*()_+[]{}|;:',.<>?/"

                    fun updateView(
                        condition: Boolean,
                        textView: AppCompatTextView,
                        imageView: AppCompatImageView
                    ) {
                        val color = if (condition) R.color.green else R.color.text_gray
                        textView.setTextColor(ContextCompat.getColor(requireActivity(), color))
                        imageView.backgroundTintList =
                            ContextCompat.getColorStateList(requireActivity(), color)
                    }

                    if (password.isEmpty()) {
                        // Reset all items to gray when text is cleared
                        listOf(
                            tvLowerCase, tvUpperCase, tvCharSpecial, tvMinEight, tvNumber
                        ).forEach {
                            it.setTextColor(
                                ContextCompat.getColor(
                                    requireActivity(),
                                    R.color.text_gray
                                )
                            )
                        }

                        listOf(
                            ivLowerCase, ivUpperCase, ivCharSpecial, ivMinEight, ivNumber
                        ).forEach {
                            it.backgroundTintList = ContextCompat.getColorStateList(
                                requireActivity(),
                                R.color.text_gray
                            )
                        }

                        binding.tiPassword.error = null
                        return
                    }
                    // Update each condition
                    updateView(password.any { it.isLowerCase() }, tvLowerCase, ivLowerCase)
                    updateView(password.any { it.isUpperCase() }, tvUpperCase, ivUpperCase)
                    updateView(
                        password.any { specialChars.contains(it) },
                        tvCharSpecial,
                        ivCharSpecial
                    )
                    updateView(password.length >= 8, tvMinEight, ivMinEight)
                    updateView(password.any { it.isDigit() }, tvNumber, ivNumber)

                    binding.tiPassword.errorIconDrawable = null
                }

            })

            tvAlreadyAccount.setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                        val currentDestination = findNavController().currentDestination
                        if (currentDestination?.id == R.id.signupFragment) {
                            findNavController().navigate(R.id.action_signupFragment_to_signInFragment)
                        }
                    }
                }
            }
            etUSerName.filters = arrayOf(InputFilter { source, start, end, dest, dstart, dend ->
                source?.let {
                    if (it.contains(" ")) {
                        return@InputFilter it.toString().replace(" ", "")
                    }
                }
                null
            })
            etUSerName.apply {
                changeCheckIcon(0)
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?, start: Int, count: Int, after: Int
                    ) {
                        if (s.toString().trim().isEmpty()) {
                            changeCheckIcon(0)
                        }
                    }

                    override fun onTextChanged(
                        s: CharSequence?, start: Int, before: Int, count: Int
                    ) {
                        if (s.toString().trim().isEmpty()) {
                            changeCheckIcon(0)
                        }
                    }

                    override fun afterTextChanged(s: Editable?) {
                        if (s.toString().trim().isNotEmpty()) {
                            val username = s.toString()
                            debounceJob?.cancel()
                            debounceJob = CoroutineScope(Dispatchers.Main).launch {
                                delay(debounceDelay)
                                hitCheckUserNameApi(username)
                            }
                        } else {
                            changeCheckIcon(0)

                        }
                    }

                })
            }
        }
    }


    private fun clickSpannableMethod() {

        val ss =
            SpannableString(requireActivity().resources.getString(R.string.service_and_privacy_policy))
        val termsOfServicesCS: ClickableSpan = object : ClickableSpan() {
            override fun onClick(textView: View) {
                val url = BuildConfig.BASE_URL+ Constants.PRIVACY_POLICY
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                if (intent.resolveActivity(requireActivity().packageManager) != null) {
                    startActivity(intent)
                } else {
                    requireActivity().makeToast("No browser found")
                }
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.isFakeBoldText = true
                ds.color = requireActivity().getColor(R.color.light_green)
            }
        }

        val privacyPolicyClickableSpan: ClickableSpan = object : ClickableSpan() {
            override fun onClick(textView: View) {
                val url = BuildConfig.BASE_URL + Constants.PRIVACY_POLICY
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                if (intent.resolveActivity(requireActivity().packageManager) != null) {
                    startActivity(intent)
                } else {
                    requireActivity().makeToast("No browser found")
                }
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.isFakeBoldText = true
                ds.color = requireActivity().getColor(R.color.light_green)
            }
        }
        ss.setSpan(
            termsOfServicesCS, 15, 31, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        ss.setSpan(
            privacyPolicyClickableSpan, 36, 50, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        //  val textView = findViewById(R.id.hello) as TextView
        binding.tvTermServices.text = ss
        binding.tvTermServices.movementMethod = LinkMovementMethod.getInstance()
        binding.tvTermServices.highlightColor = Color.TRANSPARENT
    }

    @SuppressLint("FragmentLiveDataObserve")
    private fun hitApiCreateAccount(jsonObject: JsonObject, etEmailCreate: String) {
        Extension.showProgress(requireActivity())
        CoroutineScope(Dispatchers.Main).launch {
            vm.createUser(jsonObject)
            vm.createData.observe(this@SignupFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        Extension.stopProgress()
                        bundle.putInt(Constants.typeFrom, 0)
                        bundle.putString(
                            Constants.email, binding.etEmailCreate.text.toString().trim()
                        )
                        pref.saveString(
                            Constants.USER_PASSWORD, binding.etPasswordCreate.text.toString()
                        )
                        viewLifecycleOwner.lifecycleScope.launch {
                            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                                val currentDestination = findNavController().currentDestination
                                if (currentDestination?.id == R.id.signupFragment) {
                                    findNavController().navigate(
                                        R.id.action_signupFragment_to_otpFragment, bundle
                                    )
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

    private fun hitCheckUserNameApi(username: String) {
        CoroutineScope(Dispatchers.Main).launch {
            vm.checkUsername(username)
            vm.checkUserData.observe(requireActivity()) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        viewLifecycleOwner.lifecycleScope.launch {
                            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                                val currentDestination = findNavController().currentDestination
                                if (currentDestination?.id == R.id.signupFragment) {
                                    changeCheckIcon(1)
                                }
                            }
                        }
                    }

                    Status.ERROR -> {
                        changeCheckIcon(2)
                        //its.message?.let { requireActivity().makeToast(it) }
                    }

                    Status.LOADING -> {
                        changeCheckIcon(0)

                    }
                }


            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun changeCheckIcon(value: Int) {
        if (binding.etUSerName.text.toString().trim().isNotEmpty()) {
            when (value) {
                0 -> {
                    binding.etUSerName.setCompoundDrawablesWithIntrinsicBounds(
                        requireActivity().getDrawable(
                            R.drawable.user_icon
                        ), null, null, null
                    )
                    binding.tiUserName.error = null
                }

                1 -> {
                    binding.etUSerName.setCompoundDrawablesWithIntrinsicBounds(
                        requireActivity().getDrawable(R.drawable.user_icon),
                        null,
                        requireActivity().getDrawable(R.drawable.tick_circle),
                        null
                    )
                    binding.tiUserName.error = null
                }

                2 -> {
                    binding.etUSerName.setCompoundDrawablesWithIntrinsicBounds(
                        requireActivity().getDrawable(R.drawable.user_icon),
                        null,
                        requireActivity().getDrawable(R.drawable.close_red),
                        null
                    )
                    binding.tiUserName.error = "Username Already Exists!!!"
                }
            }
        } else {
            binding.etUSerName.setCompoundDrawablesWithIntrinsicBounds(
                requireActivity().getDrawable(
                    R.drawable.user_icon
                ), null, null, null
            )
            binding.tiUserName.error = null
        }
    }


}