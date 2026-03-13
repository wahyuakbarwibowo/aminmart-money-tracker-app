package com.aminmart.moneymanager.presentation.activities

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.aminmart.moneymanager.MoneyManagerApplication
import com.aminmart.moneymanager.R
import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.presentation.viewmodels.AddTransactionViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Add/Edit Transaction Activity
 */
class AddTransactionActivity : AppCompatActivity() {

    private lateinit var app: MoneyManagerApplication
    private lateinit var viewModel: AddTransactionViewModel

    private lateinit var toolbar: Toolbar
    private lateinit var radioType: RadioGroup
    private lateinit var radioIncome: RadioButton
    private lateinit var radioExpense: RadioButton
    private lateinit var editAmount: EditText
    private lateinit var categoryDropdown: AutoCompleteTextView
    private lateinit var editDescription: EditText
    private lateinit var buttonDate: Button
    private lateinit var textDate: TextView
    private lateinit var buttonSave: Button

    private var transactionId: Long? = null
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }
    private val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        app = application as MoneyManagerApplication
        viewModel = AddTransactionViewModel(app.getTransactionByIdUseCase)

        // Check if editing existing transaction
        transactionId = intent.getLongExtra("transaction_id", 0)
        if (transactionId != null && transactionId!! > 0) {
            viewModel.editTransaction(transactionId!!)
        }

        initViews()
        setupListeners()
        observeData()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar_add_transaction)
        radioType = findViewById(R.id.radio_transaction_type)
        radioIncome = findViewById(R.id.radio_income)
        radioExpense = findViewById(R.id.radio_expense)
        editAmount = findViewById(R.id.edit_transaction_amount)
        categoryDropdown = findViewById(R.id.dropdown_transaction_category)
        editDescription = findViewById(R.id.edit_transaction_description)
        buttonDate = findViewById(R.id.button_transaction_date)
        textDate = findViewById(R.id.text_transaction_date_display)
        buttonSave = findViewById(R.id.button_transaction_save)

        setSupportActionBar(toolbar)
        supportActionBar?.title = if (transactionId == null || transactionId!! <= 0) {
            "Add Transaction"
        } else {
            "Edit Transaction"
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupListeners() {
        radioType.setOnCheckedChangeListener { _, _ ->
            val type = if (radioIncome.isChecked) {
                Transaction.TransactionType.INCOME
            } else {
                Transaction.TransactionType.EXPENSE
            }
            viewModel.setTransactionType(type)
            updateCategoryDropdown()
        }

        buttonDate.setOnClickListener {
            showDatePicker()
        }

        buttonSave.setOnClickListener {
            saveTransaction()
        }
    }

    private fun observeData() {
        viewModel.transactionType.collectInScope { type ->
            when (type) {
                Transaction.TransactionType.INCOME -> radioIncome.isChecked = true
                Transaction.TransactionType.EXPENSE -> radioExpense.isChecked = true
            }
        }

        viewModel.category.collectInScope { category ->
            updateCategoryDropdown()
        }

        viewModel.date.collectInScope { date ->
            textDate.text = dateFormat.format(java.util.Date(date))
        }

        viewModel.uiState.collectInScope { state ->
            buttonSave.isEnabled = !state.isLoading
            if (state.successMessage != null) {
                showSuccess(state.successMessage)
                finish()
            }
        }
    }

    private fun updateCategoryDropdown() {
        val categories = viewModel.currentCategories
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        categoryDropdown.setAdapter(adapter)
        
        viewModel.category.value?.let {
            categoryDropdown.setText(it, false)
        }

        categoryDropdown.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            viewModel.setCategory(categories[position])
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = viewModel.date.value
        
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                viewModel.setDate(calendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveTransaction() {
        val amountStr = editAmount.text.toString()
        val description = editDescription.text.toString()
        val category = categoryDropdown.text.toString()

        if (amountStr.isEmpty()) {
            editAmount.error = "Amount is required"
            return
        }

        if (category.isEmpty()) {
            categoryDropdown.error = "Category is required"
            return
        }

        viewModel.setAmount(amountStr)
        viewModel.setDescription(description)

        val transaction = viewModel.buildTransaction()
        if (transaction == null) {
            showError("Invalid transaction data")
            return
        }

        kotlinx.coroutines.runBlocking {
            try {
                if (transactionId != null && transactionId!! > 0) {
                    viewModel.updateTransaction(transaction)
                } else {
                    viewModel.addTransaction(transaction)
                }
                showSuccess("Transaction saved successfully")
                setResult(RESULT_OK)
                finish()
            } catch (e: Exception) {
                showError("Failed to save transaction: ${e.message}")
            }
        }
    }

    private fun showSuccess(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
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

    private inline fun <T> kotlinx.coroutines.flow.Flow<T>.collectInScope(crossinline action: suspend (T) -> Unit) {
        kotlinx.coroutines.runBlocking {
            collect { action(it) }
        }
    }
}
