package com.rootsrecipes.view.setting

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.model.ReviewErrorCode
import com.rootsrecipes.BuildConfig
import com.rootsrecipes.MainActivity
import com.rootsrecipes.R
import com.rootsrecipes.databinding.FragmentSettingBinding
import com.rootsrecipes.databinding.ShareDialogBinding
import com.rootsrecipes.utils.BaseFragment
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.Extension
import com.rootsrecipes.utils.SharedPref
import com.rootsrecipes.utils.Status
import com.rootsrecipes.utils.makeToast
import com.rootsrecipes.view.setting.viewmodel.SettingVM
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.util.LinkProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingFragment : BaseFragment() {

    private lateinit var manager: ReviewManager
    private var dialogLogout: Dialog? = null
    private var dialogDelete: Dialog? = null
    private lateinit var binding: FragmentSettingBinding
    private lateinit var bundle: Bundle
    private val pref: SharedPref by inject()
    private val vm: SettingVM by viewModel()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bundle = Bundle()
        initUi()
    }

    private fun initUi() {
        manager = ReviewManagerFactory.create(requireActivity())
        binding.apply {
            tvAppVersionName.text = BuildConfig.VERSION_NAME
            clLogout.setOnClickListener {
                logoutDialog()
            }
            clDelete.setOnClickListener {
                deleteAccountDialog()
            }
            clProfile.setOnClickListener {
                if (isAdded) {
                    findNavController().navigate(R.id.action_settingFragment_to_profileInfoFragment)
                }
            }
            clArr.setOnClickListener {
                if (isAdded) {
                    findNavController().navigate(R.id.action_settingFragment_to_aboutRootsFragment)
                }
            }
            clSr.setOnClickListener {
                if (isAdded) {
                    bundle.putInt(Constants.savedTypeForm, 0)
                    findNavController().navigate(
                        R.id.action_settingFragment_to_saveRecipesFragment, bundle
                    )
                }
            }

            clCp.setOnClickListener {
                if (isAdded) {
                    findNavController().navigate(R.id.action_settingFragment_to_changePasswordFragment)
                }
            }

            clNotification.setOnClickListener {
                if (isAdded) {
                    findNavController().navigate(R.id.action_settingFragment_to_notificationFragment)
                }
            }

            clCsm.setOnClickListener {
                if (isAdded) {
                    findNavController().navigate(R.id.action_settingFragment_to_socialMediaFragment)
                }
            }
            clRu.setOnClickListener {
                reviewApp()
            }
            clSa.setOnClickListener {
                showShareDialog()
            }
            clSubs.setOnClickListener {
                if (isAdded) {
//                    findNavController().navigate(R.id.action_settingFragment_to_subscriptionFragment)
                    findNavController().navigate(R.id.action_settingFragment_to_currentSubsFragment)
                }
            }

            clPp.setOnClickListener {
                val url = BuildConfig.BASE_URL + Constants.PRIVACY_POLICY
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                if (intent.resolveActivity(requireActivity().packageManager) != null) {
                    startActivity(intent)
                } else {
                    requireActivity().makeToast("No browser found")
                }
            }

            clTc.setOnClickListener {
                val url = BuildConfig.BASE_URL + Constants.TERMS_AND_CONDITIONS
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                if (intent.resolveActivity(requireActivity().packageManager) != null) {
                    startActivity(intent)
                } else {
                    requireActivity().makeToast("No browser found")
                }
            }


        }

    }

    private fun showShareDialog() {
        val dialogBinding = ShareDialogBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        dialog.apply {
            window?.apply {
                setGravity(Gravity.BOTTOM)
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                attributes = attributes?.apply { y = 50 }
            }
        }

        with(dialogBinding) {
            llShareVia.setOnClickListener {
                Extension.showProgress(requireActivity())
                sharedData(0)
                dialog.dismiss()
            }
            llShareWith.setOnClickListener {
                if (isAdded) {
                    findNavController().navigate(R.id.action_settingFragment_to_shareAppFragment)
                }
                dialog.dismiss()
            }
            llCopyLink.setOnClickListener {
                Extension.showProgress(requireActivity())
                sharedData(1)
                dialog.dismiss()
            }
            tvCancel.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun sharedData(type: Int) {
        val metadata: HashMap<String, String> = HashMap()
        val branchUniversalObject: BranchUniversalObject =
            BranchUniversalObject().addContentMetadata(metadata)
        val linkProperties = LinkProperties()

        branchUniversalObject.generateShortUrl(requireActivity(), linkProperties) { url, error ->
            if (error == null) {
                Extension.stopProgress()
                val url11 = "What do you think about Roots & Recipes ? $url"
                when (type) {
                    0 -> Extension.shareViaProfileMethod(url11, requireActivity())
                    1 -> Extension.copyUrlMethod(url11, requireActivity())
                }
            } else {
                Extension.stopProgress()
                requireActivity().makeToast(error.toString())
                error.toString()
            }
        }

    }

    private fun reviewApp() {
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // We got the ReviewInfo object
                val reviewInfo = task.result
                startReviewFlow(reviewInfo)
            } else {
                // There was some problem, log or handle the error code.
                @ReviewErrorCode val reviewErrorCode = (task.exception as ReviewException).errorCode
                Log.d("reviewErrorCode", reviewErrorCode.toString())
            }
        }
    }

    private fun startReviewFlow(reviewInfo: ReviewInfo) {
        reviewInfo.let {
            val flow = manager.launchReviewFlow(requireActivity(), it)
            flow.addOnCompleteListener { _ ->
                // The flow has finished, update the last prompt time
            }
        }
    }

    private fun deleteAccountDialog() {
        dialogDelete?.dismiss()
        dialogDelete = Dialog(requireContext())
        dialogDelete!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val window: Window? = dialogDelete!!.window
        val wlp = window!!.attributes
        wlp.gravity = Gravity.BOTTOM
        window.attributes = wlp
        dialogDelete!!.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        dialogDelete!!.setContentView(R.layout.dialog_delete)
        dialogDelete!!.window!!.setLayout(
            ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT
        )


        dialogDelete?.show()

        val tvCancelDelete: AppCompatTextView = dialogDelete!!.findViewById(R.id.tvCancelDelete)
        val tvYesDelete: AppCompatTextView = dialogDelete!!.findViewById(R.id.tvYesDelete)
        tvCancelDelete.setOnClickListener {
            dialogDelete!!.dismiss()
        }
        tvYesDelete.setOnClickListener {
            val userId = pref.getSignInData()!!._id
            vm.deleteUserFromFireStore(userId!!)
            vm.stopNotifications(userId){}
            vm.unregisterFromFCM()
            hitApiDeleteAccount()
            dialogDelete!!.dismiss()
        }
    }

    private fun logoutDialog() {
        dialogLogout?.dismiss()
        dialogLogout = Dialog(requireContext())
        dialogLogout!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val window: Window? = dialogLogout!!.window
        val wlp = window!!.attributes
        wlp.gravity = Gravity.BOTTOM
        window.attributes = wlp
        dialogLogout!!.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        dialogLogout!!.setContentView(R.layout.dialog_logout)
        dialogLogout!!.window!!.setLayout(
            ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        dialogLogout?.show()

        val tvCancel: AppCompatTextView = dialogLogout!!.findViewById(R.id.tvCancel)
        val tvYes: AppCompatTextView = dialogLogout!!.findViewById(R.id.tvYes)
        tvCancel.setOnClickListener {
            dialogLogout!!.dismiss()
        }
        tvYes.setOnClickListener {
            val userId = pref.getSignInData()!!._id
            updateOnlineStatus(userId!!,false)
            vm.stopNotifications(userId){}
            vm.unregisterFromFCM()
            vm.clearCache()
            vm.clearCacheHomePage()
            /**New**/
            (context as? ViewModelStoreOwner)?.viewModelStore?.clear()

            pref.clearPreference(requireContext())
            vm.myRecipesData.removeObservers(this@SettingFragment)
            vm.getUserDetailsData.removeObservers(this@SettingFragment)
            vm.updateProfileImageData.removeObservers(this@SettingFragment)
            vm.myRecipesData.removeObservers(this@SettingFragment)
            vm.allRecipesData.removeObservers(this@SettingFragment)
            vm.editProfileData.removeObservers(this@SettingFragment)
            Log.d("getSignInDataDebugSetting", "${pref.getSignInData()}")
            dialogLogout!!.dismiss()
            val intent = Intent(requireActivity(), MainActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
    }
    private fun updateOnlineStatus(userId:String,isLogin : Boolean){
        CoroutineScope(Dispatchers.Main).launch {
            vm.updateUserActiveStatus(userId,isLogin)
        }
    }
    private fun hitApiDeleteAccount() {
        Extension.showProgress(requireActivity())
        CoroutineScope(Dispatchers.Main).launch {
            vm.deleteAccount()
            vm.deleteAccountData.observe(requireActivity()) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        Extension.stopProgress()
                        viewLifecycleOwner.lifecycleScope.launch {
                            its.data?.message?.let { requireActivity().makeToast(it) }
                            pref.clearPreference(requireContext())
                            val intent = Intent(requireActivity(), MainActivity::class.java)
                            startActivity(intent)
                            requireActivity().finish()
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