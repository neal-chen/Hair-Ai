package com.hairstyle.generator.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hairstyle.generator.R
import com.hairstyle.generator.data.model.HairColorTemplate
import com.hairstyle.generator.databinding.ItemHairColorTemplateBinding

class HairColorLibraryAdapter(
    private val onItemClick: (HairColorTemplate) -> Unit
) : ListAdapter<HairColorTemplate, HairColorLibraryAdapter.ViewHolder>(HairColorDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHairColorTemplateBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemHairColorTemplateBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(hairColor: HairColorTemplate) {
            binding.tvColorName.text = hairColor.name
            binding.tvColorCategory.text = hairColor.category

            // 加载发色图片
            Glide.with(binding.ivColorImage.context)
                .load(hairColor.imageUrl)
                .placeholder(R.drawable.ic_image)
                .error(R.drawable.ic_image)
                .fitCenter()
                .into(binding.ivColorImage)

            binding.cardColor.setOnClickListener {
                onItemClick(hairColor)
            }
        }
    }

    class HairColorDiffCallback : DiffUtil.ItemCallback<HairColorTemplate>() {
        override fun areItemsTheSame(oldItem: HairColorTemplate, newItem: HairColorTemplate): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HairColorTemplate, newItem: HairColorTemplate): Boolean {
            return oldItem == newItem
        }
    }
}
