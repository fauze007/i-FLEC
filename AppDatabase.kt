package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- Room Entities ---

@Entity(tableName = "user_progress")
data class UserProgressEntity(
    @PrimaryKey val id: Int = 1,
    val streak: Int = 4,
    val coins: Int = 450,
    val xp: Int = 120,
    val lastActiveTime: Long = 0
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val scenarioId: String,
    val sender: String, // "user" or "ai" or "system"
    val text: String,
    val improvedText: String? = null,
    val wordAnalysisJson: String? = null, // Custom JSON array tracking word statuses
    val explanation: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

// --- DAO Interfaces ---

@Dao
interface UserProgressDao {
    @Query("SELECT * FROM user_progress WHERE id = 1 LIMIT 1")
    fun getProgress(): Flow<UserProgressEntity?>

    @Query("SELECT * FROM user_progress WHERE id = 1 LIMIT 1")
    suspend fun getProgressDirect(): UserProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateProgress(progress: UserProgressEntity)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE scenarioId = :scenarioId ORDER BY timestamp ASC")
    fun getMessagesForScenario(scenarioId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE scenarioId = :scenarioId")
    suspend fun clearMessagesForScenario(scenarioId: String)
}

// --- App Database Configuration ---

@Database(entities = [UserProgressEntity::class, ChatMessageEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProgressDao(): UserProgressDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "iflec_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// --- Repository Implementation ---

class LearningRepository(private val db: AppDatabase) {
    val progressFlow: Flow<UserProgressEntity?> = db.userProgressDao().getProgress()
    
    fun getMessagesForScenario(scenarioId: String): Flow<List<ChatMessageEntity>> =
        db.chatMessageDao().getMessagesForScenario(scenarioId)

    suspend fun insertMessage(message: ChatMessageEntity) {
        db.chatMessageDao().insertMessage(message)
    }

    suspend fun clearScenario(scenarioId: String) {
        db.chatMessageDao().clearMessagesForScenario(scenarioId)
    }

    suspend fun incrementCoins(amount: Int) {
        val current = db.userProgressDao().getProgressDirect() ?: UserProgressEntity()
        db.userProgressDao().updateProgress(
            current.copy(
                coins = current.coins + amount,
                xp = current.xp + (amount * 2)
            )
        )
    }

    suspend fun updateStreak() {
        val current = db.userProgressDao().getProgressDirect() ?: UserProgressEntity()
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L
        val isNewDay = (now - current.lastActiveTime) > oneDayMs
        
        val newStreak = if (current.lastActiveTime == 0L) {
            4 // Starts with 4 base streak as requested by design spec ("🔥 4 Days")
        } else if (isNewDay && (now - current.lastActiveTime) < (48 * 60 * 60 * 1000L)) {
            current.streak + 1
        } else if ((now - current.lastActiveTime) >= (48 * 60 * 60 * 1000L)) {
            1 // Streak lost reset
        } else {
            current.streak // Safe same-day check
        }
        
        db.userProgressDao().updateProgress(
            current.copy(
                streak = newStreak,
                lastActiveTime = now
            )
        )
    }

    suspend fun initializeProgressIfNeeded() {
        val current = db.userProgressDao().getProgressDirect()
        if (current == null) {
            db.userProgressDao().updateProgress(UserProgressEntity())
        }
    }
}
