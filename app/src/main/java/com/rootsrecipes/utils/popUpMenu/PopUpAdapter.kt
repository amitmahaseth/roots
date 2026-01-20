package com.rootsrecipes.utils.popUpMenu

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rootsrecipes.R
import com.rootsrecipes.databinding.PopupItemBinding

data class PopUpData(
    var text: String,
    var colorId: Int = R.color.black
)

class PopUpAdapter(
    var mList: ArrayList<PopUpData>,
    var mContext: Context,
    var getID: CommonStringListener
) :
    RecyclerView.Adapter<PopUpAdapter.ViewHolder>() {
    class ViewHolder(val databinding: PopupItemBinding) : RecyclerView.ViewHolder(databinding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val dataBinding =
            PopupItemBinding.inflate(LayoutInflater.from(mContext), parent, false)
        return ViewHolder(dataBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.databinding.tvItemText.text = mList[position].text
        holder.databinding.tvItemText.setTextColor(mContext.getColor(mList[position].colorId))
        holder.itemView.setOnClickListener {
            getID.onEventClick(position)
        }
    }

    override fun getItemCount(): Int {
        return mList.size
    }
}

interface CommonStringListener {
    fun onEventClick(position: Int)
}