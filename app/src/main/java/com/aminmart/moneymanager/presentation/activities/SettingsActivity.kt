package com.aminmart.moneymanager.presentation.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import com.aminmart.moneymanager.R

class SettingsActivity : BottomNavigationActivity() {

    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initViews()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar_settings)

        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        toolbar.navigationIcon = null

        setupBottomNavigation(R.id.nav_settings)

        findViewById<View>(R.id.button_settings_data_management).setOnClickListener {
            startActivity(Intent(this, DataManagementActivity::class.java))
        }

        findViewById<View>(R.id.button_settings_manage_categories).setOnClickListener {
            startActivity(Intent(this, ManageCategoriesActivity::class.java))
        }

        findViewById<View>(R.id.button_settings_debt_credit).setOnClickListener {
            startActivity(Intent(this, DebtActivity::class.java))
        }
    }
}
