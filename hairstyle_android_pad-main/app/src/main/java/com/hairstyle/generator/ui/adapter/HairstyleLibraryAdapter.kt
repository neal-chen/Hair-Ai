package com.hairstyle.generator.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hairstyle.generator.R
import com.hairstyle.generator.config.HairstyleImageConfig
import com.hairstyle.generator.data.model.HairstyleTemplate
import com.hairstyle.generator.databinding.ItemHairstyleTemplateBinding

class HairstyleLibraryAdapter(
    private val onItemClick: (HairstyleTemplate) -> Unit
) : ListAdapter<HairstyleTemplate, HairstyleLibraryAdapter.ViewHolder>(HairstyleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHairstyleTemplateBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemHairstyleTemplateBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(hairstyle: HairstyleTemplate) {
            binding.tvHairstyleName.text = hairstyle.name
            binding.tvHairstyleCategory.text = hairstyle.category

            // 加载发型图片 - 使用配置管理器
            val imagePath = HairstyleImageConfig.buildImagePath(
                hairstyle.category,
                hairstyle.name,
                hairstyle.description,
                hairstyle.gender
            )

            Glide.with(binding.ivHairstyle.context)
                .load(imagePath)
                .placeholder(R.drawable.ic_image)
                .error(R.drawable.ic_image)
                .fitCenter() // 使用fitCenter替代centerCrop，保证图片完整显示
                .into(binding.ivHairstyle)

            binding.cardHairstyle.setOnClickListener {
                onItemClick(hairstyle)
            }
        }
    }

    class HairstyleDiffCallback : DiffUtil.ItemCallback<HairstyleTemplate>() {
        override fun areItemsTheSame(oldItem: HairstyleTemplate, newItem: HairstyleTemplate): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HairstyleTemplate, newItem: HairstyleTemplate): Boolean {
            return oldItem == newItem
        }
    }
}