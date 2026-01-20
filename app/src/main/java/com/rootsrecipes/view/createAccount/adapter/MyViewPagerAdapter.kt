package com.rootsrecipes.view.createAccount.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.rootsrecipes.R
import com.rootsrecipes.view.createAccount.model.RecipesModel

class MyViewPagerAdapter(
    private var mContext: Context,
    private var recipesList: ArrayList<RecipesModel>
) : RecyclerView.Adapter<ViewPagerHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewPagerHolder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.item_record_recipes, parent, false)
        return ViewPagerHolder(view)
    }

    override fun getItemCount(): Int {
        return recipesList.size
    }

    override fun onBindViewHolder(holder: ViewPagerHolder, position: Int) {
        val recipe = recipesList[position]

        // Bind the data to views
        holder.ivRecordRecipes.setImageResource(recipe.image)
        holder.tvHeaderVp.text = recipe.headerRecipes
        holder.tvDetailsVp.text = recipe.detailsRecipes
    }

}

class ViewPagerHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val ivRecordRecipes: AppCompatImageView = itemView.findViewById(R.id.ivRecordRecipes)
    val tvHeaderVp: AppCompatTextView = itemView.findViewById(R.id.tvHeaderVp)
    val tvDetailsVp: AppCompatTextView = itemView.findViewById(R.id.tvDetailsVp)

}