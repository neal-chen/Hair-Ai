package com.hairstyle.generator.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.hairstyle.generator.data.model.GenerationHistory
import com.hairstyle.generator.data.repository.HairstyleRepository
import com.hairstyle.generator.databinding.ActivityHistoryBinding
import com.hairstyle.generator.ui.adapter.HistoryAdapter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 历史记录页面Activity
 */
class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: HistoryAdapter
    private lateinit var repository: HairstyleRepository

    private var historyList: MutableList<GenerationHistory> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = HairstyleRepository(this)
        setupUI()
        loadHistory()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnClearAll.setOnClickListener { showClearAllDialog() }

        // 设置RecyclerView
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
    }

    /**
     * 加载历史记录
     */
    private fun loadHistory() {
        showLoading(true)

        // 从repository加载真实的历史数据
        historyList = repository.getHistory().toMutableList()

        // 设置适配器
        adapter = HistoryAdapter(
            historyList = historyList,
            onHistoryClick = { history ->
                // 查看详细结果
                viewHistoryDetail(history)
            },
            onMoreOptionsClick = { history ->
                // 显示更多选项
                showHistoryOptions(history)
            }
        )
        binding.rvHistory.adapter = adapter

        updateEmptyState()
        showLoading(false)
    }

    /**
     * 查看历史详情
     */
    private fun viewHistoryDetail(history: GenerationHistory) {
        val intent = Intent(this, ResultsActivity::class.java)
        intent.putExtra("history_mode", true)
        intent.putExtra("user_image_url", history.userImageUrl)
        intent.putExtra("hairstyle_image_url", history.hairstyleImageUrl)
        intent.putExtra("result_urls", history.resultUrls.toTypedArray())
        startActivity(intent)
    }

    /**
     * 显示历史记录选项
     */
    private fun showHistoryOptions(history: GenerationHistory) {
        val options = arrayOf("重新生成", "分享", "删除")

        AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> regenerateHistory(history)
                    1 -> shareHistory(history)
                    2 -> deleteHistory(history)
                }
            }
            .show()
    }

    /**
     * 重新生成
     */
    private fun regenerateHistory(history: GenerationHistory) {
        val intent = Intent(this, PhotoUploadActivity::class.java)
        intent.putExtra("preset_user_image", history.userImageUrl)
        intent.putExtra("preset_hairstyle_image", history.hairstyleImageUrl)
        startActivity(intent)
    }

    /**
     * 分享历史记录
     */
    private fun shareHistory(history: GenerationHistory) {
        val shareText = "看看我的AI发型生成结果！"
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(shareIntent, "分享发型"))
    }

    /**
     * 删除历史记录
     */
    private fun deleteHistory(history: GenerationHistory) {
        AlertDialog.Builder(this)
            .setTitle("删除历史记录")
            .setMessage("确定要删除这条历史记录吗？")
            .setPositiveButton("删除") { _, _ ->
                repository.deleteHistory(history.id)
                historyList.remove(history)
                adapter.notifyDataSetChanged()
                updateEmptyState()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示清空所有对话框
     */
    private fun showClearAllDialog() {
        if (historyList.isEmpty()) return

        AlertDialog.Builder(this)
            .setTitle("清空历史记录")
            .setMessage("确定要清空所有历史记录吗？此操作不可撤销。")
            .setPositiveButton("清空") { _, _ ->
                repository.clearHistory()
                historyList.clear()
                adapter.notifyDataSetChanged()
                updateEmptyState()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 更新空状态显示
     */
    private fun updateEmptyState() {
        if (historyList.isEmpty()) {
            binding.rvHistory.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
        } else {
            binding.rvHistory.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
        }
    }

    /**
     * 显示/隐藏加载指示器
     */
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

}