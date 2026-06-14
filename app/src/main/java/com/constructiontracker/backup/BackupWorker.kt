package com.constructiontracker.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.constructiontracker.ConstructionTrackerApplication
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.flow.first

class BackupWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? ConstructionTrackerApplication
            ?: return Result.failure()

        val account = GoogleSignIn.getLastSignedInAccount(applicationContext)
        val androidAccount = account?.account ?: return Result.failure()

        return try {
            val contractors = app.repository.getAllContractors().first()
            val payments    = app.repository.getAllPayments().first()
            val purchases   = app.repository.getAllPurchases().first()

            val json = BackupSerializer.serialize(contractors, payments, purchases)
            val prefs = app.userPreferences
            val manager = DriveBackupManager(applicationContext)

            val success = manager.uploadBackup(androidAccount, json, prefs.driveFileId) { id ->
                prefs.driveFileId = id
            }

            if (success) {
                prefs.lastBackupTime = System.currentTimeMillis()
                Result.success()
            } else {
                Result.retry()
            }
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
