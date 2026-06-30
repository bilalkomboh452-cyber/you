
package com.waglo.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.waglo.app.R
import com.waglo.app.databinding.ItemGroupBinding
import com.waglo.app.model.GroupLink
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GroupAdapter(
    private val onNotesClick: (GroupLink) -> Unit,
    private val onFavoriteClick: (GroupLink) -> Unit = {}
) : ListAdapter<GroupLink, GroupAdapter.ViewHolder>(DIFF_CB) {

    companion object {
        private val DIFF_CB = object : DiffUtil.ItemCallback<GroupLink>() {
            override fun areItemsTheSame(a: GroupLink, b: GroupLink) = a.id == b.id
            override fun areContentsTheSame(a: GroupLink, b: GroupLink) = a == b
        }
        private val DATE_FMT = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGroupBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val b: ItemGroupBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(group: GroupLink) {
            // Serial + Name
            b.tvSerial.text = "#${group.serialNumber}"
            b.tvGroupName.text = group.groupName.ifBlank { b.root.context.getString(R.string.unnamed_group) }
            b.tvInviteLink.text = group.inviteLink

            // Category chip
            b.tvCategory.text = group.category
            val catColor = categoryColor(group.category)
            b.tvCategory.setBackgroundColor(ContextCompat.getColor(b.root.context, catColor))

            // Source badge
            b.tvSource.text = group.source

            // Confidence
            val confPct = (group.confidenceScore * 100).toInt()
            b.progressConfidence.progress = confPct
            b.tvConfidence.text = "${confPct}%"

            // Favorite icon
            val favIcon = if (group.isFavorite) R.drawable.ic_favorite_filled
                          else R.drawable.ic_favorite_outline
            b.btnFavorite.setImageResource(favIcon)

            // Date
            if (group.addedAt > 0) {
                b.tvDate.text = DATE_FMT.format(Date(group.addedAt))
            }

            // Notes snippet
            b.tvNotes.text = group.notes.ifBlank { "" }

            // Selection
            b.root.isSelected = group.isSelected
            b.root.alpha = if (group.isSelected) 0.85f else 1f

            // Click listeners
            b.root.setOnClickListener {
                group.isSelected = !group.isSelected
                notifyItemChanged(bindingAdapterPosition)
            }
            b.root.setOnLongClickListener {
                onNotesClick(group)
                true
            }
            b.btnFavorite.setOnClickListener { onFavoriteClick(group) }
            b.btnNotes.setOnClickListener { onNotesClick(group) }
        }
    }

    // Selection helpers
    fun selectAll()   { currentList.forEach { it.isSelected = true };  notifyDataSetChanged() }
    fun unselectAll() { currentList.forEach { it.isSelected = false }; notifyDataSetChanged() }
    fun selectByCategory(cat: String) {
        currentList.forEach { it.isSelected = it.category == cat }
        notifyDataSetChanged()
    }
    fun getSelectedGroups(): List<GroupLink> = currentList.filter { it.isSelected }
    fun getSelectedCount(): Int = currentList.count { it.isSelected }

    private fun categoryColor(category: String) = when (category) {
        GroupLink.Category.EDUCATION     -> R.color.cat_education
        GroupLink.Category.JOBS          -> R.color.cat_jobs
        GroupLink.Category.TECHNOLOGY    -> R.color.cat_technology
        GroupLink.Category.AI            -> R.color.cat_ai
        GroupLink.Category.CRYPTO        -> R.color.cat_crypto
        GroupLink.Category.NEWS          -> R.color.cat_news
        GroupLink.Category.BUSINESS      -> R.color.cat_business
        GroupLink.Category.ENTERTAINMENT -> R.color.cat_entertainment
        GroupLink.Category.SPORTS        -> R.color.cat_sports
        GroupLink.Category.COMMUNITY     -> R.color.cat_community
        else                             -> R.color.cat_others
    }
}
