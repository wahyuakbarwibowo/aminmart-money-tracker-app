package com.aminmart.moneymanager.domain.model

/**
 * Import history model to track CSV imports
 */
data class ImportHistory(
    val id: Long = 0,
    val fileName: String,
    val importDate: Long = System.currentTimeMillis(),
    val transactionCount: Int = 0,
    val status: ImportStatus = ImportStatus.SUCCESS
) {
    enum class ImportStatus {
        SUCCESS,
        PARTIAL,
        FAILED
    }
}
