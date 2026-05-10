package com.aminmart.moneymanager.domain.repository

/**
 * Repository interface for Backup operations
 */
interface BackupRepository {
    
    /**
     * Create a backup of all data
     * @param format The format of backup (JSON or CSV)
     * @param destinationPath The path to save the backup
     * @return The path to the created backup file, or null if failed
     */
    suspend fun createBackup(format: BackupFormat, destinationPath: String): String?
    
    /**
     * Restore data from a backup file
     * @param sourcePath The path to the backup file
     * @return True if restore was successful, false otherwise
     */
    suspend fun restoreBackup(sourcePath: String): Boolean
    
    /**
     * Get list of available backups
     * @return List of backup file paths
     */
    suspend fun getAvailableBackups(): List<String>
    
    /**
     * Delete a backup file
     * @param filePath The path to the backup file
     * @return True if deleted successfully
     */
    suspend fun deleteBackup(filePath: String): Boolean
    
    /**
     * Get the latest backup file
     * @return Path to the latest backup, or null if none exists
     */
    suspend fun getLatestBackup(): String?
    
    /**
     * Auto backup to default location
     * @return Path to the created backup file, or null if failed
     */
    suspend fun autoBackup(): String?
    
    enum class BackupFormat {
        JSON,
        CSV
    }
}
