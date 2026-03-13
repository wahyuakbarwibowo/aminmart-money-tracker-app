package com.aminmart.moneymanager.presentation.viewmodels

import com.aminmart.moneymanager.domain.model.DashboardStats
import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.domain.usecase.GetDashboardStatsUseCase
import com.aminmart.moneymanager.domain.usecase.GetRecentTransactionsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

/**
 * ViewModel for Dashboard screen
 */
class DashboardViewModel(
    private val getDashboardStatsUseCase: GetDashboardStatsUseCase,
    private val getRecentTransactionsUseCase: GetRecentTransactionsUseCase
) {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _stats = MutableStateFlow<DashboardStats?>(null)
    val stats: StateFlow<DashboardStats?> = _stats.asStateFlow()

    private val _recentTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val recentTransactions: StateFlow<List<Transaction>> = _recentTransactions.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        _uiState.value = _uiState.value.copy(isLoading = true)

        // Combine stats and recent transactions
        combineStatsAndTransactions()
    }

    private fun combineStatsAndTransactions() {
        // Observe dashboard stats
        getDashboardStatsUseCase().collectInScope { stats ->
            _stats.value = stats
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = null
            )
        }

        // Observe recent transactions
        getRecentTransactionsUseCase(10).collectInScope { transactions ->
            _recentTransactions.value = transactions
        }
    }

    fun refresh() {
        loadDashboardData()
    }

    private inline fun <T> Flow<T>.collectInScope(crossinline action: suspend (T) -> Unit) {
        // Simplified collection - in production use viewModelScope
        kotlinx.coroutines.runBlocking {
            collect { action(it) }
        }
    }
}

/**
 * UI State for Dashboard
 */
data class DashboardUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
