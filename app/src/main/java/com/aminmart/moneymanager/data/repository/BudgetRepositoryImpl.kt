package com.aminmart.moneymanager.data.repository

import com.aminmart.moneymanager.data.database.MoneyDatabase
import com.aminmart.moneymanager.domain.model.Budget
import com.aminmart.moneymanager.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow

/**
 * Implementation of BudgetRepository using SQLite database
 */
class BudgetRepositoryImpl(
    private val database: MoneyDatabase
) : BudgetRepository {

    override fun getAllBudgets(): Flow<List<Budget>> =
        database.getAllBudgets()

    override fun getBudgetsByMonth(month: String): Flow<List<Budget>> =
        database.getBudgetsByMonth(month)

    override suspend fun getBudgetByCategory(category: String, month: String): Budget? =
        database.getBudgetByCategory(category, month)

    override suspend fun getBudgetById(id: Long): Budget? =
        database.getBudgetById(id)

    override suspend fun insertBudget(budget: Budget): Long =
        database.insertBudget(budget)

    override suspend fun updateBudget(budget: Budget) =
        database.updateBudget(budget)

    override suspend fun deleteBudget(id: Long) =
        database.deleteBudget(id)

    override suspend fun updateBudgetSpent(category: String, month: String, spent: Double) =
        database.updateBudgetSpent(category, month, spent)

    override suspend fun getTotalBudgetForMonth(month: String): Double =
        database.getTotalBudgetForMonth(month)

    override suspend fun getTotalSpentForMonth(month: String): Double =
        database.getTotalSpentForMonth(month)
}
