package com.kelompok.anemom.ui.riwayat

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Ekspor satu Detail riwayat ke PDF (A4) dengan PdfDocument bawaan Android, lalu dibagikan
 * lewat lembar "Bagikan" (simpan ke Files/Drive, kirim WhatsApp, cetak).
 */
object HistoryPdf {

    private const val PAGE_W = 595 // A4, 1 pt = 1/72 inci
    private const val PAGE_H = 842
    private const val MARGIN = 48f

    fun share(context: Context, detail: HistoryDetail, motherName: String?) {
        val file = write(context, detail, motherName)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("application/pdf")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(file.name, uri)
        context.startActivity(Intent.createChooser(send, "Simpan atau bagikan PDF"))
    }

    private fun write(context: Context, d: HistoryDetail, motherName: String?): File {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
        val c = page.canvas
        val maroon = 0xFF6D1838.toInt()
        val pink = 0xFFE91E63.toInt()
        val muted = 0xFF8A7079.toInt()
        val title = Paint().apply { isAntiAlias = true; textSize = 20f; color = maroon; typeface = Typeface.DEFAULT_BOLD }
        val big = Paint().apply { isAntiAlias = true; textSize = 30f; color = maroon; typeface = Typeface.DEFAULT_BOLD }
        val bold = Paint().apply { isAntiAlias = true; textSize = 11f; color = maroon; typeface = Typeface.DEFAULT_BOLD }
        val body = Paint().apply { isAntiAlias = true; textSize = 11f; color = 0xFF333333.toInt() }
        val small = Paint().apply { isAntiAlias = true; textSize = 9f; color = muted }
        val accent = Paint().apply { isAntiAlias = true; textSize = 11f; color = pink; typeface = Typeface.DEFAULT_BOLD }

        var y = MARGIN + 10f
        c.drawText("Anemom · Detail riwayat pemeriksaan", MARGIN, y, accent); y += 26f
        c.drawText(d.headline, MARGIN, y, title); y += 16f
        c.drawText(listOfNotNull(motherName?.let { "Bunda $it" }, d.item.title()).joinToString(" · "), MARGIN, y, small); y += 36f
        c.drawText(d.value + (d.unit?.let { " $it" } ?: ""), MARGIN, y, big); y += 20f
        d.status?.let { c.drawText("Status: ${it.text}", MARGIN, y, accent); y += 14f }
        y = wrap(c, d.chipNote, y, small, 12f) + 18f

        c.drawText("Rincian pemeriksaan", MARGIN, y, bold); y += 16f
        d.rows.forEach { (label, value) ->
            c.drawText(label, MARGIN, y, small); y += 13f
            y = wrap(c, value, y, bold, 14f) + 6f
        }
        y += 10f
        c.drawText("Rekomendasi", MARGIN, y, bold); y += 16f
        d.advice.forEach { (head, text) ->
            c.drawText("• $head", MARGIN, y, bold); y += 14f
            y = wrap(c, text, y, body, 14f, indent = 10f) + 6f
        }
        y += 10f
        y = wrap(c, d.footnote, y, body, 14f) + 16f
        wrap(
            c,
            "Anemom adalah aplikasi skrining awal. Hasil ini bukan diagnosis; konsultasikan dengan bidan atau dokter. " +
                "Dibuat ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("d MMM yyyy HH.mm", indonesian))} dari data di aplikasi.",
            PAGE_H - MARGIN - 24f, small, 11f
        )
        doc.finishPage(page)

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // simpan yang terbaru saja
        val stamp = d.item.at.toLocalDate().toString()
        val file = File(dir, "anemom-${d.item.kind.route}-$stamp.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    /** Tulis teks dengan pindah baris otomatis; mengembalikan posisi y berikutnya. */
    private fun wrap(
        c: android.graphics.Canvas,
        text: String,
        startY: Float,
        paint: Paint,
        lineHeight: Float,
        indent: Float = 0f
    ): Float {
        val maxW = PAGE_W - 2 * MARGIN - indent
        var y = startY
        var line = ""
        text.split(" ").forEach { word ->
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(candidate) > maxW && line.isNotEmpty()) {
                c.drawText(line, MARGIN + indent, y, paint)
                y += lineHeight
                line = word
            } else {
                line = candidate
            }
        }
        if (line.isNotEmpty()) {
            c.drawText(line, MARGIN + indent, y, paint)
            y += lineHeight
        }
        return y
    }
}
