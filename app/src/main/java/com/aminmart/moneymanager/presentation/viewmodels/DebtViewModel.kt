package com.aminmart.moneymanager.presentation.viewmodels

import com.aminmart.moneymanager.domain.model.Debt
import com.aminmart.moneymanager.domain.usecase.DebtUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking

/**
 * ViewModel for Debts screen
 */
class DebtViewModel(
    private val debtUseCases: DebtUseCases
) {

    private val _uiState = MutableStateFlow(DebtUiState())
    val uiState: StateFlow<DebtUiState> = _uiState.asStateFlow()

    private val _debts = MutableStateFlow<List<Debt>>(emptyList())
    val debts: StateFlow<List<Debt>> = _debts.asStateFlow()

    init {
        loadDebts()
    }

    fun loadDebts() {
        _uiState.value = _uiState.value.copy(isLoading = true)

        debtUseCases.getAllDebts().collectInScope { debtList ->
            _debts.value = debtList
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = null
            )
        }
    }

    suspend fun addDebt(debt: Debt) {
        debtUseCases.addDebt(debt)
    }

    suspend fun updateDebt(debt: Debt) {
        debtUseCases.updateDebt(debt)
    }

    suspend fun deleteDebt(id: Long) {
        debtUseCases.deleteDebt(id)
    }

    suspend fun getDebt(id: Long): Debt? {
        return debtUseCases.getDebtById(id)
    }

    private inline fun <T> Flow<T>.collectInScope(crossinline action: suspend (T) -> Unit) {
        runBlocking {
            collect { action(it) }
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
