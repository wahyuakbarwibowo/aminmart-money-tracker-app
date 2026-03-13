package com.aminmart.moneymanager.presentation.viewmodels

import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.domain.usecase.AddTransactionUseCase
import com.aminmart.moneymanager.domain.usecase.DeleteTransactionUseCase
import com.aminmart.moneymanager.domain.usecase.GetAllTransactionsUseCase
import com.aminmart.moneymanager.domain.usecase.GetTransactionByIdUseCase
import com.aminmart.moneymanager.domain.usecase.UpdateTransactionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for Transactions screen
 */
class TransactionsViewModel(
    private val getAllTransactionsUseCase: GetAllTransactionsUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val getTransactionByIdUseCase: GetTransactionByIdUseCase
) {

    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _filterType = MutableStateFlow<Transaction.TransactionType?>(null)
    val filterType: StateFlow<Transaction.TransactionType?> = _filterType.asStateFlow()

    private val _filterCategory = MutableStateFlow<String?>(null)
    val filterCategory: StateFlow<String?> = _filterCategory.asStateFlow()

    init {
        loadTransactions()
    }

    fun loadTransactions() {
        _uiState.value = _uiState.value.copy(isLoading = true)

        getAllTransactionsUseCase().collectInScope { list ->
            var filtered = list

            // Apply type filter
            _filterType.value?.let { type ->
                filtered = filtered.filter { it.type == type }
            }

            // Apply category filter
            _filterCategory.value?.let { category ->
                filtered = filtered.filter { it.category == category }
            }

            _transactions.value = filtered
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = null
            )
        }
    }

    fun setFilterType(type: Transaction.TransactionType?) {
        _filterType.value = type
        loadTransactions()
    }

    fun setFilterCategory(category: String?) {
        _filterCategory.value = category
        loadTransactions()
    }

    fun clearFilters() {
        _filterType.value = null
        _filterCategory.value = null
        loadTransactions()
    }

    suspend fun addTransaction(transaction: Transaction): Long {
        return addTransactionUseCase(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        updateTransactionUseCase(transaction)
    }

    suspend fun deleteTransaction(id: Long) {
        deleteTransactionUseCase(id)
    }

    suspend fun getTransaction(id: Long): Transaction? {
        return getTransactionByIdUseCase(id)
    }

    fun getCategories(): List<String> {
        return _transactions.value.map { it.category }.distinct()
    }

    private inline fun <T> kotlinx.coroutines.flow.Flow<T>.collectInScope(crossinline action: suspend (T) -> Unit) {
        kotlinx.coroutines.runBlocking {
            collect { action(it) }
        }
    }
}

/**
 * UI State for Transactions
 */
data class TransactionsUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
