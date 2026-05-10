package com.aminmart.moneymanager.domain.repository

/**
 * Repository interface for Export operations
 */
interface ExportRepository {
    
    /**
     * Export transactions to CSV file
     * @param destinationPath Path to save the CSV file
     * @param startDate Start date filter (optional)
     * @param endDate End date filter (optional)
     * @return Path to the exported file, or null if failed
     */
    suspend fun exportToCsv(
        destinationPath: String,
        startDate: Long? = null,
        endDate: Long? = null
    ): String?
    
    /**
     * Export transactions to Excel file
     * @param destinationPath Path to save the Excel file
     * @param startDate Start date filter (optional)
     * @param endDate End date filter (optional)
     * @return Path to the exported file, or null if failed
     */
    suspend fun exportToExcel(
        destinationPath: String,
        startDate: Long? = null,
        endDate: Long? = null
    ): String?
    
    /**
     * Export report with summary
     * @param destinationPath Path to save the report
     * @param format Export format
     * @return Path to the exported file, or null if failed
     */
    suspend fun exportReport(
        destinationPath: String,
        format: ExportFormat
    ): String?
    
    enum class ExportFormat {
        CSV,
        EXCEL
    }
}
