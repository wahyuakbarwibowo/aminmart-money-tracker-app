package com.aminmart.moneymanager.presentation.activities

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aminmart.moneymanager.MoneyManagerApplication
import com.aminmart.moneymanager.R
import com.aminmart.moneymanager.domain.model.Budget
import com.aminmart.moneymanager.domain.model.Category
import com.aminmart.moneymanager.presentation.adapters.BudgetAdapter
import com.aminmart.moneymanager.presentation.viewmodels.BudgetViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.text.NumberFormat
import java.util.Locale

/**
 * Budget Activity - Manage monthly budgets
 */
class BudgetActivity : AppCompatActivity() {

    private lateinit var app: MoneyManagerApplication
    private lateinit var viewModel: BudgetViewModel

    private lateinit var toolbar: Toolbar
    private lateinit var textMonth: TextView
    private lateinit var progressTotal: LinearProgressIndicator
    private lateinit var textTotalBudget: TextView
    private lateinit var textTotalSpent: TextView
    private lateinit var textRemaining: TextView
    private lateinit var recyclerBudgets: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var viewEmpty: View

    private lateinit var adapter: BudgetAdapter
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget)

        app = application as MoneyManagerApplication
        viewModel = BudgetViewModel(
            app.getAllBudgetsUseCase,
            app.saveBudgetUseCase,
            app.deleteBudgetUseCase,
            app.getBudgetByCategoryUseCase
        )

        initViews()
        setupRecyclerView()
        observeData()

        fabAdd.setOnClickListener {
            showAddBudgetDialog()
        }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar_budget)
        textMonth = findViewById(R.id.text_budget_month)
        progressTotal = findViewById(R.id.progress_budget_total)
        textTotalBudget = findViewById(R.id.text_budget_total_amount)
        textTotalSpent = findViewById(R.id.text_budget_total_spent)
        textRemaining = findViewById(R.id.text_budget_total_remaining)
        recyclerBudgets = findViewById(R.id.recycler_budgets)
        fabAdd = findViewById(R.id.fab_budget_add)
        viewEmpty = findViewById(R.id.view_budget_empty)

        setSupportActionBar(toolbar)
        supportActionBar?.title = "Budget Planner"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        textMonth.setOnClickListener {
            showMonthPicker()
        }
    }

    private fun setupRecyclerView() {
        adapter = BudgetAdapter(
            onItemClick = { budget ->
                showBudgetDetail(budget)
            },
            onEditClick = { budget ->
                showEditBudgetDialog(budget)
            }
        )
        recyclerBudgets.layoutManager = LinearLayoutManager(this)
        recyclerBudgets.adapter = adapter
    }

    private fun observeData() {
        viewModel.budgets.collectInScope { budgets ->
            adapter.submitList(budgets)
            viewEmpty.visibility = if (budgets.isEmpty()) View.VISIBLE else View.GONE
            updateSummary()
        }

        viewModel.currentMonth.collectInScope { month ->
            textMonth.text = formatMonth(month)
        }
    }

    private fun updateSummary() {
        textTotalBudget.text = currencyFormat.format(viewModel.getTotalBudget())
        textTotalSpent.text = currencyFormat.format(viewModel.getTotalSpent())
        textRemaining.text = currencyFormat.format(viewModel.getRemainingBudget())
        
        val progress = viewModel.getBudgetProgress()
        progressTotal.progress = (progress * 100).toInt()
        
        // Set color based on progress
        val color = when {
            progress >= 1.0f -> getColor(R.color.expense_red)
            progress >= 0.8f -> getColor(R.color.warning_orange)
            else -> getColor(R.color.income_green)
        }
        progressTotal.setIndicatorColor(color)
    }

    private fun formatMonth(month: String): String {
        return try {
            val parts = month.split("-")
            val year = parts[0]
            val monthNum = parts[1].toInt()
            val monthFormat = java.text.SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, year.toInt())
            calendar.set(Calendar.MONTH, monthNum - 1)
            monthFormat.format(calendar.time)
        } catch (e: Exception) {
            month
        }
    }

    private fun showMonthPicker() {
        val calendar = Calendar.getInstance()
        
        AlertDialog.Builder(this)
            .setTitle("Select Month")
            .setPositiveButton("Previous") { _, _ ->
                calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 1)
                val newMonth = String.format("%04d-%02d", 
                    calendar.get(Calendar.YEAR), 
                    calendar.get(Calendar.MONTH) + 1)
                viewModel.setMonth(newMonth)
            }
            .setNeutralButton("Current") { _, _ ->
                val newMonth = String.format("%04d-%02d",
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1)
                viewModel.setMonth(newMonth)
            }
            .setNegativeButton("Next") { _, _ ->
                calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1)
                val newMonth = String.format("%04d-%02d",
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1)
                viewModel.setMonth(newMonth)
            }
            .show()
    }

    private fun showAddBudgetDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_budget, null)
        val editCategory = view.findViewById<AutoCompleteTextView>(R.id.dropdown_budget_category)
        val editAmount = view.findViewById<EditText>(R.id.edit_budget_amount)

        // Setup category dropdown
        val categories = Category.DEFAULT_EXPENSE_CATEGORIES.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        editCategory.setAdapter(adapter)

        AlertDialog.Builder(this)
            .setTitle("Add Budget")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val category = editCategory.text.toString()
                val amount = editAmount.text.toString().toDoubleOrNull()
                
                if (category.isNotEmpty() && amount != null && amount > 0) {
                    val budget = Budget(
                        category = category,
                        monthlyBudget = amount,
                        month = viewModel.currentMonth.value
                    )
                    kotlinx.coroutines.runBlocking {
                        viewModel.saveBudget(budget)
                        viewModel.loadBudgets()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditBudgetDialog(budget: Budget) {
        val view = layoutInflater.inflate(R.layout.dialog_add_budget, null)
        val editCategory = view.findViewById<AutoCompleteTextView>(R.id.dropdown_budget_category)
        val editAmount = view.findViewById<EditText>(R.id.edit_budget_amount)

        editCategory.setText(budget.category, false)
        editAmount.setText(budget.monthlyBudget.toString())

        AlertDialog.Builder(this)
            .setTitle("Edit Budget")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val amount = editAmount.text.toString().toDoubleOrNull()
                
                if (amount != null && amount > 0) {
                    val updatedBudget = budget.copy(
                        category = editCategory.text.toString(),
                        monthlyBudget = amount
                    )
                    kotlinx.coroutines.runBlocking {
                        viewModel.saveBudget(updatedBudget)
                        viewModel.loadBudgets()
                    }
                }
            }
            .setNeutralButton("Delete") { _, _ ->
                confirmDelete(budget)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(budget: Budget) {
        AlertDialog.Builder(this)
            .setTitle("Delete Budget")
            .setMessage("Are you sure you want to delete this budget?")
            .setPositiveButton("Delete") { _, _ ->
                kotlinx.coroutines.runBlocking {
                    viewModel.deleteBudget(budget.id)
                    viewModel.loadBudgets()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBudgetDetail(budget: Budget) {
        val message = """
            Category: ${budget.category}
            Monthly Budget: ${currencyFormat.format(budget.monthlyBudget)}
            Spent: ${currencyFormat.format(budget.spent)}
            Remaining: ${currencyFormat.format(budget.remaining)}
            Progress: ${(budget.percentageUsed * 100).toInt()}%
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Budget Detail")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
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
        viewModel.loadBudgets()
    }

    private inline fun <T> kotlinx.coroutines.flow.Flow<T>.collectInScope(crossinline action: suspend (T) -> Unit) {
        kotlinx.coroutines.runBlocking {
            collect { action(it) }
        }
    }
}
