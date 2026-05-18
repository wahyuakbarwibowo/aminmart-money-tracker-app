package com.aminmart.moneymanager.domain.usecase

import com.aminmart.moneymanager.domain.model.Debt
import com.aminmart.moneymanager.domain.repository.DebtRepository
import kotlinx.coroutines.flow.Flow

data class DebtUseCases(
    val getAllDebts: GetAllDebts,
    val getDebtsPage: GetDebtsPage,
    val getDebtsCount: GetDebtsCount,
    val getDebtById: GetDebtById,
    val addDebt: AddDebt,
    val updateDebt: UpdateDebt,
    val deleteDebt: DeleteDebt
)

class GetAllDebts(private val repository: DebtRepository) {
    operator fun invoke(): Flow<List<Debt>> {
        return repository.getAllDebts()
    }
}

class GetDebtById(private val repository: DebtRepository) {
    suspend operator fun invoke(id: Long): Debt? {
        return repository.getDebtById(id)
    }
}

class AddDebt(private val repository: DebtRepository) {
    suspend operator fun invoke(debt: Debt) {
        repository.insertDebt(debt)
    }
}

class UpdateDebt(private val repository: DebtRepository) {
    suspend operator fun invoke(debt: Debt) {
        repository.updateDebt(debt)
    }
}

class DeleteDebt(private val repository: DebtRepository) {
    suspend operator fun invoke(id: Long) {
        repository.deleteDebt(id)
    }
}

class GetDebtsPage(private val repository: DebtRepository) {
    suspend operator fun invoke(limit: Int, offset: Int): List<Debt> {
        return repository.getDebtsPage(limit, offset)
    }
}

class GetDebtsCount(private val repository: DebtRepository) {
    suspend operator fun invoke(): Int {
        return repository.getDebtsCount()
    }
}
