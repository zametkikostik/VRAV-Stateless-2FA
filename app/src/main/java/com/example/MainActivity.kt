package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import kotlinx.coroutines.delay
import java.util.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import java.security.MessageDigest

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

// Data holder for parsed otpauth URI
data class ParsedOtpAuth(
    val label: String,
    val issuer: String,
    val secret: String,
    val digits: Int = 6,
    val period: Int = 30
)

// URI Parser
fun parseOtpAuthUri(uriString: String): ParsedOtpAuth? {
    return try {
        val lower = uriString.trim()
        if (!lower.lowercase().startsWith("otpauth://totp/")) {
            // Also accept pure raw Base32 secret string from copy-paste
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

// Derive final AES key from wallet signature string and optional hardware token salt
fun deriveFinalKey(signature: String, yubiResponse: ByteArray?): ByteArray {
    val sigBytes = signature.toByteArray(Charsets.UTF_8)
    return if (yubiResponse != null) {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(sigBytes)
            digest.update(yubiResponse)
            digest.digest()
        } catch (e: Exception) {
            AesEncryptionUtils.deriveAesKey(signature)
        }
    } else {
        AesEncryptionUtils.deriveAesKey(signature)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Stateless2FAScreen() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    // 1. Wallets States
    var privateKeyInput by remember { mutableStateOf("") }
    var loadedAddress by remember { mutableStateOf("") }
    var walletPrivateKey by remember { mutableStateOf("") }

    // Constants
    val signatureMessage = "Sign this message to securely generate your VRAV 2FA Secret Key"

    // 2. Original Auth Flow States
    var generatedSignature by remember { mutableStateOf("") }
    var computedSecretKey by remember { mutableStateOf("") }
    var otpauthUri by remember { mutableStateOf("") }

    // 3. User verification states
    var enteredVerifyToken by remember { mutableStateOf("") }
    var verificationStatus by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    // Countdown / ticking state
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    var liveOtpCode by remember { mutableStateOf("000000") }

    // 4. VAULT SYSTEM STATES
    var isVaultUnlocked by remember { mutableStateOf(false) }
    var derivedAesKey by remember { mutableStateOf<ByteArray?>(null) }
    val vaultStore = remember { VravVaultStore(context) }
    val externalAccounts = remember { mutableStateListOf<ExternalAccount>() }
    var showDecryptionErrorDialog by remember { mutableStateOf(false) }

    // Backup & Restore Launchers using Storage Access Framework (SAF)
    val contentResolver = context.contentResolver
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                val json = vaultStore.exportEncryptedVault()
                contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(json.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, Localization.t("toast_export_success"), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "${Localization.t("toast_export_failed")}: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                contentResolver.openInputStream(it)?.use { stream ->
                    val json = stream.bufferedReader().use { r -> r.readText() }
                    if (vaultStore.importEncryptedVault(json)) {
                        externalAccounts.clear()
                        externalAccounts.addAll(vaultStore.getAccounts())
                        Toast.makeText(context, Localization.t("toast_import_success"), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, Localization.t("toast_invalid_format"), Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "${Localization.t("toast_import_failed")}: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // YubiKey Hardware Authentication States
    var useYubiKeyForVault by remember { mutableStateOf(false) }
    var yubiKeyHmacResponse by remember { mutableStateOf<ByteArray?>(null) }
    var showYubiKeyTappingOverlay by remember { mutableStateOf(false) }

    // Modals / Overlays triggers
    var showQrScanner by remember { mutableStateOf(false) }
    var showManualAddDialog by remember { mutableStateOf(false) }

    // Map to keep track of live external OTP codes updated at launch
    val externalAccountsOtp = remember { mutableStateMapOf<String, String>() }

    // Background ticking timer
    LaunchedEffect(key1 = computedSecretKey, key2 = externalAccounts.size, key3 = isVaultUnlocked) {
        while (true) {
            try {
                val now = System.currentTimeMillis() / 1000
                currentTime = now
                
                // Root statutory reproducible key
                if (computedSecretKey.isNotEmpty() && computedSecretKey != "ERRORGENERATE2FA") {
                    val secretBytes = TotpUtil.decodeBase32(computedSecretKey)
                    val currentWindow = now / 30
                    liveOtpCode = TotpUtil.generateTotp(secretBytes, currentWindow)
                }

                // External saved decrypted accounts update
                if (isVaultUnlocked && derivedAesKey != null) {
                    externalAccounts.forEach { acc ->
                        try {
                            val plainSecret = AesEncryptionUtils.decrypt(acc.encryptedSecret, derivedAesKey!!)
                            val secretBytes = TotpUtil.decodeBase32(plainSecret)
                            val currentWindow = now / acc.period
                            val code = TotpUtil.generateTotp(secretBytes, currentWindow, acc.digits)
                            externalAccountsOtp[acc.id] = code
                        } catch (e: Exception) {
                            externalAccountsOtp[acc.id] = "ERRDEC"
                        }
                    }
                }
            } catch (t: Throwable) {
                // Ignore exceptions in timer threat
            }
            delay(1000)
        }
    }

    val remainingSeconds = 30 - (currentTime % 30)
    val progressPercent = remainingSeconds.toFloat() / 30f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Core description
        Text(
            text = "Stateless, zero-gas, database-free 2FA bridging Web3 Elliptic Curve signatures with standard TOTP Google Authenticator algorithm.",
            color = Slate400,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .widthIn(max = 500.dp)
        )

        // VAULT DECRYPTED AREA (Displays only if unlocked)
        if (isVaultUnlocked) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1FDF4)), // Emerald-tinted theme
                border = BorderStroke(1.5.dp, CyberEmerald)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Vault icon",
                                tint = CyberEmerald,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "🔒 " + Localization.t("decrypted"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F5132)
                                )
                                Text(
                                    text = Localization.t("vault_active_desc"),
                                    fontSize = 11.sp,
                                    color = Color(0xFF198754)
                                )
                            }
                        }

                        // Countdown circle loader
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(36.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { progressPercent },
                                strokeWidth = 3.dp,
                                color = CyberEmerald,
                                trackColor = Color(0xFFD1E7DD),
                                modifier = Modifier.fillMaxSize()
                            )
                            Text(
                                text = "${remainingSeconds}s",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F5132)
                            )
                        }
                    }

                    // Scan and Add Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showQrScanner = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("scan_qr_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                            shape = RoundedCornerShape(23.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Scan Icon",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(Localization.t("btn_scan_qr"), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = { showManualAddDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("add_manually_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberTeal),
                            shape = RoundedCornerShape(23.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Manual icon",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(Localization.t("btn_add_manually"), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    // Accounts List
                    if (externalAccounts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFE7E0EC), RoundedCornerShape(16.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "No credentials",
                                    tint = Slate400.copy(alpha = 0.5f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    Localization.t("no_accounts_loaded"),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = Slate300
                                )
                                Text(
                                    Localization.t("no_accounts_desc"),
                                    fontSize = 11.sp,
                                    color = Slate400,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            externalAccounts.forEach { acc ->
                                val code = externalAccountsOtp[acc.id] ?: "000000"
                                
                                // Service Branding parameters
                                val brandColor = when (acc.issuer.lowercase().trim()) {
                                    "google" -> Color(0xFF4285F4)
                                    "binance" -> Color(0xFFF0B90B)
                                    "gouslugi", "gosuslugi", "госуслуги" -> Color(0xFFD32F2F)
                                    else -> CyberPurple
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, brandColor.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                // Service chip/badge
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(brandColor.copy(alpha = 0.12f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = acc.issuer.uppercase(),
                                                        color = brandColor,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                }
                                                Text(
                                                    text = acc.label,
                                                    fontSize = 12.sp,
                                                    color = Slate300,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            // The running 6-Digit TOTP Token
                                            Text(
                                                text = code.chunked(3).joinToString(" "),
                                                fontSize = 26.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Black,
                                                color = brandColor,
                                                letterSpacing = 2.sp,
                                                modifier = Modifier
                                                    .clickable {
                                                        if (code != "000000" && code != "ERRDEC") {
                                                            clipboardManager.setText(AnnotatedString(code))
                                                            Toast.makeText(context, "$code " + Localization.t("toast_copied"), Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                    .testTag("external_token_${acc.id}")
                                            )
                                        }

                                        // Actions: Copy & Delete
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(code))
                                                    Toast.makeText(context, Localization.t("toast_code_copied"), Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = "Copy code",
                                                    tint = Slate400,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    vaultStore.deleteAccount(acc.id)
                                                    externalAccounts.clear()
                                                    externalAccounts.addAll(vaultStore.getAccounts())
                                                    Toast.makeText(context, Localization.t("toast_deleted_success"), Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = Color.Red.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Locking / Vault management section
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                // Lock the Vault Session securely
                                isVaultUnlocked = false
                                derivedAesKey = null
                                externalAccounts.clear()
                                Toast.makeText(context, Localization.t("toast_session_locked"), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("lock_session_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray),
                            border = BorderStroke(1.dp, Color.Gray),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(Localization.t("lock_session"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                vaultStore.clearVault()
                                isVaultUnlocked = false
                                derivedAesKey = null
                                externalAccounts.clear()
                                Toast.makeText(context, Localization.t("toast_vault_wiped"), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1.0f)
                                .height(40.dp)
                                .testTag("purge_vault_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(Localization.t("purge_vault"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Web3 Encrypted Backup/Restore Exports Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                exportLauncher.launch("vrav_secure_2fa_vault.json")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberTeal),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("export_vault_button")
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Export backup",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(Localization.t("export_backup"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                importLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("import_vault_button")
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Import backup",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(Localization.t("import_backup"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // CARD 1: Ethereum Wallet Configuration
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = BorderStroke(1.dp, Color(0xFFEADDFF))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Wallet Icon",
                            tint = CyberPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = Localization.t("card1_title"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Slate100,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100))
                            .background(Color(0xFFE8DEF8))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(100))
                                    .background(Color(0xFF22C55E))
                            )
                            Text(
                                "POLYGON",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF21005D)
                            )
                        }
                    }
                }

                Text(
                    text = Localization.t("card1_desc"),
                    fontSize = 12.sp,
                    color = Slate300
                )

                // Input field for Private Key
                OutlinedTextField(
                    value = privateKeyInput,
                    onValueChange = { privateKeyInput = it },
                    label = { Text(Localization.t("private_key_label")) },
                    placeholder = { Text(Localization.t("private_key_placeholder")) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        color = Slate100,
                        fontSize = 13.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("private_key_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberPurple,
                        unfocusedBorderColor = Color(0xFFE7E0EC),
                        focusedLabelColor = CyberPurple,
                        unfocusedLabelColor = Slate300
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Generate new key Button
                    Button(
                        onClick = {
                            try {
                                val (priv, addr) = EthereumCryptoUtils.generateWallet()
                                privateKeyInput = priv
                                loadedAddress = addr
                                walletPrivateKey = priv
                                Toast.makeText(context, Localization.t("toast_new_key"), Toast.LENGTH_SHORT).show()
                            } catch (e: Throwable) {
                                Toast.makeText(context, "${Localization.t("toast_wallet_error")}: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("generate_key_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(Localization.t("btn_new_wallet"), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    // Import/Apply key Button
                    Button(
                        onClick = {
                            if (privateKeyInput.isBlank()) {
                                Toast.makeText(context, Localization.t("toast_key_valid"), Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            try {
                                val address = EthereumCryptoUtils.loadWallet(privateKeyInput)
                                loadedAddress = address
                                walletPrivateKey = privateKeyInput
                                Toast.makeText(context, Localization.t("toast_wallet_loaded"), Toast.LENGTH_SHORT).show()
                            } catch (e: Throwable) {
                                Toast.makeText(context, Localization.t("toast_invalid_key_hex"), Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("import_key_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberTeal),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(Localization.t("btn_apply_key"), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }

                // Show active wallet address
                if (loadedAddress.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE7E0EC), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(CyberEmerald)
                                    )
                                    Text(
                                        text = Localization.t("wallet_address"),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = CyberPurple,
                                        letterSpacing = 1.sp
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(loadedAddress))
                                        Toast.makeText(context, Localization.t("toast_address_copied"), Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Copy address",
                                        tint = Slate300,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = loadedAddress,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Slate100,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // CARD 2: Cryptographic Signature Process (MetaMask Layer)
        if (loadedAddress.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = BorderStroke(1.dp, Color(0xFFEADDFF))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Sign Icon",
                                tint = CyberPurple,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = Localization.t("card2_title"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Slate100,
                                letterSpacing = 0.5.sp
                            )
                        }
                        
                        // Small state indicator
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isVaultUnlocked) CyberEmerald.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isVaultUnlocked) Localization.t("decrypted") else Localization.t("vault_locked"),
                                color = if (isVaultUnlocked) CyberEmerald else Color.Red,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = Localization.t("card2_desc"),
                        fontSize = 12.sp,
                        color = Slate300
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE7E0EC), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "MESSAGE TO SIGN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = CyberPurple
                            )
                            Text(
                                text = "\"$signatureMessage\"",
                                fontSize = 13.sp,
                                color = Slate100,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // YubiKey Hardware Factor toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE7E0EC), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                Localization.t("hw_yubikey_2fa"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Slate100
                            )
                            Text(
                                Localization.t("hw_yubikey_desc"),
                                fontSize = 10.sp,
                                color = Slate400
                            )
                        }
                        Switch(
                            checked = useYubiKeyForVault,
                            onCheckedChange = { useYubiKeyForVault = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberTeal,
                                checkedTrackColor = CyberTeal.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.testTag("yubikey_toggle")
                        )
                    }

                    Button(
                        onClick = {
                            if (walletPrivateKey.isBlank()) {
                                Toast.makeText(context, Localization.t("toast_configure_first"), Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            try {
                                if (useYubiKeyForVault) {
                                    showYubiKeyTappingOverlay = true
                                } else {
                                    val sig = EthereumCryptoUtils.personalSign(signatureMessage, walletPrivateKey)
                                    generatedSignature = sig
                                    
                                    // Compute reproducible original secret
                                    val secret = TotpUtil.generateSecretKeyFromSignature(sig)
                                    computedSecretKey = secret
                                    otpauthUri = TotpUtil.getTOTPUri(loadedAddress, secret)
                                    
                                    // Derive AES key & Verify vault
                                    val keyBytes = AesEncryptionUtils.deriveAesKey(sig)
                                    derivedAesKey = keyBytes
                                    
                                    if (!vaultStore.hasVaultMarker()) {
                                        // brand new vault initialization
                                        vaultStore.initializeVaultMarker(keyBytes)
                                        isVaultUnlocked = true
                                        externalAccounts.clear()
                                        externalAccounts.addAll(vaultStore.getAccounts())
                                        Toast.makeText(context, Localization.t("toast_new_vault_init"), Toast.LENGTH_SHORT).show()
                                    } else {
                                        // verify stored vault marker decryption
                                        if (vaultStore.verifyVaultMarker(keyBytes)) {
                                            isVaultUnlocked = true
                                            externalAccounts.clear()
                                            externalAccounts.addAll(vaultStore.getAccounts())
                                            Toast.makeText(context, Localization.t("toast_vault_decrypted"), Toast.LENGTH_SHORT).show()
                                        } else {
                                            showDecryptionErrorDialog = true
                                            Toast.makeText(context, Localization.t("toast_decryption_error"), Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            } catch (e: Throwable) {
                                Toast.makeText(context, "${Localization.t("toast_signing_failed")}: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("sign_message_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock", modifier = Modifier.size(18.dp))
                            Text(Localization.t("btn_sign_decrypt"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    if (generatedSignature.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFE7E0EC), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "GENERATED SECP256K1 SIGNATURE",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = CyberPurple,
                                        letterSpacing = 1.sp
                                    )
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(generatedSignature))
                                            Toast.makeText(context, Localization.t("toast_signature_copied"), Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Copy signature",
                                            tint = Slate300,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = generatedSignature,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Slate100,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // CARD 3: Stateless Reproducible TOTP Token Section
        if (computedSecretKey.isNotEmpty() && computedSecretKey != "ERRORGENERATE2FA") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = BorderStroke(1.dp, Color(0xFFEADDFF))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Key Generation",
                                tint = CyberPurple,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "3. BASE STATELESS GENERATOR",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Slate100,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyberPurple.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("REPRODUCIBLE", color = CyberPurple, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = "This root 16-character base32 key is generated purely by hashing your signature. Tap below to test standard stateless 2FA verification.",
                        fontSize = 12.sp,
                        color = Slate300
                    )

                    // Displays Base32 Secret Key
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE7E0EC), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "YOUR STATELESS 16-CHAR ROOT SECRET",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = CyberPurple,
                                letterSpacing = 1.sp
                            )
                            
                            val readableSecret = computedSecretKey.chunked(4).joinToString(" ")
                            Text(
                                text = readableSecret,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = Slate100,
                                textAlign = TextAlign.Center,
                                letterSpacing = 1.5.sp
                            )

                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(computedSecretKey))
                                    Toast.makeText(context, "Secret Key Copied!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("copy_secret_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8DEF8)),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, CyberPurple)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Copy icon", tint = CyberPurple, modifier = Modifier.size(14.dp))
                                    Text("Copy Secret", color = CyberPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Authenticator app Setup link
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE7E0EC), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "OTPAUTH REGISTRATION URI",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Slate300,
                                    letterSpacing = 1.sp
                                )
                                Button(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(otpauthUri))
                                        Toast.makeText(context, "Setup Link Copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .height(28.dp)
                                        .testTag("copy_uri_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("Copy URI", color = CyberPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = otpauthUri,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Slate300,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // LIVE EMULATED GOOGLE AUTHENTICATOR
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(28.dp))
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "ROOT GENERATED TOTP",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = CyberPurple,
                                letterSpacing = 1.sp
                            )

                            Text(
                                text = liveOtpCode.chunked(3).joinToString(" "),
                                fontSize = 32.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                color = CyberPurple,
                                letterSpacing = 4.sp
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Resets in ${remainingSeconds}s",
                                    fontSize = 11.sp,
                                    color = Slate300,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(90.dp)
                                )
                                LinearProgressIndicator(
                                    progress = { progressPercent },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(100)),
                                    color = CyberPurple,
                                    trackColor = Color(0xFFE8DEF8)
                                )
                            }
                        }
                    }
                }
            }
        }

        // CARD 4: Stateless Verification Endpoint Simulation
        if (computedSecretKey.isNotEmpty() && computedSecretKey != "ERRORGENERATE2FA") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = BorderStroke(1.dp, Color(0xFFEADDFF))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Verify Icon",
                            tint = CyberPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "4. STATELESS TOKEN VERIFICATION",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Slate100,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Text(
                        text = "Enter the 6-digit dynamic key below. The system will recreate your TOTP secret purely on-the-fly and verify standard validity.",
                        fontSize = 12.sp,
                        color = Slate300
                    )

                    OutlinedTextField(
                        value = enteredVerifyToken,
                        onValueChange = { if (it.length <= 6) enteredVerifyToken = it.filter { char -> char.isDigit() } },
                        label = { Text("6-Digit Authenticator Code") },
                        placeholder = { Text("000000") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            color = Slate100,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("totp_token_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberPurple,
                            unfocusedBorderColor = Color(0xFFE7E0EC),
                            focusedLabelColor = CyberPurple,
                            unfocusedLabelColor = Slate300
                        )
                    )

                    Button(
                        onClick = {
                            if (enteredVerifyToken.length != 6) {
                                Toast.makeText(context, "Token must be exactly 6 digits", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            
                            try {
                                val isSignatureValid = EthereumCryptoUtils.verifyPersonalSignature(
                                    signatureMessage,
                                    generatedSignature,
                                    loadedAddress
                                )
                                
                                if (!isSignatureValid) {
                                    verificationStatus = Pair(false, "API Failure: Signature validation failed.")
                                    return@Button
                                }

                                val currentWindow = System.currentTimeMillis() / 1000 / 30
                                val isTotpValid = TotpUtil.verifyTotp(computedSecretKey, enteredVerifyToken, currentWindow)
                                
                                if (isTotpValid) {
                                    verificationStatus = Pair(true, "API [200 OK]: Cryptographic signature & Authenticator TOTP token validated successfully! No DB, fully stateless authentication accomplished.")
                                } else {
                                    verificationStatus = Pair(false, "API [403 Forbidden]: Invalid TOTP code. Generated secret mismatch or code expired.")
                                }
                            } catch (e: Throwable) {
                                verificationStatus = Pair(false, "API Error: ${e.message ?: "Unknown crash occurred"}")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("verify_token_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text("Verify Token (Stateless backend call)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    // Verification status result
                    verificationStatus?.let { (success, msg) ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + expandVertically()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (success) Color(0xFFE8F5E9)
                                        else Color(0xFFFFEBEE)
                                    )
                                    .border(
                                        1.dp,
                                        if (success) Color(0xFF4CAF50) else Color(0xFFEF5350),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = if (success) Icons.Default.Check else Icons.Default.Warning,
                                        contentDescription = if (success) "Success" else "Failed",
                                        tint = if (success) Color(0xFF2E7D32) else Color(0xFFC62828),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = msg,
                                        color = if (success) Color(0xFF1B5E20) else Color(0xFFC62828),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // MODAL A: QR Code CameraX scanner Overlay
    if (showQrScanner) {
        CameraScannerOverlay(
            onDismiss = { showQrScanner = false },
            onQrCodeScanned = { rawCode ->
                showQrScanner = false
                val parsed = parseOtpAuthUri(rawCode)
                if (parsed != null) {
                    if (derivedAesKey != null) {
                        try {
                            val encryptedSecret = AesEncryptionUtils.encrypt(parsed.secret, derivedAesKey!!)
                            val newAcc = ExternalAccount(
                                id = UUID.randomUUID().toString(),
                                label = parsed.label,
                                issuer = parsed.issuer,
                                encryptedSecret = encryptedSecret,
                                digits = parsed.digits,
                                period = parsed.period
                            )
                            vaultStore.addAccount(newAcc)
                            externalAccounts.clear()
                            externalAccounts.addAll(vaultStore.getAccounts())
                            Toast.makeText(context, "${parsed.issuer} scanned & decrypted successfully!", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Encryption error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Error: Decrypt session memory lost. Sign again.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "Failed: QR doesn't contain a valid otpauth:// format.", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // MODAL B: Manual Add Account dialog (To ensure 100% testability on any remote screen/emulator)
    if (showManualAddDialog) {
        var inputLabel by remember { mutableStateOf("") }
        var inputIssuer by remember { mutableStateOf("Google") }
        var inputSecret by remember { mutableStateOf("") }
        var inputDigits by remember { mutableStateOf("6") }
        var inputPeriod by remember { mutableStateOf("30") }

        AlertDialog(
            onDismissRequest = { showManualAddDialog = false },
            title = { Text(Localization.t("dialog_add_title")) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        Localization.t("dialog_add_desc"),
                        fontSize = 12.sp,
                        color = Slate400
                    )

                    // Issuer Dropdown / TextField input
                    OutlinedTextField(
                        value = inputIssuer,
                        onValueChange = { inputIssuer = it },
                        label = { Text(Localization.t("dialog_service_label")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("manual_input_issuer")
                    )

                    OutlinedTextField(
                        value = inputLabel,
                        onValueChange = { inputLabel = it },
                        label = { Text(Localization.t("dialog_account_label")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("manual_input_label")
                    )

                    OutlinedTextField(
                        value = inputSecret,
                        onValueChange = { inputSecret = it.uppercase().replace(" ", "") },
                        label = { Text(Localization.t("dialog_secret_label")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("manual_input_secret"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CyberPurple
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = inputDigits,
                            onValueChange = { inputDigits = it.filter { char -> char.isDigit() } },
                            label = { Text(Localization.t("dialog_digits_label")) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("manual_input_digits")
                        )

                        OutlinedTextField(
                            value = inputPeriod,
                            onValueChange = { inputPeriod = it.filter { char -> char.isDigit() } },
                            label = { Text(Localization.t("dialog_period_label")) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("manual_input_period")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uppercaseSecret = inputSecret.trim().uppercase()
                        if (inputLabel.isBlank() || inputIssuer.isBlank() || uppercaseSecret.isBlank()) {
                            Toast.makeText(context, Localization.t("toast_populate_all"), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        // Validate Base32 character set
                        if (!uppercaseSecret.matches(Regex("^[A-Z2-7]+$"))) {
                            Toast.makeText(context, Localization.t("toast_invalid_base32"), Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        val digitsVal = inputDigits.toIntOrNull() ?: 6
                        val periodVal = inputPeriod.toIntOrNull() ?: 30

                        if (derivedAesKey != null) {
                            try {
                                val encryptedSecret = AesEncryptionUtils.encrypt(uppercaseSecret, derivedAesKey!!)
                                val newAcc = ExternalAccount(
                                    id = UUID.randomUUID().toString(),
                                    label = inputLabel,
                                    issuer = inputIssuer,
                                    encryptedSecret = encryptedSecret,
                                    digits = digitsVal,
                                    period = periodVal
                                )
                                vaultStore.addAccount(newAcc)
                                externalAccounts.clear()
                                externalAccounts.addAll(vaultStore.getAccounts())
                                showManualAddDialog = false
                                Toast.makeText(context, "${inputIssuer} " + Localization.t("toast_added_success"), Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "${Localization.t("toast_storage_error")}: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, Localization.t("toast_vault_locked_error"), Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
                    modifier = Modifier.testTag("dialog_confirm_button")
                ) {
                    Text(Localization.t("btn_dialog_add"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showManualAddDialog = false },
                    modifier = Modifier.testTag("dialog_cancel_button")
                ) {
                    Text(Localization.t("btn_dialog_cancel"))
                }
            }
        )
    }

    // MODAL C: YubiKey hardware authentication / NFC touch listener
    if (showYubiKeyTappingOverlay) {
        var scanningState by remember { mutableStateOf("Hold your YubiKey against the NFC hotspot...") }
        val sysActivity = context as? Activity
        
        val authenticatorCallback = remember {
            object : YubiKeyCallback {
                override fun onReady() {
                    scanningState = "Ready. Tap YubiKey..."
                }
                override fun onScanning() {
                    scanningState = "Scanning... Hold still..."
                }
                override fun onSuccess(response: ByteArray) {
                    scanningState = "Hardware Signature verified! Unlocking..."
                    yubiKeyHmacResponse = response
                    showYubiKeyTappingOverlay = false
                    
                    try {
                        val sig = EthereumCryptoUtils.personalSign(signatureMessage, walletPrivateKey)
                        generatedSignature = sig
                        
                        val secret = TotpUtil.generateSecretKeyFromSignature(sig)
                        computedSecretKey = secret
                        otpauthUri = TotpUtil.getTOTPUri(loadedAddress, secret)
                        
                        // Combine MetaMask signature with YubiKey HMAC-SHA256 hardware salt
                        val finalKey = deriveFinalKey(sig, response)
                        derivedAesKey = finalKey
                        
                        if (!vaultStore.hasVaultMarker()) {
                            vaultStore.initializeVaultMarker(finalKey)
                            isVaultUnlocked = true
                            externalAccounts.clear()
                            externalAccounts.addAll(vaultStore.getAccounts())
                            Toast.makeText(context, "Hardware Key Validated. New Vault Initialized!", Toast.LENGTH_SHORT).show()
                        } else {
                            if (vaultStore.verifyVaultMarker(finalKey)) {
                                isVaultUnlocked = true
                                externalAccounts.clear()
                                externalAccounts.addAll(vaultStore.getAccounts())
                                Toast.makeText(context, "Hardware Signature Decrypted Successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                showDecryptionErrorDialog = true
                                Toast.makeText(context, "Error: Hardware failed to decrypt Vault! Is this key registered?", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Hardware Auth Failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
                override fun onFailure(error: String) {
                    scanningState = "Error: $error"
                }
            }
        }
        
        val authenticator = remember { YubiKeyHmacAuthenticator(context, authenticatorCallback) }
        
        LaunchedEffect(Unit) {
            sysActivity?.let {
                authenticator.setChallenge(signatureMessage.toByteArray(Charsets.UTF_8))
                authenticator.startNfcListening(it)
            }
        }
        
        DisposableEffect(Unit) {
            onDispose {
                sysActivity?.let {
                    authenticator.stopNfcListening(it)
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showYubiKeyTappingOverlay = false },
            title = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = "Hardware lock", tint = CyberTeal)
                    Text("Hardware YubiKey 2FA")
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Touch and hold your physical YubiKey against the device's NFC hot spot or connect it via USB-C to execute secure HMAC-SHA256 Challenge-Response.",
                        fontSize = 12.sp,
                        color = Slate300,
                        textAlign = TextAlign.Center
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberTeal.copy(alpha = 0.08f))
                            .border(1.dp, CyberTeal.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = scanningState,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = Slate100,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    Text(
                        text = "No hardware key? Use the secure virtual emulator below for validation:",
                        fontSize = 11.sp,
                        color = Slate400,
                        textAlign = TextAlign.Center
                    )
                    
                    Button(
                        onClick = {
                            val dummyHmac = authenticator.emulateYubiKeyTap(
                                softwareSeed = walletPrivateKey.toByteArray(Charsets.UTF_8),
                                challenge = signatureMessage.toByteArray(Charsets.UTF_8)
                            )
                            authenticatorCallback.onSuccess(dummyHmac)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberTeal),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("emulate_yubikey_tap")
                    ) {
                        Text("Simulate YubiKey Hardware Tap", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showYubiKeyTappingOverlay = false },
                    modifier = Modifier.testTag("yubikey_dialog_cancel_button")
                ) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // MODAL D: Decryption Error & Desynchronization Recovery Dialog
    if (showDecryptionErrorDialog) {
        AlertDialog(
            onDismissRequest = { showDecryptionErrorDialog = false },
            title = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Decryption Mismatch Warning",
                        tint = Color.Red
                    )
                    Text(Localization.t("conflict_title"))
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = Localization.t("conflict_desc"),
                        fontSize = 12.sp,
                        color = Slate300
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Red.copy(alpha = 0.05f))
                            .border(1.dp, Color.Red.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(Localization.t("conflict_reason1"), fontSize = 11.sp, color = Slate400)
                        Text(Localization.t("conflict_reason2"), fontSize = 11.sp, color = Slate400)
                        Text(Localization.t("conflict_reason3"), fontSize = 11.sp, color = Slate400)
                    }

                    Text(
                        text = Localization.t("conflict_prompt"),
                        fontSize = 12.sp,
                        color = Slate300,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            useYubiKeyForVault = !useYubiKeyForVault
                            showDecryptionErrorDialog = false
                            val stateStr = when(Localization.currentLang) {
                                "RU" -> if (useYubiKeyForVault) "Включено" else "Выключено"
                                "BG" -> if (useYubiKeyForVault) "Активирано" else "Деактивирано"
                                else -> if (useYubiKeyForVault) "Enabled" else "Disabled"
                            }
                            val endStr = when(Localization.currentLang) {
                                "RU" -> ". Пожалуйста, подпишите / повторите попытку сейчас!"
                                "BG" -> ". Моля, подпишете / опитайте отново сега!"
                                else -> ". Please sign/retry now!"
                            }
                            Toast.makeText(
                                context,
                                Localization.t("toast_yubikey_toggled") + stateStr + endStr,
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberTeal),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("dialog_toggle_yubikey")
                    ) {
                        Text(
                            Localization.t("btn_conflict_toggle_yubi"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = {
                            showDecryptionErrorDialog = false
                            importLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("dialog_import_different_vault")
                    ) {
                        Text(
                            Localization.t("btn_conflict_import"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = {
                            vaultStore.clearVault()
                            isVaultUnlocked = false
                            derivedAesKey = null
                            externalAccounts.clear()
                            showDecryptionErrorDialog = false
                            Toast.makeText(context, Localization.t("toast_vault_reset"), Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("dialog_purge_vault")
                    ) {
                        Text(
                            Localization.t("btn_conflict_reset"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }

                    TextButton(
                        onClick = { showDecryptionErrorDialog = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_dismiss_error")
                    ) {
                        Text(Localization.t("cancel"), color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        )
    }
}

// CAMERAX LIVE VIEWER AND CAPTURE RUNNER
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
                                } catch (exc: Exception) {
                                    // Binds failed silently
                                }
                            }, ContextCompat.getMainExecutor(context))
                        }
                    )
                    
                    // Center scan window visual HUD boundary
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
                
                // Scanner Dismiss control
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
