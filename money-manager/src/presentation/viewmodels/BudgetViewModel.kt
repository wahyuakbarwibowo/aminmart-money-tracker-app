package com.aminmart.moneymanager.presentation.viewmodels

import com.aminmart.moneymanager.domain.model.Budget
import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.domain.usecase.DeleteBudgetUseCase
import com.aminmart.moneymanager.domain.usecase.GetAllBudgetsUseCase
import com.aminmart.moneymanager.domain.usecase.GetBudgetByCategoryUseCase
import com.aminmart.moneymanager.domain.usecase.SaveBudgetUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for Budget screen
 */
class BudgetViewModel(
    private val getAllBudgetsUseCase: GetAllBudgetsUseCase,
    private val saveBudgetUseCase: SaveBudgetUseCase,
    private val deleteBudgetUseCase: DeleteBudgetUseCase,
    private val getBudgetByCategoryUseCase: GetBudgetByCategoryUseCase
) {

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
        _uiState.value = _uiState.value.copy(isLoading = true)

        getAllBudgetsUseCase().collectInScope { list ->
            val monthBudgets = list.filter { it.month == _currentMonth.value }
            _budgets.value = monthBudgets
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = null
            )
        }
    }

    fun setMonth(month: String) {
        _currentMonth.value = month
        loadBudgets()
    }

    suspend fun saveBudget(budget: Budget): Long {
        return saveBudgetUseCase(budget)
    }

    suspend fun deleteBudget(id: Long) {
        deleteBudgetUseCase(id)
    }

    suspend fun getBudgetForCategory(category: String): Budget? {
        return getBudgetByCategoryUseCase(category, _currentMonth.value)
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

    private inline fun <T> kotlinx.coroutines.flow.Flow<T>.collectInScope(crossinline action: suspend (T) -> Unit) {
        kotlinx.coroutines.runBlocking {
            collect { action(it) }
        }
    }
}

/**
 * UI State for Budget
 */
data class BudgetUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
