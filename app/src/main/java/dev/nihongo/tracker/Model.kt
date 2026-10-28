package dev.nihongo.tracker

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class Medium(val label: String) {
    ANKI("Anki"),
    READING("Reading"),
    LISTENING("Listening"),
    WATCHING("Watching"),
    GAMES("Games"),
    SPEAKING("Speaking"),
    TEXTBOOK("Textbook"),
    OTHER("Other"),
}

enum class Metric(val label: String) {
    CHARACTERS("文字 count"),
    MINUTES("Minutes"),
    ANKI_DAYS("Anki days"),
}

enum class Period(val label: String) {
    DAILY("Daily"),
    WEEKLY("Last 7 days"),
}

/** One study session. [epochDay] is LocalDate.toEpochDay() in the phone's local time zone. */
@Entity(tableName = "study_log", indices = [Index("epochDay")])
data class StudyLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val medium: Medium,
    val seconds: Int,
    val characters: Int,
    val note: String = "",
)

/** A target such as "5,000 文字 of Reading per day". A null [medium] means all media. */
@Entity(tableName = "goal")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val metric: Metric,
    val medium: Medium?,
    val period: Period,
    val target: Int,
)

/** A day you marked Anki as done (reviews = 1). A row existing means the day counts toward the streak. */
@Entity(tableName = "anki_day")
data class AnkiDay(
    @PrimaryKey val epochDay: Long,
    val reviews: Int,
)
