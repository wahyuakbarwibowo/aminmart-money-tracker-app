package com.aminmart.moneymanager.data.datasource

import android.content.Context
import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.domain.repository.ExportRepository
import com.aminmart.moneymanager.domain.repository.TransactionRepository
import java.io.File
import java.io.FileWriter

/**
 * Export Manager for exporting transactions to CSV and Excel formats
 */
class ExportManager(
    private val context: Context,
    private val transactionRepository: TransactionRepository
) : ExportRepository {

    companion object {
        private const val EXPORT_FOLDER = "MoneyManagerExport"
    }

    private val exportDirectory: File
        get() {
            val documentsDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS)
            val exportDir = File(documentsDir, EXPORT_FOLDER)
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            return exportDir
        }

    override suspend fun exportToCsv(
        destinationPath: String,
        startDate: Long?,
        endDate: Long?
    ): String? {
        return try {
            val transactions = getTransactions(startDate, endDate)
            val file = File(destinationPath)
            
            FileWriter(file).use { writer ->
                // Write header
                writer.appendLine("Date,Type,Category,Amount,Description")
                
                // Write transactions
                transactions.forEach { transaction ->
                    val dateStr = formatDate(transaction.date)
                    val typeStr = transaction.type.name.lowercase()
                    val amountStr = String.format("%.2f", transaction.amount)
                    
                    writer.appendLine(
                        "$dateStr," +
                        "$typeStr," +
                        "\"${escapeCsv(transaction.category)}\"," +
                        "$amountStr," +
                        "\"${escapeCsv(transaction.description)}\""
                    )
                }
                
                // Write summary
                writer.appendLine("")
                writer.appendLine("Summary")
                val totalIncome = transactions.filter { it.type == Transaction.TransactionType.INCOME }.sumOf { it.amount }
                val totalExpense = transactions.filter { it.type == Transaction.TransactionType.EXPENSE }.sumOf { it.amount }
                writer.appendLine("Total Income,${String.format("%.2f", totalIncome)}")
                writer.appendLine("Total Expense,${String.format("%.2f", totalExpense)}")
                writer.appendLine("Balance,${String.format("%.2f", totalIncome - totalExpense)}")
            }
            
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun exportToExcel(
        destinationPath: String,
        startDate: Long?,
        endDate: Long?
    ): String? {
        // For Excel export, we'll create a CSV that can be opened in Excel
        // A full XLSX implementation would require Apache POI or similar library
        return exportToCsv(destinationPath, startDate, endDate)
    }

    override suspend fun exportReport(
        destinationPath: String,
        format: ExportRepository.ExportFormat
    ): String? {
        return when (format) {
            ExportRepository.ExportFormat.CSV -> {
                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                    .format(java.util.Date())
                val fileName = "report_$timestamp.csv"
                val fullPath = File(exportDirectory, fileName).absolutePath
                exportToCsv(fullPath, null, null)
            }
            ExportRepository.ExportFormat.EXCEL -> {
                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                    .format(java.util.Date())
                val fileName = "report_$timestamp.csv"
                val fullPath = File(exportDirectory, fileName).absolutePath
                exportToExcel(fullPath, null, null)
            }
        }
    }

    private suspend fun getTransactions(startDate: Long?, endDate: Long?): List<Transaction> {
        return if (startDate != null && endDate != null) {
            var result: List<Transaction> = emptyList()
            transactionRepository.getTransactionsByDateRange(startDate, endDate).collect {
                result = it
            }
            result
        } else {
            var result: List<Transaction> = emptyList()
            transactionRepository.getAllTransactions().collect {
                result = it
            }
            result
        }
    }

    private fun formatDate(timestamp: Long): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }
}
