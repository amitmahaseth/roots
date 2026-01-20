package com.rootsrecipes.view.onBoarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.rootsrecipes.databinding.FragmentOnBoardingBinding
import com.rootsrecipes.utils.BaseFragment


class OnBoardingFragment : BaseFragment() {

    private lateinit var onBoardingVM: OnBoardingVM
    private lateinit var binding: FragmentOnBoardingBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentOnBoardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onBoardingVM = OnBoardingVM()
        binding.vm = onBoardingVM

        initUi()
    }

    private fun initUi() {

    }

}