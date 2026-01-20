package com.rootsrecipes.view.createAccount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.rootsrecipes.R
import com.rootsrecipes.databinding.FragmentWalkThroughBinding
import com.rootsrecipes.utils.BaseFragment
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.SharedPref
import com.rootsrecipes.utils.inVisible
import com.rootsrecipes.utils.visible
import com.rootsrecipes.view.createAccount.adapter.MyViewPagerAdapter
import com.rootsrecipes.view.createAccount.model.RecipesModel
import com.zhpan.indicator.enums.IndicatorSlideMode
import com.zhpan.indicator.enums.IndicatorStyle
import org.koin.android.ext.android.inject

class WalkThroughFragment : BaseFragment() {

    private var mPosition: Int = 0
    private lateinit var recipesAdapter: MyViewPagerAdapter
    private lateinit var binding: FragmentWalkThroughBinding
    private lateinit var recipesList: ArrayList<RecipesModel>
    private val pref: SharedPref by inject()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentWalkThroughBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recipesList = ArrayList()
        pref.saveBoolean(Constants.isLogin, true)
        initUi()

    }

    private fun initUi() {
        recipesList.add(
            RecipesModel(
                R.drawable.record_voice_icon,
                "Record Recipes with Your Voice",
                "No typing, no hassle—just speak your recipes, and we’ll handle the rest. Perfect for sharing your cherished dishes with ease."
            )
        )
        recipesList.add(
            RecipesModel(
                R.drawable.legacy_icon,
                "Share Your Culinary Legacy",
                "Publish your recipes to the world or keep them private. Roots & Recipes helps you pass down traditions to loved ones or inspire others."
            )
        )
        recipesList.add(
            RecipesModel(
                R.drawable.everyone_icon,
                "A Simple App for Everyone",
                "Designed for ease of use, especially for those less familiar with technology. Start capturing your recipes in minutes—no experience needed."
            )
        )

        setViewPagerAdapter()
        binding.ivNext.setOnClickListener {
            if (mPosition == recipesAdapter.itemCount - 1) {
                if (isAdded) {
                    findNavController().navigate(R.id.action_walkThroughFragment_to_discoverFragment)
                }
            } else {
                mPosition += 1
                binding.vpRecipes.setCurrentItem(mPosition, false)
            }
        }

        binding.tvSkip.setOnClickListener {
            if (isAdded) {
                findNavController().navigate(R.id.action_walkThroughFragment_to_discoverFragment)
            }
        }
        binding.ivBackWalk.setOnClickListener {
            if (mPosition != 0) {
                mPosition -= 1
                binding.vpRecipes.setCurrentItem(mPosition, false)
            }
        }

        setupViewPagerListener()
    }

    private fun setViewPagerAdapter() {
        recipesAdapter = MyViewPagerAdapter(requireActivity(), recipesList)
        binding.vpRecipes.apply { adapter = recipesAdapter }
        binding.indicatorOnboarding.apply {
            setSliderColor(
                ContextCompat.getColor(requireContext(), R.color.hint_gray),
                ContextCompat.getColor(requireContext(), R.color.green)
            )
            setSliderWidth(resources.getDimension(com.intuit.sdp.R.dimen._9sdp))
            setSliderHeight(resources.getDimension(com.intuit.sdp.R.dimen._5sdp))
            setSlideMode(IndicatorSlideMode.WORM)
            setIndicatorStyle(IndicatorStyle.ROUND_RECT)
            setupWithViewPager(binding.vpRecipes)
        }
    }

    private fun setupViewPagerListener() {
        binding.vpRecipes.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int, positionOffset: Float, positionOffsetPixels: Int
            ) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                binding.indicatorOnboarding.onPageScrolled(
                    position, positionOffset, positionOffsetPixels
                )
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                mPosition = position
                if (position > 0) {
                    binding.ivBackWalk.visible()
                } else {
                    binding.ivBackWalk.inVisible()
                }
                binding.indicatorOnboarding.onPageSelected(position)
            }
        })
    }

}