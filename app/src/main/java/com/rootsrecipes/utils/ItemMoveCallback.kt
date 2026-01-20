package com.rootsrecipes.utils

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.rootsrecipes.view.myRecipes.adapter.EditRecipeAdapter

class ItemMoveCallback(var adapter: EditRecipeAdapter?) : ItemTouchHelper.Callback() {

    override fun isLongPressDragEnabled(): Boolean = false // Drag starts manually via ImageView

    override fun isItemViewSwipeEnabled(): Boolean = false // No swipe to delete

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        return makeMovementFlags(dragFlags, 0)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        val fromPosition = viewHolder.bindingAdapterPosition
        val toPosition = target.bindingAdapterPosition
        adapter?.moveItem(fromPosition, toPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Not needed
    }
    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        // This is called when the drag & drop operation ends
        adapter?.notifyDataSetChanged()
    }
}
