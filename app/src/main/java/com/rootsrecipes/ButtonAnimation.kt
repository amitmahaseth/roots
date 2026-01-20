package com.rootsrecipes

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.animation.AnimationUtils
import com.rootsrecipes.databinding.ActivityButtonAnimationBinding

class ButtonAnimation : AppCompatActivity() {
    private var  isVisible = true
    private lateinit var binding: ActivityButtonAnimationBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        initUi()
    }

    private fun initUi() {

        binding.btnToggle.setOnClickListener {
            if (isVisible){
                binding.tvVisibleText.animate()
                    .alpha(0f)
                    .setDuration(500)
                    .withEndAction {
                        binding.tvVisibleText.visibility = View.GONE

                    }

            }else{
                binding.tvVisibleText.visibility = View.VISIBLE
                binding.tvVisibleText.alpha = 0f
                binding.tvVisibleText.animate()
                    .alpha(1f)
                    .setDuration(500)
            }
            isVisible = !isVisible
        }

    }
}