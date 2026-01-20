package com.rootsrecipes.view.myRecipes.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rootsrecipes.databinding.ItemStepsBinding
import com.rootsrecipes.utils.gone
import com.rootsrecipes.utils.inVisible
import com.rootsrecipes.utils.visible

class StepsAdapter(
    private var mList: ArrayList<String>,
    private var type: Int
) : RecyclerView.Adapter<StepsAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemStepsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepsAdapter.ViewHolder {
        val binding = ItemStepsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (type == 0) {
            holder.binding.ivCircleTick.visible()
            holder.binding.tvStepNumber.gone()
        } else if (type == 1) {
            holder.binding.ivCircleTick.gone()
            holder.binding.tvStepNumber.visible()
        }
        holder.binding.tvStepNumber.text = "${ position + 1 }"
        holder.binding.tvItemText.text = mList[position]
    }


    override fun getItemCount(): Int {
        return mList.size
    }
}

