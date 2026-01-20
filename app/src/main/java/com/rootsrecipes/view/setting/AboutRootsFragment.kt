package com.rootsrecipes.view.setting

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.rootsrecipes.databinding.FragmentAboutRootsBinding
import com.rootsrecipes.utils.BaseFragment


class AboutRootsFragment : BaseFragment() {

    private lateinit var binding: FragmentAboutRootsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentAboutRootsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
    }

    @SuppressLint("SetTextI18n")
    private fun initUI() {
        binding.apply {
            ivBackAboutRoot.setOnClickListener {
                findNavController().popBackStack()
            }
          /*  tvAboutText.text = "\"Some recipes are more than just ingredients -they're history, family, and love, passed down from one generation to the next.\" \n\n" +
                    "At Roots and Recipes, we believe that food connects us to our past, our culture, and the people we love. Too often, family recipes disappear over time, lost in memories or handwritten notes.\n\n" +
                    "That\'s why we created Roots and Recipes-a simple, effortless way to preserve ancestral recipes by letting you speak your recipe aloud, while our app does the rest. It transcribes, organizes, and even suggests enhancements, so your family\'s culinary heritage stays alive for generations.\n\n" +
                    "With an easy-to-use format, you can store, share, and pass down cherished dishes with loved ones, ensuring that no special meal is ever forgotten. Whether it's your grandmother's holiday pie, or a traditional dish that connects you to your roots, our app is here to keep those flavors and memories alive."
       */
            val text = """
            "Some recipes are more than just ingredients—they’re history, family, and love, passed down from one generation to the next."

            At Roots and Recipes, we believe that food connects us to our past, our culture, and the people we love. Too often, family recipes disappear over time, lost in memories or handwritten notes.

            That’s why we created Roots and Recipes—a simple, effortless way to preserve ancestral recipes by letting you speak your recipe aloud, while our app does the rest. It transcribes, organizes, and even suggests enhancements, so your family’s culinary heritage stays alive for generations.

            With an easy-to-use format, you can store, share, and pass down cherished dishes with loved ones, ensuring that no special meal is ever forgotten. Whether it’s your grandmother’s holiday pie, or a traditional dish that connects you to your roots, our app is here to keep those flavors and memories alive.
        """.trimIndent()

            val spannableString = SpannableString(text)

            // Apply bold style
            spannableString.setSpan(StyleSpan(Typeface.BOLD), text.indexOf("preserve ancestral recipes"), text.indexOf("preserve ancestral recipes") + 27, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannableString.setSpan(StyleSpan(Typeface.BOLD), text.indexOf("speak your recipe aloud"), text.indexOf("speak your recipe aloud") + 24, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannableString.setSpan(StyleSpan(Typeface.BOLD), text.indexOf("store, share, and pass down"), text.indexOf("store, share, and pass down") + 27, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            // Apply italic style
            spannableString.setSpan(StyleSpan(Typeface.ITALIC), text.indexOf("Some recipes are more"), text.indexOf("Some recipes are more") + 127, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) // The quote
            spannableString.setSpan(StyleSpan(Typeface.ITALIC), text.indexOf("Roots and Recipes"), text.indexOf("Roots and Recipes") + 18, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannableString.setSpan(StyleSpan(Typeface.ITALIC), text.lastIndexOf("Roots and Recipes"), text.lastIndexOf("Roots and Recipes") + 18, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            tvAboutText.text = spannableString



        }
    }
}