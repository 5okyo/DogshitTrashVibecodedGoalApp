package dev.nihongo.tracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
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
    val todaySeconds: Int,
    val progress: List<GoalProgress>,
) {
    companion object {
        fun empty() = Dashboard(LocalDate.now().toEpochDay(), 0, 0, false, false, 0, 0, emptyList())
    }
}

sealed interface UpdateState {
    data object None : UpdateState
    data class Available(val release: Updater.Release, val note: String? = null) : UpdateState
    data object Downloading : UpdateState
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
            todaySeconds = todayLogs.sumOf { it.seconds },
            progress = g.map { Stats.progress(it, l, a, today) },
        )
    }.stateIn(viewModelScope, share, Dashboard.empty())

    fun addLog(medium: Medium, seconds: Int, characters: Int, note: String = "") = viewModelScope.launch {
        dao.insertLog(StudyLog(epochDay = LocalDate.now().toEpochDay(), medium = medium, seconds = seconds, characters = characters, note = note))
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

    // ---------- Updates (GitHub Releases) ----------

    private val _update = MutableStateFlow<UpdateState>(UpdateState.None)
    val update: StateFlow<UpdateState> = _update

    init {
        checkForUpdate()
    }

    /** Quietly checks the latest GitHub release; offline or any error just means "no update". */
    fun checkForUpdate() {
        viewModelScope.launch {
            val latest = runCatching { Updater.latest() }.getOrNull()
            if (latest != null && latest.versionCode > BuildConfig.VERSION_CODE) {
                _update.value = UpdateState.Available(latest)
            }
        }
    }

    fun installUpdate(release: Updater.Release) {
        viewModelScope.launch {
            _update.value = UpdateState.Downloading
            val ctx = getApplication<Application>()
            val result = runCatching { Updater.install(ctx, Updater.download(ctx, release.apkUrl)) }
            _update.value = when {
                result.isFailure -> UpdateState.Available(release, "download failed, tap to retry")
                result.getOrNull() == false -> UpdateState.Available(release, "allow installs from this app, then tap again")
                else -> UpdateState.Available(release)
            }
        }
    }
}
