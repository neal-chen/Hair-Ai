package com.hairstyle.generator.data.storage

import android.content.Context
import com.hairstyle.generator.config.HairColorImageConfig
import com.hairstyle.generator.data.model.HairColorCategory
import com.hairstyle.generator.data.model.HairColorTemplate
import org.json.JSONArray
import java.io.BufferedReader

/**
 * 发色库管理器
 * 负责管理本地发色库数据
 */
class HairColorLibraryManager(private val context: Context) {

    companion object {
        // 色系列表
        val CATEGORIES = listOf(
            "温感色系", "元气橙系", "清冷色系", "流光金系",
            "薄暮紫系", "热情红系", "中性灰阶系", "静谧蓝调系"
        )
    }

    // 缓存发色数据
    private var hairColorsCache: List<HairColorTemplate>? = null

    /**
     * 从assets加载发色数据
     */
    private fun loadHairColors(): List<HairColorTemplate> {
        hairColorsCache?.let { return it }

        val colors = mutableListOf<HairColorTemplate>()

        try {
            val inputStream = context.assets.open("hair_colors/hair_colors_data.json")
            val jsonString = inputStream.bufferedReader().use(BufferedReader::readText)
            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val category = obj.getString("category")
                val name = obj.getString("name")
                val procedure = obj.optString("procedure", "")

                colors.add(
                    HairColorTemplate(
                        id = "color_$i",
                        name = name,
                        category = category,
                        imageUrl = HairColorImageConfig.buildImagePath(category, name),
                        procedure = procedure
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        hairColorsCache = colors
        return colors
    }

    /**
     * 获取所有发色分类
     */
    fun getAllCategories(): List<HairColorCategory> {
        val allColors = loadHairColors()

        return CATEGORIES.mapIndexed { index, categoryName ->
            val colorsInCategory = allColors.filter { it.category == categoryName }
            HairColorCategory(
                id = "category_$index",
                name = categoryName,
                colors = colorsInCategory
            )
        }.filter { it.colors.isNotEmpty() }
    }

    /**
     * 根据色系名称获取发色列表
     */
    fun getColorsByCategory(categoryName: String): List<HairColorTemplate> {
        val allColors = loadHairColors()
        return if (categoryName == "全部") {
            allColors
        } else {
            allColors.filter { it.category == categoryName }
        }
    }

    /**
     * 搜索发色
     */
    fun searchColors(query: String): List<HairColorTemplate> {
        val allColors = loadHairColors()
        return allColors.filter { color ->
            color.name.contains(query, ignoreCase = true) ||
            color.category.contains(query, ignoreCase = true)
        }
    }

    /**
     * 根据ID获取发色
     */
    fun getColorById(colorId: String): HairColorTemplate? {
        val allColors = loadHairColors()
        return allColors.find { it.id == colorId }
    }
}
