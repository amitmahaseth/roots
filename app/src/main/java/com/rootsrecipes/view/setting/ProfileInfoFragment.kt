package com.rootsrecipes.view.setting

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.JsonObject
import com.rootsrecipes.BuildConfig
import com.rootsrecipes.MainActivity
import com.rootsrecipes.R
import com.rootsrecipes.databinding.ConfirmRecordingDialogBinding
import com.rootsrecipes.databinding.FragmentProfileInfoBinding
import com.rootsrecipes.utils.AWSSharedPref
import com.rootsrecipes.utils.BaseFragment
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.Extension
import com.rootsrecipes.utils.NetworkHelper
import com.rootsrecipes.utils.SharedPref
import com.rootsrecipes.utils.Status
import com.rootsrecipes.utils.gone
import com.rootsrecipes.utils.makeToast
import com.rootsrecipes.view.setting.viewmodel.SettingVM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileInfoFragment : BaseFragment() {

    private var galleryPath: File? = null
    private lateinit var binding: FragmentProfileInfoBinding
    private val pref: SharedPref by inject()
    private val awsPref: AWSSharedPref by inject()
    private val networkHelper: NetworkHelper by inject()
    private val vm: SettingVM by viewModel()
    private var photoDialog: Dialog? = null
    private var currentPhotoUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentProfileInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUi()
    }

    private fun initUi() {
        setDataForUser()
        setOnClickMethod()
        binding.etProfileUserName.filters =
            arrayOf(InputFilter { source, start, end, dest, dstart, dend ->
                source?.let {
                    if (it.contains(" ")) {
                        return@InputFilter it.toString().replace(" ", "")
                    }
                }
                null
            })
    }

    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
    private fun setDataForUser() {
        val userData = pref.getSignInData()
        Log.d("userDataDebug",userData.toString())
        if (userData != null) {
            binding.apply {
                etProfileName.setText(userData.first_name)
                etProfileLastName.setText(userData.last_name)
                etProfileUserName.setText(userData.user_name)
                etProfileEmail.setText(userData.email)
                etProfilePhoneNumber.setText(userData.phone_number)
                var limit = 100
                if (!userData.about_me.isNullOrEmpty()) {
                    etBio.setText(userData.about_me)
                    val length = userData.about_me!!.length
                    tvLimitText.text = "$length/100"
                    limit = length
                } else {
                    limit = 100
                }

                etBio.addTextChangedListener(object : TextWatcher {
                    @SuppressLint("SetTextI18n")
                    override fun afterTextChanged(s: Editable?) {
                        tvLimitText.text = "${(etBio.text.toString().length)}/100"
                    }

                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {

                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {

                    }
                })

                if (!userData.profile_image.isNullOrEmpty()) {
                    Glide.with(requireActivity())
                        .load(BuildConfig.BASE_MEDIA_URL + userData.profile_image)
                        .placeholder(requireActivity().getDrawable(R.drawable.profile_icon))
                        .into(binding.ivUserPf)
                }
                phoneNumberTextChangeListener(userData.phone_number!!)
                // etProfilePhoneNumber.isEnabled = userData.phone_number.isNullOrEmpty()
                if (userData.phone_number.isNullOrEmpty()) {
                    endIconPhoneNumberVisibility(0)
                } else {
                    if (userData.is_phone_number_verified!!) {
                        endIconPhoneNumberVisibility(1)
                    } else {
                        endIconPhoneNumberVisibility(2)
                    }
                }
            }
        }
    }

    private fun phoneNumberTextChangeListener(phoneNumber: String) {
        binding.apply {

            etProfilePhoneNumber.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                }

                override fun afterTextChanged(s: Editable?) {
                    if (etProfilePhoneNumber.text!!.trim().toString() != phoneNumber) {
                        endIconPhoneNumberVisibility(0)
                    }
                }

            })
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun endIconPhoneNumberVisibility(value: Int) {
        binding.apply {
            when (value) {
                0 -> {
                    //number is empty or for update
                    tiProfilePhoneNumber.endIconMode = TextInputLayout.END_ICON_NONE
                    tiProfilePhoneNumber.setEndIconOnClickListener(null)
                }

                1 -> {
                    //verified number
                    tiProfilePhoneNumber.endIconMode = TextInputLayout.END_ICON_CUSTOM
                    tiProfilePhoneNumber.endIconDrawable =
                        requireContext().getDrawable(R.drawable.tick_circle)
                    tiProfilePhoneNumber.setEndIconTintList(
                        ColorStateList.valueOf(
                            requireContext().getColor(
                                R.color.green
                            )
                        )
                    )
                    tiProfilePhoneNumber.setEndIconOnClickListener(null)
                }

                2 -> {
                    //not verified
                    tiProfilePhoneNumber.endIconMode = TextInputLayout.END_ICON_CUSTOM
                    tiProfilePhoneNumber.endIconDrawable =
                        requireContext().getDrawable(R.drawable.info_yellow)
                    tiProfilePhoneNumber.setEndIconTintList(
                        ColorStateList.valueOf(
                            requireContext().getColor(
                                R.color.yellow
                            )
                        )
                    )
                    tiProfilePhoneNumber.setEndIconOnClickListener {
                        phoneNumberVerifyDialog()
                    }
                }
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
    private fun phoneNumberVerifyDialog() {
        val dialogLayoutBinding = ConfirmRecordingDialogBinding.inflate(layoutInflater)
        val dialogBuilder = AlertDialog.Builder(requireContext()).setView(dialogLayoutBinding.root)
            .setCancelable(true)


        val dialog = dialogBuilder.create()
        val params = dialog.window?.attributes
        params?.y = 50
        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes = params

        dialogLayoutBinding.ivMic.setImageDrawable(requireContext().getDrawable(R.drawable.info_green))
        dialogLayoutBinding.tvHeaderText.text = "Phone Number Not Verified"
        dialogLayoutBinding.tvConfirmQuestionText.text =
            "Your phone number is not verified. Please verify it to proceed."

        dialogLayoutBinding.btnYes.text = requireActivity().getText(R.string.verify_now)
        dialogLayoutBinding.btnYes.setTextColor(requireActivity().getColor(R.color.green))
        dialogLayoutBinding.btnNo.text = requireActivity().getText(R.string.cancel)
        dialogLayoutBinding.btnNo.setTextColor(requireActivity().getColor(R.color.black))

        dialogLayoutBinding.cbConfirmRecipe.gone()
        dialogLayoutBinding.cbText.gone()

        dialogLayoutBinding.btnYes.setOnClickListener {
            hitApiForGenerateMobileOtp()
            dialog.dismiss()
        }
        dialogLayoutBinding.btnNo.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }


    private fun setOnClickMethod() {
        binding.apply {
            ivBackProfile.setOnClickListener {
                findNavController().popBackStack()
            }

            btnUpdatePi.setOnClickListener { updateProfile() }

            ivEditPf.setOnClickListener { photoDialog() }
        }
    }

    private val getImageResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val selectedImageUri: Uri? = data?.data
                if (selectedImageUri != null) {
                    try {
                        Glide.with(requireActivity()).load(selectedImageUri).into(binding.ivUserPf)

                        galleryPath = copyUriToInternalStorage(selectedImageUri)
                        newUploadMethod(galleryPath!!)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e("CatchError", "Error: ${e.message}")
                    }
                }
            }
        }

    private fun copyUriToInternalStorage(uri: Uri): File {
        val inputStream = requireActivity().contentResolver.openInputStream(uri)
        val file =
            File(requireContext().filesDir, "profile_image_${System.currentTimeMillis()}.jpg")
        inputStream?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    private fun updateProfile() {
        val etNameOfUser = binding.etProfileName.text.toString().trim()
        val etLastName = binding.etProfileLastName.text.toString().trim()
        val etUSerName = binding.etProfileUserName.text.toString().trim()
        val etPhoneNumber = binding.etProfilePhoneNumber.text.toString().trim()
        val etAboutMe = binding.etBio.text.toString().trim()
        val codeCp = binding.codeCp.selectedCountryCode

        when {
            etNameOfUser.isEmpty() -> requireActivity().makeToast("Please enter your name!")
            etUSerName.isEmpty() -> requireActivity().makeToast("Please enter your username!")
            //  etPhoneNumber.isEmpty() -> requireActivity().makeToast("Please enter your phone number!")

            else -> {
                val jsonObject = JsonObject()
                jsonObject.addProperty(Constants.first_name, etNameOfUser)
                jsonObject.addProperty(Constants.last_name, etLastName)
                jsonObject.addProperty(Constants.user_name, etUSerName)
                jsonObject.addProperty(Constants.phone_number, etPhoneNumber)
                jsonObject.addProperty(Constants.country_code, "+$codeCp")
                jsonObject.addProperty(Constants.aboutMe, etAboutMe)
                hitApiEditProfile(jsonObject)
            }
        }
    }


    private val captureImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Use the saved URI instead of result.data?.data
                galleryPath = null
                currentPhotoUri?.let { uri ->
                    galleryPath = copyUriToInternalStorage(uri)
                    Glide.with(requireActivity()).load(uri).into(binding.ivUserPf)
                    newUploadMethod(galleryPath!!)
                }
            }
        }

    private fun photoDialog() {
        photoDialog?.dismiss()
        photoDialog = Dialog(requireContext())
        photoDialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val window: Window? = photoDialog!!.window
        val wlp = window!!.attributes
        wlp.gravity = Gravity.BOTTOM
        window.attributes = wlp
        photoDialog!!.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        photoDialog!!.setContentView(R.layout.photo_dialog)
        photoDialog!!.window!!.setLayout(
            ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        photoDialog?.show()

        val llGallery: LinearLayout = photoDialog!!.findViewById(R.id.llGallery)
        val llCamera: LinearLayout = photoDialog!!.findViewById(R.id.llCamera)
        val tvCancel: TextView = photoDialog!!.findViewById(R.id.tvCancel)
        llGallery.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                == PackageManager.PERMISSION_GRANTED
            ) {
                openGallery()
            } else {
                // Request permission
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            photoDialog?.dismiss()
        }
        llCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                openCamera()
            } else {
                // Request camera permission
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            photoDialog?.dismiss()
        }
        tvCancel.setOnClickListener {
            photoDialog!!.dismiss()
        }
    }

    private fun openGallery() {
//        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
//            type = "image/*"
//        }
//        val chooserIntent = Intent.createChooser(intent, getString(R.string.str_select_picture))
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        getImageResultLauncher.launch(intent)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openGallery()
            } else {
                requireActivity().makeToast("Permission denied")
            }
        }
    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openCamera()
            } else {
                requireActivity().makeToast("Permission denied")
            }
        }

    private fun openCamera() {
        val photoFile: File?
        try {
            photoFile = createImageFile()
        } catch (ex: IOException) {
            // Error occurred while creating the File
            ex.printStackTrace()
            return
        }

        photoFile.let { file ->
            currentPhotoUri = FileProvider.getUriForFile(
                requireActivity(), "com.rootsrecipes.fileprovider", file
            )

            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri)
            }
            captureImageLauncher.launch(cameraIntent)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        return File.createTempFile(
            imageFileName, ".jpg", storageDir
        )
    }

    @SuppressLint("FragmentLiveDataObserve")
    private fun hitApiEditProfile(jsonObject: JsonObject) {
        Extension.showProgress(requireContext())
        CoroutineScope(Dispatchers.Main).launch {
            vm.editProfile(jsonObject)
            vm.editProfileData.observe(this@ProfileInfoFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        Extension.stopProgress()
                       /* val updateData = pref.getSignInData()
                        updateData!!.apply {
                            first_name = binding.etProfileName.text.toString()
                            last_name = binding.etProfileLastName.text.toString()
                            user_name = binding.etProfileUserName.text.toString()
                            phone_number = binding.etProfilePhoneNumber.text.toString()
                            about_me = binding.etBio.text.toString()
                            country_code = jsonObject.get(Constants.country_code).toString()
                        }*/
                        its.data?.data?.let {
                            pref.saveSignInData(its.data.data) }
                        its.data?.message?.let { requireActivity().makeToast(it) }
                        Log.d("userDataDebug",pref.getSignInData().toString())
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

    override fun onDestroyView() {
        super.onDestroyView()
        vm.editProfileData.removeObservers(viewLifecycleOwner)
        vm.generateMobileOtpData.removeObservers(viewLifecycleOwner)
    }

    private fun hitApiForGenerateMobileOtp() {
        CoroutineScope(Dispatchers.Main).launch {
            vm.generateMobileOtp()
            vm.generateMobileOtpData.observe(requireActivity()) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        Extension.stopProgress()
                        viewLifecycleOwner.lifecycleScope.launch {
                            its.data?.message?.let { requireActivity().makeToast(it) }
                            val bundle = Bundle()
                            bundle.putInt(Constants.typeFrom, 2)
                            bundle.putString(
                                Constants.phone_number,
                                "+" + binding.codeCp.selectedCountryCode + " " + binding.etProfilePhoneNumber.text.toString()
                            )
                            findNavController().navigate(
                                R.id.action_profileInfoFragment_to_otpFragment, bundle
                            )
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

    private fun newUploadMethod(uploadGalleryPath: File) {
        Extension.showProgress(requireContext())
        val transferUtility = TransferUtility.builder().context(requireActivity())
            .defaultBucket(awsPref.fetchString(Constants.aws_bucket_name)).s3Client(
                MainActivity.s3client
            ).build()
        if (networkHelper.isNetworkConnected()) {
            val background = object : Thread() {
                override fun run() {
                    try {
                        val key = "RootsAndRecipes/Media/${System.currentTimeMillis()}/image.jpg"
                        val uploadObserver = transferUtility.upload(key, uploadGalleryPath)

                        uploadObserver.setTransferListener(object : TransferListener {
                            override fun onStateChanged(id: Int, state: TransferState?) {
                                if (state == TransferState.COMPLETED) {
                                    val jsonObject = JsonObject()
                                    jsonObject.addProperty(Constants.profile_image, key)
                                    hitApiUpdateProfileImage(jsonObject)
                                } else if (state == TransferState.FAILED) {
                                    Extension.stopProgress()
                                    requireActivity().makeToast("Profile picture not uploaded.")
                                }
                            }

                            override fun onProgressChanged(
                                id: Int, bytesCurrent: Long, bytesTotal: Long
                            ) {
                                // Update progress if needed
                            }

                            override fun onError(id: Int, ex: Exception?) {
                                // Handle error
                                Extension.stopProgress()
                            }
                        })
                    } catch (e: AmazonServiceException) {
                        Extension.stopProgress()
                        e.printStackTrace()
                    } catch (e: AmazonClientException) {
                        Extension.stopProgress()
                        e.printStackTrace()
                    } catch (e: Exception) {
                        Extension.stopProgress()
                        e.printStackTrace()
                    }
                }
            }
            background.start()
        } else {
            requireActivity().makeToast("Internet not available")
        }
    }

    private fun hitApiUpdateProfileImage(jsonObject: JsonObject) {
        CoroutineScope(Dispatchers.Main).launch {
            vm.updateProfileImageData(jsonObject)
            vm.updateProfileImageData.observe(this@ProfileInfoFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        Extension.stopProgress()
                        its.data?.data?.let { pref.saveSignInData(it) }

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