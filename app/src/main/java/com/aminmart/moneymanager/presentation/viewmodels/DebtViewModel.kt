package com.aminmart.moneymanager.presentation.viewmodels

import com.aminmart.moneymanager.domain.model.Debt
import com.aminmart.moneymanager.domain.usecase.DebtUseCases
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for Debts screen
 */
class DebtViewModel(
    private val debtUseCases: DebtUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(DebtUiState())
    val uiState: StateFlow<DebtUiState> = _uiState.asStateFlow()

    private val _debts = MutableStateFlow<List<Debt>>(emptyList())
    val debts: StateFlow<List<Debt>> = _debts.asStateFlow()

    init {
        loadDebts()
    }

    fun loadDebts() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            debtUseCases.getAllDebts().flowOn(Dispatchers.IO).collect { debtList ->
                _debts.value = debtList
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun deleteDebt(debt: Debt) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { debtUseCases.deleteDebt(debt.id) }
            loadDebts()
        }
    }

    fun deleteDebtById(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { debtUseCases.deleteDebt(id) }
            loadDebts()
        }
    }

    suspend fun getDebt(id: Long): Debt? {
        return withContext(Dispatchers.IO) { debtUseCases.getDebtById(id) }
    }

    fun addDebt(debt: Debt) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { debtUseCases.addDebt(debt) }
            loadDebts()
        }
    }

    fun updateDebt(debt: Debt) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { debtUseCases.updateDebt(debt) }
            loadDebts()
        }
    }
}

/**
 * UI State for Debts
 */
data class DebtUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
