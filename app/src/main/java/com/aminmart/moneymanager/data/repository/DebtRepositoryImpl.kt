package com.aminmart.moneymanager.data.repository

import com.aminmart.moneymanager.data.database.MoneyDatabase
import com.aminmart.moneymanager.domain.model.Debt
import com.aminmart.moneymanager.domain.repository.DebtRepository
import kotlinx.coroutines.flow.Flow

class DebtRepositoryImpl(
    private val db: MoneyDatabase
) : DebtRepository {

    override fun getAllDebts(): Flow<List<Debt>> {
        return db.getAllDebts()
    }

    override suspend fun getDebtById(id: Long): Debt? {
        return db.getDebtById(id)
    }

    override suspend fun insertDebt(debt: Debt): Long {
        return db.insertDebt(debt)
    }

    override suspend fun updateDebt(debt: Debt) {
        db.updateDebt(debt)
    }

    override suspend fun deleteDebt(id: Long) {
        db.deleteDebt(id)
    }

    override suspend fun getDebtsPage(limit: Int, offset: Int): List<Debt> {
        return db.getDebtsPage(limit, offset)
    }

    override suspend fun getDebtsCount(): Int {
        return db.getDebtsCount()
    }
}
