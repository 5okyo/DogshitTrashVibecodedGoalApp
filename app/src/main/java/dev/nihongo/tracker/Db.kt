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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

    @Query("DELETE FROM anki_day WHERE epochDay = :day")
    suspend fun deleteAnkiDay(day: Long)

    @Query("SELECT * FROM anki_day ORDER BY epochDay")
    fun ankiDays(): Flow<List<AnkiDay>>
}

@Database(entities = [StudyLog::class, Goal::class, AnkiDay::class], version = 2, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): TrackerDao
}

/** v1 stored whole minutes; v2 stores seconds so hours/minutes/seconds can be entered. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE study_log_new (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "epochDay INTEGER NOT NULL, " +
                "medium TEXT NOT NULL, " +
                "seconds INTEGER NOT NULL, " +
                "characters INTEGER NOT NULL, " +
                "note TEXT NOT NULL)"
        )
        db.execSQL(
            "INSERT INTO study_log_new (id, epochDay, medium, seconds, characters, note) " +
                "SELECT id, epochDay, medium, minutes * 60, characters, note FROM study_log"
        )
        db.execSQL("DROP TABLE study_log")
        db.execSQL("ALTER TABLE study_log_new RENAME TO study_log")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_study_log_epochDay ON study_log (epochDay)")
    }
}

class TrackerApp : Application() {
    val db: AppDb by lazy {
        Room.databaseBuilder(this, AppDb::class.java, "tracker.db")
            .addMigrations(MIGRATION_1_2)
            .build()
    }
}
