package dev.nihongo.tracker

/**
 * Where automatic Anki review counts could come from.
 *
 * Manual entry (the "Anki reviews today" box) works now. Automatic import is NOT wired up yet:
 * it still needs a verified way to read per-day review history on Android (see README).
 * Implement this interface and pass it to TrackerViewModel.importAnki() once one is chosen.
 */
interface AnkiSource {
    /** Reviews per day (key = epoch day) for days on or after [sinceEpochDay]. */
    suspend fun reviewsByDay(sinceEpochDay: Long): Map<Long, Int>
}

object NoAnkiSource : AnkiSource {
    override suspend fun reviewsByDay(sinceEpochDay: Long): Map<Long, Int> = emptyMap()
}
