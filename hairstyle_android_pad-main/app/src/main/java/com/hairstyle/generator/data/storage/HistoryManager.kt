package com.hairstyle.generator.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hairstyle.generator.data.model.GenerationHistory

/**
 * 历史记录管理器
 */
class HistoryManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("generation_history", Context.MODE_PRIVATE)

    private val gson = Gson()
    private val historyListType = object : TypeToken<MutableList<GenerationHistory>>() {}.type

    companion object {
        private const val KEY_HISTORY_LIST = "history_list"
    }

    /**
     * 保存生成历史记录
     */
    fun saveHistory(history: GenerationHistory) {
        val historyList = getHistory().toMutableList()
        // 检查是否已存在相同sessionId的记录
        val existingIndex = historyList.indexOfFirst { it.sessionId == history.sessionId }
        if (existingIndex != -1) {
            // 更新现有记录
            historyList[existingIndex] = history
        } else {
            // 添加新记录到列表开头
            historyList.add(0, history)
        }

        // 限制历史记录数量，最多保存100条
        if (historyList.size > 100) {
            historyList.removeAt(historyList.size - 1)
        }

        val json = gson.toJson(historyList)
        sharedPreferences.edit().putString(KEY_HISTORY_LIST, json).apply()
    }

    /**
     * 获取所有历史记录
     */
    fun getHistory(): List<GenerationHistory> {
        val json = sharedPreferences.getString(KEY_HISTORY_LIST, null)
        return if (json != null) {
            try {
                gson.fromJson(json, historyListType) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    /**
     * 删除指定的历史记录
     */
    fun deleteHistory(historyId: String) {
        val historyList = getHistory().toMutableList()
        historyList.removeAll { it.id == historyId }
        val json = gson.toJson(historyList)
        sharedPreferences.edit().putString(KEY_HISTORY_LIST, json).apply()
    }

    /**
     * 清空所有历史记录
     */
    fun clearHistory() {
        sharedPreferences.edit().remove(KEY_HISTORY_LIST).apply()
    }

    /**
     * 更新收藏状态
     */
    fun updateFavoriteStatus(historyId: String, isFavorite: Boolean) {
        val historyList = getHistory().toMutableList()
        val index = historyList.indexOfFirst { it.id == historyId }
        if (index != -1) {
            historyList[index] = historyList[index].copy(isFavorite = isFavorite)
            val json = gson.toJson(historyList)
            sharedPreferences.edit().putString(KEY_HISTORY_LIST, json).apply()
        }
    }
}