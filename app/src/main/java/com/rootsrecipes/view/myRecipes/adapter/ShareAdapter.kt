package com.rootsrecipes.view.myRecipes.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rootsrecipes.BuildConfig
import com.rootsrecipes.R
import com.rootsrecipes.model.OnClickListener
import com.rootsrecipes.utils.gone
import com.rootsrecipes.utils.visible
import com.rootsrecipes.view.myRecipes.model.AllTypeConnection
import de.hdodenhof.circleimageview.CircleImageView

class ShareAdapter(
    private var mContext: Context,
    private var allTypeList: ArrayList<AllTypeConnection>,
    private var onClick: OnClickListener
) : RecyclerView.Adapter<ShareViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShareViewHolder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.item_person, parent, false)
        return ShareViewHolder(view)
    }

    override fun getItemCount(): Int {
        return allTypeList.size
    }

    fun getAllConnection(): ArrayList<AllTypeConnection> {
        return allTypeList
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ShareViewHolder, position: Int) {
        holder.textName.text = allTypeList[position].firstName + "" + allTypeList[position].lastName
        holder.textUsername.text = allTypeList[position].userName
        Glide.with(mContext).load(BuildConfig.BASE_MEDIA_URL + allTypeList[position].profileImage)
            .placeholder(mContext.getDrawable(R.drawable.profile_icon)).into(holder.imageProfile)

        holder.cbPerson.isChecked = allTypeList[position].isCheckedUser == true

        holder.cbPerson.visible()
        holder.cbPerson.setOnClickListener {
            onClick.onClickItem(position)
        }
        if (position == allTypeList.size - 1) {
            holder.dividerView.gone()
        } else {
            holder.dividerView.visible()
        }
    }
}

class ShareViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val imageProfile: CircleImageView = itemView.findViewById(R.id.imageProfile)
    val textName: AppCompatTextView = itemView.findViewById(R.id.textName)
    val textUsername: AppCompatTextView = itemView.findViewById(R.id.textUsername)
    val cbPerson: CheckBox = itemView.findViewById(R.id.cbPerson)
    val dividerView: View = itemView.findViewById(R.id.dividerView)
}
