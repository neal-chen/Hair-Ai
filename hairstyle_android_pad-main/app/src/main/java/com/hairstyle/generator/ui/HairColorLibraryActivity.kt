package com.hairstyle.generator.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.tabs.TabLayout
import com.hairstyle.generator.data.model.HairColorTemplate
import com.hairstyle.generator.data.storage.HairColorLibraryManager
import com.hairstyle.generator.databinding.ActivityHairColorLibraryBinding
import com.hairstyle.generator.ui.adapter.HairColorLibraryAdapter
import kotlinx.coroutines.launch

/**
 * 发色库选择Activity
 */
class HairColorLibraryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHairColorLibraryBinding
    private lateinit var colorAdapter: HairColorLibraryAdapter
    private lateinit var colorManager: HairColorLibraryManager

    companion object {
        const val EXTRA_SELECTED_COLOR = "selected_color"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHairColorLibraryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        colorManager = HairColorLibraryManager(this)

        setupUI()
        setupTabs()
        setupRecyclerView()
        setupSearch()

        // 默认显示全部发色
        loadColors("全部")
    }

    private fun setupUI() {
        // 返回按钮点击事件
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 清除搜索按钮点击事件
        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.text.clear()
        }
    }

    private fun setupTabs() {
        // 添加"全部"Tab
        binding.tabCategory.addTab(binding.tabCategory.newTab().setText("全部"))

        // 添加各色系Tab
        HairColorLibraryManager.CATEGORIES.forEach { category ->
            binding.tabCategory.addTab(binding.tabCategory.newTab().setText(category))
        }

        binding.tabCategory.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val category = tab?.text.toString()
                loadColors(category)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerView() {
        colorAdapter = HairColorLibraryAdapter { hairColor ->
            onColorSelected(hairColor)
        }

        binding.rvColors.apply {
            layoutManager = GridLayoutManager(this@HairColorLibraryActivity, 3)
            adapter = colorAdapter
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                binding.btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE

                if (query.isNotEmpty()) {
                    searchColors(query)
                } else {
                    // 如果搜索框为空，恢复当前分类的发色显示
                    val category = binding.tabCategory.getTabAt(binding.tabCategory.selectedTabPosition)?.text.toString()
                    loadColors(category)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadColors(category: String) {
        binding.layoutLoading.visibility = View.VISIBLE
        binding.rvColors.visibility = View.GONE

        lifecycleScope.launch {
            val colors = colorManager.getColorsByCategory(category)

            colorAdapter.submitList(colors)
            updateEmptyState(colors.isEmpty())
            binding.layoutLoading.visibility = View.GONE
        }
    }

    private fun searchColors(query: String) {
        binding.layoutLoading.visibility = View.VISIBLE
        binding.rvColors.visibility = View.GONE

        lifecycleScope.launch {
            val colors = colorManager.searchColors(query)

            colorAdapter.submitList(colors)
            updateEmptyState(colors.isEmpty())
            binding.layoutLoading.visibility = View.GONE
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvColors.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun onColorSelected(hairColor: HairColorTemplate) {
        // 返回选中的发色
        val intent = Intent().apply {
            putExtra(EXTRA_SELECTED_COLOR, hairColor)
        }
        setResult(RESULT_OK, intent)
        finish()
    }
}
