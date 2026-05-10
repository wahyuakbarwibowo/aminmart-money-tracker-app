package com.aminmart.moneymanager.domain.usecase

import com.aminmart.moneymanager.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Use case to get expense by category for charts
 */
class GetExpenseByCategoryUseCase(
    private val repository: TransactionRepository
) {
    operator fun invoke(startDate: Long, endDate: Long): Flow<Map<String, Double>> = flow {
        emit(repository.getExpenseByCategory(startDate, endDate))
    }
}

/**
 * Use case to get monthly expenses for charts
 */
class GetMonthlyExpensesUseCase(
    private val repository: TransactionRepository
) {
    operator fun invoke(months: Int = 12): Flow<Map<String, Double>> = flow {
        emit(repository.getMonthlyExpenses(months))
    }
}

/**
 * Data class for chart entry
 */
data class ChartEntry(
    val label: String,
    val value: Float
)
