package com.aminmart.moneymanager.presentation.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.aminmart.moneymanager.R
import com.aminmart.moneymanager.domain.model.Transaction
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Main Activity with Bottom Navigation
 * Serves as the container for all fragments/screens
 */
class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var container: FrameLayout

    companion object {
        const val REQUEST_ADD_TRANSACTION = 100
        const val REQUEST_EDIT_TRANSACTION = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottom_navigation)
        container = findViewById(R.id.fragment_container)

        setupBottomNavigation()

        // Show dashboard by default
        if (savedInstanceState == null) {
            showDashboard()
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    showDashboard()
                    true
                }
                R.id.nav_transactions -> {
                    showTransactions()
                    true
                }
                R.id.nav_budget -> {
                    showBudget()
                    true
                }
                R.id.nav_statistics -> {
                    showStatistics()
                    true
                }
                R.id.nav_settings -> {
                    showSettings()
                    true
                }
                else -> false
            }
        }
    }

    private fun showDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun showTransactions() {
        startActivity(Intent(this, TransactionsActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun showBudget() {
        startActivity(Intent(this, BudgetActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun showStatistics() {
        startActivity(Intent(this, StatisticsActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun showSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    fun navigateToAddTransaction(transaction: Transaction? = null) {
        val intent = Intent(this, AddTransactionActivity::class.java)
        transaction?.let {
            intent.putExtra("transaction_id", it.id)
        }
        startActivityForResult(
            intent,
            if (transaction == null) REQUEST_ADD_TRANSACTION else REQUEST_EDIT_TRANSACTION
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (resultCode == Activity.RESULT_OK) {
            // Refresh current screen
            when (requestCode) {
                REQUEST_ADD_TRANSACTION, REQUEST_EDIT_TRANSACTION -> {
                    // Broadcast refresh event or reload data
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Update bottom navigation selection based on current activity
        updateNavigationSelection()
    }

    private fun updateNavigationSelection() {
        val currentActivity = this::class.java.simpleName
        when {
            currentActivity.contains("Dashboard") -> bottomNavigation.selectedItemId = R.id.nav_dashboard
            currentActivity.contains("Transactions") -> bottomNavigation.selectedItemId = R.id.nav_transactions
            currentActivity.contains("Budget") -> bottomNavigation.selectedItemId = R.id.nav_budget
            currentActivity.contains("Statistics") -> bottomNavigation.selectedItemId = R.id.nav_statistics
            currentActivity.contains("Settings") -> bottomNavigation.selectedItemId = R.id.nav_settings
        }
    }
}
