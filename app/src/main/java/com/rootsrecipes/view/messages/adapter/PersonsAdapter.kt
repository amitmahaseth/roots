package com.rootsrecipes.view.messages.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rootsrecipes.BuildConfig
import com.rootsrecipes.R
import com.rootsrecipes.databinding.ItemPersonBinding
import com.rootsrecipes.databinding.ItemSectionBinding
import com.rootsrecipes.utils.gone
import com.rootsrecipes.utils.visible
import com.rootsrecipes.view.myRecipes.model.AllTypeConnection


data class Contact(
    val id: String, val name: String, val profilePicUrl: String, val username: String
)

class PersonsAdapter(
    private var mContext: Context,
    private val contacts: ArrayList<AllTypeConnection>,
    private val typeFrom: Int
) : RecyclerView.Adapter<PersonsAdapter.BaseViewHolder>() {
    abstract class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private val sectionedContacts = contacts.groupBy { it.firstName[0].uppercase() }.toSortedMap()

    inner class ContactViewHolder(private val binding: ItemPersonBinding) :
        BaseViewHolder(binding.root) {

        @SuppressLint("ResourceType", "UseCompatLoadingForDrawables", "SetTextI18n")
        fun bind(contact: AllTypeConnection, isFirst: Boolean, isLast: Boolean) {
            binding.apply {
                textName.text = contact.firstName + "" + contact.lastName
                textUsername.text = contact.userName
                Glide.with(mContext).load(BuildConfig.BASE_MEDIA_URL + contact.profileImage)
                    .placeholder(mContext.getDrawable(R.drawable.profile_icon)).into(imageProfile)

                if (isFirst && !isLast) {
                    clPerson.background = mContext.getDrawable(R.drawable.top_circular_border)
                    dividerView.visible()
                } else if (isLast && !isFirst) {
                    clPerson.background = mContext.getDrawable(R.drawable.bottom_circular_border)
                    dividerView.gone()
                } else if (isLast && isFirst) {
                    clPerson.background = mContext.getDrawable(R.drawable.circular_border)
                    dividerView.gone()
                } else {
                    clPerson.setBackgroundColor(mContext.getColor(R.color.white))
                    dividerView.visible()
                }

                if (typeFrom == 0) {
                    cbPerson.gone()
                } else if (typeFrom == 1) {
                    cbPerson.visible()
                    cbPerson.isChecked = contact.isCheckedUser == true
                }
                cbPerson.setOnClickListener {
                    contact.isCheckedUser = !contact.isCheckedUser!!
                }
            }
        }
    }

    inner class SectionViewHolder(private val binding: ItemSectionBinding) :
        BaseViewHolder(binding.root) {

        fun bind(section: String) {
            binding.textSection.text = section
        }
    }

    override fun getItemViewType(position: Int): Int {
        var itemCount = 0
        for ((section, contactsInSection) in sectionedContacts) {
            if (position == itemCount) return VIEW_TYPE_SECTION
            itemCount += contactsInSection.size + 1
        }
        return VIEW_TYPE_CONTACT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return when (viewType) {
            VIEW_TYPE_CONTACT -> {
                val binding = ItemPersonBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                ContactViewHolder(binding)
            }

            VIEW_TYPE_SECTION -> {
                val binding = ItemSectionBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                SectionViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    // Data structure to hold section positions
    private data class SectionPosition(
        val headerPosition: Int, val firstItemPosition: Int, val lastItemPosition: Int
    )

    // Map to store positions for each section
    private val sectionPositions = mutableMapOf<String, SectionPosition>()

    init {
        // Calculate positions for each section
        var currentPosition = 0
        sectionedContacts.forEach { (section, contactsInSection) ->
            sectionPositions[section] = SectionPosition(
                headerPosition = currentPosition,
                firstItemPosition = currentPosition + 1,
                lastItemPosition = currentPosition + contactsInSection.size
            )
            currentPosition += contactsInSection.size + 1 // +1 for header
        }
    }

    // Helper function to get section info for any position
    private fun getSectionForPosition(position: Int): String? {
        return sectionPositions.entries.find { (_, positions) ->
            position >= positions.headerPosition && position <= positions.lastItemPosition
        }?.key
    }

    // Helper function to check if position is first in its section
    private fun isFirstInSection(position: Int): Boolean {
        return sectionPositions.values.any { it.firstItemPosition == position }
    }

    // Helper function to check if position is last in its section
    private fun isLastInSection(position: Int): Boolean {
        return sectionPositions.values.any { it.lastItemPosition == position }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val currentSection = getSectionForPosition(position)
        val sectionPosition = currentSection?.let { sectionPositions[it] }

        when {
            sectionPosition?.headerPosition == position -> {
                // It's a section header
                (holder as SectionViewHolder).bind(currentSection)
            }

            sectionPosition != null -> {
                // It's a contact item
                val contactsInSection = sectionedContacts[currentSection]!!
                val contactIndex = position - sectionPosition.headerPosition - 1
                val contact = contactsInSection[contactIndex]
                val isFirst = isFirstInSection(position)
                val isLast = isLastInSection(position)

                (holder as ContactViewHolder).bind(contact, isFirst, isLast)


            }
        }

    }

    fun scrollToSection(section: String, recyclerView: RecyclerView) {
        var position = 0
        for ((currentSection, contacts) in sectionedContacts) {
            if (currentSection == section) {
                val smoothScroller: RecyclerView.SmoothScroller =
                    object : LinearSmoothScroller(mContext) {
                        override fun getVerticalSnapPreference(): Int {
                            return SNAP_TO_START
                        }
                    }

                smoothScroller.targetPosition = position
                recyclerView.layoutManager!!.startSmoothScroll(smoothScroller)
                return
            } else if (section == "#") {
                recyclerView.smoothScrollToPosition(0)
            }
            position += contacts.size + 1 // +1 for header
        }
    }

    override fun getItemCount(): Int {
        return contacts.size + sectionedContacts.size
    }

    companion object {
        private const val VIEW_TYPE_CONTACT = 0
        private const val VIEW_TYPE_SECTION = 1
    }
}
