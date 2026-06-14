package com.constructiontracker.backup

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

private const val DRIVE_SCOPE = "oauth2:https://www.googleapis.com/auth/drive.appdata"
private const val BACKUP_FILENAME = "construction_tracker_backup.json"

class DriveBackupManager(private val context: Context) {

    fun uploadBackup(
        account: Account,
        json: String,
        existingFileId: String?,
        onNewFileId: (String) -> Unit
    ): Boolean {
        val token = getToken(account) ?: return false

        // Try updating the cached file ID first
        if (existingFileId != null && updateFile(token, existingFileId, json)) return true

        // Search for an existing file in appDataFolder
        val foundId = findFile(token)
        if (foundId != null) {
            onNewFileId(foundId)
            return updateFile(token, foundId, json)
        }

        // Create a new file
        val newId = createFile(token, json) ?: return false
        onNewFileId(newId)
        return true
    }

    private fun getToken(account: Account): String? = try {
        GoogleAuthUtil.getToken(context, account, DRIVE_SCOPE)
    } catch (_: UserRecoverableAuthException) { null }
      catch (_: GoogleAuthException)          { null }
      catch (_: IOException)                  { null }

    private fun findFile(token: String): String? {
        val url = URL(
            "https://www.googleapis.com/drive/v3/files" +
            "?spaces=appDataFolder&q=name%3D'$BACKUP_FILENAME'&fields=files(id)"
        )
        val conn = url.openConnection() as HttpsURLConnection
        return try {
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")
            if (conn.responseCode == 200) {
                val files = JSONObject(conn.inputStream.bufferedReader().readText())
                    .getJSONArray("files")
                if (files.length() > 0) files.getJSONObject(0).getString("id") else null
            } else null
        } finally { conn.disconnect() }
    }

    private fun createFile(token: String, content: String): String? {
        val boundary = "ct_boundary_${System.currentTimeMillis()}"
        val url = URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
        val conn = url.openConnection() as HttpsURLConnection
        return try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
            conn.doOutput = true

            val metadata = """{"name":"$BACKUP_FILENAME","parents":["appDataFolder"]}"""
            val body = "--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n" +
                       "$metadata\r\n--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n" +
                       "$content\r\n--$boundary--"
            conn.outputStream.bufferedWriter().use { it.write(body) }

            if (conn.responseCode in 200..201)
                JSONObject(conn.inputStream.bufferedReader().readText()).getString("id")
            else null
        } finally { conn.disconnect() }
    }

    private fun updateFile(token: String, fileId: String, content: String): Boolean {
        val url = URL(
            "https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media"
        )
        val conn = url.openConnection() as HttpsURLConnection
        return try {
            conn.requestMethod = "PATCH"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.doOutput = true
            conn.outputStream.bufferedWriter().use { it.write(content) }
            conn.responseCode in 200..204
        } catch (_: Exception) { false }
         finally { conn.disconnect() }
    }
}
