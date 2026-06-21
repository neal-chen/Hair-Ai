package com.hairstyle.generator.ui.adapter

import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hairstyle.generator.R
import com.hairstyle.generator.databinding.ItemResultCardBinding
import com.hairstyle.generator.ui.widget.ImageComparerView

/**
 * 生成结果适配器
 */
class ResultsAdapter(
    private val results: List<String>,
    private val userImageUrl: String,
    private val onResultClick: (Int) -> Unit,
    private val onRegisterSaveClick: ((String) -> Unit)? = null,
    private val onChangeColorClick: ((String) -> Unit)? = null,
    private val on3DViewClick: ((String) -> Unit)? = null
) : RecyclerView.Adapter<ResultsAdapter.ResultViewHolder>() {

    private var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val binding = ItemResultCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ResultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        if (position < results.size) {
            holder.bind(results[position], position)
        }
    }

    override fun getItemCount() = results.size

    /**
     * 获取当前选中位置
     */
    fun getSelectedPosition(): Int = selectedPosition

    /**
     * 设置选中位置
     */
    fun setSelectedPosition(position: Int) {
        val previousPosition = selectedPosition
        selectedPosition = if (selectedPosition == position) -1 else position

        if (previousPosition != -1) {
            notifyItemChanged(previousPosition)
        }
        if (selectedPosition != -1) {
            notifyItemChanged(selectedPosition)
        }
    }

    /**
     * 获取选中的图片URL
     */
    fun getSelectedImageUrl(): String? {
        return if (selectedPosition >= 0 && selectedPosition < results.size) {
            results[selectedPosition]
        } else null
    }

    /**
     * 显示图片预览弹窗（带前后对比功能）
     */
    private fun showImagePreviewDialog(context: Context, position: Int) {
        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_image_preview)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )

        var currentPosition = position

        val imageComparer = dialog.findViewById<ImageComparerView>(R.id.imageComparer)
        val tvDone = dialog.findViewById<TextView>(R.id.tvDone)
        val ivCheckMark = dialog.findViewById<ImageView>(R.id.ivCheckMark)
        val btnPrevious = dialog.findViewById<ImageView>(R.id.btnPrevious)
        val btnNext = dialog.findViewById<ImageView>(R.id.btnNext)
        val btnRegisterSave = dialog.findViewById<Button>(R.id.btnRegisterSave)
        val btnChangeColor = dialog.findViewById<Button>(R.id.btnChangeColor)
        val btn3DView = dialog.findViewById<Button>(R.id.btn3DView)

        // 加载对比图片
        fun loadComparisonImages(pos: Int) {
            if (pos >= 0 && pos < results.size) {
                Log.d("ResultsAdapter", "Loading comparison images - position: $pos")
                Log.d("ResultsAdapter", "  beforeUrl (userImageUrl): '$userImageUrl'")
                Log.d("ResultsAdapter", "  afterUrl (result): '${results[pos]}'")

                imageComparer.loadImages(
                    context = context,
                    beforeUrl = userImageUrl,
                    afterUrl = results[pos]
                )
            }
        }

        // 更新按钮可见性
        fun updateNavigationButtons() {
            btnPrevious.visibility = if (currentPosition > 0) View.VISIBLE else View.INVISIBLE
            btnNext.visibility = if (currentPosition < results.size - 1) View.VISIBLE else View.INVISIBLE
        }

        loadComparisonImages(currentPosition)
        updateNavigationButtons()

        // 完成按钮关闭弹窗
        tvDone.setOnClickListener {
            setSelectedPosition(currentPosition)
            onResultClick(currentPosition)
            imageComparer.clear()
            dialog.dismiss()
        }

        // 右上角勾选图标关闭弹窗
        ivCheckMark.setOnClickListener {
            setSelectedPosition(currentPosition)
            onResultClick(currentPosition)
            imageComparer.clear()
            dialog.dismiss()
        }

        // 上一张
        btnPrevious.setOnClickListener {
            if (currentPosition > 0) {
                currentPosition--
                loadComparisonImages(currentPosition)
                updateNavigationButtons()
            }
        }

        // 下一张
        btnNext.setOnClickListener {
            if (currentPosition < results.size - 1) {
                currentPosition++
                loadComparisonImages(currentPosition)
                updateNavigationButtons()
            }
        }

        // 底部按钮点击
        btnRegisterSave.setOnClickListener {
            val imageUrl = results[currentPosition]
            onRegisterSaveClick?.invoke(imageUrl)
        }

        btnChangeColor.setOnClickListener {
            val imageUrl = results[currentPosition]
            onChangeColorClick?.invoke(imageUrl)
        }

        btn3DView.setOnClickListener {
            val imageUrl = results[currentPosition]
            on3DViewClick?.invoke(imageUrl)
        }

        dialog.show()
    }

    inner class ResultViewHolder(
        private val binding: ItemResultCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(imageUrl: String, position: Int) {
            Glide.with(binding.root.context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_camera)
                .error(R.drawable.ic_camera)
                .skipMemoryCache(true)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                .into(binding.ivResult)

            // 设置结果标签
            binding.tvResultLabel.text = "结果 ${position + 1}"

            // 设置选中状态
            val isSelected = position == selectedPosition
            binding.selectedOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE
            binding.ivCheckIcon.visibility = if (isSelected) View.VISIBLE else View.GONE

            // 设置点击事件 - 显示预览弹窗
            binding.cardResult.setOnClickListener {
                showImagePreviewDialog(binding.root.context, position)
            }
        }
    }
}

/**
 * 加载状态适配器
 */
class LoadingAdapter : RecyclerView.Adapter<LoadingAdapter.LoadingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoadingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_loading_card, parent, false)
        return LoadingViewHolder(view)
    }

    override fun onBindViewHolder(holder: LoadingViewHolder, position: Int) {
        holder.bind()
    }

    override fun getItemCount() = 4 // 显示4个加载卡片

    inner class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val loadingIcon: View = itemView.findViewById(R.id.ivLoadingIcon)

        fun bind() {
            // 添加旋转动画
            loadingIcon.animate()
                .rotation(360f)
                .setDuration(2000)
                .withEndAction {
                    loadingIcon.rotation = 0f
                    bind() // 重新开始动画
                }
                .start()
        }
    }
}