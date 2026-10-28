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
    val ankiDoneToday: Boolean,
    val ankiDoneYesterday: Boolean,
    val todayChars: Int,
    val todayMinutes: Int,
    val progress: List<GoalProgress>,
    val weekMinutesByMedium: List<Pair<Medium, Int>>,
) {
    companion object {
        fun empty() = Dashboard(LocalDate.now().toEpochDay(), 0, 0, false, false, 0, 0, emptyList(), emptyList())
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
            ankiDoneToday = today in ankiActive,
            ankiDoneYesterday = (today - 1) in ankiActive,
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

    /**
     * The Anki buttons: flips the done state of today ([daysAgo] = 0) or yesterday ([daysAgo] = 1).
     * The date and current state are read at tap time, so this stays right even if the app was
     * left open past midnight.
     */
    fun toggleAnki(daysAgo: Long) = viewModelScope.launch {
        val day = LocalDate.now().toEpochDay() - daysAgo
        val done = ankiDays.value.any { it.epochDay == day && it.reviews > 0 }
        if (done) dao.deleteAnkiDay(day) else dao.upsertAnkiDay(AnkiDay(day, 1))
    }

    fun addGoal(goal: Goal) = viewModelScope.launch { dao.insertGoal(goal) }

    fun deleteGoal(goal: Goal) = viewModelScope.launch { dao.deleteGoal(goal) }
}
