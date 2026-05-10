package com.aminmart.moneymanager.domain.model

data class Debt(
    val id: Long = 0,
    val personName: String,
    val amount: Double,
    val type: DebtType,
    val dueDate: Long,
    val description: String?,
    val isPaid: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
) {
    enum class DebtType {
        DEBT, // Money I owe to others
        CREDIT // Money others owe me
    }
}
