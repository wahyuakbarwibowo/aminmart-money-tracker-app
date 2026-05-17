package com.aminmart.moneymanager.presentation.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.activity.result.contract.ActivityResultContracts
import com.aminmart.moneymanager.MoneyManagerApplication
import com.aminmart.moneymanager.R
import com.aminmart.moneymanager.domain.model.Debt
import com.aminmart.moneymanager.presentation.adapters.DebtAdapter
import com.aminmart.moneymanager.presentation.viewmodels.DebtViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class DebtActivity : AppCompatActivity() {

    private lateinit var app: MoneyManagerApplication
    private lateinit var viewModel: DebtViewModel
    private lateinit var debtAdapter: DebtAdapter

    private lateinit var toolbar: Toolbar
    private lateinit var recyclerDebts: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var emptyView: View
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val addDebtLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.loadDebts() // Reload debts if changes were made
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debt)

        app = application as MoneyManagerApplication
        viewModel = DebtViewModel(app.debtUseCases)

        initViews()
        setupToolbar()
        setupRecyclerView()
        observeViewModel()

        fabAdd.setOnClickListener {
            navigateToAddDebt()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadDebts()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar_debts)
        recyclerDebts = findViewById(R.id.recycler_debts)
        fabAdd = findViewById(R.id.fab_debts_add)
        emptyView = findViewById(R.id.view_debts_empty)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Utang Piutang"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupRecyclerView() {
        debtAdapter = DebtAdapter { debt ->
            showOptionsDialog(debt)
        }
        recyclerDebts.apply {
            adapter = debtAdapter
            layoutManager = LinearLayoutManager(this@DebtActivity)
        }
    }

    private fun observeViewModel() {
        viewModel.debts.collectInScope { debts ->
            debtAdapter.submitList(debts)
            emptyView.visibility = if (debts.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showOptionsDialog(debt: Debt) {
        val togglePaidOption = if (debt.isPaid) "Mark as Unpaid" else "Mark as Paid"
        val options = arrayOf("Edit", "Delete", togglePaidOption)

        AlertDialog.Builder(this)
            .setTitle(debt.personName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> navigateToAddDebt(debt)
                    1 -> confirmDelete(debt)
                    2 -> togglePaidStatus(debt)
                }
            }
            .show()
    }

    private fun confirmDelete(debt: Debt) {
        AlertDialog.Builder(this)
            .setTitle("Delete Debt")
            .setMessage("Are you sure you want to delete this record?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteDebt(debt)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun togglePaidStatus(debt: Debt) {
        val updatedDebt = debt.copy(isPaid = !debt.isPaid)
        viewModel.updateDebt(updatedDebt)
    }

    private fun navigateToAddDebt(debt: Debt? = null) {
        val intent = Intent(this, AddDebtActivity::class.java)
        debt?.let {
            intent.putExtra("debt_id", it.id)
        }
        addDebtLauncher.launch(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }

    private fun <T> Flow<T>.collectInScope(action: suspend (T) -> Unit) {
        activityScope.launch {
            collect { action(it) }
        }
    }

    companion object {
        const val REQUEST_ADD_DEBT = 1002
    }
}
