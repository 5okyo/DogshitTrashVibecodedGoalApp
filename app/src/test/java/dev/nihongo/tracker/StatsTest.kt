package dev.nihongo.tracker

import org.junit.Assert.assertEquals
import org.junit.Test

class StatsTest {
    @Test fun streakEndingToday() {
        assertEquals(3, Stats.currentStreak(setOf(8L, 9L, 10L), today = 10))
    }

    @Test fun streakSurvivesUntilDayEnds() {
        assertEquals(2, Stats.currentStreak(setOf(8L, 9L), today = 10))
    }

    @Test fun streakBrokenAfterMissedDay() {
        assertEquals(0, Stats.currentStreak(setOf(7L, 8L), today = 10))
    }

    @Test fun longestStreakFindsBestRun() {
        assertEquals(4, Stats.longestStreak(setOf(1L, 2L, 5L, 6L, 7L, 8L, 12L)))
    }

    @Test fun ankiDaysGoalCountsDaysInWindow() {
        val goal = Goal(metric = Metric.ANKI_DAYS, medium = null, period = Period.WEEKLY, target = 5)
        val anki = listOf(AnkiDay(10, 1), AnkiDay(9, 1), AnkiDay(8, 1), AnkiDay(2, 1))
        assertEquals(3, Stats.progress(goal, emptyList(), anki, today = 10).current)
    }

    @Test fun weeklyCharacterGoalCountsOnlyMatchingMediumInWindow() {
        val goal = Goal(metric = Metric.CHARACTERS, medium = Medium.READING, period = Period.WEEKLY, target = 1000)
        val logs = listOf(
            StudyLog(epochDay = 10, medium = Medium.READING, seconds = 1200, characters = 400),
            StudyLog(epochDay = 5, medium = Medium.READING, seconds = 1200, characters = 300),
            StudyLog(epochDay = 3, medium = Medium.READING, seconds = 1200, characters = 999), // outside window
            StudyLog(epochDay = 9, medium = Medium.WATCHING, seconds = 1800, characters = 0),
        )
        assertEquals(700, Stats.progress(goal, logs, emptyList(), today = 10).current)
    }

    @Test fun minutesGoalSumsSecondsAndRoundsDownToMinutes() {
        val goal = Goal(metric = Metric.MINUTES, medium = null, period = Period.DAILY, target = 60)
        val logs = listOf(
            StudyLog(epochDay = 10, medium = Medium.READING, seconds = 90, characters = 0),
            StudyLog(epochDay = 10, medium = Medium.LISTENING, seconds = 3600 + 29, characters = 0),
        )
        // 90 + 3629 = 3719 seconds = 61 minutes (and 59 seconds)
        assertEquals(61, Stats.progress(goal, logs, emptyList(), today = 10).current)
    }

    @Test fun durationFormatting() {
        assertEquals("1h 2m 5s", formatDuration(3725))
        assertEquals("45s", formatDuration(45))
        assertEquals("2h", formatDuration(7200))
        assertEquals("0m", formatDuration(0))
    }
}
