package dev.nihongo.tracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class Dashboard(
    val today: Long,
    val ankiStreak: Int,
    val ankiLongest: Int,
    val ankiToday: Int,
    val todayChars: Int,
    val todayMinutes: Int,
    val progress: List<GoalProgress>,
    val weekMinutesByMedium: List<Pair<Medium, Int>>,
) {
    companion object {
        fun empty() = Dashboard(LocalDate.now().toEpochDay(), 0, 0, 0, 0, 0, emptyList(), emptyList())
    }
}

class TrackerViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as TrackerApp).db.dao()
    private val share = SharingStarted.WhileSubscribed(5_000)

    val logs: StateFlow<List<StudyLog>> = dao.logs().stateIn(viewModelScope, share, emptyList())
    private val goals: StateFlow<List<Goal>> = dao.goals().stateIn(viewModelScope, share, emptyList())
    private val ankiDays: StateFlow<List<AnkiDay>> = dao.ankiDays().stateIn(viewModelScope, share, emptyList())

    // "Today" is recomputed whenever the data changes, not at midnight while the app sits open.
    val dashboard: StateFlow<Dashboard> = combine(logs, goals, ankiDays) { l, g, a ->
        val today = LocalDate.now().toEpochDay()
        val ankiActive = a.filter { it.reviews > 0 }.map { it.epochDay }.toSet()
        val todayLogs = l.filter { it.epochDay == today }
        Dashboard(
            today = today,
            ankiStreak = Stats.currentStreak(ankiActive, today),
            ankiLongest = Stats.longestStreak(ankiActive),
            ankiToday = a.firstOrNull { it.epochDay == today }?.reviews ?: 0,
            todayChars = todayLogs.sumOf { it.characters },
            todayMinutes = todayLogs.sumOf { it.minutes },
            progress = g.map { Stats.progress(it, l, a, today) },
            weekMinutesByMedium = l.filter { it.epochDay in (today - 6)..today }
                .groupBy { it.medium }
                .map { (m, entries) -> m to entries.sumOf { it.minutes } }
                .filter { it.second > 0 }
                .sortedByDescending { it.second },
        )
    }.stateIn(viewModelScope, share, Dashboard.empty())

    fun addLog(medium: Medium, minutes: Int, characters: Int, note: String = "") = viewModelScope.launch {
        dao.insertLog(StudyLog(epochDay = LocalDate.now().toEpochDay(), medium = medium, minutes = minutes, characters = characters, note = note))
    }

    fun deleteLog(log: StudyLog) = viewModelScope.launch { dao.deleteLog(log) }

    fun setAnkiToday(reviews: Int) = viewModelScope.launch {
        dao.upsertAnkiDay(AnkiDay(LocalDate.now().toEpochDay(), reviews))
    }

    fun addGoal(goal: Goal) = viewModelScope.launch { dao.insertGoal(goal) }

    fun deleteGoal(goal: Goal) = viewModelScope.launch { dao.deleteGoal(goal) }

    /** Merges imported per-day review counts, keeping the larger of imported vs. existing. */
    fun importAnki(source: AnkiSource, daysBack: Long = 365) = viewModelScope.launch {
        val today = LocalDate.now().toEpochDay()
        val existing = ankiDays.value.associate { it.epochDay to it.reviews }
        source.reviewsByDay(today - daysBack).forEach { (day, reviews) ->
            if (reviews > (existing[day] ?: 0)) dao.upsertAnkiDay(AnkiDay(day, reviews))
        }
    }
}
