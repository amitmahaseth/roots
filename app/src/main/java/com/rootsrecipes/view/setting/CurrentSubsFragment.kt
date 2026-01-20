package com.rootsrecipes.view.setting

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.rootsrecipes.R
import com.rootsrecipes.databinding.FragmentCurrentSubsBinding
import com.rootsrecipes.utils.gone
import com.rootsrecipes.utils.visible

class CurrentSubsFragment : Fragment() {
    private lateinit var binding: FragmentCurrentSubsBinding
    private var planType = 0
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCurrentSubsBinding.inflate(inflater)
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
            btnSubsManagePlan.setOnClickListener {
                if (isAdded) {
                    findNavController().navigate(R.id.action_currentSubsFragment_to_subscriptionFragment)
                }
            }
            ivBackBtnCurrentSubs.setOnClickListener { findNavController().navigateUp() }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setPlanTypeView() {
        binding.apply {
            if (planType == 0) {
                tvAddRecipe.text = "Add 5 recipes month"
                tvViewRecipes.text = "View 50 recipes a month"
                tvPlanCharges.text = "$0.00/mo"
                tvIncludeWith.text = "Included with free plan"
                tvNoAd.gone()
            } else if (planType == 1) {
                tvAddRecipe.text = "Unlimited recipes"
                tvViewRecipes.text = "Unlimited View recipes"
                tvPlanCharges.text = "$19.99/mo"
                tvIncludeWith.text = "Included with Premium plan"
                tvNoAd.visible()
            }
        }
    }

}