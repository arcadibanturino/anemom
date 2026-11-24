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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * AI Scan Mata: kamera dengan kotak panduan -> foto dipotong sesuai kotak -> U-Net di HP memeriksa
 * apakah konjungtiva terlihat -> pratinjau mask -> foto potongan dikirim ke server untuk dianalisis.
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
// Tahap 1: kamera
// ---------------------------------------------------------------------------------------------

@Composable
private fun CameraStep(
    checking: Boolean,
    message: String?,
    onCaptured: (android.graphics.Bitmap) -> Unit,
    onCaptureFailed: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
    var cameraError by remember { mutableStateOf<String?>(null) }

    if (hasPermission) {
        CameraWithGuide(
            previewView = previewView,
            imageCapture = imageCapture,
            camera = camera,
            lensFacing = lensFacing,
            canFlip = canFlip,
            onCanFlip = { canFlip = it },
            onFlip = {
                cameraError = null
                lensFacing = if (front) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
            },
            onCamera = { camera = it },
            onViewSize = { viewSize = it },
            onCameraError = { cameraError = it },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(24.dp))
        )
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
                "Tarik kelopak mata bawah perlahan sampai bagian dalam yang merah terlihat jelas.",
                "Pegang HP sekitar 20-25 cm dari mata. Kamera depan sulit fokus dari jarak dekat.",
                "Tahan stabil, lalu ambil foto. Bila hasilnya buram, minta bantuan orang lain dengan kamera belakang."
            )
        } else {
            listOf(
                "Cari ruangan dengan cahaya terang dan merata. Hindari cahaya dari belakang.",
                "Tarik kelopak mata bawah perlahan sampai bagian dalam yang merah terlihat jelas.",
                "Pegang HP sekitar 15 cm dari mata (kamera sudah diperbesar 2x). Jangan terlalu dekat agar tidak buram.",
                "Ketuk bagian merah di layar untuk memfokuskan, tahan stabil, lalu ambil foto."
            )
        }).forEachIndexed { i, step ->
            Text("${i + 1}. $step", style = MaterialTheme.typography.bodySmall, color = MutedText)
        }
    }
    Spacer(Modifier.height(14.dp))

    if (!hasPermission) {
        GradientButton(
            text = "Izinkan kamera",
            icon = Icons.Outlined.CameraAlt,
            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
        )
    } else {
        GradientButton(
            text = when {
                checking -> "Memeriksa foto..."
                capturing -> "Memfokuskan..."
                else -> "Ambil foto"
            },
            icon = Icons.Outlined.CameraAlt,
            enabled = !capturing && !checking && cameraError == null && camera != null && viewSize != IntSize.Zero,
            isLoading = capturing || checking,
            onClick = {
                val cam = camera ?: return@GradientButton
                val size = viewSize
                capturing = true
                scope.launch {
                    val mainExecutor = ContextCompat.getMainExecutor(context)
                    runCatching {
                        // Fokus ke pusat kotak panduan dulu, baru ambil foto.
                        val center = GuideBox.rectInView(size.width.toFloat(), size.height.toFloat())
                        val point = previewView.meteringPointFactory.createPoint(center.centerX(), center.centerY())
                        cam.cameraControl.focusOn(point, mainExecutor)
                        val proxy = imageCapture.takePictureAsync(mainExecutor)
                        withContext(Dispatchers.Default) { proxy.decodeGuideCropAndClose(size.width, size.height, mirror = front) }
                    }
                        .onSuccess(onCaptured)
                        .onFailure { onCaptureFailed("Gagal mengambil foto. Coba lagi.") }
                    capturing = false
                }
            }
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
    camera: Camera?,
    lensFacing: Int,
    canFlip: Boolean,
    onCanFlip: (Boolean) -> Unit,
    onFlip: () -> Unit,
    onCamera: (Camera?) -> Unit,
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
                onCamera(p.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture))
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
        GuideOverlay(
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
        AnemomChip(
            text = "Taruh bagian merah di dalam kotak",
            containerColor = Color.Black.copy(alpha = 0.55f),
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
        )
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

/** Gelapkan area di luar kotak panduan dan beri garis putih pada kotaknya. */
@Composable
private fun GuideOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = GuideBox.rectInView(size.width, size.height)
        val corner = CornerRadius(20.dp.toPx())
        val hole = RoundRect(r.left, r.top, r.right, r.bottom, corner)
        val dim = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(Offset.Zero, size))
            addRoundRect(hole)
        }
        drawPath(dim, Color.Black.copy(alpha = 0.45f))
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(r.left, r.top),
            size = Size(r.width(), r.height()),
            cornerRadius = corner,
            style = Stroke(width = 3.dp.toPx())
        )
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
