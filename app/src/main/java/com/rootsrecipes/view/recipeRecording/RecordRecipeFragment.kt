package com.rootsrecipes.view.recipeRecording

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.rootsrecipes.MainActivity
import com.rootsrecipes.R
import com.rootsrecipes.databinding.ConfirmRecordingDialogBinding
import com.rootsrecipes.databinding.FragmentRecordRecipeBinding
import com.rootsrecipes.databinding.TutorialDialogLayoutBinding
import com.rootsrecipes.utils.BaseFragment
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.NetworkHelper
import com.rootsrecipes.utils.SharedPref
import com.rootsrecipes.utils.inVisible
import com.rootsrecipes.utils.makeToast
import com.rootsrecipes.utils.visible
import com.rootsrecipes.view.createAccount.model.RecipeData
import org.koin.android.ext.android.inject
import java.util.Locale


class RecordRecipeFragment : BaseFragment() {
    private lateinit var animatorSet: AnimatorSet
    private lateinit var binding: FragmentRecordRecipeBinding
    private var speechText = ""
    private var isListening = false
    private var speechRecognizer: SpeechRecognizer? = null
    private val pref: SharedPref by inject()
    private val networkHelper: NetworkHelper by inject()

    private var recipeDetails: RecipeData? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentRecordRecipeBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUi()
    }

    private fun initUi() {
        getDataFromPreviousScreen()
        setOnClickListenerMethod()
        setData()
        checkAudioPermission()

    }


    private fun setData() {
        if (speechText.isNotEmpty()) {
            setRecipeContentVisibility(1)
            setReRecordAndNextVisibility(1)
            binding.tvRecipeContent.text = speechText
        }
    }

    private fun setOnClickListenerMethod() {

        binding.infoBtn.setOnClickListener { showTutorialDialog() }

        binding.btnRecordRecipe.setOnClickListener {
            if (networkHelper.isNetworkConnected()){
                startSpeechToText()
            }else{
                requireActivity().makeToast("No Internet connection!")
            }
        }

        binding.btnReRecord.setOnClickListener {
            reRecordDialog()
        }

        binding.crossRecordRecipeBtn.setOnClickListener {
            backPressedMethod()
        }
        binding.btnNext.setOnClickListener {
            val confirmDialog = pref.getBoolean(Constants.CONFIRM_DIALOG)
            if (confirmDialog) {
                val bundle = Bundle()
                bundle.putString("recorded_recipe", speechText)
                findNavController().navigate(
                    R.id.action_recordRecipeFragment_to_reviewRecordingFragment, bundle
                )
            } else {
                confirmRecipeDialog()
            }
        }

        binding.btnOpenSettings.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(
                    requireActivity(), Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission is already granted
                    requireActivity().makeToast("Permission Already Granted")
                    checkAudioPermission()

                }

                shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                    // Explain why you need the permission
                    requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    requireActivity().makeToast("Audio permission is needed for recording audio.")
                }

                else -> {
                    // Directly request the permission
                    requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }

        }


    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Permission granted
                //requireActivity().makeToast("Permission Granted")
                checkAudioPermission()
            } else {
                // Permission denied
                requireActivity().makeToast("Permission Denied")
            }
        }

    private fun showTutorialDialog() {
        val dialogLayoutBinding = TutorialDialogLayoutBinding.inflate(layoutInflater)
        val dialogBuilder = AlertDialog.Builder(requireActivity()).setView(dialogLayoutBinding.root)
            .setCancelable(true)


        val dialog = dialogBuilder.create()
        val params = dialog.window?.attributes
        params?.y = 50
        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes = params
        dialogLayoutBinding.btnOk.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(
                requireActivity(), Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            recordRecipeLayoutVisibility(0)


        } else {
            recordRecipeLayoutVisibility(1)
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MainActivity.REQUEST_AUDIO_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requireActivity().makeToast("Permission Granted")
                recordRecipeLayoutVisibility(1)
            } else {
                requireActivity().makeToast("Permission Denied")
            }
        }
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    private fun startSpeechToText() {
        if (isListening) {
            stopListening() // Stop if already listening
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            requireActivity().makeToast("Speech recognition not supported")
            return
        }
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireActivity())

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true) // Enable partial results

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                if (isAdded) {
                    binding.btnRecordRecipe.setImageDrawable(requireActivity().getDrawable(R.drawable.pause_green))
                    setReRecordAndNextVisibility(0)
                    setRecipeContentVisibility(1)
                    setWavesEffect(1)
                }
            }

            override fun onBeginningOfSpeech() {
                isListening = true
                if (isAdded) {
                    binding.btnRecordRecipe.setImageDrawable(requireActivity().getDrawable(R.drawable.pause_green))
                    setReRecordAndNextVisibility(0)
                    setRecipeContentVisibility(1)
                    setWavesEffect(1)
                }
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
                if (isAdded) {
                    binding.btnRecordRecipe.setImageDrawable(requireActivity().getDrawable(R.drawable.mic_green)) // Idle icon
                    setReRecordAndNextVisibility(1)
                    setRecipeContentVisibility(1)
                    setWavesEffect(0)
                }/* if (isAdded && isListening) {
                     Handler(Looper.getMainLooper()).postDelayed({
                         if (isListening) {
                             speechRecognizer?.startListening(intent)
                         }
                     }, 100)
                 }*/
            }

            override fun onError(error: Int) {/* if (isAdded) {
                     when (error) {
                         SpeechRecognizer.ERROR_NO_MATCH,
                         SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                             if (isListening) {
                                 Handler(Looper.getMainLooper()).postDelayed({
                                     if (isListening) {
                                         speechRecognizer?.startListening(intent)
                                     }
                                 }, 100)
                             }
                         }
                         else -> {
 //                            Toast.makeText(requireContext(), "Error: $error", Toast.LENGTH_SHORT).show()
                             if (!isListening) {
                                 binding.btnRecordRecipe.setImageDrawable(requireContext().getDrawable(R.drawable.mic_green))
                                 setReRecordAndNextVisibility(2)
                                 setRecipeContentVisibility(0)
                                 setWavesEffect(0)
                             }
                         }
                     }
                 }*/
                isListening = false
                if(speechText.isEmpty()){
                    if (isAdded) {
                        binding.btnRecordRecipe.setImageDrawable(requireActivity().getDrawable(R.drawable.mic_green)) // Revert icon on error
//                    Toast.makeText(requireActivity(), "Error: $error", Toast.LENGTH_SHORT).show()
                        setReRecordAndNextVisibility(2)
                        setRecipeContentVisibility(0)
                        setWavesEffect(0)
                    }
                }else{
                    if(isAdded) {
                        binding.btnRecordRecipe.setImageDrawable(requireActivity().getDrawable(R.drawable.mic_green)) // Idle icon
                        setReRecordAndNextVisibility(1)
                        setRecipeContentVisibility(1)
                        setWavesEffect(0)
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                if (isAdded) {
                    binding.btnRecordRecipe.setImageDrawable(requireActivity().getDrawable(R.drawable.mic_green)) // Idle icon
                    setWavesEffect(0)
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        speechText += matches[0] + " "
                        binding.tvRecipeContent.text = speechText
                    }
                }
            }

            @SuppressLint("SetTextI18n")
            override fun onPartialResults(partialResults: Bundle?) {
                val partialMatches =
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!partialMatches.isNullOrEmpty()) {
                    binding.tvRecipeContent.text =
                        speechText + partialMatches[0] // Show real-time updates
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    private fun stopListening() {
        isListening = false
        speechRecognizer?.stopListening()
        binding.btnRecordRecipe.setImageDrawable(requireActivity().getDrawable(R.drawable.mic_green)) // Revert icon
        setWavesEffect(0)
    }

    private fun setReRecordAndNextVisibility(value: Int) {
        binding.apply {
            when (value) {
                0 -> {
                    //start
                    btnReRecord.inVisible()
                    btnNext.inVisible()
                }

                1 -> {
                    //done
                    btnReRecord.visible()
                    btnNext.visible()
                }

                2 -> {
                    //error
                    btnReRecord.inVisible()
                    btnNext.inVisible()
                }
            }
        }
    }

    private fun setRecipeContentVisibility(value: Int) {
        binding.apply {
            if (value == 0) {
                //not recording
                recordInstructionLayout.visible()
                recordRecipeContentLayout.inVisible()
            } else if (value == 1) {
                //recording
                recordInstructionLayout.inVisible()
                recordRecipeContentLayout.visible()
            }
        }
    }

    private fun setWavesEffect(value: Int) {
        if (value == 0) {
            binding.ivBackgroundRecordingIcon.inVisible()
            if (::animatorSet.isInitialized) {
                animatorSet.cancel()
            }
        } else {
            binding.ivBackgroundRecordingIcon.apply {
                visible()
                startRippleEffect()
            }
        }
    }

    private fun startRippleEffect() {
        val scaleAnimation = ValueAnimator.ofFloat(1f, 1.2f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { animator ->
                val scale = animator.animatedValue as Float
                binding.ivBackgroundRecordingIcon.scaleX = scale
                binding.ivBackgroundRecordingIcon.scaleY = scale
            }
        }

        val alphaAnimation = ValueAnimator.ofFloat(1f, 0.7f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { animator ->
                binding.ivBackgroundRecordingIcon.alpha = animator.animatedValue as Float
            }
        }

        scaleAnimation.start()
        alphaAnimation.start()
    }

    private fun confirmRecipeDialog() {
        val dialogLayoutBinding = ConfirmRecordingDialogBinding.inflate(layoutInflater)
        val dialogBuilder = AlertDialog.Builder(requireActivity()).setView(dialogLayoutBinding.root)
            .setCancelable(true)


        val dialog = dialogBuilder.create()
        val params = dialog.window?.attributes
        params?.y = 50
        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes = params

        dialogLayoutBinding.cbConfirmRecipe.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                pref.saveBoolean(Constants.CONFIRM_DIALOG, true)
            }
        }
        dialogLayoutBinding.btnYes.setOnClickListener {
            val bundle = Bundle()


            if (recipeDetails != null) {
                bundle.putParcelable(
                    Constants.recipeInformation, recipeDetails
                )
                bundle.putInt(Constants.typeFrom, 1)
                bundle.putString("recorded_recipe", speechText)
                findNavController().previousBackStackEntry?.savedStateHandle?.set(
                    "requestKey", bundle
                )
                findNavController().navigateUp()
            } else {
                bundle.putInt(Constants.typeFrom, 0)
                bundle.putString("recorded_recipe", speechText)
                findNavController().navigate(
                    R.id.action_recordRecipeFragment_to_reviewRecordingFragment, bundle
                )
            }
            dialog.dismiss()
        }
        dialogLayoutBinding.btnNo.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    private fun reRecordDialog() {
        val dialogLayoutBinding = ConfirmRecordingDialogBinding.inflate(layoutInflater)
        val dialogBuilder = AlertDialog.Builder(requireActivity()).setView(dialogLayoutBinding.root)
            .setCancelable(true)


        val dialog = dialogBuilder.create()
        val params = dialog.window?.attributes
        params?.y = 50
        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes = params

        dialogLayoutBinding.ivMic.setImageDrawable(requireActivity().getDrawable(R.drawable.redo_red))
        dialogLayoutBinding.tvHeaderText.text = "Are you sure you want to start over?"
        dialogLayoutBinding.tvConfirmQuestionText.text =
            "This will erase the current recording and let you begin again."
        dialogLayoutBinding.btnYes.setTextColor(requireActivity().getColor(R.color.delete_red))
        dialogLayoutBinding.cbConfirmRecipe.inVisible()
        dialogLayoutBinding.cbText.inVisible()

        dialogLayoutBinding.btnYes.setOnClickListener {
            speechText = ""
            binding.tvRecipeContent.text = ""
            setReRecordAndNextVisibility(0)
            setRecipeContentVisibility(0)
            dialog.dismiss()
        }
        dialogLayoutBinding.btnNo.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun getDataFromPreviousScreen() {
        if (findNavController().currentBackStackEntry?.savedStateHandle != null) {
            findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Bundle>("requestKey")
                ?.observe(viewLifecycleOwner) { result ->
                    speechText = result.getString("recorded_recipe") as String
                    binding.tvRecipeContent.text = speechText
                }
        }

        val args = arguments
        if (args != null) {
            speechText = args.getString("recorded_recipe") as String
            recipeDetails = args.getParcelable(Constants.recipeInformation)
        }

    }

    private fun backPressedMethod() {
        findNavController().popBackStack()
    }

    private fun recordRecipeLayoutVisibility(value: Int) {
        if (value == 0) {
            binding.clRecordRecipe.inVisible()
            binding.clAllowMicrophoneAccess.visible()
        } else if (value == 1) {
            binding.clRecordRecipe.visible()
            binding.clAllowMicrophoneAccess.inVisible()
        }
    }
}