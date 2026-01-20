package com.rootsrecipes.view.setting.adapter

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
import com.rootsrecipes.view.myRecipes.model.ConnectionUserData

class FollowerAdapter(
    private var mContext: Context,
    private var loginId: String,
    private var followList: ArrayList<ConnectionUserData>,
    private var listType: Int,
    private var userType: Int,
    private val onItemClick: OnItemClickListener
) :
    RecyclerView.Adapter<FollowerAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ItemFollowFollowingBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemFollowFollowingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    fun updateListType(type: Int) {
        listType = type
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: FollowerAdapter.ViewHolder, position: Int) {
        holder.binding.apply {
            if (followList[position].isFollow && followList[position].toFollow) {
                tvFollowMessage.text = "Message"
                tvFollowMessage.backgroundTintList =
                    ContextCompat.getColorStateList(mContext, R.color.gray_text)
                tvFollowMessage.setTextColor(
                    ContextCompat.getColorStateList(
                        mContext, R.color.green
                    )
                )
            } else if (followList[position].isFollow) {
                tvFollowMessage.text = "Following"
                tvFollowMessage.backgroundTintList =
                    ContextCompat.getColorStateList(mContext, R.color.gray_text)
                tvFollowMessage.setTextColor(
                    ContextCompat.getColorStateList(
                        mContext, R.color.green
                    )
                )
            } else if (followList[position].toFollow) {
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
                if (followList[position].isFollow && followList[position].toFollow) {
                    //go to chat
                    onItemClick.onItemClick(position, "chat")
                } else if (followList[position].isFollow && !followList[position].toFollow) {
                    //Following
                    //un follow
                    onItemClick.onItemClick(position, "unfollow")
                } else if (!followList[position].isFollow && followList[position].toFollow) {
                    //follow back
                    onItemClick.onItemClick(position, "followBack")
                } else if (!followList[position].isFollow && !followList[position].toFollow) {
                    //follow
                    onItemClick.onItemClick(position, "follow")
                } else {
                    //follow
                    onItemClick.onItemClick(position, "follow")
                }
            }
            if (userType != 0) {
                ivDeleteFollower.gone()
            }
            if (followList[position].user_id == loginId) {
                tvFollowMessage.gone()
            }
            ivDeleteFollower.setOnClickListener {
                if (listType == 0) {
                    //remove followers
                    onItemClick.onItemClick(position, "remove")
                } else if (listType == 1) {
                    //un follow
                    onItemClick.onItemClick(position, "unfollow")
                }
            }
            tvUserNameFollower.text =
                followList[position].firstName + " " + followList[position].lastName
            tvUserIdFollower.text = followList[position].userName
            if (!followList[position].profileImage.isNullOrEmpty()) {
                Glide.with(mContext)
                    .load(BuildConfig.BASE_MEDIA_URL + followList[position].profileImage)
                    .placeholder(Extension.shimmerDrawable).into(ivFollowUser)
            } else {
                Glide.with(mContext)
                    .load(mContext.getDrawable(R.drawable.profile_icon))
                    .into(ivFollowUser)
            }
        }


    }


    override fun getItemCount(): Int {
        return followList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newList: ArrayList<ConnectionUserData>) {
        this.followList = newList
    }

    fun getFollowList(): ArrayList<ConnectionUserData> {
        return followList
    }
}


