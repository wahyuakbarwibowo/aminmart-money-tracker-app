package com.aminmart.moneymanager.domain.model

/**
 * Transaction model representing income or expense
 */
data class Transaction(
    val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val description: String,
    val date: Long, // Timestamp in milliseconds
    val createdAt: Long = System.currentTimeMillis()
) {
    enum class TransactionType {
        INCOME,
        EXPENSE
    }
}
