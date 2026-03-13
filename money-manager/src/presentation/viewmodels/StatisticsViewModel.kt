package com.aminmart.moneymanager.presentation.viewmodels

import com.aminmart.moneymanager.domain.usecase.GetExpenseByCategoryUseCase
import com.aminmart.moneymanager.domain.usecase.GetMonthlyExpensesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for Statistics/Charts screen
 */
class StatisticsViewModel(
    private val getExpenseByCategoryUseCase: GetExpenseByCategoryUseCase,
    private val getMonthlyExpensesUseCase: GetMonthlyExpensesUseCase
) {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private val _categoryData = MutableStateFlow<Map<String, Double>>(emptyMap())
    val categoryData: StateFlow<Map<String, Double>> = _categoryData.asStateFlow()

    private val _monthlyData = MutableStateFlow<Map<String, Double>>(emptyMap())
    val monthlyData: StateFlow<Map<String, Double>> = _monthlyData.asStateFlow()

    private val _selectedPeriod = MutableStateFlow(StatisticsPeriod.CURRENT_MONTH)
    val selectedPeriod: StateFlow<StatisticsPeriod> = _selectedPeriod.asStateFlow()

    init {
        loadStatistics()
    }

    fun loadStatistics() {
        _uiState.value = _uiState.value.copy(isLoading = true)

        loadCategoryData()
        loadMonthlyData()
    }

    fun setPeriod(period: StatisticsPeriod) {
        _selectedPeriod.value = period
        loadStatistics()
    }

    private fun loadCategoryData() {
        val (startDate, endDate) = getPeriodDates(_selectedPeriod.value)

        getExpenseByCategoryUseCase(startDate, endDate).collectInScope { data ->
            _categoryData.value = data
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = null
            )
        }
    }

    private fun loadMonthlyData() {
        getMonthlyExpensesUseCase(12).collectInScope { data ->
            _monthlyData.value = data
        }
    }

    fun getTotalExpense(): Double {
        return _categoryData.value.values.sum()
    }

    fun getCategoryPercentage(category: String): Float {
        val total = getTotalExpense()
        return if (total > 0) (_categoryData.value[category] ?: 0.0) / total.toFloat() else 0f
    }

    private fun getPeriodDates(period: StatisticsPeriod): Pair<Long, Long> {
        val calendar = java.util.Calendar.getInstance()
        
        return when (period) {
            StatisticsPeriod.CURRENT_MONTH -> {
                val start = getMonthStart(calendar)
                val end = getMonthEnd(calendar)
                start to end
            }
            StatisticsPeriod.LAST_MONTH -> {
                calendar.add(java.util.Calendar.MONTH, -1)
                val start = getMonthStart(calendar)
                val end = getMonthEnd(calendar)
                start to end
            }
            StatisticsPeriod.LAST_3_MONTHS -> {
                calendar.add(java.util.Calendar.MONTH, -3)
                val start = getMonthStart(calendar)
                val end = getMonthEnd(java.util.Calendar.getInstance())
                start to end
            }
            StatisticsPeriod.LAST_6_MONTHS -> {
                calendar.add(java.util.Calendar.MONTH, -6)
                val start = getMonthStart(calendar)
                val end = getMonthEnd(java.util.Calendar.getInstance())
                start to end
            }
            StatisticsPeriod.CURRENT_YEAR -> {
                calendar.set(java.util.Calendar.MONTH, java.util.Calendar.JANUARY)
                calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
                val start = calendar.timeInMillis
                val end = getMonthEnd(java.util.Calendar.getInstance())
                start to end
            }
        }
    }

    private fun getMonthStart(calendar: java.util.Calendar): Long {
        val cal = calendar.clone() as java.util.Calendar
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getMonthEnd(calendar: java.util.Calendar): Long {
        val cal = calendar.clone() as java.util.Calendar
        cal.set(java.util.Calendar.DAY_OF_MONTH, cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        cal.set(java.util.Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    private inline fun <T> kotlinx.coroutines.flow.Flow<T>.collectInScope(crossinline action: suspend (T) -> Unit) {
        kotlinx.coroutines.runBlocking {
            collect { action(it) }
        }
    }
}

/**
 * Statistics period options
 */
enum class StatisticsPeriod {
    CURRENT_MONTH,
    LAST_MONTH,
    LAST_3_MONTHS,
    LAST_6_MONTHS,
    CURRENT_YEAR
}

/**
 * UI State for Statistics
 */
data class StatisticsUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
