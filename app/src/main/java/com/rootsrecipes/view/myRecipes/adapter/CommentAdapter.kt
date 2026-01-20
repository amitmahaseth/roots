package com.rootsrecipes.view.myRecipes.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rootsrecipes.BuildConfig
import com.rootsrecipes.R
import com.rootsrecipes.databinding.ItemCommentBinding
import com.rootsrecipes.utils.Extension
import com.rootsrecipes.utils.gone
import com.rootsrecipes.utils.visible
import com.rootsrecipes.view.myRecipes.model.CommentData

class CommentAdapter(
    private var mContext: Context,
    private var commentList: ArrayList<CommentData>
) : RecyclerView.Adapter<CommentAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentAdapter.ViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.apply {
            tvFullName.text = commentList[position].user!!.first_name + " " + commentList[position].user!!.last_name
            tvComment.text = commentList[position].comment_text
            if (!commentList[position].user!!.profile_image.isNullOrEmpty()) {
                Glide.with(mContext)
                    .load(BuildConfig.BASE_MEDIA_URL + commentList[position].user!!.profile_image)
                    .into(ivProfile)
            } else {
                Glide.with(mContext).load(mContext.getDrawable(R.drawable.profile_icon))
                    .into(ivProfile)
            }
            tvTime.text = commentList[position].createdAt?.let { Extension.formatRelativeTime(it) }
            if (position == commentList.size - 1) {
                dividerView.gone()
            } else {
                dividerView.visible()
            }
        }
    }

    fun updateCommentList(newList: ArrayList<CommentData>) {
        commentList.clear()
        commentList.addAll(newList)
    }

    fun addComments(newComments: ArrayList<CommentData>) {
        val startPos = commentList.size
        commentList.addAll(newComments)
        notifyItemRangeInserted(startPos, newComments.size)
    }

    fun getCommentList(): ArrayList<CommentData> = commentList

    override fun getItemCount(): Int {
        return commentList.size
    }
}
