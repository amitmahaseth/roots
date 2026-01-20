package com.rootsrecipes.view.discover.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rootsrecipes.BuildConfig
import com.rootsrecipes.R
import com.rootsrecipes.model.OnItemClickListener
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.view.createAccount.model.RecipeData
import de.hdodenhof.circleimageview.CircleImageView
import me.zhanghai.android.materialratingbar.MaterialRatingBar

class DiscoverRecipesAdapter(
    private var mContext: Context,
    private var discoverList: ArrayList<RecipeData>,
    private var onItemClickListener: OnItemClickListener
) : RecyclerView.Adapter<DiscoverRecipeHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscoverRecipeHolder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.item_recipes_saved, parent, false)
        return DiscoverRecipeHolder(view)
    }
    fun getDiscoverList(): ArrayList<RecipeData> {
        return discoverList
    }

    override fun getItemCount(): Int {
        return discoverList.size
    }

    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
    override fun onBindViewHolder(holder: DiscoverRecipeHolder, position: Int) {
        holder.itemView.setOnClickListener {
            onItemClickListener.onItemClick(
                position, Constants.recipeInformation
            )
        }

        holder.ivSaveUnSave.setOnClickListener {
            onItemClickListener.onItemClick(
                position, Constants.saveRecipe
            )
        }
        val item = discoverList[position]
        holder.tvRecipeNameItem.text = item.title
        Glide.with(mContext).load(BuildConfig.BASE_MEDIA_URL + item.recipe_image)
            .placeholder(mContext.getDrawable(R.drawable.food_place_icon))
            .into(holder.ivItemSavedRecipes)
        holder.tvRateText.text = "(${item.avg_rating})"
        holder.rateRecipes.rating = item.avg_rating!!
        if (item.user != null) {
            holder.tvRecipeUserNameItem.text = item.user!!.first_name + " "+ item.user!!.last_name
            if (!item.user!!.profile_image.isNullOrEmpty()) {
                Glide.with(mContext).load(BuildConfig.BASE_MEDIA_URL + item.user!!.profile_image)
                    .placeholder(mContext.getDrawable(R.drawable.profile_icon))
                    .into(holder.ivUserPfReItem)
            }/* else {
                Glide.with(mContext).load(mContext.getDrawable(R.drawable.profile_icon))
                    .into(holder.ivUserPfReItem)
            }*/
        }
        if (item.isSaved!!) {
            holder.ivSaveUnSave.setImageDrawable(mContext.getDrawable(R.drawable.saved_recipe_icon_inside))
        } else {
            holder.ivSaveUnSave.setImageDrawable(mContext.getDrawable(R.drawable.unsaved_icon))
        }
    }
}

class DiscoverRecipeHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val tvRecipeNameItem: AppCompatTextView = itemView.findViewById(R.id.tvRecipeNameItem)
    val ivItemSavedRecipes: AppCompatImageView = itemView.findViewById(R.id.ivItemSavedRecipes)
    val ivSaveUnSave: AppCompatImageView = itemView.findViewById(R.id.ivSaveUnSave)
    val tvRecipeUserNameItem: AppCompatTextView = itemView.findViewById(R.id.tvRecipeUserNameItem)
    val tvRateText: AppCompatTextView = itemView.findViewById(R.id.tvRateText)
    val rateRecipes: MaterialRatingBar = itemView.findViewById(R.id.rateRecipes)
    val ivUserPfReItem: CircleImageView = itemView.findViewById(R.id.ivUserPfReItem)
}