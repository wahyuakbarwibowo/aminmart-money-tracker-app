package com.aminmart.moneymanager.domain.model

/**
 * Budget model for monthly budget planning
 */
data class Budget(
    val id: Long = 0,
    val category: String,
    val monthlyBudget: Double,
    val month: String, // Format: YYYY-MM
    val spent: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
) {
    val remaining: Double
        get() = monthlyBudget - spent
    
    val percentageUsed: Float
        get() = if (monthlyBudget > 0) (spent / monthlyBudget).toFloat() else 0f
    
    val isOverBudget: Boolean
        get() = spent > monthlyBudget
    
    val isNearLimit: Boolean
        get() = percentageUsed >= 0.8f && percentageUsed < 1.0f
}
