package com.rootsrecipes.view.myRecipes

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.rootsrecipes.BuildConfig
import com.rootsrecipes.MainActivity
import com.rootsrecipes.R
import com.rootsrecipes.databinding.AddCuisineDialogBinding
import com.rootsrecipes.databinding.BottomsheetListBinding
import com.rootsrecipes.databinding.FragmentEditRecipesBinding
import com.rootsrecipes.model.OnItemClickListener
import com.rootsrecipes.utils.AWSSharedPref
import com.rootsrecipes.utils.BaseFragment
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.Extension
import com.rootsrecipes.utils.ItemMoveCallback
import com.rootsrecipes.utils.NetworkHelper
import com.rootsrecipes.utils.Status
import com.rootsrecipes.utils.TimePickerDialog
import com.rootsrecipes.utils.gone
import com.rootsrecipes.utils.makeToast
import com.rootsrecipes.utils.visible
import com.rootsrecipes.view.createAccount.model.RecipeData
import com.rootsrecipes.view.myRecipes.adapter.BottomSheetListAdapter
import com.rootsrecipes.view.myRecipes.adapter.EditRecipeAdapter
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

class EditRecipesFragment : BaseFragment() {

    private lateinit var binding: FragmentEditRecipesBinding
    private val vm: RecipeCreateViewModel by viewModel()
    private val awsPref: AWSSharedPref by inject()
    private val networkHelper: NetworkHelper by inject()

    private lateinit var categoryList: ArrayList<String>
    private lateinit var cuisineList: ArrayList<String>
    private lateinit var subCategoryList: ArrayList<String>
    private var categoryToSubCategoriesMap: HashMap<String, ArrayList<String>> = HashMap()

    private var bottomSheetDialog: BottomSheetDialog? = null
    private var listBottomSheet: BottomsheetListBinding? = null

    private var recipeDetails: RecipeData? = null
    private var editTypeIngredients = "Ingredients"
    private var editTypeSteps = "Steps"
    private var stepsAdapter: EditRecipeAdapter? = null
    private var ingredientsAdapter: EditRecipeAdapter? = null

    // Time
    private var minTime = 0
    private var maxTime = 0
    private var initialMinMinutes = 0
    private var initialMinHours = 0
    private var initialMaxMinutes = 0
    private var initialMaxHours = 0

    // Image
    private var photoDialog: Dialog? = null
    private var currentPhotoUri: Uri? = null
    private var galleryPath: File? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditRecipesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObservers()
        initUi()
    }

    @SuppressLint("FragmentLiveDataObserve")
    private fun initObservers() {}

    private fun initUi() {
        if (arguments != null) {
            val bundle = arguments
            recipeDetails = bundle!!.getParcelable(Constants.recipeInformation)!!
        }
        binding.clSelectSubCategory.gone() // Hide sub-category by default
        getCuisinesApi()
        setOnClickMethod()
    }

    private fun getTimeValuesMethod() {
        val minTimeArray = binding.tvMinTimeValue.text.split(" ")
        when (minTimeArray.size) {
            4 -> {
                val hours = minTimeArray[0].toInt()
                val minutes = minTimeArray[2].toInt()
                initialMinHours = hours
                initialMinMinutes = minutes
                minTime = hours * 60 + minutes
            }
            2 -> {
                val minutes = minTimeArray[0].toInt()
                initialMinMinutes = minutes
                minTime = minutes
            }
            else -> minTime = 0
        }

        val maxTimeArray = binding.tvMaxTimeValue.text.split(" ")
        when (maxTimeArray.size) {
            4 -> {
                val hours = maxTimeArray[0].toInt()
                val minutes = maxTimeArray[2].toInt()
                initialMaxHours = hours
                initialMaxMinutes = minutes
                maxTime = hours * 60 + minutes
            }
            2 -> {
                val minutes = maxTimeArray[0].toInt()
                initialMaxMinutes = minutes
                maxTime = minutes
            }
            else -> maxTime = 0
        }
    }

    private fun setOnClickMethod() {
        binding.apply {
            tvAddIngredients.setOnClickListener { showAddEditItem(editTypeIngredients) }
            tvAddSteps.setOnClickListener { showAddEditItem(editTypeSteps) }
            llMinTime.setOnClickListener { showTimePicker(tvMinTimeValue, "Set Minimum Time", 0) }
            llMaxTime.setOnClickListener { showTimePicker(tvMaxTimeValue, "Set Maximum Time", 1) }
            btnUpdateRecipe.setOnClickListener { updateRecipeMethod() }
            ivImageEditIcon.setOnClickListener { photoDialog() }
            ivBackEditRecipe.setOnClickListener { findNavController().navigateUp() }
            clSelectedCategory.setOnClickListener { showBottomSheet(categoryList) }
            clSelectedCuisine.setOnClickListener { showBottomSheet(cuisineList) }
            clSelectSubCategory.setOnClickListener { showBottomSheet(subCategoryList) }
        }

      //  binding.nestedScrollView.setOnScrollChangeListener { _, _, _, _, _ -> hideKeyboard() }
    }

    private fun updateRecipeMethod() {
        when {
            binding.etNameOfRecipesEdit.text!!.trim().isEmpty() ->
                requireActivity().makeToast("Fill recipe name.")
            binding.etMinValue.text.toString().isEmpty() ->
                requireActivity().makeToast("Fill minimum serving value.")
            binding.etMaxValue.text.toString().isEmpty() ->
                requireActivity().makeToast("Fill maximum serving value.")
            binding.etMaxValue.text.toString().toInt() < binding.etMinValue.text.toString().toInt() ->
                requireActivity().makeToast("Minimum servings cannot be greater than maximum servings.")
            binding.etRecipeDescription.text.toString().isEmpty() ->
                requireActivity().makeToast("Fill recipe description.")
            ingredientsAdapter!!.getEditList().isEmpty() ->
                requireActivity().makeToast("Add ingredients")
            stepsAdapter!!.getEditList().isEmpty() ->
                requireActivity().makeToast("Add steps")
            else -> {
                Extension.showProgress(requireActivity())
                if (galleryPath != null) {
                    newUploadMethod(galleryPath!!)
                } else {
                    if (networkHelper.isNetworkConnected()) {
                        hitApiUpdateRecipe(recipeDetails!!.recipe_image!!)
                    } else {
                        requireActivity().makeToast("Internet not available.")
                    }
                }
            }
        }
    }

    private fun hideKeyboard() {
        val inputMethodManager = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val view = requireActivity().currentFocus ?: View(requireActivity())
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun setRecipeData() {
        if (recipeDetails != null) {
            binding.apply {
                Extension.trimEditText(etNameOfRecipesEdit)
                Extension.trimEditText(etRecipeDescription)
                Extension.trimEditText(etMaxValue)
                Extension.trimEditText(etMinValue)
                Extension.trimEditText(etNotes)
                Extension.trimEditText(etSuggestions)
                if (!recipeDetails!!.title.isNullOrEmpty()) etNameOfRecipesEdit.setText(recipeDetails!!.title)
                if (!recipeDetails!!.short_description.isNullOrEmpty()) etRecipeDescription.setText(recipeDetails!!.short_description)
                if (!recipeDetails!!.notes.isNullOrEmpty()) etNotes.setText(recipeDetails!!.notes)
                if (!recipeDetails!!.suggestions.isNullOrEmpty()) etSuggestions.setText(recipeDetails!!.suggestions)
                if (!recipeDetails!!.ingredients.isNullOrEmpty()) setIngredientsAdapter(recipeDetails!!.ingredients!!)
                if (!recipeDetails!!.steps.isNullOrEmpty()) setStepsAdapter(recipeDetails!!.steps!!)
                if (!recipeDetails!!.min_cooking_time.isNullOrEmpty()) tvMinTimeValue.text = recipeDetails!!.min_cooking_time
                if (!recipeDetails!!.max_cooking_time.isNullOrEmpty()) tvMaxTimeValue.text = recipeDetails!!.max_cooking_time
                if (!recipeDetails!!.min_serving_count.isNullOrEmpty()) etMinValue.setText(recipeDetails!!.min_serving_count)
                if (!recipeDetails!!.max_serving_count.isNullOrEmpty()) etMaxValue.setText(recipeDetails!!.max_serving_count)
                if (!recipeDetails!!.category.isNullOrEmpty()) {
                    tvCategoryEdit.text = recipeDetails!!.category
                    updateSubCategoryList(recipeDetails!!.category!!)
                    // Set sub_category if it exists, otherwise "None"
                    tvSubCategoryEdit.text = if (!recipeDetails!!.sub_category.isNullOrEmpty()) recipeDetails!!.sub_category else "None"
                }
                if (!recipeDetails!!.cuisine.isNullOrEmpty()) tvCuisineEdit.text = recipeDetails!!.cuisine
                if (!recipeDetails!!.recipe_image.isNullOrEmpty()) {
                    Glide.with(requireActivity())
                        .load(BuildConfig.BASE_MEDIA_URL + recipeDetails!!.recipe_image)
                        .placeholder(Extension.shimmerDrawable)
                        .into(ivRecipeImageEditAfter)
                }
                ivRecipeImageEditAfter.clipToOutline = true
            }
            getTimeValuesMethod()
        }
    }

    private fun setStepsAdapter(stepsList: ArrayList<String>) {
        val itemMoveCallback = ItemMoveCallback(null)
        val itemTouchHelper = ItemTouchHelper(itemMoveCallback)
        stepsAdapter = EditRecipeAdapter(requireContext(), stepsList, object : OnItemClickListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onItemClick(position: Int, type: String) {
                when (type) {
                    "editRecipeItem" -> showAddEditItem(editTypeSteps, position)
                    "removeRecipeItem" -> {
                        stepsAdapter!!.getEditList().removeAt(position)
                        stepsAdapter!!.notifyDataSetChanged()
                    }
                }
            }
        }, itemTouchHelper)
        binding.rvSteps.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = stepsAdapter
        }
        itemTouchHelper.attachToRecyclerView(binding.rvSteps)
        itemMoveCallback.adapter = stepsAdapter
    }

    private fun setIngredientsAdapter(ingredients: ArrayList<String>) {
        val itemMoveCallback = ItemMoveCallback(null)
        val itemTouchHelper = ItemTouchHelper(itemMoveCallback)
        ingredientsAdapter = EditRecipeAdapter(requireContext(), ingredients, object : OnItemClickListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onItemClick(position: Int, type: String) {
                when (type) {
                    "editRecipeItem" -> showAddEditItem(editTypeIngredients, position)
                    "removeRecipeItem" -> {
                        ingredientsAdapter!!.getEditList().removeAt(position)
                        ingredientsAdapter!!.notifyDataSetChanged()
                    }
                }
            }
        }, itemTouchHelper)
        binding.rvIngredients.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ingredientsAdapter
        }
        itemTouchHelper.attachToRecyclerView(binding.rvIngredients)
        itemMoveCallback.adapter = ingredientsAdapter
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    private fun showAddEditItem(editItemType: String, position: Int = -1) {
        val dialogLayoutBinding = AddCuisineDialogBinding.inflate(layoutInflater)
        val dialogBuilder = AlertDialog.Builder(requireContext()).setView(dialogLayoutBinding.root).setCancelable(true)
        val dialog = dialogBuilder.create()
        dialog.window?.apply {
            attributes?.y = 50
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            attributes = attributes
        }
        dialogLayoutBinding.ivCuisine.gone()
        dialogLayoutBinding.tvHeaderText.text = editItemType
        dialogLayoutBinding.etEnterNewCuisine.maxLines = 5
        dialogLayoutBinding.etEnterNewCuisine.minHeight = requireActivity().resources.getDimension(com.intuit.sdp.R.dimen._70sdp).toInt()
        when (editItemType) {
            editTypeIngredients -> {
                dialogLayoutBinding.tvEnterCuisineText.text = "Write the ingredient here..."
                dialogLayoutBinding.etEnterNewCuisine.hint = "Write the ingredient here..."
            }
            editTypeSteps -> {
                dialogLayoutBinding.tvEnterCuisineText.text = "Write the step here..."
                dialogLayoutBinding.etEnterNewCuisine.hint = "Write the step here..."
            }
        }
        dialogLayoutBinding.btnAddNewCuisine.text = if (position == -1) "Add" else "Update"
        if (position != -1) {
            when (editItemType) {
                editTypeIngredients -> dialogLayoutBinding.etEnterNewCuisine.setText(ingredientsAdapter!!.getEditList()[position])
                editTypeSteps -> dialogLayoutBinding.etEnterNewCuisine.setText(stepsAdapter!!.getEditList()[position])
            }
        }
        val param = dialogLayoutBinding.tvHeaderText.layoutParams as ViewGroup.MarginLayoutParams
        param.setMargins(0, 50, 0, 0)
        Extension.trimEditText(dialogLayoutBinding.etEnterNewCuisine)
        dialogLayoutBinding.btnAddNewCuisine.setOnClickListener {
            if (dialogLayoutBinding.etEnterNewCuisine.text.trim().isNotEmpty()) {
                if (position != -1) {
                    when (editItemType) {
                        editTypeIngredients -> {
                            ingredientsAdapter!!.getEditList()[position] = dialogLayoutBinding.etEnterNewCuisine.text.toString()
                            ingredientsAdapter!!.notifyDataSetChanged()
                            dialog.dismiss()
                        }
                        editTypeSteps -> {
                            stepsAdapter!!.getEditList()[position] = dialogLayoutBinding.etEnterNewCuisine.text.toString()
                            stepsAdapter!!.notifyDataSetChanged()
                            dialog.dismiss()
                        }
                    }
                } else {
                    when (editItemType) {
                        editTypeIngredients -> {
                            ingredientsAdapter!!.getEditList().add(dialogLayoutBinding.etEnterNewCuisine.text.toString())
                            ingredientsAdapter!!.notifyDataSetChanged()
                            dialog.dismiss()
                        }
                        editTypeSteps -> {
                            stepsAdapter!!.getEditList().add(dialogLayoutBinding.etEnterNewCuisine.text.toString())
                            stepsAdapter!!.notifyDataSetChanged()
                            dialog.dismiss()
                        }
                    }
                }
            } else {
                requireActivity().makeToast("Add text first.")
            }
        }
        dialogLayoutBinding.ivClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun showTimePicker(tvTime: TextView, title: String, pickerType: Int = 0) {
        TimePickerDialog.show(
            context = requireContext(),
            title = title,
            initialMinutes = if (pickerType == 0) initialMinMinutes else initialMaxMinutes,
            initialHours = if (pickerType == 0) initialMinHours else initialMaxHours,
            minTime = minTime,
            maxTime = maxTime,
            pickerType = pickerType
        ) { hours, minutes ->
            if (pickerType == 0) {
                minTime = hours * 60 + minutes
                initialMinHours = hours
                initialMinMinutes = minutes
            } else {
                maxTime = hours * 60 + minutes
                initialMaxHours = hours
                initialMaxMinutes = minutes
            }
            tvTime.text = when {
                hours == 0 && minutes == 0 -> "0 min"
                hours == 0 && minutes != 0 -> "$minutes min"
                hours != 0 && minutes == 0 -> "$hours hour"
                else -> "$hours hour $minutes min"
            }
        }
    }

    private fun photoDialog() {
        photoDialog?.dismiss()
        photoDialog = Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.apply {
                attributes.gravity = Gravity.BOTTOM
                setBackgroundDrawableResource(android.R.color.transparent)
                setLayout(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT)
            }
            setContentView(R.layout.photo_dialog)
        }
        photoDialog?.findViewById<LinearLayout>(R.id.llGallery)?.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                openGallery()
            } else {
                requestGalleryPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
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
        photoDialog?.findViewById<TextView>(R.id.tvCancel)?.setOnClickListener { photoDialog?.dismiss() }
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

    private val requestGalleryPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) openGallery() else requireActivity().makeToast("Permission denied")
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) openCamera() else requireActivity().makeToast("Permission denied")
    }

    private val getImageResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val selectedImageUri: Uri? = data?.data
            if (selectedImageUri != null) {
                try {
                    binding.apply {
                        Glide.with(requireActivity()).load(selectedImageUri).into(ivRecipeImageEditAfter)
                        ivRecipeImageEditAfter.clipToOutline = true
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
            currentPhotoUri = FileProvider.getUriForFile(requireActivity(), "com.rootsrecipes.fileprovider", file)
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri)
            }
            captureImageLauncher.launch(cameraIntent)
        }
    }

    private val captureImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            galleryPath = null
            currentPhotoUri?.let { uri ->
                galleryPath = copyUriToInternalStorage(uri)
                Glide.with(requireActivity()).load(uri).into(binding.ivRecipeImageEditAfter)
                binding.ivRecipeImageEditAfter.clipToOutline = true
            }
        }
    }

    private fun copyUriToInternalStorage(uri: Uri): File {
        val inputStream = requireActivity().contentResolver.openInputStream(uri)
        val file = File(requireContext().filesDir, "profile_image_${System.currentTimeMillis()}.jpg")
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

    @SuppressLint("FragmentLiveDataObserve")
    private fun getCuisinesApi() {
        Extension.showProgress(requireActivity())
        CoroutineScope(Dispatchers.Main).launch {
            vm.getCuisinesData()
            vm.cuisinesData.observe(this@EditRecipesFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        Extension.stopProgress()
                        cuisineList = its.data?.data?.cuisines!!.map { it } as ArrayList<String>
                        categoryList = its.data.data.categories.map { it.name } as ArrayList<String>
                        // Build category to sub-categories map
                        its.data.data.categories.forEach { category ->
                            categoryToSubCategoriesMap[category.name] = category.sub_categories ?: ArrayList()
                        }
                        subCategoryList = ArrayList() // Initialize empty, will be populated later
                        if (recipeDetails != null) {
                            setRecipeData()
                        }
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

    private fun showBottomSheet(items: ArrayList<String>) {
        bottomSheetDialog?.dismiss()
        bottomSheetDialog = BottomSheetDialog(requireActivity(), R.style.CustomBottomSheetDialogTheme)
        listBottomSheet = BottomsheetListBinding.inflate(LayoutInflater.from(requireActivity()))
        bottomSheetDialog?.setContentView(listBottomSheet!!.root)

        val recyclerView = listBottomSheet!!.rvList
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val adapter = BottomSheetListAdapter(items) { name, _ ->
            when (items) {
                categoryList -> {
                    binding.tvCategoryEdit.text = name
                    updateSubCategoryList(name)
                }
                cuisineList -> binding.tvCuisineEdit.text = name
                subCategoryList -> binding.tvSubCategoryEdit.text = name
            }
            bottomSheetDialog!!.dismiss()
        }
        listBottomSheet!!.ivCloseIcon.setOnClickListener { bottomSheetDialog!!.dismiss() }
        recyclerView.adapter = adapter
        bottomSheetDialog!!.show()
    }

    private fun updateSubCategoryList(category: String) {
        subCategoryList.clear()
        subCategoryList.add("None") // Add "None" as the first option
        subCategoryList.addAll(categoryToSubCategoriesMap[category] ?: ArrayList())
        binding.clSelectSubCategory.visible() // Show sub-category selector
    }

    @SuppressLint("FragmentLiveDataObserve")
    private fun hitApiUpdateRecipe(recipeImageKey: String) {
        val jsonObject = JsonObject()
        val gson = Gson()
        val ingredientsJsonArray = gson.toJsonTree(ingredientsAdapter!!.getEditList()).asJsonArray
        val stepsJsonArray = gson.toJsonTree(stepsAdapter!!.getEditList()).asJsonArray
        jsonObject.addProperty("recipe_id", recipeDetails!!._id)
        jsonObject.addProperty("title", binding.etNameOfRecipesEdit.text.toString())
        jsonObject.addProperty("cuisine", binding.tvCuisineEdit.text.toString())
        jsonObject.addProperty("category", binding.tvCategoryEdit.text.toString())
        // Set sub_category to empty string if "None" is selected, otherwise use the selected value
        jsonObject.addProperty("sub_category", if (binding.tvSubCategoryEdit.text.toString() == "None") "" else binding.tvSubCategoryEdit.text.toString())
        jsonObject.add("ingredients", ingredientsJsonArray)
        jsonObject.add("steps", stepsJsonArray)
        jsonObject.addProperty("min_cooking_time", binding.tvMinTimeValue.text.toString())
        jsonObject.addProperty("max_cooking_time", binding.tvMaxTimeValue.text.toString())
        jsonObject.addProperty("short_description", binding.etRecipeDescription.text.toString())
        jsonObject.addProperty("min_serving_count", binding.etMinValue.text.toString())
        jsonObject.addProperty("max_serving_count", binding.etMaxValue.text.toString())
        jsonObject.addProperty("transcribed_text", recipeDetails!!.transcribed_text)
        jsonObject.addProperty("recipe_image", recipeImageKey)
        jsonObject.addProperty("notes", binding.etNotes.text.toString())
        jsonObject.addProperty("suggestions", binding.etSuggestions.text.toString())

        CoroutineScope(Dispatchers.Main).launch {
            vm.updateRecipe(jsonObject)
            vm.updateRecipeData.observe(this@EditRecipesFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        Extension.stopProgress()
                        findNavController().navigateUp()
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

    private fun newUploadMethod(uploadGalleryPath: File) {
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
                                    hitApiUpdateRecipe(key)
                                } else if (state == TransferState.FAILED) {
                                    Extension.stopProgress()
                                    requireActivity().makeToast("Profile picture not uploaded.")
                                }
                            }
                            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {}
                            override fun onError(id: Int, ex: Exception?) {
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
}