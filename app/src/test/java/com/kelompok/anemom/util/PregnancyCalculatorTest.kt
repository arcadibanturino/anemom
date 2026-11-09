package com.kelompok.anemom.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PregnancyCalculatorTest {

    @Test
    fun weeksSince_countsFullWeeks() {
        val hpht = LocalDate.of(2026, 5, 19)
        val today = LocalDate.of(2026, 11, 3)
        assertEquals(24, PregnancyCalculator.weeksSince(hpht, today))
    }

    @Test
    fun dueDate_isHphtPlus280Days() {
        assertEquals(LocalDate.of(2027, 2, 23), PregnancyCalculator.dueDate(LocalDate.of(2026, 5, 19)))
    }

    @Test
    fun trimesterFor_usesWeekBoundaries() {
        assertEquals(1, PregnancyCalculator.trimesterFor(13))
        assertEquals(2, PregnancyCalculator.trimesterFor(14))
        assertEquals(2, PregnancyCalculator.trimesterFor(27))
        assertEquals(3, PregnancyCalculator.trimesterFor(28))
    }
}
