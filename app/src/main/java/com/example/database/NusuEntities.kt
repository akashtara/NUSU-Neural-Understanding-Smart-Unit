package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "nusu_dialogues")
data class NusuDialogue(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String, // "USER" or "NUSU"
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isCodeSnippet: Boolean = false,
    val codeLanguage: String? = null
)

@Entity(tableName = "nusu_moods")
data class MoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val moodTitle: String, // e.g. Happy, Focused, Tired
    val productivityLevel: Int, // 1 to 100
    val privateNotes: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface NusuDao {
    @Query("SELECT * FROM nusu_dialogues ORDER BY timestamp ASC")
    fun getAllDialoguesFlow(): Flow<List<NusuDialogue>>

    @Query("SELECT * FROM nusu_dialogues ORDER BY timestamp ASC")
    suspend fun getAllDialogues(): List<NusuDialogue>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDialogue(dialogue: NusuDialogue): Long

    @Query("DELETE FROM nusu_dialogues WHERE id = :id")
    suspend fun deleteDialogueById(id: Int)

    @Query("DELETE FROM nusu_dialogues")
    suspend fun clearAllDialogues()

    @Query("SELECT * FROM nusu_dialogues WHERE isCodeSnippet = 1 ORDER BY timestamp DESC")
    fun getSavedCodeSnippetsFlow(): Flow<List<NusuDialogue>>
    
    // Mood Queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodEntry(mood: MoodEntry)

    @Query("SELECT * FROM nusu_moods ORDER BY timestamp DESC")
    fun getAllMoodEntriesFlow(): Flow<List<MoodEntry>>
}
