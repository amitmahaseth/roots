package com.rootsrecipes.view.messages.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rootsrecipes.BuildConfig
import com.rootsrecipes.R
import com.rootsrecipes.databinding.ItemFollowFollowingBinding
import com.rootsrecipes.model.OnItemClickListener
import com.rootsrecipes.utils.Extension
import com.rootsrecipes.utils.gone
import com.rootsrecipes.view.messages.model.Data

class SearchAdapter(
    private var mContext: Context,
    private var loginId: String,
    private var followList: ArrayList<Data>,
    private val onItemClick: OnItemClickListener
) : RecyclerView.Adapter<SearchAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ItemFollowFollowingBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemFollowFollowingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: SearchAdapter.ViewHolder, position: Int) {
        holder.binding.apply {
            if (followList[position].is_following && followList[position].is_followed_by) {
                tvFollowMessage.text = "Message"
                tvFollowMessage.backgroundTintList =
                    ContextCompat.getColorStateList(mContext, R.color.gray_text)
                tvFollowMessage.setTextColor(
                    ContextCompat.getColorStateList(
                        mContext, R.color.green
                    )
                )
            } else if (followList[position].is_following) {
                tvFollowMessage.text = "Following"
                tvFollowMessage.backgroundTintList =
                    ContextCompat.getColorStateList(mContext, R.color.gray_text)
                tvFollowMessage.setTextColor(
                    ContextCompat.getColorStateList(
                        mContext, R.color.green
                    )
                )
            } else if (followList[position].is_followed_by) {
                tvFollowMessage.text = "Follow Back"
                tvFollowMessage.backgroundTintList =
                    ContextCompat.getColorStateList(mContext, R.color.green)
                tvFollowMessage.setTextColor(
                    ContextCompat.getColorStateList(
                        mContext, R.color.white
                    )
                )
            } else {
                tvFollowMessage.text = "Follow"
                tvFollowMessage.backgroundTintList =
                    ContextCompat.getColorStateList(mContext, R.color.green)
                tvFollowMessage.setTextColor(
                    ContextCompat.getColorStateList(
                        mContext, R.color.white
                    )
                )
            }
            tvFollowMessage.setOnClickListener {
                if (followList[position].is_following && followList[position].is_followed_by) {
                    //go to chat
                    onItemClick.onItemClick(position, "chat")
                } else if (followList[position].is_following && !followList[position].is_followed_by) {
                    //Following
                    //un follow
                    onItemClick.onItemClick(position, "unfollow")
                } else if (!followList[position].is_following && followList[position].is_followed_by) {
                    //follow back
                    onItemClick.onItemClick(position, "followBack")
                } else if (!followList[position].is_following && !followList[position].is_followed_by) {
                    //follow
                    onItemClick.onItemClick(position, "follow")
                } else {
                    //follow
                    onItemClick.onItemClick(position, "follow")
                }
            }

            ivDeleteFollower.gone()
            if (followList[position]._id == loginId) {
                tvFollowMessage.gone()
            }
            if (followList[position].last_name == null) {
                tvUserNameFollower.text = followList[position].first_name
            } else {
                tvUserNameFollower.text =
                    followList[position].first_name + " " + followList[position].last_name
            }
            tvUserIdFollower.text = followList[position].user_name
            if (!followList[position].profile_image.isNullOrEmpty()) {
                Glide.with(mContext)
                    .load(BuildConfig.BASE_MEDIA_URL + followList[position].profile_image)
                    .placeholder(Extension.shimmerDrawable).into(ivFollowUser)
            } else {
                Glide.with(mContext).load(mContext.getDrawable(R.drawable.profile_icon))
                    .into(ivFollowUser)
            }
        }


    }

    fun getFollowList(): ArrayList<Data> {
        return followList
    }

    override fun getItemCount(): Int {
        return followList.size
    }

}
