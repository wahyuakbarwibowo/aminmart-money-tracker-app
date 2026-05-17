package com.aminmart.moneymanager.presentation.viewmodels

import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.domain.usecase.AddTransactionUseCase
import com.aminmart.moneymanager.domain.usecase.DeleteTransactionUseCase
import com.aminmart.moneymanager.domain.usecase.GetAllTransactionsUseCase
import com.aminmart.moneymanager.domain.usecase.GetTransactionByIdUseCase
import com.aminmart.moneymanager.domain.usecase.UpdateTransactionUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var loadJob: Job? = null

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
        loadJob?.cancel()
        loadJob = scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                getAllTransactionsUseCase()
                    .flowOn(Dispatchers.IO)
                    .collect { list ->
                        var filtered = list

                        _filterType.value?.let { type ->
                            filtered = filtered.filter { it.type == type }
                        }

                        _filterCategory.value?.let { category ->
                            filtered = filtered.filter { it.category == category }
                        }

                        _transactions.value = filtered
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = null
                        )
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load transactions"
                )
            }
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
        return withContext(Dispatchers.IO) { addTransactionUseCase(transaction) }
    }

    suspend fun updateTransaction(transaction: Transaction) {
        withContext(Dispatchers.IO) { updateTransactionUseCase(transaction) }
    }

    suspend fun deleteTransaction(id: Long) {
        withContext(Dispatchers.IO) { deleteTransactionUseCase(id) }
    }

    suspend fun getTransaction(id: Long): Transaction? {
        return withContext(Dispatchers.IO) { getTransactionByIdUseCase(id) }
    }

    fun getCategories(): List<String> {
        return _transactions.value.map { it.category }.distinct()
    }

    fun clear() {
        scope.cancel()
    }
}

/**
 * UI State for Transactions
 */
data class TransactionsUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
