package com.kelompok.anemom.data

import com.kelompok.anemom.data.model.HbRisk
import org.junit.Assert.assertEquals
import org.junit.Test

class HbRiskTest {
    @Test
    fun classify_boundaries() {
        assertEquals(HbRisk.NORMAL, HbRisk.classify(11f))
        assertEquals(HbRisk.NORMAL, HbRisk.classify(12.5f))
        assertEquals(HbRisk.MODERATE, HbRisk.classify(10.9f))
        assertEquals(HbRisk.MODERATE, HbRisk.classify(10f))
        assertEquals(HbRisk.HIGH, HbRisk.classify(9.9f))
    }
}
