package com.aminmart.moneymanager.data.repository

import com.aminmart.moneymanager.data.database.MoneyDatabase
import com.aminmart.moneymanager.domain.model.ImportHistory
import com.aminmart.moneymanager.domain.repository.ImportHistoryRepository
import kotlinx.coroutines.flow.Flow

/**
 * Implementation of ImportHistoryRepository using SQLite database
 */
class ImportHistoryRepositoryImpl(
    private val database: MoneyDatabase
) : ImportHistoryRepository {

    override fun getAllImportHistory(): Flow<List<ImportHistory>> =
        database.getAllImportHistory()

    override suspend fun getImportHistoryById(id: Long): ImportHistory? =
        database.getImportHistoryById(id)

    override suspend fun insertImportHistory(history: ImportHistory): Long =
        database.insertImportHistory(history)

    override suspend fun isFileImported(fileName: String): Boolean =
        database.isFileImported(fileName)

    override suspend fun deleteImportHistory(id: Long) =
        database.deleteImportHistory(id)
}
