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
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
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
                                            "STATELESS 2FA",
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

    // 2. Auth Flow States
    var generatedSignature by remember { mutableStateOf("") }
    var computedSecretKey by remember { mutableStateOf("") }
    var otpauthUri by remember { mutableStateOf("") }

    // 3. User verification states
    var enteredVerifyToken by remember { mutableStateOf("") }
    var verificationStatus by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    // 4. Countdown states for live authenticating preview
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    var liveOtpCode by remember { mutableStateOf("000000") }

    // Background ticking timer
    LaunchedEffect(key1 = computedSecretKey) {
        while (true) {
            try {
                val now = System.currentTimeMillis() / 1000
                currentTime = now
                
                if (computedSecretKey.isNotEmpty() && computedSecretKey != "ERRORGENERATE2FA") {
                    val secretBytes = TotpUtil.decodeBase32(computedSecretKey)
                    val currentWindow = now / 30
                    liveOtpCode = TotpUtil.generateTotp(secretBytes, currentWindow)
                }
            } catch (t: Throwable) {
                // Prevent crash in background ticking thread
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
        // App intro
        Text(
            text = "Stateless, zero-gas, database-free 2FA bridging Web3 Elliptic Curve signatures with standard TOTP Google Authenticator algorithm.",
            color = Slate400,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .widthIn(max = 500.dp)
        )

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

        // CARD 2: Cryptographic Signature Process
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

                    Text(
                        text = "Sign the fixed, deterministic system string to verify ownership of your private key and seed your stateless secret.",
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
                                
                                // Compute secret generator statelessly on-the-fly
                                val secret = TotpUtil.generateSecretKeyFromSignature(sig)
                                computedSecretKey = secret
                                otpauthUri = TotpUtil.getTOTPUri(loadedAddress, secret)
                                
                                Toast.makeText(context, "Message Signed Successfully!", Toast.LENGTH_SHORT).show()
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
                            Text("Sign Deterministic Message", fontWeight = FontWeight.Bold, fontSize = 15.sp)
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

        // CARD 3: Stateless TOTP Google Authenticator Setup URI & Secret Output
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
                                text = "3. TOTP SECRET GENERATION",
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
                        text = "This 16-character Base32 secret key was generated purely by hashing your cryptographic signature. Anyone can reproduce it statelessly on-the-fly with the same signature.",
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
                                text = "YOUR 16-CHAR BASE32 SECRET",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = CyberPurple,
                                letterSpacing = 1.sp
                            )
                            
                            // Splitting the secret for readability (XXXX-XXXX-XXXX-XXXX)
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
                                text = "ENTER TOTP CODE",
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

        // CARD 4: Verification Endpoint Testing (Stateless Backend)
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
                                // Simulate stateless API: /api/2fa/verify
                                // We verify:
                                // 1. That the Signature recovers to the designated loadedAddress
                                val isSignatureValid = EthereumCryptoUtils.verifyPersonalSignature(
                                    signatureMessage,
                                    generatedSignature,
                                    loadedAddress
                                )
                                
                                if (!isSignatureValid) {
                                    verificationStatus = Pair(false, "API Failure: Signature validation failed.")
                                    return@Button
                                }

                                // 2. Hash signature to secret key and check TOTP verification
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
}
