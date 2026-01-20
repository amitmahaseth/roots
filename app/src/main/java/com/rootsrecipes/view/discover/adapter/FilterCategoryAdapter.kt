package com.rootsrecipes.view.discover.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rootsrecipes.R
import com.rootsrecipes.databinding.ItemCategoryListBinding
import com.rootsrecipes.model.OnItemClickListener
import com.rootsrecipes.utils.inVisible
import com.rootsrecipes.utils.visible
import com.rootsrecipes.view.discover.model.FilterCategory

class FilterCategoryAdapter(
    private var mContext: Context,
    var mList: ArrayList<FilterCategory>,
    private var checkedPosition: Int = 0,
    private var onItemClickListener: OnItemClickListener
) :
    RecyclerView.Adapter<FilterCategoryAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ItemCategoryListBinding) :
        RecyclerView.ViewHolder(binding.root)

    init {
        // Mark the first item as checked by default
        if (mList.isNotEmpty()) {
            mList[checkedPosition].isChecked = true
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FilterCategoryAdapter.ViewHolder {
        val binding =
            ItemCategoryListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.apply {
            tvCategoryName.text = mList[position].filterName

            clItemCategoryList.setOnClickListener {
                mList.forEach { it.isChecked = false }
                mList[position].isChecked = true
                onItemClickListener.onItemClick(position, "filter_type")
                notifyDataSetChanged()
            }
            if (mList[position].totalFilter == 0) {
                tvCategoryNumberSelect.inVisible()
            } else {
                tvCategoryNumberSelect.visible()
            }
            tvCategoryNumberSelect.text = mList[position].totalFilter.toString()
            if (mList[position].isChecked) {
                clItemCategoryList.setBackgroundColor(mContext.getColor(R.color.green))
                tvCategoryName.setTextColor(mContext.getColor(R.color.white))
                tvCategoryNumberSelect.setTextColor(mContext.getColor(R.color.white))
            } else {
                clItemCategoryList.setBackgroundColor(mContext.getColor(R.color.category_color))
                tvCategoryName.setTextColor(mContext.getColor(R.color.black))
                tvCategoryNumberSelect.setTextColor(mContext.getColor(R.color.black))
            }
        }

    }


    override fun getItemCount(): Int {
        return mList.size
    }


}
