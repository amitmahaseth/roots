package com.rootsrecipes.view.notification.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rootsrecipes.BuildConfig
import com.rootsrecipes.R
import com.rootsrecipes.databinding.ItemNotificationBinding
import com.rootsrecipes.model.Notification
import com.rootsrecipes.model.OnItemClickListener
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.gone
import com.rootsrecipes.utils.visible

class NotificationAdapter(
    private var mContext: Context,
    private var mList: ArrayList<Notification>,
    private var onItemClickListener: OnItemClickListener
) :
    RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {
    class ViewHolder(val databinding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(databinding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val dataBinding =
            ItemNotificationBinding.inflate(LayoutInflater.from(mContext), parent, false)
        return ViewHolder(dataBinding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.databinding.apply {
            ivNotificationProfile.setOnClickListener {
                onItemClickListener.onItemClick(position, Constants.targetUserId)
            }
            ivNotificationImage.setOnClickListener {
                onItemClickListener.onItemClick(position, Constants.GET_RECIPE)
            }

            tvMessageAndFollow.gone()

            if (mList[position].notification_type == "comment" || mList[position].notification_type == "rate") {
                if (mList[position].notification_type == "comment") {
                    tvNotificationText.text =
                        "${mList[position].user.user_name} Commented : ${mList[position].comment}"
                } else if (mList[position].notification_type == "rate") {
                    tvNotificationText.text =
                        "${mList[position].user.user_name} Rate ${mList[position].rate?.toInt()} to your recipe."
                }

                if (!mList[position].recipeData!!.recipe_image.isNullOrEmpty()) {
                    ivNotificationImage.visible()
                    ivNotificationImage.background = mContext.getDrawable(R.drawable.corner_cl)
                    ivNotificationImage.scaleType = ImageView.ScaleType.CENTER_CROP
                    ivNotificationImage.clipToOutline = true
                    Glide.with(mContext)
                        .load(BuildConfig.BASE_MEDIA_URL + mList[position].recipeData!!.recipe_image)
                        .into(ivNotificationImage)
                } else {
                    ivNotificationImage.gone()
                }
            } else if (mList[position].notification_type == "follow") {
                ivNotificationImage.gone()
                tvNotificationText.text = "${mList[position].user.user_name} started following you."
            } else {
                ivNotificationImage.gone()
                tvNotificationText.text = mList[position].notification_text
            }

            if (!mList[position].user.profile_image.isNullOrEmpty()) {
                Glide.with(mContext)
                    .load(BuildConfig.BASE_MEDIA_URL + mList[position].user.profile_image)
                    .into(ivNotificationProfile)
            } else {
                Glide.with(mContext).load(R.drawable.profile_icon).into(ivNotificationProfile)

            }

            val currentDate = mList[position].updatedAt.substring(0, 10) // "2025-05-01"

            if (position == 0) {
                holder.databinding.tvDayText.visible()
                holder.databinding.tvDayText.text = getFormattedDate(currentDate)
            } else {
                val previousDate = mList[position - 1].updatedAt.substring(0, 10)
                if (currentDate == previousDate) {
                    holder.databinding.tvDayText.gone()
                } else {
                    holder.databinding.tvDayText.visible()
                    holder.databinding.tvDayText.text = getFormattedDate(currentDate)
                }
            }
        }

        /* if () {
             //followed
             holder.databinding.tvMessageAndFollow.apply {
                 backgroundTintList = ColorStateList.valueOf(
                     mContext.getColor(
                         R.color.gray_notification
                     )
                 )
                 setTextColor(
                     mContext.getColor(
                         R.color.green
                     )
                 )
                 text = mContext.getString(R.string.message)
             }
         } else {
             //not followed
             holder.databinding.tvMessageAndFollow.apply {
                 backgroundTintList = ColorStateList.valueOf(
                     mContext.getColor(
                         R.color.green
                     )
                 )
                 setTextColor(
                     mContext.getColor(
                         R.color.gray_notification
                     )
                 )
                 text = mContext.getString(R.string.follow)
             }
         }*/

    }

    private fun getFormattedDate(dateStr: String): String {
        return try {
            val inputFormat =
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val date = inputFormat.parse(dateStr)

            val cal = java.util.Calendar.getInstance()
            val today = cal.time

            cal.add(java.util.Calendar.DATE, -1)
            val yesterday = cal.time

            val dateOnly = inputFormat.format(date!!)
            val todayOnly = inputFormat.format(today)
            val yesterdayOnly = inputFormat.format(yesterday)

            when (dateOnly) {
                todayOnly -> "Today"
                yesterdayOnly -> "Yesterday"
                else -> {
                    val outputFormat =
                        java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                    outputFormat.format(date)
                }
            }
        } catch (e: Exception) {
            dateStr
        }
    }


    fun getList() = mList
    override fun getItemCount() = mList.size
}