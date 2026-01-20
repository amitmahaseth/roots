package com.rootsrecipes.view.messages.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.rootsrecipes.R
import com.rootsrecipes.databinding.LeftChatItemBinding
import com.rootsrecipes.databinding.RightChatItemBinding
import com.rootsrecipes.model.OnItemClickListener
import com.rootsrecipes.utils.Extension
import com.rootsrecipes.utils.gone
import com.rootsrecipes.utils.isSameDay
import com.rootsrecipes.utils.visible
import com.rootsrecipes.view.messages.model.GetAllMessages


class ChatAdapter(
    private var mList: ArrayList<GetAllMessages>,
    private var onItemClickListener: OnItemClickListener,
    private var userId: String,
    private var mContext: Context
) : RecyclerView.Adapter<ChatAdapter.MyViewHolder>() {

    // Sealed class to handle different layouts
    sealed class MyViewHolder(binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {
        class RightViewHolder(val binding: RightChatItemBinding) : MyViewHolder(binding)
        class LeftViewHolder(val binding: LeftChatItemBinding) : MyViewHolder(binding)
    }

    override fun getItemViewType(position: Int): Int {
        // 0 -> Other user, 1 -> You
        return if (userId == mList[position].senderID) {
            1
        } else {
            0
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return if (viewType == 1) {
            val binding =
                RightChatItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            MyViewHolder.RightViewHolder(binding)
        } else {
            val binding =
                LeftChatItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            MyViewHolder.LeftViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val chatModel = mList[position]
        when (holder) {
            is MyViewHolder.RightViewHolder -> {
                holder.binding.textMessageTextView.text = chatModel.message
                holder.binding.tvChatTime.text =
                    Extension.getCurrentTimeOrDate(chatModel.messageTime!!, "time")
                holder.binding.root.setOnClickListener {
                    onItemClickListener.onItemClick(position)
                }
                if (position == 0) {
                    holder.binding.chatDateTextView.visible()
                } else {
                    holder.binding.chatDateTextView.gone()
                    val previousItem = mList[position - 1]
                    if (previousItem.messageTime!!.isSameDay(mList[position].messageTime!!)) {
                        holder.binding.chatDateTextView.gone()
                    } else {
                        holder.binding.chatDateTextView.visible()
                    }
                }
                val currentTimeMillis = System.currentTimeMillis()
                if (mList[position].messageTime!!.isSameDay(currentTimeMillis)) {
                    holder.binding.chatDateTextView.text = "Today"
                } else if (mList[position].messageTime!!.isSameDay(currentTimeMillis - 86400000)) { // 86400000 milliseconds = 1 day
                    holder.binding.chatDateTextView.text = "Yesterday"
                } else {
                    holder.binding.chatDateTextView.text
                    Extension.getCurrentTimeOrDate(mList[position].messageTime!!, "date")
                }

                if (mList[position].readStatus!!) {
                    holder.binding.checkReadBtn.imageTintList =
                        ColorStateList.valueOf(mContext.resources.getColor(R.color.green))
                } else {
                    holder.binding.checkReadBtn.imageTintList =
                        ColorStateList.valueOf(mContext.resources.getColor(R.color.gray_text))
                }
            }

            is MyViewHolder.LeftViewHolder -> {
                holder.binding.textMessageTextView.text = chatModel.message
                holder.binding.tvChatTime.text =
                    Extension.getCurrentTimeOrDate(chatModel.messageTime!!, "time")
                holder.binding.root.setOnClickListener {
                    onItemClickListener.onItemClick(position)
                }
                if (position == 0) {
                    holder.binding.chatDateTextView.visible()
                } else {
                    holder.binding.chatDateTextView.gone()
                    val previousItem = mList[position - 1]
                    if (previousItem.messageTime!!.isSameDay(mList[position].messageTime!!)) {
                        holder.binding.chatDateTextView.gone()
                    } else {
                        holder.binding.chatDateTextView.visible()
                    }
                }
                val currentTimeMillis = System.currentTimeMillis()
                if (mList[position].messageTime!!.isSameDay(currentTimeMillis)) {
                    holder.binding.chatDateTextView.text = "Today"
                } else if (mList[position].messageTime!!.isSameDay(currentTimeMillis - 86400000)) { // 86400000 milliseconds = 1 day
                    holder.binding.chatDateTextView.text = "Yesterday"
                } else {
                    holder.binding.chatDateTextView.text
                    Extension.getCurrentTimeOrDate(mList[position].messageTime!!, "date")
                }
            }
        }
    }

    fun getList(): ArrayList<GetAllMessages> = mList
    override fun getItemCount(): Int = mList.size
}
