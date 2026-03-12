package com.aminmart.moneymanager.data.backup

import android.content.Context
import android.os.Environment
import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.domain.model.Budget
import com.aminmart.moneymanager.domain.repository.BackupRepository
import com.aminmart.moneymanager.domain.repository.TransactionRepository
import com.aminmart.moneymanager.domain.repository.BudgetRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.io.BufferedReader
import java.io.FileReader

/**
 * Backup Manager for creating and restoring data backups
 */
class BackupManager(
    private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository
) : BackupRepository {

    companion object {
        private const val BACKUP_FOLDER = "MoneyManagerBackup"
        private const val BACKUP_PREFIX = "backup_"
        private const val DATE_FORMAT = "yyyyMMdd_HHmmss"
    }

    private val backupDirectory: File
        get() {
            val documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val backupDir = File(documentsDir, BACKUP_FOLDER)
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            return backupDir
        }

    override suspend fun createBackup(format: BackupRepository.BackupFormat, destinationPath: String): String? {
        return try {
            when (format) {
                BackupRepository.BackupFormat.JSON -> createJsonBackup(destinationPath)
                BackupRepository.BackupFormat.CSV -> createCsvBackup(destinationPath)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun restoreBackup(sourcePath: String): Boolean {
        return try {
            val file = File(sourcePath)
            if (!file.exists()) return false

            when {
                sourcePath.endsWith(".json") -> restoreFromJson(sourcePath)
                sourcePath.endsWith(".csv") -> restoreFromCsv(sourcePath)
                else -> false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun getAvailableBackups(): List<String> {
        return try {
            backupDirectory.listFiles()
                ?.filter { it.isFile && (it.name.endsWith(".json") || it.name.endsWith(".csv")) }
                ?.map { it.absolutePath }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun deleteBackup(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists() && file.absolutePath.startsWith(backupDirectory.absolutePath)) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getLatestBackup(): String? {
        return getAvailableBackups().firstOrNull()
    }

    override suspend fun autoBackup(): String? {
        val timestamp = java.text.SimpleDateFormat(DATE_FORMAT, java.util.Locale.getDefault())
            .format(java.util.Date())
        val fileName = "${BACKUP_PREFIX}${timestamp}.json"
        val destinationPath = File(backupDirectory, fileName).absolutePath
        return createBackup(BackupRepository.BackupFormat.JSON, destinationPath)
    }

    private suspend fun createJsonBackup(destinationPath: String): String {
        val jsonObject = JSONObject()
        
        // Get all transactions
        val transactions = getAllTransactionsSync()
        val transactionsArray = JSONArray()
        transactions.forEach { transaction ->
            transactionsArray.put(JSONObject().apply {
                put("id", transaction.id)
                put("type", transaction.type.name)
                put("amount", transaction.amount)
                put("category", transaction.category)
                put("description", transaction.description)
                put("date", transaction.date)
                put("createdAt", transaction.createdAt)
            })
        }
        jsonObject.put("transactions", transactionsArray)

        // Get all budgets
        val budgets = getAllBudgetsSync()
        val budgetsArray = JSONArray()
        budgets.forEach { budget ->
            budgetsArray.put(JSONObject().apply {
                put("id", budget.id)
                put("category", budget.category)
                put("monthlyBudget", budget.monthlyBudget)
                put("month", budget.month)
                put("spent", budget.spent)
                put("createdAt", budget.createdAt)
            })
        }
        jsonObject.put("budgets", budgetsArray)

        // Write to file
        val file = File(destinationPath)
        FileWriter(file).use { writer ->
            writer.write(jsonObject.toString(2)) // Pretty print with 2-space indent
        }

        return file.absolutePath
    }

    private suspend fun createCsvBackup(destinationPath: String): String {
        val file = File(destinationPath)
        FileWriter(file).use { writer ->
            // Write header
            writer.appendLine("id,type,amount,category,description,date,createdAt")
            
            // Write transactions
            val transactions = getAllTransactionsSync()
            transactions.forEach { transaction ->
                writer.appendLine(
                    "${transaction.id}," +
                    "${transaction.type.name}," +
                    "${transaction.amount}," +
                    "\"${escapeCsv(transaction.category)}\"," +
                    "\"${escapeCsv(transaction.description)}\"," +
                    "${transaction.date}," +
                    "${transaction.createdAt}"
                )
            }
        }
        return file.absolutePath
    }

    private suspend fun restoreFromJson(sourcePath: String): Boolean {
        val file = File(sourcePath)
        val reader = BufferedReader(FileReader(file))
        
        try {
            val jsonString = reader.readText()
            val jsonObject = JSONObject(jsonString)
            
            // Restore transactions
            val transactionsArray = jsonObject.optJSONArray("transactions")
            if (transactionsArray != null) {
                val transactions = mutableListOf<Transaction>()
                for (i in 0 until transactionsArray.length()) {
                    val obj = transactionsArray.getJSONObject(i)
                    transactions.add(Transaction(
                        id = obj.optLong("id", 0),
                        type = Transaction.TransactionType.valueOf(obj.getString("type")),
                        amount = obj.getDouble("amount"),
                        category = obj.getString("category"),
                        description = obj.getString("description"),
                        date = obj.getLong("date"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    ))
                }
                // Clear existing transactions and insert new ones
                transactionRepository.deleteAllTransactions()
                transactionRepository.insertTransactions(transactions)
            }

            // Restore budgets
            val budgetsArray = jsonObject.optJSONArray("budgets")
            if (budgetsArray != null) {
                // Note: Budget restore would require BudgetRepository implementation
                // For now, we skip budget restore or implement similarly
            }

            return true
        } finally {
            reader.close()
        }
    }

    private suspend fun restoreFromCsv(sourcePath: String): Boolean {
        val file = File(sourcePath)
        val reader = BufferedReader(FileReader(file))
        
        try {
            var line: String?
            var isFirstLine = true
            val transactions = mutableListOf<Transaction>()

            while (reader.readLine().also { line = it } != null) {
                if (isFirstLine) {
                    isFirstLine = false
                    continue // Skip header
                }

                val values = parseCsvLine(line!!)
                if (values.size >= 7) {
                    transactions.add(Transaction(
                        id = values[0].toLongOrNull() ?: 0,
                        type = Transaction.TransactionType.valueOf(values[1]),
                        amount = values[2].toDoubleOrNull() ?: 0.0,
                        category = unescapeCsv(values[3]),
                        description = unescapeCsv(values[4]),
                        date = values[5].toLongOrNull() ?: System.currentTimeMillis(),
                        createdAt = values[6].toLongOrNull() ?: System.currentTimeMillis()
                    ))
                }
            }

            // Clear existing transactions and insert new ones
            transactionRepository.deleteAllTransactions()
            transactionRepository.insertTransactions(transactions)

            return true
        } finally {
            reader.close()
        }
    }

    private suspend fun getAllTransactionsSync(): List<Transaction> {
        // This is a simplified version - in production, use proper coroutine handling
        var result: List<Transaction> = emptyList()
        transactionRepository.getAllTransactions().collect {
            result = it
        }
        return result
    }

    private suspend fun getAllBudgetsSync(): List<Budget> {
        var result: List<Budget> = emptyList()
        budgetRepository.getAllBudgets().collect {
            result = it
        }
        return result
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }

    private fun unescapeCsv(value: String): String {
        return value.removeSurrounding("\"").replace("\"\"", "\"")
    }

    private fun parseCsvLine(line: String): List<String> {
        val values = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when (char) {
                '"' -> inQuotes = !inQuotes
                ',' -> {
                    if (inQuotes) {
                        current.append(char)
                    } else {
                        values.add(current.toString())
                        current = StringBuilder()
                    }
                }
                else -> current.append(char)
            }
        }
        values.add(current.toString())

        return values
    }
}
