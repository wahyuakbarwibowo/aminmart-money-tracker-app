package com.aminmart.moneymanager.domain.repository

import com.aminmart.moneymanager.domain.model.Debt
import kotlinx.coroutines.flow.Flow

interface DebtRepository {

    fun getAllDebts(): Flow<List<Debt>>

    suspend fun getDebtById(id: Long): Debt?

    suspend fun insertDebt(debt: Debt): Long

    suspend fun updateDebt(debt: Debt)

    suspend fun deleteDebt(id: Long)

    suspend fun getDebtsPage(limit: Int, offset: Int): List<Debt>

    suspend fun getDebtsCount(): Int
}
