package com.aminmart.moneymanager.domain.usecase

import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.domain.repository.BudgetRepository
import com.aminmart.moneymanager.domain.repository.TransactionRepository
import java.util.Calendar

/**
 * Use case to add a transaction
 */
class AddTransactionUseCase(
    private val repository: TransactionRepository,
    private val syncBudgetSpentUseCase: SyncBudgetSpentUseCase
) {
    suspend operator fun invoke(transaction: Transaction): Long {
        val id = repository.insertTransaction(transaction)
        syncBudgetSpentUseCase.syncForTransaction(transaction)
        return id
    }
}

/**
 * Use case to update a transaction
 */
class UpdateTransactionUseCase(
    private val repository: TransactionRepository,
    private val syncBudgetSpentUseCase: SyncBudgetSpentUseCase
) {
    suspend operator fun invoke(transaction: Transaction) {
        val existing = repository.getTransactionById(transaction.id)
        repository.updateTransaction(transaction)
        existing?.let { syncBudgetSpentUseCase.syncForTransaction(it) }
        syncBudgetSpentUseCase.syncForTransaction(transaction)
    }
}

/**
 * Use case to delete a transaction
 */
class DeleteTransactionUseCase(
    private val repository: TransactionRepository,
    private val syncBudgetSpentUseCase: SyncBudgetSpentUseCase
) {
    suspend operator fun invoke(id: Long) {
        val existing = repository.getTransactionById(id)
        repository.deleteTransaction(id)
        existing?.let { syncBudgetSpentUseCase.syncForTransaction(it) }
    }
}

/**
 * Use case to get a transaction by ID
 */
class GetTransactionByIdUseCase(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(id: Long): Transaction? {
        return repository.getTransactionById(id)
    }
}

/**
 * Recalculate spent amount for a budget category/month from actual expense transactions.
 */
class SyncBudgetSpentUseCase(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository
) {
    suspend fun syncForTransaction(transaction: Transaction) {
        if (transaction.type != Transaction.TransactionType.EXPENSE) {
            return
        }

        val month = transaction.date.toBudgetMonth()
        val budget = budgetRepository.getBudgetByCategory(transaction.category, month) ?: return
        val (startDate, endDate) = month.toDateRange()
        val expensesByCategory = transactionRepository.getExpenseByCategory(startDate, endDate)
        val spent = expensesByCategory[transaction.category] ?: 0.0
        budgetRepository.updateBudgetSpent(budget.category, budget.month, spent)
    }

    private fun Long.toBudgetMonth(): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = this@toBudgetMonth }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        return String.format("%04d-%02d", year, month)
    }

    private fun String.toDateRange(): Pair<Long, Long> {
        val parts = split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt() - 1

        val start = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val end = (start.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
            add(Calendar.MILLISECOND, -1)
        }

        return start.timeInMillis to end.timeInMillis
    }
}
