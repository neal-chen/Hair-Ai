package com.hairstyle.generator.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.tabs.TabLayout
import com.hairstyle.generator.R
import com.hairstyle.generator.data.model.HairstyleTemplate
import com.hairstyle.generator.data.storage.HairstyleLibraryManager
import com.hairstyle.generator.databinding.ActivityHairstyleLibraryBinding
import com.hairstyle.generator.ui.adapter.HairstyleLibraryAdapter
import kotlinx.coroutines.launch

/**
 * 发型库选择Activity
 */
class HairstyleLibraryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHairstyleLibraryBinding
    private lateinit var hairstyleAdapter: HairstyleLibraryAdapter
    private lateinit var hairstyleManager: HairstyleLibraryManager

    companion object {
        const val EXTRA_SELECTED_HAIRSTYLE = "selected_hairstyle"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHairstyleLibraryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hairstyleManager = HairstyleLibraryManager(this)

        setupUI()
        setupTabs()
        setupRecyclerView()
        setupSearch()

        // 默认显示女性发型
        loadHairstyles("女", "全部")
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
        // 设置性别选择Tab
        binding.tabGender.addTab(binding.tabGender.newTab().setText("女"))
        binding.tabGender.addTab(binding.tabGender.newTab().setText("男"))

        binding.tabGender.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val gender = tab?.text.toString()
                setupCategoryTabs(gender)
                val selectedCategory = binding.tabCategory.getTabAt(0)?.text.toString() ?: "全部"
                loadHairstyles(gender, selectedCategory)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // 初始化分类Tab (女性分类)
        setupCategoryTabs("女")
    }

    private fun setupCategoryTabs(gender: String) {
        binding.tabCategory.removeAllTabs()

        if (gender == "女") {
            // 女性发型分类
            val femaleCategories = listOf("全部", "日式", "欧美", "氛围感", "甜酷风", "韩式")
            femaleCategories.forEach { category ->
                binding.tabCategory.addTab(binding.tabCategory.newTab().setText(category))
            }
        } else {
            // 男性发型分类
            val maleCategories = listOf("全部", "中等长度", "短发", "长发")
            maleCategories.forEach { category ->
                binding.tabCategory.addTab(binding.tabCategory.newTab().setText(category))
            }
        }

        binding.tabCategory.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val category = tab?.text.toString() ?: "全部"
                val gender = binding.tabGender.getTabAt(binding.tabGender.selectedTabPosition)?.text.toString() ?: "女"
                loadHairstyles(gender, category)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerView() {
        hairstyleAdapter = HairstyleLibraryAdapter { hairstyle ->
            onHairstyleSelected(hairstyle)
        }

        binding.rvHairstyles.apply {
            layoutManager = GridLayoutManager(this@HairstyleLibraryActivity, 3)
            adapter = hairstyleAdapter
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                binding.btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE

                if (query.isNotEmpty()) {
                    searchHairstyles(query)
                } else {
                    // 如果搜索框为空，恢复当前分类的发型显示
                    val gender = binding.tabGender.getTabAt(binding.tabGender.selectedTabPosition)?.text.toString() ?: "女"
                    val category = binding.tabCategory.getTabAt(binding.tabCategory.selectedTabPosition)?.text.toString() ?: "全部"
                    loadHairstyles(gender, category)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadHairstyles(gender: String, category: String) {
        binding.layoutLoading.visibility = View.VISIBLE
        binding.rvHairstyles.visibility = View.GONE

        lifecycleScope.launch {
            val hairstyles = if (category == "全部") {
                if (gender == "女") {
                    hairstyleManager.getFemaleCategories().flatMap { it.hairstyles }
                } else {
                    hairstyleManager.getMaleCategories().flatMap { it.hairstyles }
                }
            } else {
                if (gender == "女") {
                    hairstyleManager.getFemaleCategories()
                        .find { it.name == category }?.hairstyles ?: emptyList()
                } else {
                    hairstyleManager.getMaleCategories()
                        .find { it.name == category }?.hairstyles ?: emptyList()
                }
            }

            hairstyleAdapter.submitList(hairstyles)
            updateEmptyState(hairstyles.isEmpty())
            binding.layoutLoading.visibility = View.GONE
        }
    }

    private fun searchHairstyles(query: String) {
        binding.layoutLoading.visibility = View.VISIBLE
        binding.rvHairstyles.visibility = View.GONE

        lifecycleScope.launch {
            val hairstyles = hairstyleManager.searchHairstyles(query)

            hairstyleAdapter.submitList(hairstyles)
            updateEmptyState(hairstyles.isEmpty())
            binding.layoutLoading.visibility = View.GONE
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvHairstyles.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun onHairstyleSelected(hairstyle: HairstyleTemplate) {
        // 返回选中的发型
        val intent = Intent().apply {
            putExtra(EXTRA_SELECTED_HAIRSTYLE, hairstyle)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

}