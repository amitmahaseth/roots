package com.rootsrecipes.view.myRecipes.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rootsrecipes.BuildConfig
import com.rootsrecipes.R
import com.rootsrecipes.databinding.ItemMyRecipesBinding
import com.rootsrecipes.model.OnItemClickListener
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.Extension
import com.rootsrecipes.utils.gone
import com.rootsrecipes.utils.visible
import com.rootsrecipes.view.createAccount.model.RecipeData


class MyRecipeAdapter(
    private var mContext: Context,
    var mList: ArrayList<RecipeData>,
    private var onItemClickListener: OnItemClickListener,
    private var userTypeFrom: Int = 0
) : RecyclerView.Adapter<MyRecipeAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyRecipeAdapter.ViewHolder {
        val binding =
            ItemMyRecipesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    fun getAddList(): ArrayList<RecipeData> {
        return mList
    }

    fun addItems(newItems: ArrayList<RecipeData>) {
        mList.addAll(newItems)
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.setOnClickListener {
            onItemClickListener.onItemClick(
                position, Constants.recipeInformation
            )
        }

        holder.binding.apply {
            tvRecipeNameItem.text = mList[position].title
            rateRecipes.rating = mList[position].avg_rating!!
            tvRateText.text = "(${mList[position].avg_rating!!})"
            if (!mList[position].recipe_image.isNullOrEmpty()) {
                Glide.with(mContext).load(BuildConfig.BASE_MEDIA_URL + mList[position].recipe_image)
                    .placeholder(Extension.shimmerDrawable)
                    .into(ivRecipeImage)
            }

            if (mList[position].my_recipe_status == "Public") {
                tvStatus.text = mList[position].my_recipe_status
                tvStatus.setTextColor(ContextCompat.getColor(mContext, R.color.green))
                llPublicPrivate.backgroundTintList =
                    ContextCompat.getColorStateList(mContext, R.color.cream_color)
            } else {
                tvStatus.text = mList[position].my_recipe_status
                tvStatus.setTextColor(ContextCompat.getColor(mContext, R.color.blue))
                llPublicPrivate.backgroundTintList =
                    ContextCompat.getColorStateList(mContext, R.color.private_blue)
            }
            if (userTypeFrom == 0) {
                llPublicPrivate.visible()
                llUserOfThatRecipeItem.gone()
            } else {
                llPublicPrivate.gone()
                llUserOfThatRecipeItem.visible()
                if (mList[position].user != null) {
                    mList[position].user?.let {
                        tvUserNameItem.text = it.user_name + " " + it.last_name
                        if (it.profile_image.isNullOrEmpty()) {
                            Glide.with(mContext)
                                .load(BuildConfig.BASE_MEDIA_URL + it.profile_image)
                                .placeholder(Extension.shimmerDrawable)
                                .into(ivProfileImageItem)
                        }
                    }
                }
            }
        }

    }


    inner class ViewHolder(val binding: ItemMyRecipesBinding) :
        RecyclerView.ViewHolder(binding.root)
}
