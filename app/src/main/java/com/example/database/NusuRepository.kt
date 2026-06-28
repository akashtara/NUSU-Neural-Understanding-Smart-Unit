package com.example.database

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class NusuRepository(private val nusuDao: NusuDao) {

    val allDialogues: Flow<List<NusuDialogue>> = nusuDao.getAllDialoguesFlow()

    val savedCodeSnippets: Flow<List<NusuDialogue>> = nusuDao.getSavedCodeSnippetsFlow()

    val allMoodEntries: Flow<List<MoodEntry>> = nusuDao.getAllMoodEntriesFlow()

    suspend fun insertMoodEntry(mood: MoodEntry) = withContext(Dispatchers.IO) {
        nusuDao.insertMoodEntry(mood)
    }

    suspend fun insertDialogue(dialogue: NusuDialogue): Long = withContext(Dispatchers.IO) {
        nusuDao.insertDialogue(dialogue)
    }

    suspend fun deleteDialogueById(id: Int) = withContext(Dispatchers.IO) {
        nusuDao.deleteDialogueById(id)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        nusuDao.clearAllDialogues()
    }

    suspend fun getAllDialoguesDirect(): List<NusuDialogue> = withContext(Dispatchers.IO) {
        nusuDao.getAllDialogues()
    }

    /**
     * Backup helper: Converts all stored dialogues/code snippets to a JSON file in the app private files folder
     * and returns the file path.
     */
    suspend fun exportCodeAndDialogBackup(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            val list = nusuDao.getAllDialogues()
            val root = JSONObject()
            root.put("version", 1)
            root.put("createdAt", System.currentTimeMillis())
            root.put("type", "NUSU_CODE_DIALOGUE_BACKUP")
            
            val array = JSONArray()
            for (dialogue in list) {
                val item = JSONObject()
                item.put("sender", dialogue.sender)
                item.put("messageText", dialogue.messageText)
                item.put("timestamp", dialogue.timestamp)
                item.put("isCodeSnippet", dialogue.isCodeSnippet)
                item.put("codeLanguage", dialogue.codeLanguage ?: "")
                array.put(item)
            }
            root.put("data", array)

            // Save inside external files dir or filesDir
            val backupDir = File(context.filesDir, "nusu_backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            val backupFile = File(backupDir, "nusu_memory_backup.json")
            backupFile.writeText(root.toString(4))
            backupFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Restore helper: Imports a previously backed-up file JSON string, parses and saves it into the database.
     */
    suspend fun restoreCodeAndDialogBackup(context: Context, backupJson: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(backupJson)
            val formatType = root.optString("type", "")
            if (formatType != "NUSU_CODE_DIALOGUE_BACKUP") {
                return@withContext false
            }

            val array = root.optJSONArray("data") ?: return@withContext false
            
            // Perform in transaction or pure sequence, we can clear and import
            nusuDao.clearAllDialogues()

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val sender = item.optString("sender", "NUSU")
                val messageText = item.optString("messageText", "")
                val timestamp = item.optLong("timestamp", System.currentTimeMillis())
                val isCodeSnippet = item.optBoolean("isCodeSnippet", false)
                val codeLanguage = item.optString("codeLanguage", "")

                val dialogue = NusuDialogue(
                    sender = sender,
                    messageText = messageText,
                    timestamp = timestamp,
                    isCodeSnippet = isCodeSnippet,
                    codeLanguage = if (codeLanguage.isNotEmpty()) codeLanguage else null
                )
                nusuDao.insertDialogue(dialogue)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Permanent Sentinel Auto-Update: Saves all dialogues directly into a protected path
     * that is decoupled from normal DB clears.
     */
    suspend fun updatePermanentSentinel(context: Context) = withContext(Dispatchers.IO) {
        try {
            val list = nusuDao.getAllDialogues()
            val root = JSONObject()
            root.put("version", 1)
            root.put("createdAt", System.currentTimeMillis())
            root.put("type", "NUSU_PERMANENT_SENTINEL_BACKUP")
            
            val array = JSONArray()
            for (dialogue in list) {
                val item = JSONObject()
                item.put("sender", dialogue.sender)
                item.put("messageText", dialogue.messageText)
                item.put("timestamp", dialogue.timestamp)
                item.put("isCodeSnippet", dialogue.isCodeSnippet)
                item.put("codeLanguage", dialogue.codeLanguage ?: "")
                array.put(item)
            }
            root.put("data", array)

            val sentinelDir = File(context.filesDir, "nusu_permanent_vault")
            if (!sentinelDir.exists()) {
                sentinelDir.mkdirs()
            }
            val sentinelFile = File(sentinelDir, "sentinel_brain_archive.json")
            sentinelFile.writeText(root.toString(4))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Restore from Protected Sentinel: Re-seeds standard database structures from the decoupled archive.
     */
    suspend fun restoreFromPermanentSentinel(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val sentinelDir = File(context.filesDir, "nusu_permanent_vault")
            val sentinelFile = File(sentinelDir, "sentinel_brain_archive.json")
            if (!sentinelFile.exists()) return@withContext false
            val backupJson = sentinelFile.readText()
            
            val root = JSONObject(backupJson)
            val array = root.optJSONArray("data") ?: return@withContext false
            
            nusuDao.clearAllDialogues()
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val sender = item.optString("sender", "NUSU")
                val messageText = item.optString("messageText", "")
                val timestamp = item.optLong("timestamp", System.currentTimeMillis())
                val isCodeSnippet = item.optBoolean("isCodeSnippet", false)
                val codeLanguage = item.optString("codeLanguage", "")

                val dialogue = NusuDialogue(
                    sender = sender,
                    messageText = messageText,
                    timestamp = timestamp,
                    isCodeSnippet = isCodeSnippet,
                    codeLanguage = if (codeLanguage.isNotEmpty()) codeLanguage else null
                )
                nusuDao.insertDialogue(dialogue)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
