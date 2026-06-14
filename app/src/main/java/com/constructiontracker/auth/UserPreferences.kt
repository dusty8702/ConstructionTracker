package com.constructiontracker.auth

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    var isAutoBackupEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_BACKUP, false)
        set(v) { prefs.edit().putBoolean(KEY_AUTO_BACKUP, v).apply() }

    var lastBackupTime: Long
        get() = prefs.getLong(KEY_LAST_BACKUP, 0L)
        set(v) { prefs.edit().putLong(KEY_LAST_BACKUP, v).apply() }

    var driveFileId: String?
        get() = prefs.getString(KEY_FILE_ID, null)
        set(v) { prefs.edit().putString(KEY_FILE_ID, v).apply() }

    companion object {
        private const val KEY_AUTO_BACKUP = "auto_backup_enabled"
        private const val KEY_LAST_BACKUP  = "last_backup_time"
        private const val KEY_FILE_ID      = "drive_file_id"
    }
}
