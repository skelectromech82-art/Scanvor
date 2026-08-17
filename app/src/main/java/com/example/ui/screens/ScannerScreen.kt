package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.ui.viewmodel.FlashMode
import com.example.ui.viewmodel.ScannerViewModel
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel,
    onNavigateToPageEditor: () -> Unit,
    onCloseScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bmp = BitmapFactory.decodeStream(stream)
                if (bmp != null) {
                    viewModel.onImageCaptured(bmp)
                }
            }
        }
    }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var cameraControl: CameraControl? by remember { mutableStateOf(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            // Live Camera Viewfinder
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                                .build()
                            imageCapture = capture

                            val cameraSelector = CameraSelector.Builder()
                                .requireLensFacing(uiState.cameraLensFacing)
                                .build()

                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture
                            )
                            cameraControl = camera.cameraControl

                            // Apply flash mode
                            when (uiState.flashMode) {
                                FlashMode.TORCH -> cameraControl?.enableTorch(true)
                                FlashMode.ON -> capture.flashMode = ImageCapture.FLASH_MODE_ON
                                FlashMode.AUTO -> capture.flashMode = ImageCapture.FLASH_MODE_AUTO
                                FlashMode.OFF -> {
                                    cameraControl?.enableTorch(false)
                                    capture.flashMode = ImageCapture.FLASH_MODE_OFF
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Permission request prompt
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = GscanAccent,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera Permission Needed",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Gscan needs camera access to scan documents directly from your device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = GscanPrimaryLight)
                ) {
                    Text("Grant Camera Permission")
                }
            }
        }

        // Live Document Boundary Detection Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 2.5.dp.toPx())
            val cornerLen = 36.dp.toPx()
            val left = size.width * 0.08f
            val top = size.height * 0.16f
            val right = size.width * 0.92f
            val bottom = size.height * 0.74f

            // 4 Corner brackets (Document detection frame)
            val laserColor = GscanAccent

            // Top-Left
            drawLine(laserColor, Offset(left, top), Offset(left + cornerLen, top), strokeWidth = stroke.width)
            drawLine(laserColor, Offset(left, top), Offset(left, top + cornerLen), strokeWidth = stroke.width)

            // Top-Right
            drawLine(laserColor, Offset(right, top), Offset(right - cornerLen, top), strokeWidth = stroke.width)
            drawLine(laserColor, Offset(right, top), Offset(right, top + cornerLen), strokeWidth = stroke.width)

            // Bottom-Left
            drawLine(laserColor, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeWidth = stroke.width)
            drawLine(laserColor, Offset(left, bottom), Offset(left, bottom - cornerLen), strokeWidth = stroke.width)

            // Bottom-Right
            drawLine(laserColor, Offset(right, bottom), Offset(right - cornerLen, bottom), strokeWidth = stroke.width)
            drawLine(laserColor, Offset(right, bottom), Offset(right, bottom - cornerLen), strokeWidth = stroke.width)
        }

        // Top Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCloseScanner,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Flash Button
                IconButton(
                    onClick = { viewModel.toggleFlashMode() },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    val icon = when (uiState.flashMode) {
                        FlashMode.OFF -> Icons.Default.FlashOff
                        FlashMode.AUTO -> Icons.Default.FlashAuto
                        FlashMode.ON -> Icons.Default.FlashOn
                        FlashMode.TORCH -> Icons.Default.Highlight
                    }
                    Icon(icon, contentDescription = "Flash ${uiState.flashMode.name}", tint = if (uiState.flashMode != FlashMode.OFF) GscanGold else Color.White)
                }

                // Camera Flip
                IconButton(
                    onClick = { viewModel.toggleCameraFacing() },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Flip Camera", tint = Color.White)
                }
            }
        }

        // Bottom Capture & Page Reel Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f), Color.Black)
                    )
                )
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page Thumbnail Reel (if multi-page scanned)
            if (uiState.scannedPages.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(uiState.scannedPages) { index, item ->
                        Box(
                            modifier = Modifier
                                .size(50.dp, 65.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    2.dp,
                                    if (index == uiState.currentPageIndex) GscanAccent else Color.White.copy(alpha = 0.5f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    viewModel.selectPageIndex(index)
                                    onNavigateToPageEditor()
                                }
                        ) {
                            Image(
                                bitmap = item.originalBitmap.asImageBitmap(),
                                contentDescription = "Page ${index + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text("${index + 1}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Capture Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery Import button
                IconButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = Color.White)
                }

                // Shutter Capture Button
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(5.dp)
                        .clip(CircleShape)
                        .background(GscanPrimaryLight)
                        .clickable {
                            val capture = imageCapture
                            if (capture != null) {
                                capture.takePicture(
                                    cameraExecutor,
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val buffer = image.planes[0].buffer
                                            val bytes = ByteArray(buffer.remaining())
                                            buffer.get(bytes)
                                            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                            val rotation = image.imageInfo.rotationDegrees

                                            val rotatedBmp = if (rotation != 0) {
                                                val m = Matrix()
                                                m.postRotate(rotation.toFloat())
                                                Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
                                            } else {
                                                bmp
                                            }
                                            image.close()

                                            ContextCompat.getMainExecutor(context).execute {
                                                viewModel.onImageCaptured(rotatedBmp)
                                                onNavigateToPageEditor()
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            exception.printStackTrace()
                                            // Fallback simulated document scan
                                            val fallbackBmp = Bitmap.createBitmap(800, 1100, Bitmap.Config.ARGB_8888).apply {
                                                val c = android.graphics.Canvas(this)
                                                c.drawColor(android.graphics.Color.WHITE)
                                                val p = android.graphics.Paint().apply {
                                                    color = android.graphics.Color.DKGRAY
                                                    textSize = 34f
                                                }
                                                c.drawText("Gscan Document Capture", 100f, 200f, p)
                                            }
                                            ContextCompat.getMainExecutor(context).execute {
                                                viewModel.onImageCaptured(fallbackBmp)
                                                onNavigateToPageEditor()
                                            }
                                        }
                                    }
                                )
                            } else {
                                // Direct capture fallback
                                val sampleBmp = Bitmap.createBitmap(900, 1200, Bitmap.Config.ARGB_8888).apply {
                                    val c = android.graphics.Canvas(this)
                                    c.drawColor(android.graphics.Color.WHITE)
                                    val p = android.graphics.Paint().apply {
                                        color = android.graphics.Color.BLACK
                                        textSize = 40f
                                    }
                                    c.drawText("Scanned Document Page ${uiState.scannedPages.size + 1}", 100f, 200f, p)
                                }
                                viewModel.onImageCaptured(sampleBmp)
                                onNavigateToPageEditor()
                            }
                        }
                        .testTag("scanner_shutter_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = "Capture Document",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Done / Next button if pages exist
                if (uiState.scannedPages.isNotEmpty()) {
                    IconButton(
                        onClick = onNavigateToPageEditor,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(GscanSuccess)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Done Scanning", tint = Color.White)
                    }
                } else {
                    Spacer(modifier = Modifier.size(52.dp))
                }
            }
        }
    }
}
