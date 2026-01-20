package com.rootsrecipes.view.messages.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rootsrecipes.BuildConfig
import com.rootsrecipes.databinding.ItemMessageBinding
import com.rootsrecipes.model.OnItemClickListener
import com.rootsrecipes.utils.gone
import com.rootsrecipes.utils.visible
import com.rootsrecipes.view.messages.model.ChatData
import java.text.SimpleDateFormat
import java.util.*

class MessagesAdapter(
    var mContext: Context, var mList: ArrayList<ChatData>,
    private val onItemClickListener: OnItemClickListener
) : ListAdapter<ChatData, MessagesAdapter.ViewHolder>(ChatDiffCallback()) {

    inner class ViewHolder(val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chat = getItem(position)
        holder.binding.apply {
            // Opponent info
            tvFullName.text = chat.opponentUser.name
            if(!chat.opponentUser.image.isNullOrEmpty()) {
                Glide.with(root.context)
                    .load(BuildConfig.BASE_MEDIA_URL + chat.opponentUser.image)
                    .circleCrop()
                    .into(ivProfile)
            }

            // Last message
            tvLastMessage.text = chat.message.message ?: ""

            // Typing status
            if (chat.isTyping) {
                tvLastMessage.text = "Typing..."
                tvLastMessage.setTypeface(null, android.graphics.Typeface.ITALIC)
            } else {
                tvLastMessage.setTypeface(null, android.graphics.Typeface.NORMAL)
            }

            // Timestamp
            chat.message.messageTime?.let {
                if (it is Long && it > 0) {
                    val messageDate = Date(it * 1000)
                    val now = Calendar.getInstance()
                    val msgCal = Calendar.getInstance().apply { time =  messageDate }

                    val isToday = now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                            now.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)

                    val format = if (isToday) {
                        SimpleDateFormat("HH:mm", Locale.getDefault())
                    } else {
                        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                    }

                    tvTime.text = format.format(messageDate)
                } else {
                    tvTime.text = ""
                }
            }

            // Unread count
            if (chat.unreadMessageCount > 0) {
                messagePendingCount.visibility = View.VISIBLE
                messagePendingCount.text = chat.unreadMessageCount.toString()
            } else {
                messagePendingCount.visibility = View.GONE
            }

            // Blocked status
            if (chat.blockedStatus) {
                root.alpha = 0.6f
                root.isEnabled = false
            } else {
                root.alpha = 1.0f
                root.isEnabled = true
            }

            // Divider visibility
            if (position == itemCount - 1) {
                dividerView.gone()
            } else {
                dividerView.visible()
            }

            root.setOnClickListener {
                onItemClickListener.onItemClick(position, "chat")
            }
        }
    }

    public override fun getItem(position: Int): ChatData {
        return currentList[position]
    }
}

class ChatDiffCallback : DiffUtil.ItemCallback<ChatData>() {
    override fun areItemsTheSame(oldItem: ChatData, newItem: ChatData): Boolean {
        return oldItem.chatID == newItem.chatID
    }

    override fun areContentsTheSame(oldItem: ChatData, newItem: ChatData): Boolean {
        return oldItem == newItem
    }
}