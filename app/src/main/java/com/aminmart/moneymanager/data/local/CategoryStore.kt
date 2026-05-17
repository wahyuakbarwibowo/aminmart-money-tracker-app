package com.aminmart.moneymanager.data.local

import android.content.Context
import com.aminmart.moneymanager.domain.model.Category
import com.aminmart.moneymanager.domain.model.Transaction
import org.json.JSONArray

class CategoryStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getIncomeCategories(): List<String> {
        return getCategories(KEY_INCOME, Category.DEFAULT_INCOME_CATEGORIES.map { it.name })
    }

    fun getExpenseCategories(): List<String> {
        return getCategories(KEY_EXPENSE, Category.DEFAULT_EXPENSE_CATEGORIES.map { it.name })
    }

    fun addCategory(type: Transaction.TransactionType, name: String): Boolean {
        val normalized = name.trim()
        if (normalized.isBlank()) return false

        val current = when (type) {
            Transaction.TransactionType.INCOME -> getIncomeCategories()
            Transaction.TransactionType.EXPENSE -> getExpenseCategories()
        }
        if (current.any { it.equals(normalized, ignoreCase = true) }) {
            return false
        }

        val updated = current + normalized
        saveCategories(type, updated)
        return true
    }

    fun removeCategory(type: Transaction.TransactionType, name: String): Boolean {
        val current = when (type) {
            Transaction.TransactionType.INCOME -> getIncomeCategories()
            Transaction.TransactionType.EXPENSE -> getExpenseCategories()
        }
        if (current.size <= 1) return false

        val updated = current.filterNot { it.equals(name, ignoreCase = true) }
        if (updated.size == current.size || updated.isEmpty()) {
            return false
        }
        saveCategories(type, updated)
        return true
    }

    private fun saveCategories(type: Transaction.TransactionType, categories: List<String>) {
        val key = if (type == Transaction.TransactionType.INCOME) KEY_INCOME else KEY_EXPENSE
        val array = JSONArray()
        categories.forEach { array.put(it) }
        prefs.edit().putString(key, array.toString()).apply()
    }

    private fun getCategories(key: String, fallback: List<String>): List<String> {
        val raw = prefs.getString(key, null) ?: return fallback
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val value = array.optString(i).trim()
                    if (value.isNotEmpty()) add(value)
                }
            }.ifEmpty { fallback }
        } catch (_: Exception) {
            fallback
        }
    }

    companion object {
        private const val PREFS_NAME = "category_settings"
        private const val KEY_INCOME = "income_categories"
        private const val KEY_EXPENSE = "expense_categories"
    }
}
