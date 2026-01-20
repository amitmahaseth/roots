package com.rootsrecipes.view.onBoarding

import android.view.View
import androidx.lifecycle.ViewModel
import androidx.navigation.findNavController
import com.rootsrecipes.R

class OnBoardingVM : ViewModel() {
    fun onClicks(view: View) {
        when (view.id) {
            R.id.btnSignUp -> {
                val currentDestination = view.findNavController().currentDestination
                if (currentDestination?.id == R.id.onBoardingFragment) {
                    view.findNavController()
                        .navigate(R.id.action_onBoardingFragment_to_signupFragment)

                }
            }

            R.id.btnLoginOnBoard -> {
                val currentDestination = view.findNavController().currentDestination
                if (currentDestination?.id == R.id.onBoardingFragment) {
                    view.findNavController()
                        .navigate(R.id.action_onBoardingFragment_to_signInFragment)

                }

            }
        }

    }
}