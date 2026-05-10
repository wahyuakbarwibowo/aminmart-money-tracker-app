package com.aminmart.moneymanager.presentation.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aminmart.moneymanager.MoneyManagerApplication
import com.aminmart.moneymanager.R
import com.aminmart.moneymanager.domain.repository.BackupRepository
import com.aminmart.moneymanager.presentation.viewmodels.SettingsViewModel
import com.google.android.material.switchmaterial.SwitchMaterial
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Settings Activity - Backup, Restore, Import, Export
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var app: MoneyManagerApplication
    private lateinit var viewModel: SettingsViewModel

    private lateinit var toolbar: Toolbar
    private lateinit var switchAutoBackup: SwitchMaterial
    private lateinit var textBackupLocation: TextView
    private lateinit var recyclerBackups: RecyclerView
    private lateinit var viewEmpty: View

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            performBackup()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            restoreFromUri(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        app = application as MoneyManagerApplication
        viewModel = SettingsViewModel(
            app.createBackupUseCase,
            app.restoreBackupUseCase,
            app.getAvailableBackupsUseCase,
            app.autoBackupUseCase,
            app.deleteBackupUseCase
        )

        initViews()
        setupRecyclerView()
        observeData()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar_settings)
        switchAutoBackup = findViewById(R.id.switch_settings_auto_backup)
        textBackupLocation = findViewById(R.id.text_settings_backup_location)
        recyclerBackups = findViewById(R.id.recycler_settings_backups)
        viewEmpty = findViewById(R.id.view_settings_empty)

        setSupportActionBar(toolbar)
        supportActionBar?.title = "Settings"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Set backup location text
        textBackupLocation.text = viewModel.getBackupFolder()

        // Setup buttons
        findViewById<View>(R.id.button_settings_backup).setOnClickListener {
            checkPermissionAndBackup()
        }

        findViewById<View>(R.id.button_settings_restore).setOnClickListener {
            showRestoreOptions()
        }

        findViewById<View>(R.id.button_settings_import_csv).setOnClickListener {
            startActivity(Intent(this, ImportCsvActivity::class.java))
        }

        findViewById<View>(R.id.button_settings_export_report).setOnClickListener {
            startActivity(Intent(this, ExportReportActivity::class.java))
        }

        switchAutoBackup.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAutoBackupEnabled(isChecked)
        }
    }

    private fun setupRecyclerView() {
        val adapter = BackupAdapter(
            onItemClick = { path ->
                confirmRestore(path)
            },
            onDeleteClick = { path ->
                confirmDeleteBackup(path)
            }
        )
        recyclerBackups.layoutManager = LinearLayoutManager(this)
        recyclerBackups.adapter = adapter

        viewModel.backups.collectInScope { backups ->
            adapter.submitList(backups)
            viewEmpty.visibility = if (backups.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun observeData() {
        viewModel.autoBackupEnabled.collectInScope { enabled ->
            switchAutoBackup.isChecked = enabled
        }
    }

    private fun checkPermissionAndBackup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                performBackup()
            } else {
                requestManageStorage()
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                performBackup()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun requestManageStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        }
    }

    private fun performBackup() {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "backup_$timestamp.json"
        val backupDir = File(
            getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "MoneyManagerBackup"
        )
        backupDir.mkdirs()
        val destinationPath = File(backupDir, fileName).absolutePath

        kotlinx.coroutines.runBlocking {
            val result = viewModel.createBackup(BackupRepository.BackupFormat.JSON, destinationPath)
            if (result != null) {
                Toast.makeText(this@SettingsActivity, "Backup created: $fileName", Toast.LENGTH_LONG).show()
                viewModel.loadBackups()
            } else {
                Toast.makeText(this@SettingsActivity, "Backup failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRestoreOptions() {
        val options = arrayOf("Choose from backups", "Select file")
        AlertDialog.Builder(this)
            .setTitle("Restore From")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showBackupsList()
                    1 -> filePickerLauncher.launch("*/*")
                }
            }
            .show()
    }

    private fun showBackupsList() {
        val backups = viewModel.backups.value
        if (backups.isEmpty()) {
            Toast.makeText(this, "No backups available", Toast.LENGTH_SHORT).show()
            return
        }

        val backupNames = backups.map { File(it).name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select Backup")
            .setItems(backupNames) { _, index ->
                confirmRestore(backups[index])
            }
            .show()
    }

    private fun confirmRestore(path: String) {
        AlertDialog.Builder(this)
            .setTitle("Restore Backup")
            .setMessage("Restore from ${File(path).name}? This will replace all current data.")
            .setPositiveButton("Restore") { _, _ ->
                restoreBackup(path)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun restoreFromUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val tempFile = File(cacheDir, "restore_temp.json")
            tempFile.outputStream().use { output ->
                inputStream?.copyTo(output)
            }
            restoreBackup(tempFile.absolutePath)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to read file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun restoreBackup(path: String) {
        kotlinx.coroutines.runBlocking {
            val success = viewModel.restoreBackup(path)
            if (success) {
                Toast.makeText(this@SettingsActivity, "Restore successful", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@SettingsActivity, "Restore failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDeleteBackup(path: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Backup")
            .setMessage("Delete ${File(path).name}?")
            .setPositiveButton("Delete") { _, _ ->
                kotlinx.coroutines.runBlocking {
                    val success = viewModel.deleteBackup(path)
                    if (success) {
                        Toast.makeText(this@SettingsActivity, "Backup deleted", Toast.LENGTH_SHORT).show()
                        viewModel.loadBackups()
                    } else {
                        Toast.makeText(this@SettingsActivity, "Failed to delete", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
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
        viewModel.loadBackups()
    }

    private inline fun <T> kotlinx.coroutines.flow.Flow<T>.collectInScope(crossinline action: suspend (T) -> Unit) {
        kotlinx.coroutines.runBlocking {
            collect { action(it) }
        }
    }
}

/**
 * Simple adapter for backup list
 */
class BackupAdapter(
    private var backups: List<String> = emptyList(),
    private val onItemClick: (String) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<BackupAdapter.BackupViewHolder>() {

    class BackupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textName: TextView = itemView.findViewById(android.R.id.text1)
        val textDate: TextView = itemView.findViewById(android.R.id.text2)
        val buttonDelete: View = itemView.findViewById(R.id.button_backup_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BackupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return BackupViewHolder(view)
    }

    override fun onBindViewHolder(holder: BackupViewHolder, position: Int) {
        val path = backups[position]
        val file = File(path)
        
        holder.textName.text = file.name
        holder.textDate.text = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
            .format(Date(file.lastModified()))

        holder.itemView.setOnClickListener { onItemClick(path) }
        holder.buttonDelete.setOnClickListener { onDeleteClick(path) }
    }

    override fun getItemCount(): Int = backups.size

    fun submitList(newBackups: List<String>) {
        backups = newBackups
        notifyDataSetChanged()
    }
}
