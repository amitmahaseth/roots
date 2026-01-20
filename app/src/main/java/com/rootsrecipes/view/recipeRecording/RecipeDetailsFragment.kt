package com.rootsrecipes.view.recipeRecording

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.airbnb.lottie.LottieDrawable
import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.ChipGroup
import com.google.gson.JsonObject
import com.rootsrecipes.BuildConfig
import com.rootsrecipes.MainActivity
import com.rootsrecipes.R
import com.rootsrecipes.databinding.AddCuisineDialogBinding
import com.rootsrecipes.databinding.ConfirmRecordingDialogBinding
import com.rootsrecipes.databinding.FragmentRecipeDetailsBinding
import com.rootsrecipes.databinding.ProcessDialogBinding
import com.rootsrecipes.databinding.RecordBottomsheetBinding
import com.rootsrecipes.databinding.TutorialDialogLayoutBinding
import com.rootsrecipes.utils.AWSSharedPref
import com.rootsrecipes.utils.BaseFragment
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.Extension
import com.rootsrecipes.utils.NetworkHelper
import com.rootsrecipes.utils.Status
import com.rootsrecipes.utils.gone
import com.rootsrecipes.utils.makeToast
import com.rootsrecipes.utils.visible
import com.rootsrecipes.view.createAccount.model.RecipeData
import com.rootsrecipes.view.recipeRecording.model.ChipData
import com.rootsrecipes.view.recipeRecording.viewmodel.RecipeCreateViewModel
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

class RecipeDetailsFragment : BaseFragment() {
    private var processDialog: AlertDialog? = null
    private var recordBottomSheet: RecordBottomsheetBinding? = null
    private var bottomSheetDialog: BottomSheetDialog? = null
    private lateinit var binding: FragmentRecipeDetailsBinding
    private var audioPermission = false
    private var isListening = false
    private var speechRecognizer: SpeechRecognizer? = null
    private var speechText = ""
    private var selectedCategory = -1
    private var selectedCuisine = -1
    private var selectedSubCategory = -1

    private val vm: RecipeCreateViewModel by viewModel()
    private var photoDialog: Dialog? = null
    private var currentPhotoUri: Uri? = null
    private var galleryPath: File? = null
    private val awsPref: AWSSharedPref by inject()
    private val networkHelper: NetworkHelper by inject()

    private lateinit var chipDataCategoryList: ArrayList<ChipData>
    private lateinit var chipDataCuisineList: ArrayList<ChipData>
    private lateinit var chipDataSubCategoryList: ArrayList<ChipData>
    private var recipeDetails: RecipeData? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentRecipeDetailsBinding.inflate(inflater)
        chipDataSubCategoryList = ArrayList()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getCuisinesApi()
        initUi()
    }

    @SuppressLint("FragmentLiveDataObserve")
    private fun getCuisinesApi() {
        Extension.showProgress(requireActivity())
        CoroutineScope(Dispatchers.Main).launch {
            vm.getCuisinesData()
            vm.cuisinesData.observe(this@RecipeDetailsFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        Extension.stopProgress()
                        chipDataCuisineList = its.data?.data?.cuisines!!.map { ChipData(it) } as ArrayList<ChipData>
                        chipDataCategoryList = its.data.data.categories.map {
                            ChipData(it.name, subCategories = it.sub_categories)
                        } as ArrayList<ChipData>
                        if (recipeDetails != null) {
                            setRecipeDetails()
                        }
                        setChipGroups()
                    }
                    Status.ERROR -> {
                        Extension.stopProgress()
                        its.message?.let { requireActivity().makeToast(it) }
                    }
                    Status.LOADING -> {}
                }
            }
        }
    }

    private fun initUi() {
        if (requireArguments().getInt(Constants.typeFrom) == 0) {
            if (requireArguments().getString("recorded_recipe") != null) {
                speechText = requireArguments().getString("recorded_recipe") as String
            }
        } else if (requireArguments().getInt(Constants.typeFrom) == 1) {
            recipeDetails = requireArguments().getParcelable(Constants.recipeInformation)
            speechText = requireArguments().getString("recorded_recipe") as String
        }
        if (Constants.EDITRECIPE) {
            recipeDetails = Constants.recipeDetailsEdit
        }
        setOnClickMethod()
        checkAudioPermission()
        setBottomSheetAudioRecord()
    }

    private fun setRecipeDetails() {
        binding.apply {
            etNameOfRecipes.setText(recipeDetails!!.title)
            for (it in chipDataCategoryList.indices) {
                if (chipDataCategoryList[it].text.equals(recipeDetails!!.category!!, ignoreCase = true)) {
                    chipDataCategoryList[it].isChecked = true
                    selectedCategory = it
                    updateSubCategoryChips(chipDataCategoryList[it].subCategories)
                    break
                } else {
                    chipDataCategoryList[it].isChecked = false
                }
            }
            for (it in chipDataCuisineList.indices) {
                if (chipDataCuisineList[it].text.equals(recipeDetails!!.cuisine!!, ignoreCase = true)) {
                    chipDataCuisineList[it].isChecked = true
                    selectedCuisine = it
                    break
                } else {
                    chipDataCuisineList[it].isChecked = false
                }
            }
            if (!recipeDetails!!.recipe_image.isNullOrEmpty()) {
                Glide.with(requireActivity())
                    .load(BuildConfig.BASE_MEDIA_URL + recipeDetails!!.recipe_image)
                    .into(ivAddPhoto)
                ivAddPhoto.clipToOutline = true
                clUploadPhoto.gone()
                editRecipeImage.visible()
            }
        }
    }

    private fun checkAudioPermission() {
        audioPermission = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun setOnClickMethod() {
        binding.apply {
            ivBackRecipeDetail.setOnClickListener {
                findNavController().navigateUp()
            }
            infoBtn.setOnClickListener { showTutorialDialog() }
            tvAddCuisine.setOnClickListener { showAddNewCuisineDialog() }
            clUploadPhoto.setOnClickListener { photoDialog() }
            editRecipeImage.setOnClickListener { photoDialog() }
            btnProceed.setOnClickListener {
                when {
                    etNameOfRecipes.text.isEmpty() -> {
                        requireActivity().makeToast("Enter name of recipe")
                    }
                    selectedCategory == -1 -> {
                        requireActivity().makeToast("Choose a category")
                    }
                    selectedCuisine == -1 -> {
                        requireActivity().makeToast("Choose a cuisine")
                    }
                    /*chipDataSubCategoryList.isNotEmpty() && selectedSubCategory == -1 -> {
                        requireActivity().makeToast("Choose a sub-category")
                    }*/
                    !networkHelper.isNetworkConnected() -> {
                        requireActivity().makeToast("Internet not available")
                    }
                    else -> {
                        val jsonObject = JsonObject().apply {
                            addProperty("title", etNameOfRecipes.text.toString())
                            addProperty("cuisine", chipDataCuisineList[selectedCuisine].text)
                            addProperty("category", chipDataCategoryList[selectedCategory].text)
                            if (selectedSubCategory != -1) {
                                addProperty("sub_category", chipDataSubCategoryList[selectedSubCategory].text)
                            }
                            addProperty("transcribed_text", speechText)
                        }
                        if (recipeDetails == null) {
                            if (galleryPath != null) {
                                newUploadMethod(galleryPath!!, jsonObject)
                            } else {
                                showProcessDialog()
                                hitApiExtractDetails(jsonObject)
                            }
                        } else {
                            if (galleryPath == null) {
                                jsonObject.addProperty("recipe_image", recipeDetails!!.recipe_image ?: "")
                                showProcessDialog()
                                hitApiExtractDetails(jsonObject)
                            } else {
                                newUploadMethod(galleryPath!!, jsonObject)
                            }
                        }
                    }
                }
            }
            ivMicNameOfRecipe.setOnClickListener {
                if (audioPermission) {
                    startSpeechToText()
                } else {
                    showAudioPermissionDialog()
                }
            }
        }
    }

    @SuppressLint("FragmentLiveDataObserve")
    private fun hitApiExtractDetails(jsonObject: JsonObject) {
        CoroutineScope(Dispatchers.Main).launch {
            vm.extractDetails(jsonObject)
            vm.extractDetailsData.observe(this@RecipeDetailsFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        processDialog?.dismiss()
                        recipeDetails = its.data!!.data
                        Extension.stopProgress()
                        val bundle = Bundle().apply {
                            putParcelable(Constants.recipeInformation, its.data.data)
                            putInt(Constants.typeFrom, 0)
                        }
                        its.message?.let { requireActivity().makeToast(it) }
                        viewLifecycleOwner.lifecycleScope.launch {
                            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                                val currentDestination = findNavController().currentDestination
                                if (currentDestination?.id == R.id.recipeDetailsFragment) {
                                    Constants.EDITRECIPE = false
                                    Constants.recipeDetailsEdit = RecipeData()
                                    findNavController().navigate(
                                        R.id.action_recipeDetailsFragment_to_recipeInformationFragment,
                                        bundle
                                    )
                                }
                            }
                        }
                    }
                    Status.ERROR -> {
                        Extension.stopProgress()
                        processDialog?.dismiss()
                        its.message?.let { requireActivity().makeToast(it) }
                    }
                    Status.LOADING -> {}
                }
            }
        }
    }

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    private fun showAudioPermissionDialog() {
        val dialogLayoutBinding = ConfirmRecordingDialogBinding.inflate(layoutInflater)
        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogLayoutBinding.root)
            .setCancelable(true)
        val dialog = dialogBuilder.create()
        dialog.window?.apply {
            attributes.y = 50
            setGravity(Gravity.BOTTOM)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialogLayoutBinding.apply {
            ivMic.setImageDrawable(requireContext().getDrawable(R.drawable.info_green))
            tvHeaderText.text = "Allow Microphone and Speech\nRecognizer access for Voice Search?"
            tvConfirmQuestionText.text = "It'll help us translate your voice to search\nwithin Roots & Recipes "
            btnYes.text = requireActivity().getString(R.string.continue_text)
            btnYes.setTextColor(requireActivity().getColor(R.color.green))
            btnNo.text = requireActivity().getString(R.string.not_now)
            btnNo.setTextColor(requireActivity().getColor(R.color.black))
            cbConfirmRecipe.gone()
            cbText.gone()
            btnYes.setOnClickListener {
                when {
                    ContextCompat.checkSelfPermission(
                        requireContext(), Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        requireActivity().makeToast("Permission Already Granted")
                        checkAudioPermission()
                    }
                    shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        requireActivity().makeToast("Audio permission is needed for recording audio.")
                    }
                    else -> {
                        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
                dialog.dismiss()
            }
            btnNo.setOnClickListener { dialog.dismiss() }
        }
        dialog.show()
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                checkAudioPermission()
                startSpeechToText()
            } else {
                requireActivity().makeToast("Permission Denied")
            }
        }

    @SuppressLint("InflateParams")
    private fun setBottomSheetAudioRecord() {
        bottomSheetDialog = BottomSheetDialog(requireActivity(), R.style.CustomBottomSheetDialogTheme)
        recordBottomSheet = RecordBottomsheetBinding.inflate(LayoutInflater.from(requireActivity()))
        recordBottomSheet!!.apply {
            ivRecordingIcon.apply {
                repeatCount = LottieDrawable.INFINITE
                playAnimation()
            }
            ivBackgroundRecordingIcon.apply {
                repeatCount = LottieDrawable.INFINITE
                playAnimation()
            }
        }
        bottomSheetDialog!!.setContentView(recordBottomSheet!!.root)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun startSpeechToText() {
        if (isListening) {
            stopListening()
            return
        }
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                bottomSheetDialog!!.show()
            }
            override fun onBeginningOfSpeech() {
                isListening = true
                bottomSheetDialog!!.show()
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
                if (isAdded) bottomSheetDismiss()
            }
            override fun onError(error: Int) {
                isListening = false
                bottomSheetDismiss()
                requireActivity().makeToast("Error: $error")
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                bottomSheetDismiss()
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    speechText += matches[0] + " "
                    binding.etNameOfRecipes.setText(speechText)
                }
            }
            @SuppressLint("SetTextI18n")
            override fun onPartialResults(partialResults: Bundle?) {
                val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!partialMatches.isNullOrEmpty()) {
                    recordBottomSheet!!.tvTextRecorded.text = speechText + partialMatches[0]
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer?.startListening(intent)
    }

    private fun bottomSheetDismiss() {
        recordBottomSheet!!.tvTextRecorded.text = ""
        bottomSheetDialog!!.dismiss()
    }

    private fun stopListening() {
        isListening = false
        speechRecognizer?.stopListening()
    }

    private fun showAddNewCuisineDialog() {
        val dialogLayoutBinding = AddCuisineDialogBinding.inflate(layoutInflater)
        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogLayoutBinding.root)
            .setCancelable(true)
        val dialog = dialogBuilder.create()
        dialog.window?.apply {
            attributes.y = 50
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        Extension.setMaxLength(dialogLayoutBinding.etEnterNewCuisine, 25)
        dialogLayoutBinding.apply {
            btnAddNewCuisine.setOnClickListener {
                if (etEnterNewCuisine.text.isEmpty()) {
                    requireActivity().makeToast("Enter New Cuisine")
                } else {
                    addCuisinesApi(etEnterNewCuisine.text.toString())
                    dialog.dismiss()
                }
            }
            ivClose.setOnClickListener { dialog.dismiss() }
        }
        dialog.show()
    }

    @SuppressLint("FragmentLiveDataObserve")
    private fun addCuisinesApi(cuisinesValue: String) {
        Extension.showProgress(requireActivity())
        CoroutineScope(Dispatchers.Main).launch {
            val jsonObject = JsonObject().apply {
                addProperty(Constants.cuisine, cuisinesValue)
            }
            vm.addCuisinesData(jsonObject)
            vm.addCuisinesData.observe(this@RecipeDetailsFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        Extension.stopProgress()
                        its.data?.data?.let { ChipData(it.cuisine) }?.let { chipDataCuisineList.add(it) }
                        addChips(chipDataCuisineList, binding.cgSelectCuisine, 1)
                    }
                    Status.ERROR -> {
                        its.message?.let { requireActivity().makeToast(it) }
                        Extension.stopProgress()
                    }
                    Status.LOADING -> {}
                }
            }
        }
    }

    private fun setChipGroups() {
        addChips(chipDataCuisineList, binding.cgSelectCuisine, 1)
        addChips(chipDataCategoryList, binding.cgSelectCategory, 0)
    }

    private fun addChips(list: ArrayList<ChipData>, chipGroup: ChipGroup, type: Int) {
        chipGroup.removeAllViews()
        chipGroup.requestLayout()
        if (list.isNotEmpty()) {
            for (i in list.indices) {
                val linearLayout = LinearLayout(requireActivity()).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    setBackgroundResource(R.drawable.chip_bg)
                    setPadding(20, 0, 20, 0)
                }
                val textview = TextView(requireActivity()).apply {
                    text = list[i].text
                    setTextColor(requireActivity().getColor(R.color.black))
                    textSize = 16f
                    typeface = ResourcesCompat.getFont(requireContext(), R.font.work_sans)
                    setPadding(50, 30, 48, 33)
                }
                linearLayout.addView(textview)
                chipGroup.addView(linearLayout)
                if ((type == 0 && selectedCategory == i) || (type == 1 && selectedCuisine == i)) {
                    linearLayout.setBackgroundResource(R.drawable.chip_bg_selected)
                    textview.setTextColor(requireActivity().getColor(R.color.white))
                    if (type == 0) updateSubCategoryChips(list[i].subCategories)
                }
                linearLayout.setOnClickListener {
                    for (j in 0 until chipGroup.childCount) {
                        val child = chipGroup.getChildAt(j) as LinearLayout
                        val childTextView = child.getChildAt(0) as TextView
                        childTextView.setTextColor(requireActivity().getColor(R.color.black))
                        child.setBackgroundResource(R.drawable.chip_bg)
                        if (type == 0) chipDataCategoryList[j].isChecked = false
                        else if (type == 1) chipDataCuisineList[j].isChecked = false
                    }
                    linearLayout.setBackgroundResource(R.drawable.chip_bg_selected)
                    textview.setTextColor(requireActivity().getColor(R.color.white))
                    if (type == 0) {
                        selectedCategory = i
                        chipDataCategoryList[i].isChecked = true
                        updateSubCategoryChips(list[i].subCategories)
                    } else if (type == 1) {
                        selectedCuisine = i
                        chipDataCuisineList[i].isChecked = true
                    }
                }
            }
            chipGroup.chipSpacingVertical = 40
            chipGroup.chipSpacingHorizontal = 30
        }
    }

    private fun addSubCategoryChips(list: ArrayList<ChipData>, chipGroup: ChipGroup) {
        chipGroup.removeAllViews()
        chipGroup.requestLayout()
        if (list.isNotEmpty()) {
            for (i in list.indices) {
                val linearLayout = LinearLayout(requireActivity()).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    setBackgroundResource(R.drawable.chip_bg)
                    setPadding(20, 0, 20, 0)
                }
                val textview = TextView(requireActivity()).apply {
                    text = list[i].text
                    setTextColor(requireActivity().getColor(R.color.black))
                    textSize = 16f
                    typeface = ResourcesCompat.getFont(requireContext(), R.font.work_sans)
                    setPadding(50, 30, 48, 33)
                }
                linearLayout.addView(textview)
                chipGroup.addView(linearLayout)
                if (selectedSubCategory == i) {
                    linearLayout.setBackgroundResource(R.drawable.chip_bg_selected)
                    textview.setTextColor(requireActivity().getColor(R.color.white))
                }
                linearLayout.setOnClickListener {
                    for (j in 0 until chipGroup.childCount) {
                        val child = chipGroup.getChildAt(j) as LinearLayout
                        val childTextView = child.getChildAt(0) as TextView
                        childTextView.setTextColor(requireActivity().getColor(R.color.black))
                        child.setBackgroundResource(R.drawable.chip_bg)
                        chipDataSubCategoryList[j].isChecked = false
                    }
                    linearLayout.setBackgroundResource(R.drawable.chip_bg_selected)
                    textview.setTextColor(requireActivity().getColor(R.color.white))
                    selectedSubCategory = i
                    chipDataSubCategoryList[i].isChecked = true
                }
            }
            chipGroup.chipSpacingVertical = 40
            chipGroup.chipSpacingHorizontal = 30
        }
    }

    private fun updateSubCategoryChips(subCategories: ArrayList<String>?) {
        binding.clSelectSubCategory.visible()
        chipDataSubCategoryList.clear()
        selectedSubCategory = -1
        binding.cgSelectSubCategory.removeAllViews()
        if (subCategories != null && subCategories.isNotEmpty()) {
            chipDataSubCategoryList.addAll(subCategories.map { ChipData(it) })
            addSubCategoryChips(chipDataSubCategoryList, binding.cgSelectSubCategory)
        }
    }

    private fun showTutorialDialog() {
        val dialogLayoutBinding = TutorialDialogLayoutBinding.inflate(layoutInflater)
        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogLayoutBinding.root)
            .setCancelable(true)
        val dialog = dialogBuilder.create()
        dialog.window?.apply {
            attributes.y = 50
            setGravity(Gravity.BOTTOM)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialogLayoutBinding.btnOk.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showProcessDialog() {
        val dialogLayoutBinding = ProcessDialogBinding.inflate(layoutInflater)
        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogLayoutBinding.root)
            .setCancelable(false)
        processDialog = dialogBuilder.create()
        processDialog?.window?.apply {
            attributes.y = 50
            setGravity(Gravity.BOTTOM)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialogLayoutBinding.btnCancelProcess.gone()
        dialogLayoutBinding.btnCancelProcess.setOnClickListener { processDialog?.dismiss() }
        processDialog?.show()
    }

    private fun photoDialog() {
        photoDialog?.dismiss()
        photoDialog = Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.photo_dialog)
            window?.apply {
                setBackgroundDrawableResource(android.R.color.transparent)
                attributes.gravity = Gravity.BOTTOM
                setLayout(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT)
            }
        }
        photoDialog?.findViewById<LinearLayout>(R.id.llGallery)?.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    requestGalleryPermissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
                }
            } else {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    requestGalleryPermissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                }
            }
            photoDialog?.dismiss()
        }
        photoDialog?.findViewById<LinearLayout>(R.id.llCamera)?.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            photoDialog?.dismiss()
        }
        photoDialog?.findViewById<TextView>(R.id.tvCancel)?.setOnClickListener {
            photoDialog?.dismiss()
        }
        photoDialog?.show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        getImageResultLauncher.launch(intent)
    }

    private val requestGalleryPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false
            if (granted) openGallery() else requireActivity().makeToast("Permission denied")
        }

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) openCamera() else requireActivity().makeToast("Permission denied")
        }

    private val getImageResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val selectedImageUri: Uri? = data?.data
                if (selectedImageUri != null) {
                    try {
                        binding.apply {
                            Glide.with(requireActivity()).load(selectedImageUri).into(ivAddPhoto)
                            ivAddPhoto.clipToOutline = true
                            clUploadPhoto.gone()
                            editRecipeImage.visible()
                        }
                        galleryPath = copyUriToInternalStorage(selectedImageUri)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e("CatchError", "Error: ${e.message}")
                    }
                }
            }
        }

    private fun openCamera() {
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            ex.printStackTrace()
            return
        }
        photoFile?.let { file ->
            currentPhotoUri = FileProvider.getUriForFile(
                requireActivity(), "com.rootsrecipes.fileprovider", file
            )
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri)
            }
            captureImageLauncher.launch(cameraIntent)
        }
    }

    private val captureImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                galleryPath = null
                currentPhotoUri?.let { uri ->
                    galleryPath = copyUriToInternalStorage(uri)
                    Glide.with(requireActivity()).load(uri).into(binding.ivAddPhoto)
                    binding.ivAddPhoto.clipToOutline = true
                    binding.clUploadPhoto.gone()
                    binding.editRecipeImage.visible()
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
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    private fun newUploadMethod(uploadGalleryPath: File, jsonObject: JsonObject) {
        showProcessDialog()
        val transferUtility = TransferUtility.builder()
            .context(requireActivity())
            .defaultBucket(awsPref.fetchString(Constants.aws_bucket_name))
            .s3Client(MainActivity.s3client)
            .build()
        if (networkHelper.isNetworkConnected()) {
            val background = object : Thread() {
                override fun run() {
                    try {
                        val key = "RootsAndRecipes/Media/${System.currentTimeMillis()}/image.jpg"
                        val uploadObserver = transferUtility.upload(key, uploadGalleryPath)
                        uploadObserver.setTransferListener(object : TransferListener {
                            override fun onStateChanged(id: Int, state: TransferState?) {
                                if (state == TransferState.COMPLETED) {
                                    jsonObject.addProperty(Constants.recipe_image, key)
                                    hitApiExtractDetails(jsonObject)
                                } else if (state == TransferState.FAILED) {
                                    Extension.stopProgress()
                                    processDialog?.dismiss()
                                    requireActivity().makeToast("Profile picture not uploaded.")
                                }
                            }
                            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {}
                            override fun onError(id: Int, ex: Exception?) {
                                Extension.stopProgress()
                                processDialog?.dismiss()
                            }
                        })
                    } catch (e: AmazonServiceException) {
                        Extension.stopProgress()
                        processDialog?.dismiss()
                        e.printStackTrace()
                    } catch (e: AmazonClientException) {
                        Extension.stopProgress()
                        processDialog?.dismiss()
                        e.printStackTrace()
                    } catch (e: Exception) {
                        Extension.stopProgress()
                        processDialog?.dismiss()
                        e.printStackTrace()
                    }
                }
            }
            background.start()
        } else {
            requireActivity().makeToast("Internet not available")
            processDialog?.dismiss()
        }
    }
}