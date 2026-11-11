package com.kelompok.anemom.data

import com.kelompok.anemom.data.local.HealthCodec
import com.kelompok.anemom.data.model.HbResult
import com.kelompok.anemom.data.model.TtdEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class HealthCodecTest {
    @Test
    fun hb_roundTrip_sortedByDate() {
        val a = HbResult(LocalDate.of(2026, 10, 2), 10.8f, "Puskesmas", "Laboratorium")
        val b = HbResult(LocalDate.of(2026, 9, 1), 10.2f, "Lab | Kota", "Laboratorium")
        val decoded = HealthCodec.decodeHb(HealthCodec.encodeHb(listOf(a, b)))
        assertEquals(2, decoded.size)
        assertEquals(b.date, decoded[0].date)
        assertEquals(10.8f, decoded[1].value, 0.001f)
        assertEquals(false, decoded[0].facility.contains("|"))
    }

    @Test
    fun ttd_roundTrip_and_invalid() {
        val entry = TtdEntry(LocalDate.of(2026, 11, 10), LocalTime.of(20, 5))
        assertEquals(entry, HealthCodec.decodeTtd(HealthCodec.encodeTtd(entry)))
        assertNull(HealthCodec.decodeTtd("bukan-data"))
    }
}
