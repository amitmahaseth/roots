package com.rootsrecipes.view.myRecipes.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.rootsrecipes.databinding.EditItemBinding
import com.rootsrecipes.model.OnItemClickListener
import java.util.Collections

class EditRecipeAdapter(
    private val mContext: Context,
    private var mList: ArrayList<String>,
    val onItemClickListener: OnItemClickListener,
    private val itemTouchHelper: ItemTouchHelper
) :
    RecyclerView.Adapter<EditRecipeAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: EditItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = EditItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = mList.size
    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.apply {
            etItemText.text = mList[position]
            ivEditIcon.setOnClickListener {
                onItemClickListener.onItemClick(
                    position,
                    "editRecipeItem"
                )
            }
            ivRemoveIcon.setOnClickListener {
                onItemClickListener.onItemClick(
                    position,
                    "removeRecipeItem"
                )
            }
            ivShiftIcon.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    itemTouchHelper.startDrag(holder)
                }
                false
            }
        }
    }

    fun getEditList(): ArrayList<String> {
        return mList
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        Collections.swap(mList, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }
}