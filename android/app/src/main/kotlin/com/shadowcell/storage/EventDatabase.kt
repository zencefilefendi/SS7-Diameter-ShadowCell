package com.shadowcell.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.shadowcell.scoring.ThreatEvent
import kotlinx.coroutines.flow.Flow
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Dao
interface EventDao {
    @Insert
    suspend fun insert(event: ThreatEvent): Long

    @Update
    suspend fun update(event: ThreatEvent)

    @Query("SELECT * FROM threat_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEvents(limit: Int = 200): Flow<List<ThreatEvent>>

    @Query("SELECT * FROM threat_events WHERE timestamp > :since ORDER BY timestamp DESC")
    suspend fun getEventsSince(since: Long): List<ThreatEvent>

    @Query("SELECT * FROM threat_events WHERE timestamp > :since AND score >= :minScore ORDER BY timestamp DESC")
    suspend fun getHighRiskEventsSince(since: Long, minScore: Int = 40): List<ThreatEvent>

    @Query("DELETE FROM threat_events WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("SELECT COUNT(*) FROM threat_events WHERE timestamp > :since")
    suspend fun countSince(since: Long): Int
}

@Database(entities = [ThreatEvent::class], version = 1, exportSchema = false)
abstract class EventDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var INSTANCE: EventDatabase? = null

        fun get(context: Context, passphrase: ByteArray = ByteArray(0)): EventDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    EventDatabase::class.java,
                    "shadowcell_events.db"
                )
                // SQLCipher şifrelemesi: passphrase boşsa şifresiz, doluysa şifreli
                if (passphrase.isNotEmpty()) {
                    val factory = SupportFactory(SQLiteDatabase.getBytes(
                        String(passphrase).toCharArray()
                    ))
                    builder.openHelperFactory(factory)
                }
                builder.build().also { INSTANCE = it }
            }
        }
    }
}
