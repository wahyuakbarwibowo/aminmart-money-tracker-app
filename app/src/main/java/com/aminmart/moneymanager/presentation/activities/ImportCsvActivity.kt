package com.aminmart.moneymanager.presentation.activities

import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aminmart.moneymanager.MoneyManagerApplication
import com.aminmart.moneymanager.R
import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.presentation.adapters.TransactionAdapter
import com.aminmart.moneymanager.presentation.viewmodels.ImportCsvViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.io.File
import java.io.FileOutputStream

/**
 * Import CSV Activity - Import transactions from bank CSV files
 */
class ImportCsvActivity : AppCompatActivity() {

    private lateinit var app: MoneyManagerApplication
    private lateinit var viewModel: ImportCsvViewModel

    private lateinit var toolbar: Toolbar
    private lateinit var buttonSelectFile: MaterialButton
    private lateinit var textFileName: TextView
    private lateinit var progressImport: LinearProgressIndicator
    private lateinit var textStatus: TextView
    private lateinit var recyclerPreview: RecyclerView
    private lateinit var buttonImport: MaterialButton
    private lateinit var viewEmpty: View

    private var selectedFileUri: Uri? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            handleFileSelection(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_csv)

        app = application as MoneyManagerApplication
        viewModel = ImportCsvViewModel(
            app.importCsvUseCase,
            app.validateCsvUseCase,
            app.getCsvPreviewUseCase
        )

        initViews()
        setupRecyclerView()
        observeData()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar_import_csv)
        buttonSelectFile = findViewById(R.id.button_import_select_file)
        textFileName = findViewById(R.id.text_import_file_name)
        progressImport = findViewById(R.id.progress_import)
        textStatus = findViewById(R.id.text_import_status)
        recyclerPreview = findViewById(R.id.recycler_import_preview)
        buttonImport = findViewById(R.id.button_import_execute)
        viewEmpty = findViewById(R.id.view_import_empty)

        setSupportActionBar(toolbar)
        supportActionBar?.title = "Import CSV"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        buttonSelectFile.setOnClickListener {
            filePickerLauncher.launch("text/*")
        }

        buttonImport.setOnClickListener {
            performImport()
        }
    }

    private fun setupRecyclerView() {
        val adapter = TransactionAdapter(
            onItemClick = { },
            onItemLongClick = { }
        )
        recyclerPreview.layoutManager = LinearLayoutManager(this)
        recyclerPreview.adapter = adapter

        viewModel.preview.collectInScope { transactions ->
            adapter.submitList(transactions)
            viewEmpty.visibility = if (transactions.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun observeData() {
        viewModel.uiState.collectInScope { state ->
            progressImport.isIndeterminate = state.isLoading
            buttonSelectFile.isEnabled = !state.isLoading
            buttonImport.isEnabled = !state.isLoading && selectedFileUri != null

            state.error?.let { error ->
                textStatus.text = error
                textStatus.visibility = View.VISIBLE
            }

            state.successMessage?.let { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        viewModel.importResult.collectInScope { result ->
            result?.let {
                textStatus.text = buildStatusText(it)
                textStatus.visibility = View.VISIBLE
            }
        }
    }

    private fun handleFileSelection(uri: Uri) {
        try {
            // Copy file to cache directory
            val tempFile = File(cacheDir, "import_${System.currentTimeMillis()}.csv")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            textFileName.text = tempFile.name
            
            // Validate and preview
            kotlinx.coroutines.runBlocking {
                val isValid = viewModel.validateCsvFile(tempFile.absolutePath)
                if (isValid) {
                    viewModel.loadPreview(tempFile.absolutePath)
                } else {
                    textStatus.text = "Invalid CSV format. Expected columns: Date, Description, Amount"
                    textStatus.visibility = View.VISIBLE
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to read file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performImport() {
        selectedFileUri?.let { uri ->
            try {
                val tempFile = File(cacheDir, "import_${System.currentTimeMillis()}.csv")
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                kotlinx.coroutines.runBlocking {
                    viewModel.importCsv(tempFile.absolutePath)
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun buildStatusText(result: com.aminmart.moneymanager.domain.repository.CsvImportResult): String {
        return buildString {
            appendLine("Import completed!")
            appendLine("✓ Imported: ${result.importedCount}")
            if (result.skippedCount > 0) {
                appendLine("⊘ Skipped (duplicates): ${result.skippedCount}")
            }
            if (result.failedCount > 0) {
                appendLine("✗ Failed: ${result.failedCount}")
            }
        }
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
