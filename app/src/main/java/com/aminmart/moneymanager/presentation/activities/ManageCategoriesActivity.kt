package com.aminmart.moneymanager.presentation.activities

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aminmart.moneymanager.R
import com.aminmart.moneymanager.data.local.CategoryStore
import com.aminmart.moneymanager.domain.model.Transaction
import com.google.android.material.textfield.TextInputEditText

class ManageCategoriesActivity : AppCompatActivity() {

    private lateinit var categoryStore: CategoryStore

    private lateinit var toolbar: Toolbar
    private lateinit var recyclerIncome: RecyclerView
    private lateinit var recyclerExpense: RecyclerView
    private lateinit var editIncome: TextInputEditText
    private lateinit var editExpense: TextInputEditText
    private lateinit var buttonAddIncome: Button
    private lateinit var buttonAddExpense: Button
    private lateinit var textIncomeEmpty: TextView
    private lateinit var textExpenseEmpty: TextView

    private lateinit var incomeAdapter: EditableCategoryAdapter
    private lateinit var expenseAdapter: EditableCategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_categories)

        categoryStore = CategoryStore(this)
        initViews()
        setupRecycler()
        setupListeners()
        refreshLists()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar_manage_categories)
        recyclerIncome = findViewById(R.id.recycler_income_categories)
        recyclerExpense = findViewById(R.id.recycler_expense_categories)
        editIncome = findViewById(R.id.edit_add_income_category)
        editExpense = findViewById(R.id.edit_add_expense_category)
        buttonAddIncome = findViewById(R.id.button_add_income_category)
        buttonAddExpense = findViewById(R.id.button_add_expense_category)
        textIncomeEmpty = findViewById(R.id.text_income_empty)
        textExpenseEmpty = findViewById(R.id.text_expense_empty)

        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.manage_categories)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupRecycler() {
        incomeAdapter = EditableCategoryAdapter { name ->
            removeCategory(Transaction.TransactionType.INCOME, name)
        }
        expenseAdapter = EditableCategoryAdapter { name ->
            removeCategory(Transaction.TransactionType.EXPENSE, name)
        }

        recyclerIncome.layoutManager = LinearLayoutManager(this)
        recyclerIncome.adapter = incomeAdapter
        recyclerExpense.layoutManager = LinearLayoutManager(this)
        recyclerExpense.adapter = expenseAdapter
    }

    private fun setupListeners() {
        buttonAddIncome.setOnClickListener {
            addCategory(Transaction.TransactionType.INCOME, editIncome.text?.toString().orEmpty())
        }
        buttonAddExpense.setOnClickListener {
            addCategory(Transaction.TransactionType.EXPENSE, editExpense.text?.toString().orEmpty())
        }
    }

    private fun refreshLists() {
        val income = categoryStore.getIncomeCategories()
        val expense = categoryStore.getExpenseCategories()

        incomeAdapter.submitList(income)
        expenseAdapter.submitList(expense)

        textIncomeEmpty.visibility = if (income.isEmpty()) View.VISIBLE else View.GONE
        textExpenseEmpty.visibility = if (expense.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun addCategory(type: Transaction.TransactionType, name: String) {
        val success = categoryStore.addCategory(type, name)
        if (!success) {
            Toast.makeText(this, getString(R.string.category_invalid_or_exists), Toast.LENGTH_SHORT).show()
            return
        }

        if (type == Transaction.TransactionType.INCOME) {
            editIncome.setText("")
        } else {
            editExpense.setText("")
        }
        refreshLists()
    }

    private fun removeCategory(type: Transaction.TransactionType, name: String) {
        val success = categoryStore.removeCategory(type, name)
        if (!success) {
            Toast.makeText(this, getString(R.string.at_least_one_category_remain), Toast.LENGTH_SHORT).show()
            return
        }
        refreshLists()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

private class EditableCategoryAdapter(
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<EditableCategoryAdapter.ViewHolder>() {
    private var categories: List<String> = emptyList()

    class ViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val textName: TextView = view.findViewById(R.id.text_category_name)
        val buttonDelete: Button = view.findViewById(R.id.button_delete_category)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_editable_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val name = categories[position]
        holder.textName.text = name
        holder.buttonDelete.setOnClickListener { onDeleteClick(name) }
    }

    override fun getItemCount(): Int = categories.size

    fun submitList(list: List<String>) {
        categories = list
        notifyDataSetChanged()
    }
}
