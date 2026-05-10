package com.aminmart.moneymanager.data.importer

import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.domain.repository.CsvImportRepository
import com.aminmart.moneymanager.domain.repository.CsvImportResult
import java.io.File
import java.io.BufferedReader
import java.io.FileReader

/**
 * CSV Importer for bank transaction statements
 */
class CsvImporter(
    private val transactionRepository: CsvImportRepository
) : CsvImportRepository {

    companion object {
        private const val DATE_FORMATS = listOf(
            "yyyy-MM-dd",
            "dd-MM-yyyy",
            "MM/dd/yyyy",
            "yyyy/MM/dd",
            "dd/MM/yyyy"
        )
    }

    override suspend fun parseCsvFile(filePath: String): CsvImportResult {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                return CsvImportResult(
                    success = false,
                    errorMessage = "File not found: $filePath"
                )
            }

            val transactions = mutableListOf<Transaction>()
            val reader = BufferedReader(FileReader(file))
            
            try {
                var line: String?
                var isFirstLine = true
                var headerMap: Map<String, Int>? = null

                while (reader.readLine().also { line = it } != null) {
                    if (isFirstLine) {
                        headerMap = parseHeader(line!!)
                        isFirstLine = false
                        continue
                    }

                    val transaction = parseLine(line!!, headerMap)
                    if (transaction != null) {
                        transactions.add(transaction)
                    }
                }
            } finally {
                reader.close()
            }

            CsvImportResult(
                success = true,
                importedCount = transactions.size,
                transactions = transactions
            )
        } catch (e: Exception) {
            CsvImportResult(
                success = false,
                errorMessage = "Error parsing CSV: ${e.message}"
            )
        }
    }

    override suspend fun importFromCsv(filePath: String, skipDuplicates: Boolean): CsvImportResult {
        val parseResult = parseCsvFile(filePath)
        
        if (!parseResult.success) {
            return parseResult
        }

        val importedTransactions = mutableListOf<Transaction>()
        var skippedCount = 0
        var failedCount = 0

        parseResult.transactions.forEach { transaction ->
            try {
                if (skipDuplicates) {
                    val exists = transactionRepository.transactionExists(
                        transaction.date,
                        transaction.amount,
                        transaction.description
                    )
                    if (exists) {
                        skippedCount++
                        return@forEach
                    }
                }
                importedTransactions.add(transaction)
            } catch (e: Exception) {
                failedCount++
            }
        }

        return CsvImportResult(
            success = importedTransactions.isNotEmpty(),
            importedCount = importedTransactions.size,
            skippedCount = skippedCount,
            failedCount = failedCount,
            transactions = importedTransactions
        )
    }

    override suspend fun validateCsvFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists()) return false

            val reader = BufferedReader(FileReader(file))
            val headerLine = reader.readLine()
            reader.close()

            if (headerLine == null) return false

            val headerMap = parseHeader(headerLine)
            headerMap.containsKey("date") && 
            headerMap.containsKey("description") && 
            headerMap.containsKey("amount")
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getCsvPreview(filePath: String, limit: Int): List<Transaction> {
        val result = parseCsvFile(filePath)
        return if (result.success) {
            result.transactions.take(limit)
        } else {
            emptyList()
        }
    }

    private fun parseHeader(headerLine: String): Map<String, Int> {
        val headers = headerLine.split(",").map { it.trim().lowercase() }
        return headers.mapIndexed { index, header ->
            when {
                header.contains("date") -> "date" to index
                header.contains("desc") || header.contains("particular") -> "description" to index
                header.contains("amount") || header.contains("value") -> "amount" to index
                header.contains("type") || header.contains("dr/cr") -> "type" to index
                header.contains("category") -> "category" to index
                else -> null
            }
        }.filterNotNull().toMap()
    }

    private fun parseLine(line: String, headerMap: Map<String, Int>): Transaction? {
        try {
            val values = line.split(",").map { it.trim() }
            
            val dateIndex = headerMap["date"] ?: return null
            val descriptionIndex = headerMap["description"] ?: return null
            val amountIndex = headerMap["amount"] ?: return null

            if (values.size <= maxOf(dateIndex, descriptionIndex, amountIndex)) {
                return null
            }

            val date = parseDate(values[dateIndex])
            val description = values[descriptionIndex]
            val amount = parseAmount(values[amountIndex])
            
            val typeIndex = headerMap["type"]
            val type = if (typeIndex != null && values.size > typeIndex) {
                parseType(values[typeIndex], amount)
            } else {
                if (amount > 0) Transaction.TransactionType.INCOME else Transaction.TransactionType.EXPENSE
            }

            val categoryIndex = headerMap["category"]
            val category = if (categoryIndex != null && values.size > categoryIndex) {
                values[categoryIndex].ifEmpty { "Other" }
            } else {
                categorizeTransaction(description, type)
            }

            return Transaction(
                type = type,
                amount = kotlin.math.abs(amount),
                category = category,
                description = description,
                date = date
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseDate(dateString: String): Long {
        // Try multiple date formats
        val formats = listOf(
            "yyyy-MM-dd",
            "dd-MM-yyyy",
            "MM/dd/yyyy",
            "yyyy/MM/dd",
            "dd/MM/yyyy",
            "yyyy-MM-dd HH:mm:ss",
            "dd-MM-yyyy HH:mm:ss"
        )

        for (format in formats) {
            try {
                val sdf = java.text.SimpleDateFormat(format, java.util.Locale.getDefault())
                sdf.isLenient = false
                return sdf.parse(dateString)?.time ?: continue
            } catch (e: Exception) {
                continue
            }
        }

        // Fallback: try to extract date using regex
        val regex = Regex("(\\d{4}[-/]\\d{1,2}[-/]\\d{1,2})|(\\d{1,2}[-/]\\d{1,2}[-/]\\d{4})")
        val match = regex.find(dateString)
        if (match != null) {
            val extractedDate = match.value
            return parseDate(extractedDate)
        }

        // Last resort: return current time
        return System.currentTimeMillis()
    }

    private fun parseAmount(amountString: String): Double {
        // Remove currency symbols and clean up
        val cleaned = amountString
            .replace(Regex("[^\\d.,\\-]"), "")
            .replace(",", "")
        
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    private fun parseType(typeString: String, amount: Double): Transaction.TransactionType {
        val normalized = typeString.lowercase().trim()
        return when {
            normalized.contains("cr") || normalized.contains("credit") -> Transaction.TransactionType.INCOME
            normalized.contains("dr") || normalized.contains("debit") -> Transaction.TransactionType.EXPENSE
            else -> if (amount > 0) Transaction.TransactionType.INCOME else Transaction.TransactionType.EXPENSE
        }
    }

    private fun categorizeTransaction(description: String, type: Transaction.TransactionType): String {
        val lowerDesc = description.lowercase()
        
        val expenseCategories = mapOf(
            listOf("food", "restaurant", "cafe", "coffee", "lunch", "dinner", "breakfast") to "Food & Drink",
            listOf("uber", "grab", "gojek", "taxi", "fuel", "gas", "parking", "toll") to "Transportation",
            listOf("shopping", "mall", "store", "amazon", "shop") to "Shopping",
            listOf("movie", "cinema", "netflix", "spotify", "game", "entertainment") to "Entertainment",
            listOf("electric", "water", "internet", "phone", "utility", "bill") to "Bills & Utilities",
            listOf("hospital", "clinic", "pharmacy", "medicine", "health", "doctor") to "Health",
            listOf("school", "university", "course", "education", "book") to "Education",
            listOf("rent", "mortgage", "property", "housing") to "Housing"
        )

        val incomeCategories = mapOf(
            listOf("salary", "payroll", "wage") to "Salary",
            listOf("business", "company", "client") to "Business",
            listOf("investment", "dividend", "interest", "stock") to "Investment",
            listOf("gift", "present", "bonus") to "Gift"
        )

        if (type == Transaction.TransactionType.EXPENSE) {
            for ((keywords, category) in expenseCategories) {
                if (keywords.any { lowerDesc.contains(it) })) {
                    return category
                }
            }
        } else {
            for ((keywords, category) in incomeCategories) {
                if (keywords.any { lowerDesc.contains(it) })) {
                    return category
                }
            }
        }

        return "Other"
    }
}
