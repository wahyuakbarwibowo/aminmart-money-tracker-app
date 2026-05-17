package com.aminmart.moneymanager.presentation.activities

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.aminmart.moneymanager.MoneyManagerApplication
import com.aminmart.moneymanager.R
import com.aminmart.moneymanager.domain.model.Debt
import com.aminmart.moneymanager.presentation.ui.formatWholeAmount
import com.aminmart.moneymanager.presentation.ui.parseWholeAmount
import com.aminmart.moneymanager.presentation.viewmodels.DebtViewModel
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddDebtActivity : AppCompatActivity() {

    private lateinit var viewModel: DebtViewModel
    private var editingDebt: Debt? = null
    private var dueDateCalendar: Calendar = Calendar.getInstance()
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var toolbar: Toolbar
    private lateinit var personNameEditText: TextInputEditText
    private lateinit var amountEditText: TextInputEditText
    private lateinit var typeRadioGroup: RadioGroup
    private lateinit var creditRadioButton: RadioButton
    private lateinit var debtRadioButton: RadioButton
    private lateinit var dueDateEditText: TextInputEditText
    private lateinit var descriptionEditText: TextInputEditText
    private lateinit var paidSwitch: SwitchMaterial
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_debt)

        val app = application as MoneyManagerApplication
        viewModel = DebtViewModel(app.debtUseCases)

        initViews()
        setupToolbar()
        setupDueDatepicker()

        val debtId = intent.getLongExtra("debt_id", -1L)
        if (debtId != -1L) {
            loadEditingDebt(debtId)
        }

        saveButton.setOnClickListener { saveDebt() }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar_add_debt)
        personNameEditText = findViewById(R.id.edit_debt_person_name)
        amountEditText = findViewById(R.id.edit_debt_amount)
        typeRadioGroup = findViewById(R.id.radiogroup_debt_type)
        creditRadioButton = findViewById(R.id.radio_debt_credit)
        debtRadioButton = findViewById(R.id.radio_debt_debt)
        dueDateEditText = findViewById(R.id.edit_debt_due_date)
        descriptionEditText = findViewById(R.id.edit_debt_description)
        paidSwitch = findViewById(R.id.switch_debt_paid)
        saveButton = findViewById(R.id.button_save_debt)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (editingDebt == null) "Add Debt" else "Edit Debt"
    }

    private fun setupDueDatepicker() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            dueDateCalendar.set(Calendar.YEAR, year)
            dueDateCalendar.set(Calendar.MONTH, month)
            dueDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDueDateInView()
        }

        dueDateEditText.setOnClickListener {
            DatePickerDialog(
                this,
                dateSetListener,
                dueDateCalendar.get(Calendar.YEAR),
                dueDateCalendar.get(Calendar.MONTH),
                dueDateCalendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        updateDueDateInView()
    }

    private fun updateDueDateInView() {
        val myFormat = "dd/MM/yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        dueDateEditText.setText(sdf.format(dueDateCalendar.time))
    }

    private fun loadEditingDebt(debtId: Long) {
        activityScope.launch {
            editingDebt = viewModel.getDebt(debtId)
            editingDebt?.let { debt ->
                personNameEditText.setText(debt.personName)
                amountEditText.setText(formatWholeAmount(debt.amount))
                descriptionEditText.setText(debt.description)
                paidSwitch.isChecked = debt.isPaid
                dueDateCalendar.timeInMillis = debt.dueDate
                updateDueDateInView()

                if (debt.type == Debt.DebtType.CREDIT) {
                    creditRadioButton.isChecked = true
                } else {
                    debtRadioButton.isChecked = true
                }
                supportActionBar?.title = "Edit Debt"
            }
        }
    }

    private fun saveDebt() {
        val name = personNameEditText.text.toString()
        val amount = parseWholeAmount(amountEditText.text)

        if (name.isBlank()) {
            Toast.makeText(this, "Person name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val type = if (creditRadioButton.isChecked) Debt.DebtType.CREDIT else Debt.DebtType.DEBT
        val now = System.currentTimeMillis()

        val debt = editingDebt?.copy(
            personName = name,
            amount = amount,
            type = type,
            dueDate = dueDateCalendar.timeInMillis,
            description = descriptionEditText.text.toString(),
            isPaid = paidSwitch.isChecked,
            updatedAt = now
        ) ?: Debt(
            personName = name,
            amount = amount,
            type = type,
            dueDate = dueDateCalendar.timeInMillis,
            description = descriptionEditText.text.toString(),
            isPaid = paidSwitch.isChecked,
            createdAt = now,
            updatedAt = now
        )

        if (editingDebt == null) {
            viewModel.addDebt(debt)
        } else {
            viewModel.updateDebt(debt)
        }
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }
}
