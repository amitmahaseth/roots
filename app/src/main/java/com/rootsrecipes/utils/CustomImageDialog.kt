package com.rootsrecipes.utils

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import com.bumptech.glide.Glide
import com.rootsrecipes.BuildConfig
import com.rootsrecipes.R


class ImageDialog(context: Context, private val imageUrl: String) :
    Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.custom_image_dialog)

        val imageView = findViewById<AppCompatImageView>(R.id.imageView)
        val closeButton = findViewById<ImageView>(R.id.closeButton)


        Glide.with(context)
            .load(BuildConfig.BASE_MEDIA_URL + imageUrl)
            .placeholder(R.drawable.food_place_icon)
            .into(imageView)


        // Set click listener for the close button to dismiss the dialog
        closeButton.setOnClickListener {
            dismiss()
        }
    }
}
