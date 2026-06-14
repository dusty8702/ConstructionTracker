package com.constructiontracker.ui.screens.account

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.constructiontracker.ConstructionTrackerApplication
import com.constructiontracker.backup.BackupSerializer
import com.constructiontracker.backup.BackupWorker
import com.constructiontracker.backup.DriveBackupManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

data class AccountUiState(
    val isSignedIn: Boolean = false,
    val userName: String? = null,
    val userEmail: String? = null,
    val userInitial: Char? = null,
    val isAutoBackupEnabled: Boolean = false,
    val lastBackupTime: Long? = null,
    val isBackingUp: Boolean = false,
    val backupMessage: String? = null
)

class AccountViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ConstructionTrackerApplication
    private val prefs = app.userPreferences

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        refreshAccountState()
    }

    fun refreshAccountState() {
        val account = GoogleSignIn.getLastSignedInAccount(getApplication())
        _uiState.update {
            if (account != null) {
                it.copy(
                    isSignedIn = true,
                    userName = account.displayName,
                    userEmail = account.email,
                    userInitial = account.displayName?.firstOrNull()?.uppercaseChar(),
                    isAutoBackupEnabled = prefs.isAutoBackupEnabled,
                    lastBackupTime = prefs.lastBackupTime.takeIf { t -> t > 0L }
                )
            } else {
                it.copy(isSignedIn = false, userName = null, userEmail = null, userInitial = null)
            }
        }
    }

    fun buildSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.appdata"))
            .build()
        return GoogleSignIn.getClient(getApplication(), gso)
    }

    fun handleSignInResult(data: Intent?) {
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                .getResult(ApiException::class.java)
            _uiState.update {
                it.copy(
                    isSignedIn = true,
                    userName = account.displayName,
                    userEmail = account.email,
                    userInitial = account.displayName?.firstOrNull()?.uppercaseChar(),
                    isAutoBackupEnabled = prefs.isAutoBackupEnabled,
                    lastBackupTime = prefs.lastBackupTime.takeIf { t -> t > 0L }
                )
            }
        } catch (_: ApiException) { }
    }

    fun signOut() {
        buildSignInClient().signOut().addOnCompleteListener {
            prefs.isAutoBackupEnabled = false
            cancelBackupSchedule()
            _uiState.update {
                AccountUiState()
            }
        }
    }

    fun setAutoBackup(enabled: Boolean) {
        prefs.isAutoBackupEnabled = enabled
        _uiState.update { it.copy(isAutoBackupEnabled = enabled) }
        if (enabled) scheduleBackup() else cancelBackupSchedule()
    }

    fun backupNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackingUp = true, backupMessage = null) }
            val account = GoogleSignIn.getLastSignedInAccount(getApplication())
            val androidAccount = account?.account

            if (androidAccount == null) {
                _uiState.update { it.copy(isBackingUp = false, backupMessage = "Sign in first.") }
                return@launch
            }

            try {
                val contractors = app.repository.getAllContractors().first()
                val payments    = app.repository.getAllPayments().first()
                val purchases   = app.repository.getAllPurchases().first()
                val json = BackupSerializer.serialize(contractors, payments, purchases)
                val manager = DriveBackupManager(getApplication())

                val success = withContext(Dispatchers.IO) {
                    manager.uploadBackup(androidAccount, json, prefs.driveFileId) { id ->
                        prefs.driveFileId = id
                    }
                }

                if (success) {
                    val now = System.currentTimeMillis()
                    prefs.lastBackupTime = now
                    _uiState.update { it.copy(isBackingUp = false, lastBackupTime = now, backupMessage = "Backup successful.") }
                } else {
                    _uiState.update { it.copy(isBackingUp = false, backupMessage = "Backup failed. Check internet connection.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isBackingUp = false, backupMessage = "Backup failed: ${e.message}") }
            }
        }
    }

    private fun scheduleBackup() {
        val request = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(getApplication()).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun cancelBackupSchedule() {
        WorkManager.getInstance(getApplication()).cancelUniqueWork(WORK_NAME)
    }

    companion object {
        private const val WORK_NAME = "drive_daily_backup"
    }
}
