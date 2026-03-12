package com.aminmart.moneymanager.domain.usecase

import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.domain.repository.TransactionRepository

/**
 * Use case to add a transaction
 */
class AddTransactionUseCase(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction): Long {
        return repository.insertTransaction(transaction)
    }
}

/**
 * Use case to update a transaction
 */
class UpdateTransactionUseCase(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction) {
        repository.updateTransaction(transaction)
    }
}

/**
 * Use case to delete a transaction
 */
class DeleteTransactionUseCase(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(id: Long) {
        repository.deleteTransaction(id)
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
