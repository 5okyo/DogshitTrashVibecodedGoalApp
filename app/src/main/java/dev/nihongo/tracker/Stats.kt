package dev.nihongo.tracker

data class GoalProgress(val goal: Goal, val current: Int) {
    val fraction: Float get() = (current.toFloat() / goal.target.coerceAtLeast(1)).coerceIn(0f, 1f)
    val done: Boolean get() = current >= goal.target
}

/** 3725 -> "1h 2m 5s". Zero-valued parts are skipped; 0 -> "0m". */
fun formatDuration(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = totalSeconds % 3600 / 60
    val s = totalSeconds % 60
    val parts = buildList {
        if (h > 0) add("${h}h")
        if (m > 0) add("${m}m")
        if (s > 0) add("${s}s")
    }
    return if (parts.isEmpty()) "0m" else parts.joinToString(" ")
}

object Stats {
    /** Consecutive days ending today, or ending yesterday if today has no activity yet. */
    fun currentStreak(days: Set<Long>, today: Long): Int {
        var d = if (today in days) today else today - 1
        var n = 0
        while (d in days) {
            n++
            d--
        }
        return n
    }

    fun longestStreak(days: Set<Long>): Int {
        var best = 0
        var run = 0
        var prev: Long? = null
        for (d in days.sorted()) {
            run = if (prev != null && d == prev + 1) run + 1 else 1
            if (run > best) best = run
            prev = d
        }
        return best
    }

    fun windowStart(period: Period, today: Long): Long =
        if (period == Period.DAILY) today else today - 6

    fun progress(goal: Goal, logs: List<StudyLog>, anki: List<AnkiDay>, today: Long): GoalProgress {
        val from = windowStart(goal.period, today)
        val current = when (goal.metric) {
            Metric.ANKI_DAYS -> anki.count { it.epochDay in from..today && it.reviews > 0 }
            else -> {
                val inScope = logs.filter {
                    it.epochDay in from..today && (goal.medium == null || it.medium == goal.medium)
                }
                if (goal.metric == Metric.CHARACTERS) inScope.sumOf { it.characters } else inScope.sumOf { it.seconds } / 60
            }
        }
        return GoalProgress(goal, current)
    }
}
