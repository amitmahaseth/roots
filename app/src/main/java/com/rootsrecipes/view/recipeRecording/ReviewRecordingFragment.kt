package com.rootsrecipes.view.recipeRecording

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.appcompat.app.AlertDialog
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import androidx.navigation.fragment.findNavController
import com.rootsrecipes.R
import com.rootsrecipes.databinding.FragmentReviewRecordingBinding
import com.rootsrecipes.databinding.InfoReviewDialogBinding
import com.rootsrecipes.databinding.TutorialDialogLayoutBinding
import com.rootsrecipes.utils.BaseFragment
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.gone
import com.rootsrecipes.utils.makeToast
import com.rootsrecipes.utils.visible
import com.rootsrecipes.view.createAccount.model.RecipeData

class ReviewRecordingFragment : BaseFragment() {

    private lateinit var binding: FragmentReviewRecordingBinding
    private var recipeDetails: RecipeData? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentReviewRecordingBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showInfoDialog()
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
                val nextButtonParams = binding.btnNext.layoutParams as ViewGroup.MarginLayoutParams
                val modifyButtonParams =
                    binding.btnModify.layoutParams as ViewGroup.MarginLayoutParams
                val clBtnParams = binding.clBtnLayout.layoutParams as ViewGroup.MarginLayoutParams
                val headerParams =
                    binding.reviewRecordingHeaderLayout.layoutParams as ViewGroup.MarginLayoutParams

                // Calculate used height in pixels
                val usedHeight =
                    (binding.clBtnLayout.height + binding.clBtnLayout.paddingTop + binding.clBtnLayout.paddingBottom +
                            clBtnParams.bottomMargin + clBtnParams.topMargin) +
                            (binding.reviewRecordingHeaderLayout.height + binding.reviewRecordingHeaderLayout.paddingTop +
                                    binding.reviewRecordingHeaderLayout.paddingBottom +
                                    headerParams.topMargin + headerParams.bottomMargin)

                // Calculate available height
                val availableHeight = totalHeightPixels - usedHeight

                // Set the height of content layout
                val params = binding.recordRecipeContentLayout.layoutParams
                params.height = availableHeight
                binding.recordRecipeContentLayout.layoutParams = params

                Log.d("setHeightDebugs", "totalHeightPixels: $totalHeightPixels")
                Log.d("setHeightDebugs", "usedHeight: $usedHeight")
                Log.d("setHeightDebugs", "availableHeight: $availableHeight")
                Log.d(
                    "setHeightDebugs",
                    "final content height: ${binding.recordRecipeContentLayout.height}"
                )
            }
        })
    }

    private fun getData() {
        if (findNavController().currentBackStackEntry?.savedStateHandle != null) {
            findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Bundle>("requestKey")
                ?.observe(viewLifecycleOwner) { result ->
                    binding.tvRecordedRecipeContent.setText(result.getString("recorded_recipe"))
                    recipeDetails = result.getParcelable(Constants.recipeInformation)
                    binding.tvReviewRecording.text =
                        requireActivity().getString(R.string.edit_recording)
                }
        }
        if (arguments != null) {
            val bundle = arguments
            if (bundle!!.getInt(Constants.typeFrom) == 0) {
                if (bundle.getString("recorded_recipe") != null) {
                    binding.tvRecordedRecipeContent.setText(requireArguments().getString("recorded_recipe") as String)
                    binding.tvReviewRecording.text =
                        requireActivity().getString(R.string.review_recording)
                    binding.btnModify.visible()
                }
            } else if (bundle.getInt(Constants.typeFrom) == 1) {
                recipeDetails = bundle.getParcelable(Constants.recipeInformation)
                binding.tvRecordedRecipeContent.setText(recipeDetails!!.transcribed_text)
                binding.tvReviewRecording.text =
                    requireActivity().getString(R.string.edit_recording)

                binding.btnModify.gone()
            }
        }
    }

    private fun initUi() {
        getData()
        setOnClickMethod()
    }

    private fun setOnClickMethod() {
        binding.btnModify.setOnClickListener {
            if (recipeDetails == null) {
                val bundle = Bundle()
                bundle.putString("recorded_recipe", binding.tvRecordedRecipeContent.text.toString())
                findNavController().previousBackStackEntry?.savedStateHandle?.set(
                    "requestKey",
                    bundle
                )
                findNavController().navigateUp()
            } else {
                val bundle = Bundle()
                bundle.putParcelable(Constants.recipeInformation, recipeDetails)
                bundle.putString("recorded_recipe", binding.tvRecordedRecipeContent.text.toString())
                findNavController().navigate(
                    R.id.action_reviewRecordingFragment_to_recordRecipeFragment,
                    bundle
                )

            }
        }
        binding.ivBackReviewRecording.setOnClickListener { backPressedMethod() }

        binding.infoBtn.setOnClickListener { showTutorialDialog() }

        binding.btnNext.setOnClickListener {
            if (binding.tvRecordedRecipeContent.text.isEmpty()) {
                requireActivity().makeToast("No data to proceed!")
            } else {
                if (Constants.EDITRECIPE){
                     recipeDetails = Constants.recipeDetailsEdit
                }
                if (recipeDetails != null) {
                    val bundle = Bundle()
                    bundle.putParcelable(
                        Constants.recipeInformation, recipeDetails
                    )
                    bundle.putInt(Constants.typeFrom, 1)
                    bundle.putString(
                        "recorded_recipe",
                        binding.tvRecordedRecipeContent.text.toString()
                    )
                    findNavController().navigate(
                        R.id.action_reviewRecordingFragment_to_recipeDetailsFragment,
                        bundle
                    )
                } else {
                    val bundle = Bundle()
                    bundle.putString(
                        "recorded_recipe",
                        binding.tvRecordedRecipeContent.text.toString()
                    )
                    bundle.putInt(Constants.typeFrom, 0)
                    findNavController().navigate(
                        R.id.action_reviewRecordingFragment_to_recipeDetailsFragment,
                        bundle
                    )
                }
            }
        }
    }

    private fun backPressedMethod() {
        findNavController().popBackStack()
    }

    private fun showTutorialDialog() {
        val dialogLayoutBinding = TutorialDialogLayoutBinding.inflate(layoutInflater)
        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogLayoutBinding.root)
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

    private fun showInfoDialog(){
        val dialogLayoutBinding = InfoReviewDialogBinding.inflate(layoutInflater)
        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogLayoutBinding.root)
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
}