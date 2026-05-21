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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                                            "VRAV Auth",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 16.sp,
                                            color = Slate100,
                                            lineHeight = 18.sp
                                        )
                                        Text(
                                            "HYBRID SECURE VAULT",
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
                            actions = {}
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
    val secret: String
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
            secret = secret.uppercase()
        )
    } catch (e: Exception) {
        null
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
                            val currentWindow = now / 30
                            val code = TotpUtil.generateTotp(secretBytes, currentWindow)
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
                                    "🔒 SECURE VAULT DECRYPTED",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F5132)
                                )
                                Text(
                                    "Saved Accounts Decrypted is Active",
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
                                Text("Scan 2FA QR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                                Text("Add Manually", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                                    "No external accounts scanned yet.",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = Slate300
                                )
                                Text(
                                    "Google, GoUslugi, and Binance are supported offline. Swipe or click above to add.",
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
                                                            Toast.makeText(context, "$code Copied!", Toast.LENGTH_SHORT).show()
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
                                                    Toast.makeText(context, "Code Copied!", Toast.LENGTH_SHORT).show()
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
                                                    Toast.makeText(context, "Deleted Account successfully.", Toast.LENGTH_SHORT).show()
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
                                Toast.makeText(context, "Session Locked & Cryptographic Memory Flushed.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("lock_session_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray),
                            border = BorderStroke(1.dp, Color.Gray),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Lock Session", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                vaultStore.clearVault()
                                isVaultUnlocked = false
                                derivedAesKey = null
                                externalAccounts.clear()
                                Toast.makeText(context, "Vault Completely Wiped & Destroyed.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1.0f)
                                .height(40.dp)
                                .testTag("purge_vault_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Purge Vault Data", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                            text = "1. WALLET CONTROLLER",
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
                    text = "Load an existing Polygon (Ethereum) private key or generate a secure fresh one on-the-fly to perform MetaMask signing.",
                    fontSize = 12.sp,
                    color = Slate300
                )

                // Input field for Private Key
                OutlinedTextField(
                    value = privateKeyInput,
                    onValueChange = { privateKeyInput = it },
                    label = { Text("Private Key (HEX)") },
                    placeholder = { Text("0x...") },
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
                                Toast.makeText(context, "New Secure Key Generated!", Toast.LENGTH_SHORT).show()
                            } catch (e: Throwable) {
                                Toast.makeText(context, "Wallet Gen Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("generate_key_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("New Wallet", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    // Import/Apply key Button
                    Button(
                        onClick = {
                            if (privateKeyInput.isBlank()) {
                                Toast.makeText(context, "Please enter a valid private key hex", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            try {
                                val address = EthereumCryptoUtils.loadWallet(privateKeyInput)
                                loadedAddress = address
                                walletPrivateKey = privateKeyInput
                                Toast.makeText(context, "Wallet Loaded Successfully!", Toast.LENGTH_SHORT).show()
                            } catch (e: Throwable) {
                                Toast.makeText(context, "Invalid Private key Hex!", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("import_key_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberTeal),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Apply Key", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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
                                        text = "ACTIVE PUBLIC ADDRESS",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = CyberPurple,
                                        letterSpacing = 1.sp
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(loadedAddress))
                                        Toast.makeText(context, "Address Copied!", Toast.LENGTH_SHORT).show()
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
                                text = "2. METAMASK SIGNATURE EMULATION",
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
                                text = if (isVaultUnlocked) "DECRYPTED" else "VAULT LOCKED",
                                color = if (isVaultUnlocked) CyberEmerald else Color.Red,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "Sign the fixed, deterministic system string to verify ownership of your private key, seed your stateless secret, and decrypt your offline 2FA vault.",
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

                    Button(
                        onClick = {
                            try {
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
                                    Toast.makeText(context, "Signature generated. New Vault initialized!", Toast.LENGTH_SHORT).show()
                                } else {
                                    // verify stored vault marker decryption
                                    if (vaultStore.verifyVaultMarker(keyBytes)) {
                                        isVaultUnlocked = true
                                        externalAccounts.clear()
                                        externalAccounts.addAll(vaultStore.getAccounts())
                                        Toast.makeText(context, "Wallet Signature Authenticated. Vault Decrypted!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Error: Signature failed to decrypt stored Vault! Is this the correct wallet?", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } catch (e: Throwable) {
                                Toast.makeText(context, "Signing failed: ${e.message}", Toast.LENGTH_LONG).show()
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
                            Text("Sign Message to Decrypt & Sync", fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                                            Toast.makeText(context, "Signature Copied!", Toast.LENGTH_SHORT).show()
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
                                encryptedSecret = encryptedSecret
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

        AlertDialog(
            onDismissRequest = { showManualAddDialog = false },
            title = { Text("Add 2FA Account Manually") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Enter the credential parameter manually. Ideal for testing and non-camera workspace environments.",
                        fontSize = 12.sp,
                        color = Slate400
                    )

                    // Issuer Dropdown / TextField input
                    OutlinedTextField(
                        value = inputIssuer,
                        onValueChange = { inputIssuer = it },
                        label = { Text("Service (e.g. Google, GoUslugi, Binance)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("manual_input_issuer")
                    )

                    OutlinedTextField(
                        value = inputLabel,
                        onValueChange = { inputLabel = it },
                        label = { Text("Account / User Label (e.g. developer@company)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("manual_input_label")
                    )

                    OutlinedTextField(
                        value = inputSecret,
                        onValueChange = { inputSecret = it.uppercase().replace(" ", "") },
                        label = { Text("Base32 Key (e.g. JBSWY3DPEHPK3PXP)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("manual_input_secret"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CyberPurple
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uppercaseSecret = inputSecret.trim().uppercase()
                        if (inputLabel.isBlank() || inputIssuer.isBlank() || uppercaseSecret.isBlank()) {
                            Toast.makeText(context, "Please populate all fields.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        // Validate Base32 character set
                        if (!uppercaseSecret.matches(Regex("^[A-Z2-7]+$"))) {
                            Toast.makeText(context, "Invalid key: Must contain base32 characters only.", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        if (derivedAesKey != null) {
                            try {
                                val encryptedSecret = AesEncryptionUtils.encrypt(uppercaseSecret, derivedAesKey!!)
                                val newAcc = ExternalAccount(
                                    id = UUID.randomUUID().toString(),
                                    label = inputLabel,
                                    issuer = inputIssuer,
                                    encryptedSecret = encryptedSecret
                                )
                                vaultStore.addAccount(newAcc)
                                externalAccounts.clear()
                                externalAccounts.addAll(vaultStore.getAccounts())
                                showManualAddDialog = false
                                Toast.makeText(context, "${inputIssuer} added successfully!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Storage error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "Error: Vault is locked.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
                    modifier = Modifier.testTag("dialog_confirm_button")
                ) {
                    Text("Add Account")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showManualAddDialog = false },
                    modifier = Modifier.testTag("dialog_cancel_button")
                ) {
                    Text("Cancel")
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
                        text = "Align standard 2FA QR code within bounds",
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
                            text = "Camera Permission Required",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "VRAV Auth uses camera feeds fully offline to instantly recognize external TOTP secrets.",
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
                            Text("Grant Permission", fontWeight = FontWeight.Bold)
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
