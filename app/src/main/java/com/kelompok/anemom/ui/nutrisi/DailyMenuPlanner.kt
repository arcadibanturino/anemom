package com.kelompok.anemom.ui.nutrisi

import com.kelompok.anemom.data.model.FoodItem
import com.kelompok.anemom.data.model.HbRisk
import java.time.LocalDate

/**
 * Memilih bahan yang disarankan di Beranda hari ini dari daftar rekomendasi server.
 *
 * Pola (tanpa takaran; jumlah konsumsi ditentukan tenaga kesehatan):
 * 1. Komposisi seimbang: sumber zat besi hewani + sayur + kacang-kacangan + buah kaya vitamin C
 *    (vitamin C membantu penyerapan zat besi, Kemenkes).
 * 2. Jika Hb terakhir di bawah normal, porsi pilihan hewani dinaikkan jadi 2 bahan
 *    (besi heme lebih mudah diserap, Kemenkes). Pilihan bahannya saja, bukan jumlah.
 * 3. Variasi harian: urutan tiap kelompok diputar mengikuti tanggal, jadi isi berganti tiap hari
 *    tetapi tetap sama sepanjang hari yang sama.
 * 4. Bahan ber-peringatan kehamilan dan penghambat zat besi tidak ikut disarankan.
 */
object DailyMenuPlanner {

    fun plan(foods: List<FoodItem>, risk: HbRisk?, today: LocalDate = LocalDate.now()): List<FoodItem> {
        val candidates = foods.filter { it.pregnancyWarning == null && !it.isIronInhibitor }
        val day = today.toEpochDay()

        val animal = candidates.filter { it.ironType == "heme" }
        val vegetables = candidates.filter { !it.isIronBooster && it.category == "sayur" }
        val legumes = candidates.filter { !it.isIronBooster && it.category == "nabati" }
        val fruits = candidates.filter { it.isIronBooster }

        val belowNormal = risk != null && risk != HbRisk.NORMAL

        return buildList {
            addAll(animal.rotated(day).take(if (belowNormal) 2 else 1))
            addAll(vegetables.rotated(day).take(1))
            addAll(legumes.rotated(day).take(1))
            addAll(fruits.rotated(day).take(1))
        }
            .distinctBy { it.name }
            .ifEmpty { candidates.rotated(day) }
    }

    private fun <T> List<T>.rotated(by: Long): List<T> {
        if (isEmpty()) return this
        val shift = Math.floorMod(by, size.toLong()).toInt()
        return drop(shift) + take(shift)
    }
}
