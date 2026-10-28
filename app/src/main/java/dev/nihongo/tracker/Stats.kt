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

enum class HeatMetric(val label: String) {
    ANKI("Anki"),
    TIME("Time"),
    CHARS("文字"),
}

/** Per-day amount for the heatmap (epoch day -> 1 for Anki days, seconds for Time, 文字 for CHARS). Empty days are left out. */
fun heatValues(metric: HeatMetric, logs: List<StudyLog>, anki: List<AnkiDay>): Map<Long, Int> = when (metric) {
    HeatMetric.ANKI -> anki.filter { it.reviews > 0 }.associate { it.epochDay to 1 }
    HeatMetric.TIME -> logs.groupBy { it.epochDay }.mapValues { (_, l) -> l.sumOf { it.seconds } }.filterValues { it > 0 }
    HeatMetric.CHARS -> logs.groupBy { it.epochDay }.mapValues { (_, l) -> l.sumOf { it.characters } }.filterValues { it > 0 }
}

/** 0 = nothing, 1..4 = which quarter of your busiest day this day reached. */
fun heatLevel(value: Int, max: Int): Int {
    if (value <= 0 || max <= 0) return 0
    val f = value.toFloat() / max
    return when {
        f < 0.25f -> 1
        f < 0.5f -> 2
        f < 0.75f -> 3
        else -> 4
    }
}

data class HeatSummary(
    val averagePerActiveDay: Int,
    val percentDaysActive: Int,
    val longestStreak: Int,
    val currentStreak: Int,
)

/** Summary line under the heatmap. "Active %" is measured from your first active day to today. */
fun heatSummary(values: Map<Long, Int>, today: Long): HeatSummary {
    if (values.isEmpty()) return HeatSummary(0, 0, 0, 0)
    val days = values.keys.toSet()
    val span = (today - days.min() + 1).coerceAtLeast(1)
    return HeatSummary(
        averagePerActiveDay = values.values.sum() / values.size,
        percentDaysActive = (values.size * 100L / span).toInt().coerceAtMost(100),
        longestStreak = Stats.longestStreak(days),
        currentStreak = Stats.currentStreak(days, today),
    )
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
