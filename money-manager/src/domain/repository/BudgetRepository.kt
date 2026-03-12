package com.aminmart.moneymanager.domain.repository

import com.aminmart.moneymanager.domain.model.Budget
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Budget operations
 */
interface BudgetRepository {
    
    /**
     * Get all budgets as Flow
     */
    fun getAllBudgets(): Flow<List<Budget>>
    
    /**
     * Get budgets for a specific month
     */
    fun getBudgetsByMonth(month: String): Flow<List<Budget>>
    
    /**
     * Get budget by category and month
     */
    suspend fun getBudgetByCategory(category: String, month: String): Budget?
    
    /**
     * Get budget by ID
     */
    suspend fun getBudgetById(id: Long): Budget?
    
    /**
     * Insert a new budget
     */
    suspend fun insertBudget(budget: Budget): Long
    
    /**
     * Update an existing budget
     */
    suspend fun updateBudget(budget: Budget)
    
    /**
     * Delete a budget
     */
    suspend fun deleteBudget(id: Long)
    
    /**
     * Update spent amount for a budget
     */
    suspend fun updateBudgetSpent(category: String, month: String, spent: Double)
    
    /**
     * Get total budget for a month
     */
    suspend fun getTotalBudgetForMonth(month: String): Double
    
    /**
     * Get total spent for a month
     */
    suspend fun getTotalSpentForMonth(month: String): Double
}
