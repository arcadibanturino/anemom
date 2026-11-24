package com.kelompok.anemom.alarm

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kelompok.anemom.AnemomApp
import com.kelompok.anemom.MainActivity
import com.kelompok.anemom.ui.theme.AnemomTheme

/**
 * Layar alarm TTD layar penuh. Dibuka oleh notifikasi alarm (full-screen intent), tampil di atas
 * layar kunci dan menyalakan layar. Juga dipakai untuk "Pratinjau alarm layar penuh" (tanpa bunyi,
 * tidak menyimpan apa pun).
 */
class AlarmActivity : ComponentActivity() {

    private val viewModel: AlarmViewModel by viewModels {
        val app = application as AnemomApp
        val demo = intent.getBooleanExtra(EXTRA_PREVIEW, false) || intent.getBooleanExtra(EXTRA_TEST, false)
        viewModelFactory {
            initializer {
                AlarmViewModel(
                    repository = app.container.ttdRepository,
                    scheduler = app.container.ttdAlarm,
                    stopRinging = { AlarmNotifier.cancel(app) },
                    demo = demo
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        enableEdgeToEdge()
        val preview = intent.getBooleanExtra(EXTRA_PREVIEW, false)
        val test = intent.getBooleanExtra(EXTRA_TEST, false)

        setContent {
            AnemomTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                AlarmFlow(
                    state = state,
                    modeLabel = when {
                        preview -> "Pratinjau · tidak dicatat"
                        test -> "Alarm uji · tidak dicatat"
                        else -> null
                    },
                    onTaken = viewModel::confirmTaken,
                    onNotTaken = viewModel::reportNotTaken,
                    onOpenSnooze = viewModel::openSnooze,
                    onSnooze = viewModel::snooze,
                    onBackToAlarm = viewModel::backToAlarm,
                    onOpenTracker = ::openTracker,
                    onClose = ::finish
                )
            }
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun openTracker() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_TTD)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }

    companion object {
        private const val EXTRA_PREVIEW = "preview"
        private const val EXTRA_TEST = "test"

        fun newIntent(context: Context, preview: Boolean = false, test: Boolean = false): Intent =
            Intent(context, AlarmActivity::class.java)
                .putExtra(EXTRA_PREVIEW, preview)
                .putExtra(EXTRA_TEST, test)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
    }
}
