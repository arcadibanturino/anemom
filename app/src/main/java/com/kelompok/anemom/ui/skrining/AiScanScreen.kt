package com.kelompok.anemom.ui.skrining

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.ZoomState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelompok.anemom.data.repository.Screening
import com.kelompok.anemom.ml.segmentation.MaskQuality
import com.kelompok.anemom.ui.AppViewModelProvider
import com.kelompok.anemom.ui.components.AnemomCard
import com.kelompok.anemom.ui.components.AnemomChip
import com.kelompok.anemom.ui.components.AnemomOutlinedButton
import com.kelompok.anemom.ui.components.AnemomScreen
import com.kelompok.anemom.ui.components.GradientButton
import com.kelompok.anemom.ui.components.NoteCard
import com.kelompok.anemom.ui.skrining.scan.AiScanViewModel
import com.kelompok.anemom.ui.skrining.scan.GuideBox
import com.kelompok.anemom.ui.skrining.scan.ScanStep
import com.kelompok.anemom.ui.skrining.scan.captureResolutionSelector
import com.kelompok.anemom.ui.skrining.scan.decodeGuideCropAndClose
import com.kelompok.anemom.ui.skrining.scan.focusOn
import com.kelompok.anemom.ui.skrining.scan.previewResolutionSelector
import com.kelompok.anemom.ui.skrining.scan.takePictureAsync
import com.kelompok.anemom.ui.theme.AmberBg
import com.kelompok.anemom.ui.theme.AmberText
import com.kelompok.anemom.ui.theme.ErrorRed
import com.kelompok.anemom.ui.theme.GreenBg
import com.kelompok.anemom.ui.theme.GreenText
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500
import androidx.camera.core.ImageAnalysis
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.kelompok.anemom.BuildConfig
import com.kelompok.anemom.ml.segmentation.ConjunctivaSegmenter
import com.kelompok.anemom.ui.skrining.scan.EyeGuideOverlay
import com.kelompok.anemom.ui.skrining.scan.GuideChecklist
import com.kelompok.anemom.ui.skrining.scan.GuideHint
import com.kelompok.anemom.ui.skrining.scan.GuideHintBanner
import com.kelompok.anemom.ui.skrining.scan.LiveEyeAnalyzer
import com.kelompok.anemom.ui.skrining.scan.LiveFrame
import com.kelompok.anemom.ui.skrining.scan.LiveGuide
import com.kelompok.anemom.ui.skrining.scan.LiveGuideEngine
import com.kelompok.anemom.ui.skrining.scan.analysisResolutionSelector
import com.kelompok.anemom.ui.skrining.scan.debugText
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * AI Scan Mata: kamera dengan bingkai mata + petunjuk live (detektor wajah ML Kit saat jauh, U-Net saat
 * dekat) -> foto diambil OTOMATIS saat mata di bingkai, kelopak ditarik, tajam, dan stabil (tombol manual
 * tetap ada sebagai cadangan) -> foto dipotong sesuai kotak -> U-Net di HP memeriksa ulang -> pratinjau
 * mask -> foto potongan dikirim ke server untuk dianalisis.
 * Model segmentasi (U-Net / YOLO) ditentukan flavor build, lihat ml/segmentation.
 */
@Composable
fun AiScanScreen(
    onBack: () -> Unit,
    onInputHb: () -> Unit,
    viewModel: AiScanViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AnemomScreen(
        title = "AI Scan Mata",
        subtitle = "Estimasi awal dari konjungtiva",
        onBack = onBack
    ) {
        when (val step = state.step) {
            ScanStep.Camera, ScanStep.Checking -> CameraStep(
                checking = step == ScanStep.Checking,
                message = state.message,
                segmenterProvider = viewModel.liveSegmenterProvider,
                onCaptured = viewModel::onPhotoCaptured,
                onCaptureFailed = viewModel::onCaptureFailed
            )
            is ScanStep.Review -> ReviewStep(step, onSend = viewModel::send, onRetake = viewModel::retake)
            is ScanStep.Sent -> SentStep(step.screening, onScanAgain = viewModel::retake, onInputHb = onInputHb)
        }
        Spacer(Modifier.height(12.dp))
        NoteCard("Hasil AI hanya estimasi dan bukan diagnosis. Konfirmasi dengan pemeriksaan Hb darah.")
        Spacer(Modifier.height(20.dp))
    }
}

// ---------------------------------------------------------------------------------------------
// Tahap 1: kamera dengan petunjuk live dan jepret otomatis
// ---------------------------------------------------------------------------------------------

@Composable
private fun CameraStep(
    checking: Boolean,
    message: String?,
    segmenterProvider: () -> ConjunctivaSegmenter,
    onCaptured: (android.graphics.Bitmap) -> Unit,
    onCaptureFailed: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setResolutionSelector(captureResolutionSelector)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(ImageCapture.FLASH_MODE_OFF)
            .build()
    }
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    // Kamera depan bawaan (Bunda bisa memotret sendiri); belakang bisa dipilih untuk hasil lebih tajam.
    var lensFacing by rememberSaveable { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    var canFlip by remember { mutableStateOf(false) }
    val front = lensFacing == CameraSelector.LENS_FACING_FRONT
    var capturing by remember { mutableStateOf(false) }
    /** Foto sudah terambil (sensor selesai), tinggal diproses: Bunda boleh melepas kelopak. */
    var shutterDone by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    // --- Petunjuk live: frame kecil -> LiveEyeAnalyzer (thread analisis) -> LiveGuideEngine (thread utama).
    val engine = remember { LiveGuideEngine() }
    var guide by remember { mutableStateOf(LiveGuide()) }
    var liveFrame by remember { mutableStateOf<LiveFrame?>(null) }
    /** false bila HP tidak sanggup menjalankan kamera + analisis bersamaan: tinggal mode manual. */
    var analysisBound by remember { mutableStateOf(true) }
    /** Bertambah satu setiap kali mesin petunjuk meminta foto (tidak pernah terlewat oleh recomposition). */
    var autoCaptureRequests by remember { mutableIntStateOf(0) }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val analyzer = remember {
        LiveEyeAnalyzer(segmenterProvider) { frame ->
            mainExecutor.execute {
                liveFrame = frame
                val g = engine.update(frame.observation)
                guide = g
                if (g.shouldCapture) autoCaptureRequests++
            }
        }
    }
    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setResolutionSelector(analysisResolutionSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setOutputImageRotationEnabled(true)
            .build()
            .also { it.setAnalyzer(analysisExecutor, analyzer) }
    }
    DisposableEffect(Unit) {
        onDispose {
            analyzer.paused = true
            imageAnalysis.clearAnalyzer()
            analysisExecutor.execute { analyzer.close() }
            analysisExecutor.shutdown()
        }
    }
    SideEffect {
        analyzer.viewWidth = viewSize.width
        analyzer.viewHeight = viewSize.height
        analyzer.paused = capturing || checking || !hasPermission
    }
    fun resetGuide() {
        engine.reset()
        guide = LiveGuide()
        liveFrame = null
    }
    LaunchedEffect(lensFacing) { resetGuide() }

    val autoAvailable = analysisBound && guide.hint != GuideHint.AUTO_UNAVAILABLE

    fun capture(auto: Boolean) {
        val cam = camera ?: return
        val size = viewSize
        if (capturing || checking || size == IntSize.Zero) return
        // Otomatis: potong ketat seperti yang terlihat di kotak pembatas. Manual: seluruh kotak panduan.
        val focus = if (auto) liveFrame?.focus else null
        capturing = true
        analyzer.paused = true
        scope.launch {
            runCatching {
                // Manual: fokus ke pusat kotak panduan dulu. Otomatis: LANGSUNG jepret, karena gambar sudah
                // dinilai tajam dan stabil; menunggu fokus ulang (s.d. 1,2 dtk) hanya membuat mata perih,
                // dan kamera depan berfokus tetap sehingga fokus ulang tidak berguna.
                if (!auto) {
                    val center = GuideBox.rectInView(size.width.toFloat(), size.height.toFloat())
                    val point = previewView.meteringPointFactory.createPoint(center.centerX(), center.centerY())
                    cam.cameraControl.focusOn(point, mainExecutor)
                }
                val proxy = imageCapture.takePictureAsync(mainExecutor)
                // Sensor selesai: getar + "boleh lepas". Pemotongan dan pemeriksaan U-Net berjalan sesudahnya.
                shutterDone = true
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                withContext(Dispatchers.Default) { proxy.decodeGuideCropAndClose(size.width, size.height, mirror = front, focus = focus) }
            }
                .onSuccess(onCaptured)
                .onFailure { onCaptureFailed("Gagal mengambil foto. Coba lagi.") }
            capturing = false
            shutterDone = false
            resetGuide()
        }
    }
    // Analisis terpasang tetapi tidak ada frame sama sekali (mis. konversi gambar gagal di HP ini): mode manual.
    val checkingNow by rememberUpdatedState(checking)
    LaunchedEffect(camera) {
        if (camera != null) {
            delay(5000)
            if (liveFrame == null && !capturing && !checkingNow) analysisBound = false
        }
    }
    LaunchedEffect(autoCaptureRequests) {
        if (autoCaptureRequests > 0) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            capture(auto = true)
        }
    }

    if (hasPermission) {
        CameraWithGuide(
            previewView = previewView,
            imageCapture = imageCapture,
            imageAnalysis = imageAnalysis,
            camera = camera,
            lensFacing = lensFacing,
            canFlip = canFlip,
            guide = guide,
            liveFrame = liveFrame,
            autoAvailable = autoAvailable,
            busy = capturing || checking,
            shutterDone = shutterDone || checking,
            onCanFlip = { canFlip = it },
            onFlip = {
                cameraError = null
                lensFacing = if (front) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
            },
            onCamera = { camera = it },
            onAnalysisBound = { analysisBound = it },
            onViewSize = { viewSize = it },
            onCameraError = { cameraError = it },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(24.dp))
        )
        if (autoAvailable) {
            Spacer(Modifier.height(12.dp))
            GuideChecklist(guide.checks, Modifier.padding(horizontal = 4.dp))
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(24.dp))
                .background(Pink100)
                .border(2.dp, Pink500.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Visibility, contentDescription = null, tint = Pink500, modifier = Modifier.size(72.dp))
        }
    }

    Spacer(Modifier.height(14.dp))
    AnemomCard(Modifier.fillMaxWidth()) {
        Text("Cara memotret", style = MaterialTheme.typography.titleMedium, color = Maroon)
        Spacer(Modifier.height(6.dp))
        (if (front) {
            listOf(
                "Kamera depan tidak punya lampu: cari cahaya terang dari depan wajah, misalnya dekat jendela.",
                "Taruh mata di garis atas bingkai dan kelopak bawah di lengkung merah. Lihat ke atas, tarik kelopak bawah, lalu arahkan kamera sedikit dari atas (agak menukik) ke kelopak bawah.",
                "Kamera depan sulit fokus dari jarak sangat dekat. Bila diminta, jauhkan HP sedikit.",
                "Tarik kelopak hanya saat diminta. Saat bingkai hijau, tahan sebentar sampai HP bergetar, lalu kelopak boleh dilepas."
            )
        } else {
            listOf(
                "Cari ruangan dengan cahaya terang dan merata. Hindari cahaya dari belakang.",
                "Taruh mata di garis atas bingkai dan kelopak bawah di lengkung merah. Lihat ke atas, tarik kelopak bawah, lalu arahkan kamera sedikit dari atas (agak menukik) ke kelopak bawah.",
                "Kamera sudah diperbesar 2x, jadi HP cukup dipegang sekitar 15 cm dari mata.",
                "Tarik kelopak hanya saat diminta. Saat bingkai hijau, tahan sebentar sampai HP bergetar, lalu kelopak boleh dilepas."
            )
        }).forEachIndexed { i, step ->
            Text("${i + 1}. $step", style = MaterialTheme.typography.bodySmall, color = MutedText)
        }
    }
    Spacer(Modifier.height(14.dp))

    val canShoot = !capturing && !checking && cameraError == null && camera != null && viewSize != IntSize.Zero
    when {
        !hasPermission -> GradientButton(
            text = "Izinkan kamera",
            icon = Icons.Outlined.CameraAlt,
            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
        )
        // Petunjuk otomatis tidak jalan di HP ini: tombol jepret jadi tombol utama seperti sebelumnya.
        !autoAvailable -> GradientButton(
            text = when {
                checking -> "Memeriksa foto..."
                capturing -> "Memfokuskan..."
                else -> "Ambil foto manual"
            },
            icon = Icons.Outlined.CameraAlt,
            enabled = canShoot,
            isLoading = capturing || checking,
            onClick = { capture(auto = false) }
        )
        checking || capturing -> GradientButton(
            text = if (checking) "Memeriksa foto..." else "Memotret...",
            icon = Icons.Outlined.CameraAlt,
            enabled = false,
            isLoading = true,
            onClick = {}
        )
        // Cadangan bila petunjuk otomatis tidak kunjung hijau (mis. cahaya kurang pas).
        else -> AnemomOutlinedButton(
            text = "Ambil foto manual",
            onClick = { if (canShoot) capture(auto = false) }
        )
    }
    (cameraError ?: message)?.let {
        Spacer(Modifier.height(8.dp))
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = AmberText,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Zoom yang bisa dipilih. Kamera belakang 2x bawaan: HP bisa dipegang lebih jauh sehingga tetap fokus.
 * Kamera depan 1x bawaan: zoom di kamera depan hampir selalu digital dan mengurangi ketajaman.
 */
private val ZOOM_STEPS = listOf(1f, 2f, 3f)
private const val DEFAULT_ZOOM = 2f
private const val DEFAULT_ZOOM_FRONT = 1f

@Composable
private fun CameraWithGuide(
    previewView: PreviewView,
    imageCapture: ImageCapture,
    imageAnalysis: ImageAnalysis,
    camera: Camera?,
    lensFacing: Int,
    canFlip: Boolean,
    guide: LiveGuide,
    liveFrame: LiveFrame?,
    autoAvailable: Boolean,
    busy: Boolean,
    shutterDone: Boolean,
    onCanFlip: (Boolean) -> Unit,
    onFlip: () -> Unit,
    onCamera: (Camera?) -> Unit,
    onAnalysisBound: (Boolean) -> Unit,
    onViewSize: (IntSize) -> Unit,
    onCameraError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val front = lensFacing == CameraSelector.LENS_FACING_FRONT
    var torchOn by remember(lensFacing) { mutableStateOf(false) }
    var zoom by remember(lensFacing) { mutableStateOf(if (front) DEFAULT_ZOOM_FRONT else DEFAULT_ZOOM) }

    DisposableEffect(lifecycleOwner, lensFacing) {
        var disposed = false
        var provider: ProcessCameraProvider? = null
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (disposed) return@addListener
            runCatching {
                val p = future.get().also { provider = it }
                val preview = Preview.Builder().setResolutionSelector(previewResolutionSelector).build()
                preview.setSurfaceProvider(previewView.surfaceProvider)
                onCanFlip(
                    runCatching {
                        p.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) && p.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                    }.getOrDefault(false)
                )
                p.unbindAll()
                val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                // Preview + foto + analisis live. Bila HP tidak sanggup ketiganya, tanpa analisis (mode manual).
                val cam = runCatching {
                    p.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture, imageAnalysis)
                        .also { onAnalysisBound(true) }
                }.getOrElse {
                    p.unbindAll()
                    onAnalysisBound(false)
                    p.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                }
                onCamera(cam)
            }.onFailure {
                // HP tanpa kamera depan (atau gagal dibuka): pindah otomatis ke kamera belakang.
                if (front) onFlip() else onCameraError("Kamera tidak dapat dibuka di perangkat ini.")
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            disposed = true
            provider?.unbindAll()
            onCamera(null)
        }
    }
    // Zoom maksimum baru diketahui setelah kamera terbuka, jadi dipantau dari ZoomState.
    var maxZoom by remember { mutableStateOf(1f) }
    DisposableEffect(camera, lifecycleOwner) {
        val live = camera?.cameraInfo?.zoomState
        val observer = Observer<ZoomState> { maxZoom = it.maxZoomRatio }
        live?.observe(lifecycleOwner, observer)
        onDispose { live?.removeObserver(observer) }
    }
    LaunchedEffect(camera, zoom, maxZoom) {
        camera?.cameraControl?.setZoomRatio(zoom.coerceAtMost(maxZoom))
    }
    LaunchedEffect(camera, torchOn) {
        camera?.takeIf { it.cameraInfo.hasFlashUnit() }?.cameraControl?.enableTorch(torchOn)
    }

    Box(modifier.onSizeChanged(onViewSize)) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        EyeGuideOverlay(
            guide = guide,
            frame = liveFrame,
            mirror = front,
            showLive = autoAvailable && !busy,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(camera) {
                    // Ketuk untuk fokus di titik yang diketuk.
                    detectTapGestures { offset ->
                        val cam = camera ?: return@detectTapGestures
                        val point = previewView.meteringPointFactory.createPoint(offset.x, offset.y)
                        scope.launch { cam.cameraControl.focusOn(point, ContextCompat.getMainExecutor(context)) }
                    }
                }
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GuideHintBanner(
                text = when {
                    shutterDone -> "Foto diambil. Kelopak boleh dilepas."
                    busy -> GuideHint.CAPTURING.text
                    autoAvailable -> guide.hint.text
                    else -> "Taruh mata di dalam bingkai, tarik kelopak bawah, lalu ambil foto."
                },
                ready = autoAvailable && (guide.ready || busy)
            )
            if (BuildConfig.DEBUG && autoAvailable) {
                liveFrame?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${it.observation.debugText()} · ${it.processMs} ms",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
        val zoomChoices = ZOOM_STEPS.filter { it <= maxZoom }
        if (zoomChoices.size > 1) {
            TextButton(
                onClick = {
                    val i = zoomChoices.indexOfFirst { it >= zoom.coerceAtMost(maxZoom) }.coerceAtLeast(0)
                    zoom = zoomChoices[(i + 1) % zoomChoices.size]
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                Text("${zoom.coerceAtMost(maxZoom).roundToInt()}x", color = Color.White)
            }
        }
        if (canFlip) {
            IconButton(
                onClick = onFlip,
                enabled = !busy,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                Icon(
                    Icons.Outlined.Cameraswitch,
                    contentDescription = if (front) "Pakai kamera belakang" else "Pakai kamera depan",
                    tint = Color.White
                )
            }
        }
        if (camera?.cameraInfo?.hasFlashUnit() == true) {
            IconButton(
                onClick = { torchOn = !torchOn },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                Icon(
                    if (torchOn) Icons.Outlined.FlashlightOn else Icons.Outlined.FlashlightOff,
                    contentDescription = if (torchOn) "Matikan lampu" else "Nyalakan lampu",
                    tint = Color.White
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Tahap 2: pratinjau hasil U-Net
// ---------------------------------------------------------------------------------------------

@Composable
private fun ReviewStep(review: ScanStep.Review, onSend: () -> Unit, onRetake: () -> Unit) {
    val photo = remember(review.photo) { review.photo.asImageBitmap() }
    val mask = remember(review.maskOverlay) { review.maskOverlay.asImageBitmap() }
    val ok = review.verdict == MaskQuality.Verdict.OK

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(review.photo.width / review.photo.height.toFloat())
            .clip(RoundedCornerShape(24.dp))
    ) {
        Image(photo, contentDescription = "Foto mata", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        Image(mask, contentDescription = "Area konjungtiva", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
    }
    Spacer(Modifier.height(12.dp))
    AnemomCard(Modifier.fillMaxWidth()) {
        AnemomChip(
            text = if (ok) "Konjungtiva terdeteksi" else "Foto perlu diulang",
            containerColor = if (ok) GreenBg else AmberBg,
            contentColor = if (ok) GreenText else AmberText
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = review.verdict.message
                ?: "Area hijau adalah bagian yang akan dianalisis. Pastikan area hijau berada di bagian merah dalam kelopak.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedText
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Luas area terdeteksi: ${(review.areaRatio * 1000).roundToInt() / 10.0}%",
            style = MaterialTheme.typography.labelSmall,
            color = MutedText
        )
    }
    Spacer(Modifier.height(14.dp))
    if (ok) {
        GradientButton(
            text = if (review.isSending) "Mengirim..." else "Kirim untuk dianalisis",
            icon = Icons.AutoMirrored.Outlined.Send,
            enabled = !review.isSending,
            isLoading = review.isSending,
            onClick = onSend
        )
        Spacer(Modifier.height(10.dp))
    }
    AnemomOutlinedButton(text = "Ulangi foto", onClick = onRetake)
    review.sendError?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, style = MaterialTheme.typography.bodySmall, color = ErrorRed, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

// ---------------------------------------------------------------------------------------------
// Tahap 3: hasil dari server
// ---------------------------------------------------------------------------------------------

@Composable
private fun SentStep(screening: Screening, onScanAgain: () -> Unit, onInputHb: () -> Unit) {
    AnemomCard(Modifier.fillMaxWidth()) {
        Text("Hasil AI Scan Mata", style = MaterialTheme.typography.titleMedium, color = Maroon)
        Spacer(Modifier.height(10.dp))
        when {
            screening.label != null -> {
                // Bukan diagnosis: model penelitian dengan ambang skrining rendah. Label "anemia" berarti
                // skor melewati batas skrining, jadi tampilannya "Disarankan cek Hb", bukan nama penyakit.
                val followUp = screening.label == "anemia"
                AnemomChip(
                    text = if (followUp) "Disarankan cek Hb darah" else "Tidak ada tanda yang perlu ditindaklanjuti",
                    containerColor = if (followUp) AmberBg else GreenBg,
                    contentColor = if (followUp) AmberText else GreenText
                )
                screening.score?.let { score ->
                    Spacer(Modifier.height(10.dp))
                    val threshold = screening.scoreThreshold
                    Text(
                        text = if (threshold != null) {
                            "Skor AI: ${percent(score)} (batas ${percent(threshold)})"
                        } else {
                            "Skor AI: ${percent(score)}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Maroon
                    )
                    if (threshold != null) {
                        Spacer(Modifier.height(6.dp))
                        ScoreBar(score = score, threshold = threshold)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (followUp) {
                        "Skor melewati batas skrining. Ini belum tentu anemia: pastikan dengan tes Hb darah " +
                            "di puskesmas atau laboratorium, lalu catat hasilnya di aplikasi."
                    } else {
                        "Skor di bawah batas skrining. Tetap lakukan pemeriksaan Hb rutin sesuai jadwal kehamilan."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Model AI ini masih tahap penelitian dan belum divalidasi untuk ibu hamil. Hasil bisa berbeda " +
                        "antar foto, tergantung cahaya dan kamera.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedText
                )
            }
            screening.status == "failed" -> Text(
                "Foto tersimpan, tetapi analisis di server gagal. Silakan coba scan lagi nanti.",
                style = MaterialTheme.typography.bodySmall,
                color = AmberText
            )
            else -> {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Pink500, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Foto sudah tersimpan dan menunggu dianalisis di server. Hasil estimasi belum tersedia.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    GradientButton(text = "Scan lagi", icon = Icons.Outlined.CameraAlt, onClick = onScanAgain)
    Spacer(Modifier.height(10.dp))
    AnemomOutlinedButton(text = "Input Hb darah", onClick = onInputHb)
}

private fun percent(value: Float): String {
    val p = (value * 1000).roundToInt() / 10.0
    return if (p % 1.0 == 0.0) "${p.toInt()}%" else "${p.toString().replace('.', ',')}%"
}

/** Batang skor 0-100% dengan garis batas skrining. */
@Composable
private fun ScoreBar(score: Float, threshold: Float) {
    val fill = if (score >= threshold) AmberText else GreenText
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
    ) {
        drawRect(Pink100)
        drawRect(fill, size = Size(size.width * score.coerceIn(0f, 1f), size.height))
        val x = size.width * threshold.coerceIn(0f, 1f)
        drawLine(Maroon, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2.dp.toPx())
    }
}
