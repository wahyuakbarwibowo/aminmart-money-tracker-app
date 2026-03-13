package com.aminmart.moneymanager.presentation.activities

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.aminmart.moneymanager.MoneyManagerApplication
import com.aminmart.moneymanager.R
import com.aminmart.moneymanager.presentation.viewmodels.StatisticsPeriod
import com.aminmart.moneymanager.presentation.viewmodels.StatisticsViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.NumberFormat
import java.util.Locale

/**
 * Statistics Activity - Display charts and graphs
 */
class StatisticsActivity : AppCompatActivity() {

    private lateinit var app: MoneyManagerApplication
    private lateinit var viewModel: StatisticsViewModel

    private lateinit var toolbar: Toolbar
    private lateinit var spinnerPeriod: Spinner
    private lateinit var chartPie: PieChart
    private lateinit var chartBar: BarChart
    private lateinit var textTotalExpense: TextView
    private lateinit var viewEmpty: View

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        app = application as MoneyManagerApplication
        viewModel = StatisticsViewModel(
            app.getExpenseByCategoryUseCase,
            app.getMonthlyExpensesUseCase
        )

        initViews()
        setupCharts()
        observeData()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar_statistics)
        spinnerPeriod = findViewById(R.id.spinner_statistics_period)
        chartPie = findViewById(R.id.chart_statistics_pie)
        chartBar = findViewById(R.id.chart_statistics_bar)
        textTotalExpense = findViewById(R.id.text_statistics_total_expense)
        viewEmpty = findViewById(R.id.view_statistics_empty)

        setSupportActionBar(toolbar)
        supportActionBar?.title = "Statistics"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupPeriodSpinner()
    }

    private fun setupPeriodSpinner() {
        val periods = arrayOf(
            "Current Month",
            "Last Month",
            "Last 3 Months",
            "Last 6 Months",
            "Current Year"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, periods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPeriod.adapter = adapter

        spinnerPeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val period = when (position) {
                    0 -> StatisticsPeriod.CURRENT_MONTH
                    1 -> StatisticsPeriod.LAST_MONTH
                    2 -> StatisticsPeriod.LAST_3_MONTHS
                    3 -> StatisticsPeriod.LAST_6_MONTHS
                    4 -> StatisticsPeriod.CURRENT_YEAR
                    else -> StatisticsPeriod.CURRENT_MONTH
                }
                viewModel.setPeriod(period)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupCharts() {
        // Setup Pie Chart
        chartPie.description = Description().apply { text = "" }
        chartPie.setUsePercentages(true)
        chartPie.isDrawHoleEnabled = true
        chartPie.setHoleColor(Color.WHITE)
        chartPie.setTransparentCircleRadius(58f)
        chartPie.legend.apply {
            orientation = Legend.LegendOrientation.VERTICAL
            horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
            verticalAlignment = Legend.LegendVerticalAlignment.CENTER
        }

        // Setup Bar Chart
        chartBar.description = Description().apply { text = "" }
        chartBar.setPinchZoom(false)
        chartBar.setDrawGridBackground(false)
        chartBar.xAxis.setDrawGridLines(false)
        chartBar.axisLeft.setDrawGridLines(true)
        chartBar.axisRight.isEnabled = false
        chartBar.legend.apply {
            orientation = Legend.LegendOrientation.HORIZONTAL
            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        }
    }

    private fun observeData() {
        viewModel.categoryData.collectInScope { data ->
            if (data.isEmpty()) {
                viewEmpty.visibility = View.VISIBLE
                chartPie.visibility = View.GONE
            } else {
                viewEmpty.visibility = View.GONE
                chartPie.visibility = View.VISIBLE
                updatePieChart(data)
            }
            textTotalExpense.text = "Total: ${currencyFormat.format(viewModel.getTotalExpense())}"
        }

        viewModel.monthlyData.collectInScope { data ->
            if (data.isEmpty()) {
                chartBar.visibility = View.GONE
            } else {
                chartBar.visibility = View.VISIBLE
                updateBarChart(data)
            }
        }

        viewModel.uiState.collectInScope { state ->
            // Handle loading state if needed
        }
    }

    private fun updatePieChart(data: Map<String, Double>) {
        val entries = data.map { (category, amount) ->
            PieEntry(amount.toFloat(), category)
        }

        val dataSet = PieDataSet(entries, "Expense by Category").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 12f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "${value.toInt()}%"
                }
            }
        }

        chartPie.data = PieData(dataSet).apply {
            setValueFormatter(object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "${value.toInt()}%"
                }
            })
        }
        chartPie.invalidate()
    }

    private fun updateBarChart(data: Map<String, Double>) {
        val entries = data.mapIndexed { index, (month, amount) ->
            BarEntry(index.toFloat(), amount.toFloat())
        }

        val dataSet = BarDataSet(entries, "Monthly Expense").apply {
            colors = listOf(ColorTemplate.getHoloBlue())
            valueTextSize = 10f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return currencyFormat.format(value.toDouble()).substring(0, 5)
                }
            }
        }

        chartBar.data = BarData(dataSet).apply {
            barWidth = 0.8f
        }

        // Set month labels
        chartBar.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index < data.keys.size) data.keys.elementAt(index) else ""
            }
        }

        chartBar.invalidate()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadStatistics()
    }

    private inline fun <T> kotlinx.coroutines.flow.Flow<T>.collectInScope(crossinline action: suspend (T) -> Unit) {
        kotlinx.coroutines.runBlocking {
            collect { action(it) }
        }
    }
}
