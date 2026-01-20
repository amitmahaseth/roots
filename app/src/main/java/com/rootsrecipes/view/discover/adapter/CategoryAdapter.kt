package com.rootsrecipes.view.discover.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rootsrecipes.databinding.ItemCategoryBinding
import com.rootsrecipes.model.OnClickListener
import com.rootsrecipes.view.discover.model.CategoryItemData

class CategoryAdapter(
    private var mContext: Context,
    private var categoryList: ArrayList<CategoryItemData>,
    private var onItemClick: OnClickListener
) :
    RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryAdapter.ViewHolder {
        val binding =
            ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return categoryList.size
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onBindViewHolder(holder: CategoryAdapter.ViewHolder, position: Int) {
        holder.itemView.setOnClickListener {
            onItemClick.onClickItem(position)
        }
        holder.binding.apply {
            ivAllRecipes.setImageDrawable(mContext.getDrawable(categoryList[position].categoryIcon))
            tvAllRecipes.text = categoryList[position].categoryName
        }
    }
}

