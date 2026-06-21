package com.hairstyle.generator.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hairstyle.generator.R
import com.hairstyle.generator.data.model.GenerationHistory
import com.hairstyle.generator.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * 历史记录适配器
 */
class HistoryAdapter(
    private val historyList: List<GenerationHistory>,
    private val onHistoryClick: (GenerationHistory) -> Unit,
    private val onMoreOptionsClick: (GenerationHistory) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private val dateFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(historyList[position])
    }

    override fun getItemCount() = historyList.size

    inner class HistoryViewHolder(
        private val binding: ItemHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(history: GenerationHistory) {
            // 设置生成时间
            binding.tvGenerationTime.text = dateFormat.format(Date(history.generatedAt))

            // 加载用户照片
            Glide.with(binding.root.context)
                .load(history.userImageUrl.replace("http://", "https://"))
                .centerCrop()
                .placeholder(R.drawable.ic_camera)
                .into(binding.ivUserPhoto)

            // 加载发型参考图
            Glide.with(binding.root.context)
                .load(history.hairstyleImageUrl.replace("http://", "https://"))
                .centerCrop()
                .placeholder(R.drawable.ic_scissors)
                .into(binding.ivHairstyleReference)

            // 设置生成结果预览
            val resultAdapter = HistoryResultAdapter(history.resultUrls)
            binding.rvResults.layoutManager = LinearLayoutManager(
                binding.root.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            binding.rvResults.adapter = resultAdapter

            // 设置点击事件
            binding.cardHistory.setOnClickListener { onHistoryClick(history) }
            binding.btnMoreOptions.setOnClickListener { onMoreOptionsClick(history) }
        }
    }
}

/**
 * 历史结果预览适配器
 */
class HistoryResultAdapter(
    private val resultUrls: List<String>
) : RecyclerView.Adapter<HistoryResultAdapter.ResultViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_result, parent, false)
        return ResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        holder.bind(resultUrls[position])
    }

    override fun getItemCount() = resultUrls.size

    inner class ResultViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: android.widget.ImageView =
            itemView.findViewById(R.id.ivResultPreview)

        fun bind(imageUrl: String) {
            Glide.with(itemView.context)
                .load(imageUrl.replace("http://", "https://"))
                .centerCrop()
                .placeholder(R.drawable.ic_camera)
                .into(imageView)
        }
    }
}