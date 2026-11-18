package com.kelompok.anemom.ui.nutrisi

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Foto bahan dari server ([imageUrl], publik tanpa token). Selama dimuat, jika gagal, atau jika
 * belum ada foto, tampil ikon makanan di latar pink.
 * Foto di server: JPEG/WebP 16:9 (1280x720), objek di tengah karena kartu memotong dengan Crop.
 *
 * Dimuat dengan API bawaan Android (tanpa library gambar) dan disimpan di cache memori selama
 * aplikasi berjalan.
 */
@Composable
fun MenuImage(
    imageUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val bitmap by produceState<Bitmap?>(
        initialValue = imageUrl?.let { MenuImageLoader.cached(it) },
        key1 = imageUrl
    ) {
        value = if (imageUrl.isNullOrBlank()) null else MenuImageLoader.load(imageUrl)
    }

    val loaded = bitmap
    if (loaded != null) {
        Image(
            bitmap = loaded.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.background(Pink100),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Restaurant,
                contentDescription = contentDescription,
                tint = Pink500,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

private object MenuImageLoader {
    /** Lebar maksimum hasil decode; foto 1280 px diperkecil agar hemat memori. */
    private const val MAX_WIDTH = 960

    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 16).toInt()   // KB: 1/16 memori aplikasi
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    fun cached(url: String): Bitmap? = cache.get(url)

    /** Null jika gagal (jaringan, 404 saat foto belum diunggah, atau berkas bukan gambar). */
    suspend fun load(url: String): Bitmap? {
        cache.get(url)?.let { return it }
        val bitmap = withContext(Dispatchers.IO) { runCatching { download(url) }.getOrNull() }
        if (bitmap != null) cache.put(url, bitmap)
        return bitmap
    }

    private fun download(url: String): Bitmap? {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val bytes = connection.inputStream.use { it.readBytes() }

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0) return null

            var sample = 1
            while (bounds.outWidth / (sample * 2) >= MAX_WIDTH) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } finally {
            connection.disconnect()
        }
    }
}