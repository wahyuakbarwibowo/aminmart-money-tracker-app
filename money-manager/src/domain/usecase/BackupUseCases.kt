package com.aminmart.moneymanager.domain.usecase

import com.aminmart.moneymanager.domain.repository.BackupRepository

/**
 * Use case to create backup
 */
class CreateBackupUseCase(
    private val repository: BackupRepository
) {
    suspend operator fun invoke(
        format: BackupRepository.BackupFormat,
        destinationPath: String
    ): String? {
        return repository.createBackup(format, destinationPath)
    }
}

/**
 * Use case to restore backup
 */
class RestoreBackupUseCase(
    private val repository: BackupRepository
) {
    suspend operator fun invoke(sourcePath: String): Boolean {
        return repository.restoreBackup(sourcePath)
    }
}

/**
 * Use case to get available backups
 */
class GetAvailableBackupsUseCase(
    private val repository: BackupRepository
) {
    suspend operator fun invoke(): List<String> {
        return repository.getAvailableBackups()
    }
}

/**
 * Use case to auto backup
 */
class AutoBackupUseCase(
    private val repository: BackupRepository
) {
    suspend operator fun invoke(): String? {
        return repository.autoBackup()
    }
}

/**
 * Use case to delete backup
 */
class DeleteBackupUseCase(
    private val repository: BackupRepository
) {
    suspend operator fun invoke(filePath: String): Boolean {
        return repository.deleteBackup(filePath)
    }
}
