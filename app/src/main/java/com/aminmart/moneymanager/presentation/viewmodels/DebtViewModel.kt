package com.aminmart.moneymanager.presentation.viewmodels

import com.aminmart.moneymanager.domain.model.Debt
import com.aminmart.moneymanager.domain.usecase.DebtUseCases
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for Debts screen
 */
class DebtViewModel(
    private val debtUseCases: DebtUseCases
) : ViewModel() {
    private var currentOffset = 0
    private var totalCount = 0
    private val pageSize = 30

    private val _uiState = MutableStateFlow(DebtUiState())
    val uiState: StateFlow<DebtUiState> = _uiState.asStateFlow()

    private val _debts = MutableStateFlow<List<Debt>>(emptyList())
    val debts: StateFlow<List<Debt>> = _debts.asStateFlow()

    init {
        loadInitialDebts()
    }

    fun loadInitialDebts() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            totalCount = withContext(Dispatchers.IO) { debtUseCases.getDebtsCount() }
            val firstPage = withContext(Dispatchers.IO) { debtUseCases.getDebtsPage(pageSize, 0) }
            _debts.value = firstPage
            currentOffset = firstPage.size
            _uiState.value = _uiState.value.copy(isLoading = false, isLoadingMore = false)
        }
    }

    fun loadNextPage() {
        if (_uiState.value.isLoading || _uiState.value.isLoadingMore || currentOffset >= totalCount) return
        _uiState.value = _uiState.value.copy(isLoadingMore = true)
        viewModelScope.launch {
            val page = withContext(Dispatchers.IO) { debtUseCases.getDebtsPage(pageSize, currentOffset) }
            if (page.isNotEmpty()) {
                _debts.value = _debts.value + page
                currentOffset += page.size
            }
            _uiState.value = _uiState.value.copy(isLoadingMore = false)
        }
    }

    fun hasMoreData(): Boolean = currentOffset < totalCount

    fun loadDebts() {
        loadInitialDebts()
    }

    fun refreshDebtsAfterMutation() {
        viewModelScope.launch {
            totalCount = withContext(Dispatchers.IO) { debtUseCases.getDebtsCount() }
            val page = withContext(Dispatchers.IO) { debtUseCases.getDebtsPage(currentOffset.coerceAtLeast(pageSize), 0) }
            _debts.value = page
            currentOffset = page.size
            if (currentOffset > totalCount) currentOffset = totalCount
            _uiState.value = _uiState.value.copy(isLoading = false, isLoadingMore = false)
        }
    }

    fun deleteDebt(debt: Debt) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { debtUseCases.deleteDebt(debt.id) }
            refreshDebtsAfterMutation()
        }
    }

    fun deleteDebtById(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { debtUseCases.deleteDebt(id) }
            refreshDebtsAfterMutation()
        }
    }

    suspend fun getDebt(id: Long): Debt? {
        return withContext(Dispatchers.IO) { debtUseCases.getDebtById(id) }
    }

    fun addDebt(debt: Debt) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { debtUseCases.addDebt(debt) }
            refreshDebtsAfterMutation()
        }
    }

    fun updateDebt(debt: Debt) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { debtUseCases.updateDebt(debt) }
            refreshDebtsAfterMutation()
        }
    }
}

/**
 * UI State for Debts
 */
data class DebtUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null
)
