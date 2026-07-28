package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview as CameraPreviewBuilder
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vaultStore = VravVaultStore(this)
        Localization.currentLang = vaultStore.getLanguage()
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(CyberPurple)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Shield",
                                            tint = Color.White,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .align(Alignment.Center)
                                        )
                                    }
                                    Column {
                                        Text(
                                            Localization.t("app_title"),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 16.sp,
                                            color = Slate100,
                                            lineHeight = 18.sp
                                        )
                                        Text(
                                            Localization.t("app_subtitle"),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberPurple,
                                            letterSpacing = 1.2.sp
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = DarkBg,
                                titleContentColor = Slate100
                            ),
                            modifier = Modifier.border(width = 1.dp, color = Color(0xFFE7E0EC)),
                            actions = {
                                var dropdownExpanded by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF232126))
                                        .clickable { dropdownExpanded = true }
                                        .border(1.dp, Color(0xFFE7E0EC).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("language_selector_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = when (Localization.currentLang) {
                                                "RU" -> "🇷🇺 RU"
                                                "BG" -> "🇧🇬 BG"
                                                else -> "🇺🇸 EN"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate100
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Switch language",
                                            tint = CyberTeal,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = dropdownExpanded,
                                        onDismissRequest = { dropdownExpanded = false },
                                        modifier = Modifier.background(Color(0xFF232126))
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("English (EN)", color = Slate100, fontSize = 12.sp) },
                                            onClick = {
                                                Localization.currentLang = "EN"
                                                vaultStore.saveLanguage("EN")
                                                dropdownExpanded = false
                                            },
                                            modifier = Modifier.testTag("lang_en")
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Русский (RU)", color = Slate100, fontSize = 12.sp) },
                                            onClick = {
                                                Localization.currentLang = "RU"
                                                vaultStore.saveLanguage("RU")
                                                dropdownExpanded = false
                                            },
                                            modifier = Modifier.testTag("lang_ru")
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Български (BG)", color = Slate100, fontSize = 12.sp) },
                                            onClick = {
                                                Localization.currentLang = "BG"
                                                vaultStore.saveLanguage("BG")
                                                dropdownExpanded = false
                                            },
                                            modifier = Modifier.testTag("lang_bg")
                                        )
                                    }
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkBg)
                            .padding(innerPadding)
                    ) {
                        Stateless2FAScreen()
                    }
                }
            }
        }
    }
}

data class ParsedOtpAuth(
    val label: String,
    val issuer: String,
    val secret: String,
    val digits: Int = 6,
    val period: Int = 30
)

fun parseOtpAuthUri(uriString: String): ParsedOtpAuth? {
    return try {
        val lower = uriString.trim()
        if (!lower.lowercase().startsWith("otpauth://totp/")) {
            if (lower.matches(Regex("^[A-Z2-7]{10,64}$", RegexOption.IGNORE_CASE))) {
                return ParsedOtpAuth(
                    label = "Imported Token",
                    issuer = "External App",
                    secret = lower.uppercase()
                )
            }
            return null
        }
        val uri = android.net.Uri.parse(uriString)
        val secret = uri.getQueryParameter("secret") ?: return null
        var issuer = uri.getQueryParameter("issuer") ?: ""
        val digits = uri.getQueryParameter("digits")?.toIntOrNull() ?: 6
        val period = uri.getQueryParameter("period")?.toIntOrNull() ?: 30

        var path = uri.path ?: ""
        if (path.startsWith("/")) {
            path = path.substring(1)
        }

        var label = path
        if (path.contains(":")) {
            val parts = path.split(":", limit = 2)
            val pathIssuer = parts[0].trim()
            val pathLabel = parts[1].trim()
            if (issuer.isEmpty()) {
                issuer = pathIssuer
            }
            label = pathLabel
        }

        if (issuer.isEmpty()) {
            issuer = "External Service"
        }

        ParsedOtpAuth(
            label = if (label.isNotEmpty()) label else "Account",
            issuer = issuer,
            secret = secret.uppercase(),
            digits = digits,
            period = period
        )
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalGetImage::class, ExperimentalPermissionsApi::class)
@Composable
fun CameraScannerOverlay(
    onDismiss: () -> Unit,
    onQrCodeScanned: (String) -> Unit
) {
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (cameraPermissionState.status.isGranted) {
                    val context = LocalContext.current
                    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { previewView ->
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val previewUseCase = CameraPreviewBuilder.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val imageAnalysisUseCase = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also {
                                        it.setAnalyzer(
                                            ContextCompat.getMainExecutor(context),
                                            QrCodeAnalyzer { resultString ->
                                                onQrCodeScanned(resultString)
                                            }
                                        )
                                    }

                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        previewUseCase,
                                        imageAnalysisUseCase
                                    )
                                } catch (_: Exception) {
                                }
                            }, ContextCompat.getMainExecutor(context))
                        }
                    )

                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .align(Alignment.Center)
                            .border(BorderStroke(3.dp, CyberPurple), RoundedCornerShape(24.dp))
                    )

                    Text(
                        text = Localization.t("camera_align_bounds"),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 70.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Permission Needed",
                            tint = CyberPurple,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = Localization.t("camera_permission_title"),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Localization.t("camera_permission_fallback"),
                            color = Slate300,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { cameraPermissionState.launchPermissionRequest() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(Localization.t("btn_grant_permission"), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .padding(24.dp)
                        .align(Alignment.TopEnd)
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(100))
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close scanner drawer", tint = Color.White)
                }
            }
        }
    }
}
