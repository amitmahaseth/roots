package com.rootsrecipes.view.setting

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.rootsrecipes.R
import com.rootsrecipes.databinding.FragmentSubscriptionBinding
import com.rootsrecipes.utils.gone
import com.rootsrecipes.utils.visible

class SubscriptionFragment : Fragment() {
    private lateinit var binding: FragmentSubscriptionBinding
    private var planType = 0
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSubscriptionBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUi()
    }

    private fun initUi() {
        setPlanTypeView()
        setOnClickMethod()
    }

    private fun setOnClickMethod() {
        binding.apply {
            clFreePlan.setOnClickListener {
                planType = 0
                setPlanTypeView()
            }
            clPaidPlan.setOnClickListener {
                planType = 1
                setPlanTypeView()
            }
            ivCloseSubscription.setOnClickListener { findNavController().navigateUp() }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setPlanTypeView() {
        binding.apply {
            if (planType == 0) {
                clFreePlan.backgroundTintList =
                    ColorStateList.valueOf(requireActivity().getColor(R.color.light_green))
                clPaidPlan.backgroundTintList = null
                tvAddRecipe.text = "Add 5 recipes month"
                tvViewRecipes.text = "View 50 recipes a month"
                tvPlanCharges.text = "$0.00/mo"
                tvContinueText.text = "Continue – $0.00"
                tvIncludeWith.text = "Included with free plan"
                tvNoAd.gone()
            } else if (planType == 1) {
                clPaidPlan.backgroundTintList =
                    ColorStateList.valueOf(requireActivity().getColor(R.color.light_green))
                clFreePlan.backgroundTintList = null
                tvAddRecipe.text = "Unlimited recipes"
                tvViewRecipes.text = "Unlimited View recipes"
                tvPlanCharges.text = "$19.99/mo"
                tvContinueText.text = "Continue – $19.99"
                tvIncludeWith.text = "Included with Premium plan"
                tvNoAd.visible()
            }
        }
    }
}