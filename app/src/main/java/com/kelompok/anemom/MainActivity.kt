package com.kelompok.anemom

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.kelompok.anemom.ui.navigation.AppNavGraph
import com.kelompok.anemom.ui.navigation.SessionGuard
import com.kelompok.anemom.ui.theme.AnemomTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Aplikasi hanya punya tema terang, jadi ikon status bar dibuat gelap.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        val events = (application as AnemomApp).container.events
        setContent {
            AnemomTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    val navController = rememberNavController()

                    // Pesan dari lapisan data (galat jaringan, gagal menyimpan, dst.).
                    LaunchedEffect(Unit) {
                        events.messages.collect { message ->
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    }

                    // Sesi berakhir (token ditolak server) -> kembali ke halaman Masuk.
                    SessionGuard(navController)

                    AppNavGraph(navController)
                }
            }
        }
    }
}
