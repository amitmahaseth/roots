package com.rootsrecipes.view.setting.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rootsrecipes.BuildConfig
import com.rootsrecipes.R
import com.rootsrecipes.databinding.ItemRecipesSavedBinding
import com.rootsrecipes.model.OnItemClickListener
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.Extension
import com.rootsrecipes.utils.gone
import com.rootsrecipes.utils.visible
import com.rootsrecipes.view.createAccount.model.RecipeData

class SavedRecipesAdapter(
    private var mContext: Context,
    private var mList: ArrayList<RecipeData>,
    private var savedTypeForm: Int,
    private var onItemClickListener: OnItemClickListener
) : RecyclerView.Adapter<SavedRecipesAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ItemRecipesSavedBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): SavedRecipesAdapter.ViewHolder {
        val binding =
            ItemRecipesSavedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.apply {
            tvRecipeNameItem.text = mList[position].title
            rateRecipes.rating = mList[position].avg_rating!!
            tvRateText.text = "(${mList[position].avg_rating!!})"
            if (!mList[position].recipe_image.isNullOrEmpty()) {
                Glide.with(mContext).load(BuildConfig.BASE_MEDIA_URL + mList[position].recipe_image)
                    .placeholder(R.drawable.food_place_icon).into(ivItemSavedRecipes)
            } else {
                Glide.with(mContext).load(mContext.getDrawable(R.drawable.food_place_icon))
                    .into(ivItemSavedRecipes)

            }

            if (mList[position].user != null) {
                tvRecipeUserNameItem.text = mList[position].user!!.first_name + " " +mList[position].user!!.last_name
                if (!mList[position].user!!.profile_image.isNullOrEmpty()) {
                    Glide.with(mContext)
                        .load(BuildConfig.BASE_MEDIA_URL + mList[position].user!!.profile_image)
                        .into(ivUserPfReItem)
                } else {
                    Glide.with(mContext).load(BuildConfig.BASE_MEDIA_URL + "")
                        .placeholder(R.drawable.food_place_icon).into(ivUserPfReItem)
                }
            }

            if (mList[position].isSaved!!) {
                ivSaveUnSave.setImageDrawable(mContext.getDrawable(R.drawable.saved_recipe_icon_inside))
            } else {
                ivSaveUnSave.setImageDrawable(mContext.getDrawable(R.drawable.unsaved_icon))
            }
            ivSaveUnSave.setOnClickListener {
                onItemClickListener.onItemClick(
                    position, Constants.saveRecipe
                )
            }

            if (savedTypeForm == 0) {
                ivSaveUnSave.visible()
                llPublicPrivate.gone()
            } else
                if (savedTypeForm == 1) {
                    llPublicPrivate.visible()
                    ivSaveUnSave.gone()
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
                }
        }
        holder.itemView.setOnClickListener {
            onItemClickListener.onItemClick(
                position, Constants.recipeInformation
            )
        }
    }


    override fun getItemCount(): Int {
        return mList.size
    }

    fun getAddList(): ArrayList<RecipeData> {
        return mList
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setList(newList: ArrayList<RecipeData>) {
        mList = newList
        notifyDataSetChanged()
    }

    fun addItems(newItems: List<RecipeData>) {
        val startPosition = mList.size
        mList.addAll(newItems)
        // Notify for just the new items to prevent full screen refresh
        notifyItemRangeInserted(startPosition, newItems.size)
    }
}
