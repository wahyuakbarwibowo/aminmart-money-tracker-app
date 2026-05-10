package com.aminmart.moneymanager.domain.usecase

import com.aminmart.moneymanager.domain.model.DashboardStats
import com.aminmart.moneymanager.domain.repository.TransactionRepository
import com.aminmart.moneymanager.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Use case to get dashboard statistics
 */
class GetDashboardStatsUseCase(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository
) {
    
    operator fun invoke(): Flow<DashboardStats> = flow {
        val now = System.currentTimeMillis()
        val currentMonthStart = getMonthStart(now)
        val currentMonthEnd = getMonthEnd(now)
        
        val allTransactions = transactionRepository.getAllTransactions()
        
        allTransactions.collect { transactions ->
            val totalIncome = transactions
                .filter { it.type == com.aminmart.moneymanager.domain.model.Transaction.TransactionType.INCOME }
                .sumOf { it.amount }
            
            val totalExpense = transactions
                .filter { it.type == com.aminmart.moneymanager.domain.model.Transaction.TransactionType.EXPENSE }
                .sumOf { it.amount }
            
            val monthIncome = transactions
                .filter { 
                    it.type == com.aminmart.moneymanager.domain.model.Transaction.TransactionType.INCOME &&
                    it.date >= currentMonthStart && it.date <= currentMonthEnd
                }
                .sumOf { it.amount }
            
            val monthExpense = transactions
                .filter { 
                    it.type == com.aminmart.moneymanager.domain.model.Transaction.TransactionType.EXPENSE &&
                    it.date >= currentMonthStart && it.date <= currentMonthEnd
                }
                .sumOf { it.amount }
            
            emit(DashboardStats(
                totalBalance = totalIncome - totalExpense,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                transactionCount = transactions.size,
                monthIncome = monthIncome,
                monthExpense = monthExpense
            ))
        }
    }
    
    private fun getMonthStart(timestamp: Long): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    private fun getMonthEnd(timestamp: Long): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(java.util.Calendar.DAY_OF_MONTH, calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        calendar.set(java.util.Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
}
