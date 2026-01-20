package com.rootsrecipes.view.myRecipes.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.rootsrecipes.databinding.BsListItemBinding

class BottomSheetListAdapter(
    private val items: ArrayList<String>,
    private val onItemClick: (String, Int) -> Unit
) : RecyclerView.Adapter<BottomSheetListAdapter.ViewHolder>() {

    class ViewHolder(val binding: BsListItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = BsListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.itemName.text = item

        holder.itemView.setOnClickListener {
            onItemClick(item, position)
        }
    }

    override fun getItemCount() = items.size
}