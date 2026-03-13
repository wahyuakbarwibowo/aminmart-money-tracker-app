package com.aminmart.moneymanager.presentation.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aminmart.moneymanager.MoneyManagerApplication
import com.aminmart.moneymanager.R
import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.presentation.adapters.TransactionAdapter
import com.aminmart.moneymanager.presentation.viewmodels.TransactionsViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * Transactions Activity - List all transactions with filtering
 */
class TransactionsActivity : AppCompatActivity() {

    private lateinit var app: MoneyManagerApplication
    private lateinit var viewModel: TransactionsViewModel

    private lateinit var toolbar: Toolbar
    private lateinit var recyclerTransactions: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var viewEmpty: View
    private lateinit var textFilterInfo: TextView

    private lateinit var adapter: TransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transactions)

        app = application as MoneyManagerApplication
        viewModel = TransactionsViewModel(
            app.getAllTransactionsUseCase,
            app.addTransactionUseCase,
            app.updateTransactionUseCase,
            app.deleteTransactionUseCase,
            app.getTransactionByIdUseCase
        )

        initViews()
        setupRecyclerView()
        observeData()

        fabAdd.setOnClickListener {
            navigateToAddTransaction()
        }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar_transactions)
        recyclerTransactions = findViewById(R.id.recycler_transactions)
        fabAdd = findViewById(R.id.fab_transactions_add)
        viewEmpty = findViewById(R.id.view_transactions_empty)
        textFilterInfo = findViewById(R.id.text_transactions_filter_info)

        setSupportActionBar(toolbar)
        supportActionBar?.title = "Transactions"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter(
            onItemClick = { transaction ->
                showTransactionOptions(transaction)
            },
            onItemLongClick = { transaction ->
                showTransactionOptions(transaction)
            }
        )
        recyclerTransactions.layoutManager = LinearLayoutManager(this)
        recyclerTransactions.adapter = adapter
    }

    private fun observeData() {
        viewModel.transactions.collectInScope { transactions ->
            adapter.submitList(transactions)
            viewEmpty.visibility = if (transactions.isEmpty()) View.VISIBLE else View.GONE
            updateFilterInfo()
        }
    }

    private fun updateFilterInfo() {
        val type = viewModel.filterType.value
        val category = viewModel.filterCategory.value
        
        val filters = mutableListOf<String>()
        type?.let { filters.add(it.name) }
        category?.let { filters.add(it) }
        
        textFilterInfo.text = if (filters.isEmpty()) {
            ""
        } else {
            "Filtered: ${filters.joinToString(", ")}"
        }
        textFilterInfo.visibility = if (filters.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showTransactionOptions(transaction: Transaction) {
        val options = arrayOf("Edit", "Delete")
        AlertDialog.Builder(this)
            .setTitle(transaction.category)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> navigateToAddTransaction(transaction)
                    1 -> confirmDelete(transaction)
                }
            }
            .show()
    }

    private fun confirmDelete(transaction: Transaction) {
        AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                kotlinx.coroutines.runBlocking {
                    viewModel.deleteTransaction(transaction.id)
                    viewModel.loadTransactions()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun navigateToAddTransaction(transaction: Transaction? = null) {
        val intent = Intent(this, AddTransactionActivity::class.java)
        transaction?.let {
            intent.putExtra("transaction_id", it.id)
        }
        startActivityForResult(intent, MainActivity.REQUEST_ADD_TRANSACTION)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_transactions, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_filter -> {
                showFilterDialog()
                true
            }
            R.id.action_clear_filter -> {
                viewModel.clearFilters()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showFilterDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_filter_transactions, null)
        val spinnerType = view.findViewById<Spinner>(R.id.spinner_filter_type)
        val spinnerCategory = view.findViewById<Spinner>(R.id.spinner_filter_category)

        // Setup spinners would go here

        AlertDialog.Builder(this)
            .setTitle("Filter Transactions")
            .setView(view)
            .setPositiveButton("Apply") { _, _ ->
                // Apply filters
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadTransactions()
    }

    private inline fun <T> kotlinx.coroutines.flow.Flow<T>.collectInScope(crossinline action: suspend (T) -> Unit) {
        kotlinx.coroutines.runBlocking {
            collect { action(it) }
        }
    }
}
