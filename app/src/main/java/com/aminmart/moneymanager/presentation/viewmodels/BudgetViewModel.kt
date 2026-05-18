package com.aminmart.moneymanager.presentation.viewmodels

import com.aminmart.moneymanager.domain.model.Budget
import com.aminmart.moneymanager.domain.usecase.DeleteBudgetUseCase
import com.aminmart.moneymanager.domain.usecase.GetBudgetByCategoryUseCase
import com.aminmart.moneymanager.domain.usecase.GetBudgetsCountUseCase
import com.aminmart.moneymanager.domain.usecase.GetBudgetsPageUseCase
import com.aminmart.moneymanager.domain.usecase.SaveBudgetUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for Budget screen
 */
class BudgetViewModel(
    private val getBudgetsPageUseCase: GetBudgetsPageUseCase,
    private val getBudgetsCountUseCase: GetBudgetsCountUseCase,
    private val saveBudgetUseCase: SaveBudgetUseCase,
    private val deleteBudgetUseCase: DeleteBudgetUseCase,
    private val getBudgetByCategoryUseCase: GetBudgetByCategoryUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var loadJob: Job? = null
    private var currentOffset = 0
    private var totalCount = 0
    private val pageSize = 30

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    val budgets: StateFlow<List<Budget>> = _budgets.asStateFlow()

    private val _currentMonth = MutableStateFlow(getCurrentMonth())
    val currentMonth: StateFlow<String> = _currentMonth.asStateFlow()

    init {
        loadInitialBudgets()
    }

    fun loadInitialBudgets() {
        loadJob?.cancel()
        loadJob = scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                currentOffset = 0
                totalCount = withContext(Dispatchers.IO) {
                    getBudgetsCountUseCase(_currentMonth.value)
                }
                val firstPage = withContext(Dispatchers.IO) {
                    getBudgetsPageUseCase(_currentMonth.value, pageSize, 0)
                }
                _budgets.value = firstPage
                currentOffset = firstPage.size
                _uiState.value = _uiState.value.copy(isLoading = false, error = null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load budgets"
                )
            }
        }
    }

    fun loadNextPage() {
        if (_uiState.value.isLoading || _uiState.value.isLoadingMore || currentOffset >= totalCount) return
        scope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            try {
                val page = withContext(Dispatchers.IO) {
                    getBudgetsPageUseCase(_currentMonth.value, pageSize, currentOffset)
                }
                if (page.isNotEmpty()) {
                    _budgets.value = _budgets.value + page
                    currentOffset += page.size
                }
                _uiState.value = _uiState.value.copy(isLoadingMore = false, error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    error = e.message ?: "Failed to load more budgets"
                )
            }
        }
    }

    fun hasMoreData(): Boolean = currentOffset < totalCount

    fun setMonth(month: String) {
        _currentMonth.value = month
        loadInitialBudgets()
    }

    suspend fun saveBudget(budget: Budget): Long {
        return withContext(Dispatchers.IO) { saveBudgetUseCase(budget) }
    }

    suspend fun deleteBudget(id: Long) {
        withContext(Dispatchers.IO) { deleteBudgetUseCase(id) }
    }

    suspend fun getBudgetForCategory(category: String): Budget? {
        return withContext(Dispatchers.IO) {
            getBudgetByCategoryUseCase(category, _currentMonth.value)
        }
    }

    fun getTotalBudget(): Double {
        return _budgets.value.sumOf { it.monthlyBudget }
    }

    fun getTotalSpent(): Double {
        return _budgets.value.sumOf { it.spent }
    }

    fun getRemainingBudget(): Double {
        return getTotalBudget() - getTotalSpent()
    }

    fun getBudgetProgress(): Float {
        val total = getTotalBudget()
        return if (total > 0) (getTotalSpent() / total).toFloat() else 0f
    }

    private fun getCurrentMonth(): String {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        return String.format("%04d-%02d", year, month)
    }

    fun clear() {
        scope.cancel()
    }
}

/**
 * UI State for Budget
 */
data class BudgetUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null
)
