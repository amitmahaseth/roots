package com.rootsrecipes.view.discover.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rootsrecipes.BuildConfig
import com.rootsrecipes.R
import com.rootsrecipes.databinding.ItemBreakFastRecipesBinding
import com.rootsrecipes.model.OnItemClickListener
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.Extension
import com.rootsrecipes.view.createAccount.model.RecipeData

class BreakFastAdapter(
    private var mContext: Context,
    var mList: ArrayList<RecipeData>,
    private var onItemClickListener: OnItemClickListener
) : RecyclerView.Adapter<BreakFastAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ItemBreakFastRecipesBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BreakFastAdapter.ViewHolder {
        val binding =
            ItemBreakFastRecipesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.setOnClickListener {
            onItemClickListener.onItemClick(
                position,
                Constants.recipeInformation
            )
        }
        holder.binding.apply {
            tvBreakFastName.text = mList[position].title
            rateBreakFast.rating = mList[position].avg_rating!!
            tvRateBreakFast.text = "(${mList[position].avg_rating!!})"

            if (!mList[position].recipe_image.isNullOrEmpty()) {
                Glide.with(mContext).load(BuildConfig.BASE_MEDIA_URL + mList[position].recipe_image)
                    .into(ivItemBreakFast)
            }else{
                Glide.with(mContext)
                    .load(mContext.getDrawable(R.drawable.food_place_icon))
                    .into(ivItemBreakFast)
            }
            if (mList[position].user != null) {
                tvBreakFastUserNameItem.text = mList[position].user!!.first_name +" "+mList[position].user!!.last_name
                if (!mList[position].user!!.profile_image.isNullOrEmpty()) {
                    Glide.with(mContext)
                        .load(BuildConfig.BASE_MEDIA_URL + mList[position].user!!.profile_image)
                        .placeholder(Extension.shimmerDrawable)
                        .into(ivUserPfBreakFast)
                }
            }

            clSaveIcon.setOnClickListener {
                onItemClickListener.onItemClick(position, Constants.saveRecipe)
            }

            if (mList[position].isSaved!!) {
                ivSaveIcon.setImageDrawable(mContext.getDrawable(R.drawable.saved_recipe_icon_inside))
            } else {
                ivSaveIcon.setImageDrawable(mContext.getDrawable(R.drawable.unsaved_icon))
            }

            tvRateBreakFast.text = "(${mList[position].avg_rating})"
            rateBreakFast.rating = mList[position].avg_rating!!
        }
    }


    override fun getItemCount(): Int {
        return mList.size
    }

    fun getList(): ArrayList<RecipeData> {
        return mList
    }

}

