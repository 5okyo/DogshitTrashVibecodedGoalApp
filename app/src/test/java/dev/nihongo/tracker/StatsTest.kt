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

    @Test fun weeklyCharacterGoalCountsOnlyMatchingMediumInWindow() {
        val goal = Goal(metric = Metric.CHARACTERS, medium = Medium.READING, period = Period.WEEKLY, target = 1000)
        val logs = listOf(
            StudyLog(epochDay = 10, medium = Medium.READING, minutes = 20, characters = 400),
            StudyLog(epochDay = 5, medium = Medium.READING, minutes = 20, characters = 300),
            StudyLog(epochDay = 3, medium = Medium.READING, minutes = 20, characters = 999), // outside window
            StudyLog(epochDay = 9, medium = Medium.WATCHING, minutes = 30, characters = 0),
        )
        assertEquals(700, Stats.progress(goal, logs, emptyList(), today = 10).current)
    }
}
