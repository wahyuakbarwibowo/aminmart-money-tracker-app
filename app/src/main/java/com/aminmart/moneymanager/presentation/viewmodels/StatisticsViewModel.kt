package com.aminmart.moneymanager.presentation.viewmodels

import com.aminmart.moneymanager.domain.usecase.GetExpenseByCategoryUseCase
import com.aminmart.moneymanager.domain.usecase.GetMonthlyExpensesUseCase
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
import kotlin.math.max

/**
 * ViewModel for Statistics/Charts screen
 */
class StatisticsViewModel(
    private val getExpenseByCategoryUseCase: GetExpenseByCategoryUseCase,
    private val getMonthlyExpensesUseCase: GetMonthlyExpensesUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var loadJob: Job? = null


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
        loadJob?.cancel()
        loadJob = scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val (startDate, endDate) = getPeriodDates(_selectedPeriod.value)
                coroutineScope {
                    launch {
                        getExpenseByCategoryUseCase(startDate, endDate)
                            .flowOn(Dispatchers.IO)
                            .collect { data ->
                                _categoryData.value = data
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = null
                                )
                            }
                    }

                    launch {
                        getMonthlyExpensesUseCase(getTrendMonthCount(_selectedPeriod.value))
                            .flowOn(Dispatchers.IO)
                            .collect { data ->
                                _monthlyData.value = data
                            }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load statistics"
                )
            }
        }
    }

    fun setPeriod(period: StatisticsPeriod) {
        _selectedPeriod.value = period
        loadStatistics()
    }

    fun getTotalExpense(): Double {
        return _categoryData.value.values.sum()
    }

    fun getCategoryPercentage(category: String): Float {
        val total = getTotalExpense()
        return if (total > 0) ((_categoryData.value[category] ?: 0.0) / total).toFloat() else 0f
    }

    fun getTopCategory(): Pair<String, Double>? {
        return _categoryData.value.maxByOrNull { it.value }?.toPair()
    }

    fun getCategoryCount(): Int {
        return _categoryData.value.size
    }

    fun getAverageMonthlyExpense(): Double {
        if (_monthlyData.value.isEmpty()) {
            return 0.0
        }
        return _monthlyData.value.values.sum() / max(_monthlyData.value.size, 1)
    }

    fun getPeakMonth(): Pair<String, Double>? {
        return _monthlyData.value.maxByOrNull { it.value }?.toPair()
    }

    fun getSelectedPeriodLabel(): String {
        return when (_selectedPeriod.value) {
            StatisticsPeriod.CURRENT_MONTH -> "Current Month"
            StatisticsPeriod.LAST_MONTH -> "Last Month"
            StatisticsPeriod.LAST_3_MONTHS -> "Last 3 Months"
            StatisticsPeriod.LAST_6_MONTHS -> "Last 6 Months"
            StatisticsPeriod.CURRENT_YEAR -> "Current Year"
        }
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

    private fun getTrendMonthCount(period: StatisticsPeriod): Int {
        return when (period) {
            StatisticsPeriod.CURRENT_MONTH -> 4
            StatisticsPeriod.LAST_MONTH -> 4
            StatisticsPeriod.LAST_3_MONTHS -> 3
            StatisticsPeriod.LAST_6_MONTHS -> 6
            StatisticsPeriod.CURRENT_YEAR -> 12
        }
    }

    fun clear() {
        scope.cancel()
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
