package com.aminmart.moneymanager.domain.usecase

import com.aminmart.moneymanager.domain.model.Budget
import com.aminmart.moneymanager.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to get all budgets
 */
class GetAllBudgetsUseCase(
    private val repository: BudgetRepository
) {
    operator fun invoke(): Flow<List<Budget>> = repository.getAllBudgets()
}

/**
 * Use case to get budgets for current month
 */
class GetCurrentMonthBudgetsUseCase(
    private val repository: BudgetRepository
) {
    operator fun invoke(): Flow<List<Budget>> {
        val currentMonth = getCurrentMonth()
        return repository.getBudgetsByMonth(currentMonth)
    }
    
    private fun getCurrentMonth(): String {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        return String.format("%04d-%02d", year, month)
    }
}

/**
 * Use case to add/update budget
 */
class SaveBudgetUseCase(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(budget: Budget): Long {
        val existing = repository.getBudgetByCategory(budget.category, budget.month)
        return if (existing != null) {
            val updated = budget.copy(id = existing.id)
            repository.updateBudget(updated)
            existing.id
        } else {
            repository.insertBudget(budget)
        }
    }
}

/**
 * Use case to delete budget
 */
class DeleteBudgetUseCase(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(id: Long) {
        repository.deleteBudget(id)
    }
}

/**
 * Use case to get budget by category
 */
class GetBudgetByCategoryUseCase(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(category: String, month: String): Budget? {
        return repository.getBudgetByCategory(category, month)
    }
}
