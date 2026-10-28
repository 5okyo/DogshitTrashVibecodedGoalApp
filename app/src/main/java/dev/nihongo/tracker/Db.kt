package dev.nihongo.tracker

import android.app.Application
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackerDao {
    @Insert
    suspend fun insertLog(log: StudyLog)

    @Delete
    suspend fun deleteLog(log: StudyLog)

    @Query("SELECT * FROM study_log ORDER BY epochDay DESC, id DESC")
    fun logs(): Flow<List<StudyLog>>

    @Insert
    suspend fun insertGoal(goal: Goal)

    @Delete
    suspend fun deleteGoal(goal: Goal)

    @Query("SELECT * FROM goal ORDER BY id")
    fun goals(): Flow<List<Goal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnkiDay(day: AnkiDay)

    @Query("SELECT * FROM anki_day ORDER BY epochDay")
    fun ankiDays(): Flow<List<AnkiDay>>
}

@Database(entities = [StudyLog::class, Goal::class, AnkiDay::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): TrackerDao
}

class TrackerApp : Application() {
    val db: AppDb by lazy { Room.databaseBuilder(this, AppDb::class.java, "tracker.db").build() }
}
