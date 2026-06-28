package com.example.services

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object BackupManager {
    private const val TAG = "BackupManager"

    fun performBackup(context: Context) {
        try {
            val prefs = context.getSharedPreferences("NUSU_PRO_PREFS", Context.MODE_PRIVATE)
            val backupData = JSONObject()

            val allEntries = prefs.all
            for ((key, value) in allEntries) {
                backupData.put(key, value)
            }

            // Serialize and save to local file
            val backupDir = File(context.filesDir, "backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            val backupFile = File(backupDir, "nusu_state_backup_${System.currentTimeMillis()}.json")
            val outputStream = FileOutputStream(backupFile)
            outputStream.write(backupData.toString(4).toByteArray())
            outputStream.close()

            Log.d(TAG, "Backup completed successfully at ${backupFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error performing backup: ${e.message}")
        }
    }
}
