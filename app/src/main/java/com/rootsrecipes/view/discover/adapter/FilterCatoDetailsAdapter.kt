package com.rootsrecipes.view.discover.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rootsrecipes.BuildConfig
import com.rootsrecipes.databinding.ItemCategoryDetailsBinding
import com.rootsrecipes.model.OnItemClickListener
import com.rootsrecipes.utils.gone
import com.rootsrecipes.utils.visible
import com.rootsrecipes.view.discover.model.FilterCategory

class FilterCatoDetailsAdapter(
    private var mContext: Context,
    var mList: ArrayList<FilterCategory>,
    private var filterType: Int = 0,
    private var onItemClickListener: OnItemClickListener
) :
    RecyclerView.Adapter<FilterCatoDetailsAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ItemCategoryDetailsBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FilterCatoDetailsAdapter.ViewHolder {
        val binding =
            ItemCategoryDetailsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.apply {
            tvCategoryNameDetails.text = mList[position].filterName
            cbItemCategoryDetails.isChecked = mList[position].isChecked
            cbItemCategoryDetails.setOnCheckedChangeListener { _, isChecked ->
                mList[position].isChecked = isChecked
                onItemClickListener.onItemClick(position, "filter_details")
            }
            if (filterType == 0) {
                ivProfileImage.gone()
            } else {
                ivProfileImage.visible()
                if (mList[position].profileImage.isNotEmpty()) {
                    Glide.with(mContext)
                        .load(BuildConfig.BASE_MEDIA_URL + mList[position].profileImage)
                        .into(ivProfileImage)
                }
            }

        }
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    fun updateData(newList: ArrayList<FilterCategory>) {
        mList.clear()
        mList.addAll(newList)
        notifyDataSetChanged() // Notify the adapter of data changes
    }

    fun setFilterType(type:Int){
        filterType = type
    }

}

