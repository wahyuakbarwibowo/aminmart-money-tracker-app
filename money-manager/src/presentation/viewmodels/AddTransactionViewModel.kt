package com.aminmart.moneymanager.presentation.viewmodels

import com.aminmart.moneymanager.domain.model.Category
import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.domain.usecase.GetTransactionByIdUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for Add/Edit Transaction screen
 */
class AddTransactionViewModel(
    private val getTransactionByIdUseCase: GetTransactionByIdUseCase
) {

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    private val _transactionType = MutableStateFlow(Transaction.TransactionType.EXPENSE)
    val transactionType: StateFlow<Transaction.TransactionType> = _transactionType.asStateFlow()

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _category = MutableStateFlow<String?>(null)
    val category: StateFlow<String?> = _category.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _date = MutableStateFlow(System.currentTimeMillis())
    val date: StateFlow<Long> = _date.asStateFlow()

    private var editingTransactionId: Long? = null

    val incomeCategories = Category.DEFAULT_INCOME_CATEGORIES.map { it.name }
    val expenseCategories = Category.DEFAULT_EXPENSE_CATEGORIES.map { it.name }

    val currentCategories: List<String>
        get() = if (_transactionType.value == Transaction.TransactionType.INCOME) {
            incomeCategories
        } else {
            expenseCategories
        }

    init {
        // Set default category
        _category.value = currentCategories.firstOrNull()
    }

    fun setTransactionType(type: Transaction.TransactionType) {
        _transactionType.value = type
        // Reset category to first in the new type's list
        _category.value = if (type == Transaction.TransactionType.INCOME) {
            incomeCategories.firstOrNull()
        } else {
            expenseCategories.firstOrNull()
        }
    }

    fun setAmount(amount: String) {
        _amount.value = amount
    }

    fun setCategory(category: String) {
        _category.value = category
    }

    fun setDescription(description: String) {
        _description.value = description
    }

    fun setDate(date: Long) {
        _date.value = date
    }

    fun editTransaction(transactionId: Long) {
        editingTransactionId = transactionId
        _uiState.value = _uiState.value.copy(isLoading = true)

        getTransactionByIdUseCase(transactionId).collectInScope { transaction ->
            transaction?.let {
                _transactionType.value = it.type
                _amount.value = it.amount.toString()
                _category.value = it.category
                _description.value = it.description
                _date.value = it.date
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun buildTransaction(): Transaction? {
        val amountValue = _amount.value.toDoubleOrNull() ?: return null
        if (amountValue <= 0) return null
        if (_category.value == null) return null

        return Transaction(
            id = editingTransactionId ?: 0,
            type = _transactionType.value,
            amount = amountValue,
            category = _category.value!!,
            description = _description.value,
            date = _date.value
        )
    }

    fun isFormValid(): Boolean {
        val amountValue = _amount.value.toDoubleOrNull() ?: return false
        return amountValue > 0 && _category.value != null
    }

    fun clearForm() {
        _transactionType.value = Transaction.TransactionType.EXPENSE
        _amount.value = ""
        _category.value = expenseCategories.firstOrNull()
        _description.value = ""
        _date.value = System.currentTimeMillis()
        editingTransactionId = null
    }

    private inline fun <T> kotlinx.coroutines.flow.Flow<T?>.collectInScope(crossinline action: suspend (T?) -> Unit) {
        kotlinx.coroutines.runBlocking {
            collect { action(it) }
        }
    }
}

/**
 * UI State for Add Transaction
 */
data class AddTransactionUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
