package com.aminmart.moneymanager.domain.model

/**
 * Dashboard statistics model
 */
data class DashboardStats(
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val transactionCount: Int = 0,
    val monthIncome: Double = 0.0,
    val monthExpense: Double = 0.0
) {
    val savingsRate: Float
        get() = if (totalIncome > 0) ((totalIncome - totalExpense) / totalIncome).toFloat() else 0f
}
