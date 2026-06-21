package com.hairstyle.generator.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hairstyle.generator.R
import com.hairstyle.generator.data.model.HairstyleTemplate
import com.hairstyle.generator.databinding.ItemHairstyleTemplateBinding

/**
 * 发型模板适配器
 */
class HairstyleAdapter(
    private val hairstyles: List<HairstyleTemplate>,
    private val onHairstyleClick: (HairstyleTemplate) -> Unit
) : RecyclerView.Adapter<HairstyleAdapter.HairstyleViewHolder>() {

    private var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HairstyleViewHolder {
        val binding = ItemHairstyleTemplateBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HairstyleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HairstyleViewHolder, position: Int) {
        holder.bind(hairstyles[position], position)
    }

    override fun getItemCount() = hairstyles.size

    /**
     * 设置选中位置
     */
    fun setSelectedPosition(position: Int) {
        val previousPosition = selectedPosition
        selectedPosition = position

        if (previousPosition != -1) {
            notifyItemChanged(previousPosition)
        }
        notifyItemChanged(selectedPosition)
    }

    inner class HairstyleViewHolder(
        private val binding: ItemHairstyleTemplateBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(hairstyle: HairstyleTemplate, position: Int) {
            // 加载发型图片
            Glide.with(binding.root.context)
                .load(hairstyle.imageUrl)
                .centerCrop()
                .placeholder(R.drawable.ic_scissors)
                .error(R.drawable.ic_scissors)
                .into(binding.ivHairstyle)

            // 设置发型信息
            binding.tvHairstyleName.text = hairstyle.name
            binding.tvHairstyleCategory.text = "${hairstyle.category} • ${hairstyle.tags.joinToString(" • ")}"

            // 设置选中状态
            val isSelected = position == selectedPosition
            binding.ivSelected.visibility = if (isSelected) View.VISIBLE else View.GONE

            // 设置点击事件
            binding.cardHairstyle.setOnClickListener {
                onHairstyleClick(hairstyle)
                setSelectedPosition(position)
            }
        }
    }
}