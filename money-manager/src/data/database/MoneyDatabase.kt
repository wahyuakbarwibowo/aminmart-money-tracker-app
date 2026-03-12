package com.aminmart.moneymanager.data.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.aminmart.moneymanager.domain.model.Transaction
import com.aminmart.moneymanager.domain.model.Budget
import com.aminmart.moneymanager.domain.model.ImportHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * SQLite Database helper for Money Manager
 */
class MoneyDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "moneymanager.db"
        private const val DATABASE_VERSION = 1

        // Transaction table
        private const val TABLE_TRANSACTIONS = "transactions"
        private const val COL_ID = "id"
        private const val COL_TYPE = "type"
        private const val COL_AMOUNT = "amount"
        private const val COL_CATEGORY = "category"
        private const val COL_DESCRIPTION = "description"
        private const val COL_DATE = "date"
        private const val COL_CREATED_AT = "created_at"

        // Budget table
        private const val TABLE_BUDGETS = "budgets"
        private const val COL_MONTHLY_BUDGET = "monthly_budget"
        private const val COL_MONTH = "month"
        private const val COL_SPENT = "spent"

        // Import history table
        private const val TABLE_IMPORT_HISTORY = "import_history"
        private const val COL_FILE_NAME = "file_name"
        private const val COL_IMPORT_DATE = "import_date"
        private const val COL_TRANSACTION_COUNT = "transaction_count"
        private const val COL_STATUS = "status"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create transactions table
        db.execSQL("""
            CREATE TABLE $TABLE_TRANSACTIONS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TYPE TEXT NOT NULL,
                $COL_AMOUNT REAL NOT NULL,
                $COL_CATEGORY TEXT NOT NULL,
                $COL_DESCRIPTION TEXT,
                $COL_DATE INTEGER NOT NULL,
                $COL_CREATED_AT INTEGER NOT NULL
            )
        """)

        // Create budgets table
        db.execSQL("""
            CREATE TABLE $TABLE_BUDGETS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CATEGORY TEXT NOT NULL,
                $COL_MONTHLY_BUDGET REAL NOT NULL,
                $COL_MONTH TEXT NOT NULL,
                $COL_SPENT REAL DEFAULT 0,
                $COL_CREATED_AT INTEGER NOT NULL,
                UNIQUE($COL_CATEGORY, $COL_MONTH)
            )
        """)

        // Create import history table
        db.execSQL("""
            CREATE TABLE $TABLE_IMPORT_HISTORY (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_FILE_NAME TEXT NOT NULL,
                $COL_IMPORT_DATE INTEGER NOT NULL,
                $COL_TRANSACTION_COUNT INTEGER DEFAULT 0,
                $COL_STATUS TEXT DEFAULT 'SUCCESS'
            )
        """)

        // Create indexes for better performance
        db.execSQL("CREATE INDEX idx_transactions_type ON $TABLE_TRANSACTIONS($COL_TYPE)")
        db.execSQL("CREATE INDEX idx_transactions_date ON $TABLE_TRANSACTIONS($COL_DATE)")
        db.execSQL("CREATE INDEX idx_transactions_category ON $TABLE_TRANSACTIONS($COL_CATEGORY)")
        db.execSQL("CREATE INDEX idx_budgets_month ON $TABLE_BUDGETS($COL_MONTH)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle database upgrades
        if (oldVersion < newVersion) {
            // For now, just recreate tables
            db.execSQL("DROP TABLE IF EXISTS $TABLE_TRANSACTIONS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_BUDGETS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_IMPORT_HISTORY")
            onCreate(db)
        }
    }

    // ==================== Transaction Operations ====================

    fun getAllTransactions(): Flow<List<Transaction>> = flow {
        emit(queryAllTransactions())
    }

    fun getTransactionsByType(type: Transaction.TransactionType): Flow<List<Transaction>> = flow {
        emit(queryTransactionsByType(type))
    }

    fun getTransactionsByMonth(year: Int, month: Int): Flow<List<Transaction>> = flow {
        emit(queryTransactionsByMonth(year, month))
    }

    fun getTransactionsByCategory(category: String): Flow<List<Transaction>> = flow {
        emit(queryTransactionsByCategory(category))
    }

    fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = flow {
        emit(queryRecentTransactions(limit))
    }

    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> = flow {
        emit(queryTransactionsByDateRange(startDate, endDate))
    }

    suspend fun getTransactionById(id: Long): Transaction? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            "$COL_ID = ?",
            arrayOf(id.toString()),
            null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) {
                cursorToTransaction(it)
            } else {
                null
            }
        }
    }

    suspend fun insertTransaction(transaction: Transaction): Long {
        val db = writableDatabase
        val values = transactionToContentValues(transaction)
        return db.insert(TABLE_TRANSACTIONS, null, values)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        val db = writableDatabase
        val values = transactionToContentValues(transaction)
        db.update(
            TABLE_TRANSACTIONS,
            values,
            "$COL_ID = ?",
            arrayOf(transaction.id.toString())
        )
    }

    suspend fun deleteTransaction(id: Long) {
        val db = writableDatabase
        db.delete(TABLE_TRANSACTIONS, "$COL_ID = ?", arrayOf(id.toString()))
    }

    suspend fun deleteAllTransactions() {
        val db = writableDatabase
        db.delete(TABLE_TRANSACTIONS, null, null)
    }

    suspend fun getTotalIncome(startDate: Long, endDate: Long): Double {
        val db = readableDatabase
        val cursor = db.rawQuery("""
            SELECT SUM($COL_AMOUNT) FROM $TABLE_TRANSACTIONS
            WHERE $COL_TYPE = ? AND $COL_DATE BETWEEN ? AND ?
        """, arrayOf(Transaction.TransactionType.INCOME.name, startDate.toString(), endDate.toString()))
        return cursor.use {
            if (it.moveToFirst()) it.getDouble(0) else 0.0
        }
    }

    suspend fun getTotalExpense(startDate: Long, endDate: Long): Double {
        val db = readableDatabase
        val cursor = db.rawQuery("""
            SELECT SUM($COL_AMOUNT) FROM $TABLE_TRANSACTIONS
            WHERE $COL_TYPE = ? AND $COL_DATE BETWEEN ? AND ?
        """, arrayOf(Transaction.TransactionType.EXPENSE.name, startDate.toString(), endDate.toString()))
        return cursor.use {
            if (it.moveToFirst()) it.getDouble(0) else 0.0
        }
    }

    suspend fun getExpenseByCategory(startDate: Long, endDate: Long): Map<String, Double> {
        val db = readableDatabase
        val cursor = db.rawQuery("""
            SELECT $COL_CATEGORY, SUM($COL_AMOUNT) FROM $TABLE_TRANSACTIONS
            WHERE $COL_TYPE = ? AND $COL_DATE BETWEEN ? AND ?
            GROUP BY $COL_CATEGORY
        """, arrayOf(Transaction.TransactionType.EXPENSE.name, startDate.toString(), endDate.toString()))
        
        val result = mutableMapOf<String, Double>()
        cursor.use {
            while (it.moveToNext()) {
                result[it.getString(0)] = it.getDouble(1)
            }
        }
        return result
    }

    suspend fun getMonthlyExpenses(months: Int): Map<String, Double> {
        val db = readableDatabase
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.MONTH, -months + 1)
        val startDate = calendar.timeInMillis
        
        val cursor = db.rawQuery("""
            SELECT strftime('%Y-%m', $COL_DATE / 1000, 'unixepoch') as month, SUM($COL_AMOUNT)
            FROM $TABLE_TRANSACTIONS
            WHERE $COL_TYPE = ? AND $COL_DATE >= ?
            GROUP BY month
            ORDER BY month
        """, arrayOf(Transaction.TransactionType.EXPENSE.name, startDate.toString()))
        
        val result = mutableMapOf<String, Double>()
        cursor.use {
            while (it.moveToNext()) {
                result[it.getString(0)] = it.getDouble(1)
            }
        }
        return result
    }

    suspend fun transactionExists(date: Long, amount: Double, description: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("""
            SELECT COUNT(*) FROM $TABLE_TRANSACTIONS
            WHERE $COL_DATE = ? AND $COL_AMOUNT = ? AND $COL_DESCRIPTION = ?
        """, arrayOf(date.toString(), amount.toString(), description))
        return cursor.use {
            it.moveToFirst() && it.getInt(0) > 0
        }
    }

    suspend fun insertTransactions(transactions: List<Transaction>): List<Long> {
        val db = writableDatabase
        val ids = mutableListOf<Long>()
        db.beginTransaction()
        try {
            transactions.forEach { transaction ->
                val id = db.insert(TABLE_TRANSACTIONS, null, transactionToContentValues(transaction))
                ids.add(id)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return ids
    }

    // ==================== Budget Operations ====================

    fun getAllBudgets(): Flow<List<Budget>> = flow {
        emit(queryAllBudgets())
    }

    fun getBudgetsByMonth(month: String): Flow<List<Budget>> = flow {
        emit(queryBudgetsByMonth(month))
    }

    suspend fun getBudgetByCategory(category: String, month: String): Budget? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_BUDGETS,
            null,
            "$COL_CATEGORY = ? AND $COL_MONTH = ?",
            arrayOf(category, month),
            null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) {
                cursorToBudget(it)
            } else {
                null
            }
        }
    }

    suspend fun getBudgetById(id: Long): Budget? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_BUDGETS,
            null,
            "$COL_ID = ?",
            arrayOf(id.toString()),
            null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) {
                cursorToBudget(it)
            } else {
                null
            }
        }
    }

    suspend fun insertBudget(budget: Budget): Long {
        val db = writableDatabase
        val values = budgetToContentValues(budget)
        return db.insertWithOnConflict(TABLE_BUDGETS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    suspend fun updateBudget(budget: Budget) {
        val db = writableDatabase
        val values = budgetToContentValues(budget)
        db.update(
            TABLE_BUDGETS,
            values,
            "$COL_ID = ?",
            arrayOf(budget.id.toString())
        )
    }

    suspend fun deleteBudget(id: Long) {
        val db = writableDatabase
        db.delete(TABLE_BUDGETS, "$COL_ID = ?", arrayOf(id.toString()))
    }

    suspend fun updateBudgetSpent(category: String, month: String, spent: Double) {
        val db = writableDatabase
        val values = ContentValues()
        values.put(COL_SPENT, spent)
        db.update(
            TABLE_BUDGETS,
            values,
            "$COL_CATEGORY = ? AND $COL_MONTH = ?",
            arrayOf(category, month)
        )
    }

    suspend fun getTotalBudgetForMonth(month: String): Double {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT SUM($COL_MONTHLY_BUDGET) FROM $TABLE_BUDGETS WHERE $COL_MONTH = ?",
            arrayOf(month)
        )
        return cursor.use {
            if (it.moveToFirst()) it.getDouble(0) else 0.0
        }
    }

    suspend fun getTotalSpentForMonth(month: String): Double {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT SUM($COL_SPENT) FROM $TABLE_BUDGETS WHERE $COL_MONTH = ?",
            arrayOf(month)
        )
        return cursor.use {
            if (it.moveToFirst()) it.getDouble(0) else 0.0
        }
    }

    // ==================== Import History Operations ====================

    fun getAllImportHistory(): Flow<List<ImportHistory>> = flow {
        emit(queryAllImportHistory())
    }

    suspend fun getImportHistoryById(id: Long): ImportHistory? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_IMPORT_HISTORY,
            null,
            "$COL_ID = ?",
            arrayOf(id.toString()),
            null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) {
                cursorToImportHistory(it)
            } else {
                null
            }
        }
    }

    suspend fun insertImportHistory(history: ImportHistory): Long {
        val db = writableDatabase
        val values = importHistoryToContentValues(history)
        return db.insert(TABLE_IMPORT_HISTORY, null, values)
    }

    suspend fun isFileImported(fileName: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_IMPORT_HISTORY,
            arrayOf(COL_ID),
            "$COL_FILE_NAME = ?",
            arrayOf(fileName),
            null, null, null
        )
        return cursor.use { it.count > 0 }
    }

    suspend fun deleteImportHistory(id: Long) {
        val db = writableDatabase
        db.delete(TABLE_IMPORT_HISTORY, "$COL_ID = ?", arrayOf(id.toString()))
    }

    // ==================== Helper Methods ====================

    private fun queryAllTransactions(): List<Transaction> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            null, null, null, null,
            "$COL_DATE DESC"
        )
        return cursorToTransactionList(cursor)
    }

    private fun queryTransactionsByType(type: Transaction.TransactionType): List<Transaction> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            "$COL_TYPE = ?",
            arrayOf(type.name),
            null, null,
            "$COL_DATE DESC"
        )
        return cursorToTransactionList(cursor)
    }

    private fun queryTransactionsByMonth(year: Int, month: Int): List<Transaction> {
        val db = readableDatabase
        val startDate = getMonthStart(year, month)
        val endDate = getMonthEnd(year, month)
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            "$COL_DATE >= ? AND $COL_DATE <= ?",
            arrayOf(startDate.toString(), endDate.toString()),
            null, null,
            "$COL_DATE DESC"
        )
        return cursorToTransactionList(cursor)
    }

    private fun queryTransactionsByCategory(category: String): List<Transaction> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            "$COL_CATEGORY = ?",
            arrayOf(category),
            null, null,
            "$COL_DATE DESC"
        )
        return cursorToTransactionList(cursor)
    }

    private fun queryRecentTransactions(limit: Int): List<Transaction> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            null, null, null, null,
            "$COL_DATE DESC",
            limit.toString()
        )
        return cursorToTransactionList(cursor)
    }

    private fun queryTransactionsByDateRange(startDate: Long, endDate: Long): List<Transaction> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            "$COL_DATE >= ? AND $COL_DATE <= ?",
            arrayOf(startDate.toString(), endDate.toString()),
            null, null,
            "$COL_DATE DESC"
        )
        return cursorToTransactionList(cursor)
    }

    private fun queryAllBudgets(): List<Budget> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_BUDGETS,
            null,
            null, null, null, null,
            "$COL_MONTH DESC, $COL_CATEGORY ASC"
        )
        return cursorToBudgetList(cursor)
    }

    private fun queryBudgetsByMonth(month: String): List<Budget> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_BUDGETS,
            null,
            "$COL_MONTH = ?",
            arrayOf(month),
            null, null,
            "$COL_CATEGORY ASC"
        )
        return cursorToBudgetList(cursor)
    }

    private fun queryAllImportHistory(): List<ImportHistory> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_IMPORT_HISTORY,
            null,
            null, null, null, null,
            "$COL_IMPORT_DATE DESC"
        )
        return cursorToImportHistoryList(cursor)
    }

    private fun cursorToTransactionList(cursor: Cursor): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        cursor.use {
            while (it.moveToNext()) {
                transactions.add(cursorToTransaction(it))
            }
        }
        return transactions
    }

    private fun cursorToTransaction(cursor: Cursor): Transaction {
        return Transaction(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
            type = Transaction.TransactionType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COL_TYPE))),
            amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMOUNT)),
            category = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY)),
            description = cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION) ?: ""),
            date = cursor.getLong(cursor.getColumnIndexOrThrow(COL_DATE)),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED_AT))
        )
    }

    private fun cursorToBudgetList(cursor: Cursor): List<Budget> {
        val budgets = mutableListOf<Budget>()
        cursor.use {
            while (it.moveToNext()) {
                budgets.add(cursorToBudget(it))
            }
        }
        return budgets
    }

    private fun cursorToBudget(cursor: Cursor): Budget {
        return Budget(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
            category = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY)),
            monthlyBudget = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_MONTHLY_BUDGET)),
            month = cursor.getString(cursor.getColumnIndexOrThrow(COL_MONTH)),
            spent = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_SPENT)),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED_AT))
        )
    }

    private fun cursorToImportHistoryList(cursor: Cursor): List<ImportHistory> {
        val history = mutableListOf<ImportHistory>()
        cursor.use {
            while (it.moveToNext()) {
                history.add(cursorToImportHistory(it))
            }
        }
        return history
    }

    private fun cursorToImportHistory(cursor: Cursor): ImportHistory {
        return ImportHistory(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
            fileName = cursor.getString(cursor.getColumnIndexOrThrow(COL_FILE_NAME)),
            importDate = cursor.getLong(cursor.getColumnIndexOrThrow(COL_IMPORT_DATE)),
            transactionCount = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSACTION_COUNT)),
            status = ImportHistory.ImportStatus.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COL_STATUS) ?: "SUCCESS"))
        )
    }

    private fun transactionToContentValues(transaction: Transaction): ContentValues {
        return ContentValues().apply {
            put(COL_TYPE, transaction.type.name)
            put(COL_AMOUNT, transaction.amount)
            put(COL_CATEGORY, transaction.category)
            put(COL_DESCRIPTION, transaction.description)
            put(COL_DATE, transaction.date)
            put(COL_CREATED_AT, transaction.createdAt)
        }
    }

    private fun budgetToContentValues(budget: Budget): ContentValues {
        return ContentValues().apply {
            put(COL_CATEGORY, budget.category)
            put(COL_MONTHLY_BUDGET, budget.monthlyBudget)
            put(COL_MONTH, budget.month)
            put(COL_SPENT, budget.spent)
            put(COL_CREATED_AT, budget.createdAt)
        }
    }

    private fun importHistoryToContentValues(history: ImportHistory): ContentValues {
        return ContentValues().apply {
            put(COL_FILE_NAME, history.fileName)
            put(COL_IMPORT_DATE, history.importDate)
            put(COL_TRANSACTION_COUNT, history.transactionCount)
            put(COL_STATUS, history.status.name)
        }
    }

    private fun getMonthStart(year: Int, month: Int): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getMonthEnd(year: Int, month: Int): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(year, month - 1, 1, 23, 59, 59)
        calendar.set(java.util.Calendar.MILLISECOND, 999)
        calendar.set(java.util.Calendar.DAY_OF_MONTH, calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
        return calendar.timeInMillis
    }
}
