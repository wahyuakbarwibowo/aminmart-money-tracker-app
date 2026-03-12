package com.aminmart.moneymanager.domain.repository

import com.aminmart.moneymanager.domain.model.ImportHistory
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Import History operations
 */
interface ImportHistoryRepository {
    
    /**
     * Get all import history as Flow
     */
    fun getAllImportHistory(): Flow<List<ImportHistory>>
    
    /**
     * Get import history by ID
     */
    suspend fun getImportHistoryById(id: Long): ImportHistory?
    
    /**
     * Insert import history
     */
    suspend fun insertImportHistory(history: ImportHistory): Long
    
    /**
     * Check if file was already imported
     */
    suspend fun isFileImported(fileName: String): Boolean
    
    /**
     * Delete import history
     */
    suspend fun deleteImportHistory(id: Long)
}
