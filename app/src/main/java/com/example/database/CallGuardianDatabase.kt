package com.example.database

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ----------------------------------------------------
// ENTITIES
// ----------------------------------------------------

@Entity(tableName = "call_history")
data class CallHistoryRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val callerName: String,
    val phoneNumber: String,
    val companyName: String,
    val reasonForCalling: String,
    val priorityLevel: String, // High, Medium, Low
    val urgencyStatus: String, // Emergency, Normal
    val emotionStatus: String, // Calm, Angry, Happy, Sad, Professional
    val callTime: Long = System.currentTimeMillis(),
    val voiceTranscript: String, // Plain JSON/Text block of dialogue
    val callbackRequest: Boolean = false,
    val callbackDetails: String = "",
    val aiSummary: String = "",
    val spamRisk: Int = 0, // 0 to 100
    val trustScore: Int = 100, // 0 to 100
    val callStatus: String // Blocked, Completed, Connected, Rejected
)

@Entity(tableName = "blocked_contacts")
data class BlockedContact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val callerName: String,
    @ColumnInfo(name = "phone_number") val phoneNumber: String, // E.g., +919876543210
    val reasonForBlocking: String = "Spam Indicator Detected",
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "vip_contacts")
data class VIPContact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    @ColumnInfo(name = "phone_number") val phoneNumber: String,
    val category: String, // Family, Emergency, Favorite, Business
    val priorityLevel: String = "CRITICAL"
)

// ----------------------------------------------------
// DATA ACCESS OBJECT (DAO)
// ----------------------------------------------------

@Dao
interface CallGuardianDao {

    // Call History Queries
    @Query("SELECT * FROM call_history ORDER BY callTime DESC")
    fun getAllCallHistory(): Flow<List<CallHistoryRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallRecord(record: CallHistoryRecord): Long

    @Query("DELETE FROM call_history WHERE id = :id")
    suspend fun deleteCallRecordById(id: Int)

    @Query("DELETE FROM call_history")
    suspend fun clearAllCallHistory()

    // Blocked/Spam Contacts Queries
    @Query("SELECT * FROM blocked_contacts ORDER BY dateAdded DESC")
    fun getAllBlockedContacts(): Flow<List<BlockedContact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun blockContact(contact: BlockedContact)

    @Query("DELETE FROM blocked_contacts WHERE phone_number = :phoneNumber")
    suspend fun unblockContact(phoneNumber: String)

    @Query("SELECT COUNT(*) FROM blocked_contacts WHERE phone_number = :phoneNumber")
    suspend fun isBlocked(phoneNumber: String): Int

    // VIP Contacts Queries
    @Query("SELECT * FROM vip_contacts ORDER BY name ASC")
    fun getAllVIPContacts(): Flow<List<VIPContact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVIPContact(contact: VIPContact)

    @Query("DELETE FROM vip_contacts WHERE phone_number = :phoneNumber")
    suspend fun deleteVIPContact(phoneNumber: String)

    @Query("DELETE FROM vip_contacts WHERE id = :id")
    suspend fun deleteVIPContactById(id: Int)
}

// ----------------------------------------------------
// ROOM DATABASE HOLDER
// ----------------------------------------------------

@Database(
    entities = [CallHistoryRecord::class, BlockedContact::class, VIPContact::class, NusuDialogue::class, MoodEntry::class],
    version = 4,
    exportSchema = false
)
abstract class CallGuardianDatabase : RoomDatabase() {

    abstract fun callGuardianDao(): CallGuardianDao
    abstract fun nusuDao(): NusuDao

    companion object {
        @Volatile
        private var INSTANCE: CallGuardianDatabase? = null

        fun getDatabase(context: Context): CallGuardianDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CallGuardianDatabase::class.java,
                    "nusu_call_guardian_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
