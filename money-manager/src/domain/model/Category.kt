package com.aminmart.moneymanager.domain.model

/**
 * Category model for transaction categorization
 */
data class Category(
    val id: Long = 0,
    val name: String,
    val type: Transaction.TransactionType,
    val icon: String = "",
    val color: Int = 0
) {
    companion object {
        val DEFAULT_INCOME_CATEGORIES = listOf(
            Category(name = "Salary", type = Transaction.TransactionType.INCOME),
            Category(name = "Business", type = Transaction.TransactionType.INCOME),
            Category(name = "Investment", type = Transaction.TransactionType.INCOME),
            Category(name = "Gift", type = Transaction.TransactionType.INCOME),
            Category(name = "Other", type = Transaction.TransactionType.INCOME)
        )
        
        val DEFAULT_EXPENSE_CATEGORIES = listOf(
            Category(name = "Food & Drink", type = Transaction.TransactionType.EXPENSE),
            Category(name = "Transportation", type = Transaction.TransactionType.EXPENSE),
            Category(name = "Shopping", type = Transaction.TransactionType.EXPENSE),
            Category(name = "Entertainment", type = Transaction.TransactionType.EXPENSE),
            Category(name = "Bills & Utilities", type = Transaction.TransactionType.EXPENSE),
            Category(name = "Health", type = Transaction.TransactionType.EXPENSE),
            Category(name = "Education", type = Transaction.TransactionType.EXPENSE),
            Category(name = "Housing", type = Transaction.TransactionType.EXPENSE),
            Category(name = "Other", type = Transaction.TransactionType.EXPENSE)
        )
    }
}
