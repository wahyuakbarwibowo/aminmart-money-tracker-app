package com.aminmart.moneymanager.presentation.viewmodels

import com.aminmart.moneymanager.domain.model.DashboardStats
import com.aminmart.moneymanager.domain.model.Debt
import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.domain.usecase.DebtUseCases
import com.aminmart.moneymanager.domain.usecase.GetDashboardStatsUseCase
import com.aminmart.moneymanager.domain.usecase.GetRecentTransactionsUseCase
import com.aminmart.moneymanager.domain.usecase.DeleteTransactionUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * ViewModel for Dashboard screen
 */
class DashboardViewModel(
    private val getDashboardStatsUseCase: GetDashboardStatsUseCase,
    private val getRecentTransactionsUseCase: GetRecentTransactionsUseCase,
    private val debtUseCases: DebtUseCases,
    private val deleteTransactionUseCase: DeleteTransactionUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var loadJob: Job? = null

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _stats = MutableStateFlow<DashboardStats?>(null)
    val stats: StateFlow<DashboardStats?> = _stats.asStateFlow()

    private val _recentTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val recentTransactions: StateFlow<List<Transaction>> = _recentTransactions.asStateFlow()

    private val _debtSummary = MutableStateFlow(DebtSummary())
    val debtSummary: StateFlow<DebtSummary> = _debtSummary.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        loadJob?.cancel()
        loadJob = scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                coroutineScope {
                    launch {
                        getDashboardStatsUseCase()
                            .flowOn(Dispatchers.IO)
                            .collect { stats ->
                                _stats.value = stats
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = null
                                )
                            }
                    }

                    launch {
                        getRecentTransactionsUseCase(10)
                            .flowOn(Dispatchers.IO)
                            .collect { transactions ->
                                _recentTransactions.value = transactions
                            }
                    }

                    launch {
                        debtUseCases.getAllDebts()
                            .flowOn(Dispatchers.IO)
                            .collect { debts ->
                                val totalDebt = debts
                                    .filter { it.type == Debt.DebtType.DEBT && !it.isPaid }
                                    .sumOf { it.amount }
                                val totalCredit = debts
                                    .filter { it.type == Debt.DebtType.CREDIT && !it.isPaid }
                                    .sumOf { it.amount }
                                _debtSummary.value = DebtSummary(totalDebt, totalCredit)
                            }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load dashboard data"
                )
            }
        }
    }

    fun refresh() {
        loadDashboardData()
    }

    fun clear() {
        scope.cancel()
    }

    fun deleteTransaction(transactionId: Long) {
        scope.launch {
            deleteTransactionUseCase(transactionId)
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

/**
 * Data class for Debt Summary
 */
data class DebtSummary(
    val totalDebt: Double = 0.0,
    val totalCredit: Double = 0.0
)
