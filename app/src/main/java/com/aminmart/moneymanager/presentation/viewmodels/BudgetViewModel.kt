package com.aminmart.moneymanager.presentation.viewmodels

import com.aminmart.moneymanager.domain.model.Budget
import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.domain.usecase.DeleteBudgetUseCase
import com.aminmart.moneymanager.domain.usecase.GetAllBudgetsUseCase
import com.aminmart.moneymanager.domain.usecase.GetBudgetByCategoryUseCase
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for Budget screen
 */
class BudgetViewModel(
    private val getAllBudgetsUseCase: GetAllBudgetsUseCase,
    private val saveBudgetUseCase: SaveBudgetUseCase,
    private val deleteBudgetUseCase: DeleteBudgetUseCase,
    private val getBudgetByCategoryUseCase: GetBudgetByCategoryUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var loadJob: Job? = null

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    val budgets: StateFlow<List<Budget>> = _budgets.asStateFlow()

    private val _currentMonth = MutableStateFlow(getCurrentMonth())
    val currentMonth: StateFlow<String> = _currentMonth.asStateFlow()

    init {
        loadBudgets()
    }

    fun loadBudgets() {
        loadJob?.cancel()
        loadJob = scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                getAllBudgetsUseCase()
                    .flowOn(Dispatchers.IO)
                    .collect { list ->
                        val monthBudgets = list.filter { it.month == _currentMonth.value }
                        _budgets.value = monthBudgets
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
                    error = e.message ?: "Failed to load budgets"
                )
            }
        }
    }

    fun setMonth(month: String) {
        _currentMonth.value = month
        loadBudgets()
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
    val error: String? = null
)
