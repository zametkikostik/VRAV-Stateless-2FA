package com.example

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import java.util.UUID

fun deriveFinalKey(signature: String, yubiResponse: ByteArray?): ByteArray =
    CryptoKeyDerive.deriveFinalKey(signature, yubiResponse)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Stateless2FAScreen() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    var privateKeyInput by remember { mutableStateOf("") }
    var loadedAddress by remember { mutableStateOf("") }
    var walletPrivateKey by remember { mutableStateOf("") }
    val signatureMessage = "Sign this message to securely generate your VRAV 2FA Secret Key"

    var generatedSignature by remember { mutableStateOf("") }
    var computedSecretKey by remember { mutableStateOf("") }
    var otpauthUri by remember { mutableStateOf("") }
    var enteredVerifyToken by remember { mutableStateOf("") }
    var verificationStatus by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    var liveOtpCode by remember { mutableStateOf("000000") }

    var isVaultUnlocked by remember { mutableStateOf(false) }
    var derivedAesKey by remember { mutableStateOf<ByteArray?>(null) }
    val vaultStore = remember { VravVaultStore(context) }
    val externalAccounts = remember { mutableStateListOf<ExternalAccount>() }
    var showDecryptionErrorDialog by remember { mutableStateOf(false) }

    val plainSecrets = remember { mutableStateMapOf<String, String>() }
    var storedAccountCount by remember { mutableIntStateOf(vaultStore.getStoredAccountCount()) }
    val metaMaskInstalled = remember { MetaMaskHelper.isInstalled(context) }
    val lastSavedAddress = remember { vaultStore.getLastAddress() }

    var useYubiKeyForVault by remember { mutableStateOf(false) }
    var showYubiKeyTappingOverlay by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    var showManualAddDialog by remember { mutableStateOf(false) }
    val externalAccountsOtp = remember { mutableStateMapOf<String, String>() }

    fun refreshPlainSecrets(key: ByteArray, accounts: List<ExternalAccount>) {
        plainSecrets.clear()
        accounts.forEach { acc ->
            try {
                plainSecrets[acc.id] = AesEncryptionUtils.decrypt(acc.encryptedSecret, key)
            } catch (_: Exception) {
                plainSecrets[acc.id] = ""
            }
        }
    }

    fun unlockVault(keyBytes: ByteArray, yubi: Boolean) {
        derivedAesKey = keyBytes
        isVaultUnlocked = true
        val accounts = vaultStore.getAccounts()
        externalAccounts.clear()
        externalAccounts.addAll(accounts)
        refreshPlainSecrets(keyBytes, accounts)
        storedAccountCount = vaultStore.getStoredAccountCount()
        if (loadedAddress.isNotEmpty()) vaultStore.saveLastAddress(loadedAddress)
        vaultStore.saveAuthMode(if (yubi) "wallet_yubi" else "wallet_only")
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { s -> s.write(vaultStore.exportEncryptedVault().toByteArray()) }
                Toast.makeText(context, Localization.t("toast_export_success"), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "${Localization.t("toast_export_failed")}: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val json = stream.bufferedReader().readText()
                    if (vaultStore.importEncryptedVault(json)) {
                        externalAccounts.clear()
                        externalAccounts.addAll(vaultStore.getAccounts())
                        storedAccountCount = vaultStore.getStoredAccountCount()
                        derivedAesKey?.let { k -> refreshPlainSecrets(k, externalAccounts.toList()) }
                        Toast.makeText(context, Localization.t("toast_import_success"), Toast.LENGTH_SHORT).show()
                    } else Toast.makeText(context, Localization.t("toast_invalid_format"), Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "${Localization.t("toast_import_failed")}: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(computedSecretKey, externalAccounts.size, isVaultUnlocked, plainSecrets.size) {
        while (true) {
            try {
                val now = System.currentTimeMillis() / 1000
                currentTime = now
                if (computedSecretKey.isNotEmpty() && computedSecretKey != "ERRORGENERATE2FA") {
                    liveOtpCode = TotpUtil.generateTotp(TotpUtil.decodeBase32(computedSecretKey), now / 30)
                }
                if (isVaultUnlocked) {
                    externalAccounts.forEach { acc ->
                        val plain = plainSecrets[acc.id]
                        externalAccountsOtp[acc.id] = if (!plain.isNullOrEmpty()) {
                            try {
                                TotpUtil.generateTotp(TotpUtil.decodeBase32(plain), now / acc.period, acc.digits)
                            } catch (_: Exception) { "ERRDEC" }
                        } else "ERRDEC"
                    }
                }
            } catch (_: Throwable) {}
            delay(1000)
        }
    }

    val remainingSeconds = 30 - (currentTime % 30)

    Column(
        Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Stateless, zero-gas, database-free 2FA bridging Web3 signatures with TOTP.",
            color = Slate400, fontSize = 13.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp).widthIn(max = 500.dp)
        )

        if (!isVaultUnlocked && storedAccountCount > 0) {
            Card(
                Modifier.fillMaxWidth().widthIn(max = 600.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                border = BorderStroke(1.dp, Color(0xFFFFB300))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Vault locked — $storedAccountCount account(s) still saved", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF6D4C00))
                    Text(
                        "Data survives reboot. Enter the SAME private key and Sign to unlock." +
                            if (lastSavedAddress.isNotEmpty()) " Last: ${lastSavedAddress.take(6)}…${lastSavedAddress.takeLast(4)}" else "",
                        fontSize = 12.sp, color = Color(0xFF8D6E00)
                    )
                }
            }
        }

        if (isVaultUnlocked) {
            Card(
                Modifier.fillMaxWidth().widthIn(max = 600.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1FDF4)),
                border = BorderStroke(1.5.dp, CyberEmerald)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("🔒 " + Localization.t("decrypted"), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F5132))
                        Text("${remainingSeconds}s", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F5132))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { showQrScanner = true }, Modifier.weight(1f).height(46.dp).testTag("scan_qr_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald), shape = RoundedCornerShape(23.dp)) {
                            Text(Localization.t("btn_scan_qr"), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(onClick = { showManualAddDialog = true }, Modifier.weight(1f).height(46.dp).testTag("add_manually_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberTeal), shape = RoundedCornerShape(23.dp)) {
                            Text(Localization.t("btn_add_manually"), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    if (externalAccounts.isEmpty()) {
                        Text(Localization.t("no_accounts_loaded"), fontSize = 13.sp, color = Slate300)
                    } else {
                        externalAccounts.forEach { acc ->
                            val code = externalAccountsOtp[acc.id] ?: "000000"
                            val brandColor = when (acc.issuer.lowercase().trim()) {
                                "google" -> Color(0xFF4285F4)
                                "binance" -> Color(0xFFF0B90B)
                                "gouslugi", "gosuslugi", "госуслуги" -> Color(0xFFD32F2F)
                                else -> CyberPurple
                            }
                            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, brandColor.copy(alpha = 0.3f))) {
                                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("${acc.issuer.uppercase()} · ${acc.label}", fontSize = 11.sp, color = Slate300, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(code.chunked(3).joinToString(" "), fontSize = 24.sp, fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Black, color = brandColor,
                                            modifier = Modifier.clickable {
                                                if (code != "000000" && code != "ERRDEC") {
                                                    clipboardManager.setText(AnnotatedString(code))
                                                    Toast.makeText(context, "$code " + Localization.t("toast_copied"), Toast.LENGTH_SHORT).show()
                                                }
                                            }.testTag("external_token_${acc.id}"))
                                    }
                                    IconButton(onClick = {
                                        vaultStore.deleteAccount(acc.id)
                                        plainSecrets.remove(acc.id)
                                        externalAccounts.clear()
                                        externalAccounts.addAll(vaultStore.getAccounts())
                                        storedAccountCount = vaultStore.getStoredAccountCount()
                                    }) { Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.7f)) }
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            isVaultUnlocked = false; derivedAesKey = null; plainSecrets.clear()
                            externalAccounts.clear(); storedAccountCount = vaultStore.getStoredAccountCount()
                            Toast.makeText(context, Localization.t("toast_session_locked"), Toast.LENGTH_SHORT).show()
                        }, Modifier.weight(1f).height(40.dp).testTag("lock_session_button"), shape = RoundedCornerShape(20.dp)) {
                            Text(Localization.t("lock_session"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(onClick = {
                            vaultStore.clearVault(); isVaultUnlocked = false; derivedAesKey = null
                            plainSecrets.clear(); externalAccounts.clear(); storedAccountCount = 0
                            Toast.makeText(context, Localization.t("toast_vault_wiped"), Toast.LENGTH_SHORT).show()
                        }, Modifier.weight(1f).height(40.dp).testTag("purge_vault_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)), shape = RoundedCornerShape(20.dp)) {
                            Text(Localization.t("purge_vault"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { exportLauncher.launch("vrav_secure_2fa_vault.json") },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberTeal), shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f).height(40.dp).testTag("export_vault_button")) {
                            Text(Localization.t("export_backup"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberPurple), shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f).height(40.dp).testTag("import_vault_button")) {
                            Text(Localization.t("import_backup"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth().widthIn(max = 600.dp), shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard), border = BorderStroke(1.dp, Color(0xFFEADDFF))) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(Localization.t("card1_title"), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate100)
                Text(Localization.t("card1_desc"), fontSize = 12.sp, color = Slate300)
                OutlinedTextField(
                    value = privateKeyInput, onValueChange = { privateKeyInput = it },
                    label = { Text(Localization.t("private_key_label")) },
                    placeholder = { Text(Localization.t("private_key_placeholder")) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, color = Slate100, fontSize = 13.sp),
                    modifier = Modifier.fillMaxWidth().testTag("private_key_input"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberPurple, unfocusedBorderColor = Color(0xFFE7E0EC), focusedLabelColor = CyberPurple, unfocusedLabelColor = Slate300)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        try {
                            val (priv, addr) = EthereumCryptoUtils.generateWallet()
                            privateKeyInput = priv; loadedAddress = addr; walletPrivateKey = priv
                            vaultStore.saveLastAddress(addr)
                            Toast.makeText(context, Localization.t("toast_new_key"), Toast.LENGTH_SHORT).show()
                        } catch (e: Throwable) {
                            Toast.makeText(context, "${Localization.t("toast_wallet_error")}: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }, Modifier.weight(1f).height(48.dp).testTag("generate_key_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPurple), shape = RoundedCornerShape(24.dp)) {
                        Text(Localization.t("btn_new_wallet"), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    Button(onClick = {
                        if (privateKeyInput.isBlank()) {
                            Toast.makeText(context, Localization.t("toast_key_valid"), Toast.LENGTH_SHORT).show(); return@Button
                        }
                        try {
                            val address = EthereumCryptoUtils.loadWallet(privateKeyInput)
                            loadedAddress = address; walletPrivateKey = privateKeyInput
                            vaultStore.saveLastAddress(address)
                            Toast.makeText(context, Localization.t("toast_wallet_loaded"), Toast.LENGTH_SHORT).show()
                        } catch (_: Throwable) {
                            Toast.makeText(context, Localization.t("toast_invalid_key_hex"), Toast.LENGTH_LONG).show()
                        }
                    }, Modifier.weight(1f).height(48.dp).testTag("import_key_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberTeal), shape = RoundedCornerShape(24.dp)) {
                        Text(Localization.t("btn_apply_key"), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
                if (loadedAddress.isNotEmpty()) {
                    Text(loadedAddress, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Slate100, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        if (loadedAddress.isNotEmpty()) {
            Card(Modifier.fillMaxWidth().widthIn(max = 600.dp), shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard), border = BorderStroke(1.dp, Color(0xFFEADDFF))) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(Localization.t("card2_title"), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate100)
                    Text(Localization.t("card2_desc"), fontSize = 12.sp, color = Slate300)
                    Text("\"$signatureMessage\"", fontSize = 12.sp, color = Slate100, fontFamily = FontFamily.Monospace)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(Localization.t("hw_yubikey_2fa"), fontSize = 13.sp, color = Slate100)
                        Switch(checked = useYubiKeyForVault, onCheckedChange = { useYubiKeyForVault = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberTeal), modifier = Modifier.testTag("yubikey_toggle"))
                    }
                    if (metaMaskInstalled) {
                        OutlinedButton(
                            onClick = {
                                MetaMaskHelper.openMetaMask(context)
                                Toast.makeText(context, "MetaMask opened. Use the same wallet key above, then Sign.", Toast.LENGTH_LONG).show()
                            },
                            Modifier.fillMaxWidth().height(48.dp).testTag("open_metamask_button"),
                            border = BorderStroke(1.5.dp, Color(0xFFE2761B)), shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Open MetaMask", fontWeight = FontWeight.Bold, color = Color(0xFFE2761B))
                        }
                    }
                    Button(onClick = {
                        if (walletPrivateKey.isBlank()) {
                            Toast.makeText(context, Localization.t("toast_configure_first"), Toast.LENGTH_SHORT).show(); return@Button
                        }
                        try {
                            if (useYubiKeyForVault) {
                                showYubiKeyTappingOverlay = true
                            } else {
                                val sig = EthereumCryptoUtils.personalSign(signatureMessage, walletPrivateKey)
                                generatedSignature = sig
                                computedSecretKey = TotpUtil.generateSecretKeyFromSignature(sig)
                                otpauthUri = TotpUtil.getTOTPUri(loadedAddress, computedSecretKey)
                                val keyBytes = CryptoKeyDerive.deriveFinalKey(sig, null)
                                if (!vaultStore.hasVaultMarker()) {
                                    vaultStore.initializeVaultMarker(keyBytes)
                                    unlockVault(keyBytes, false)
                                    Toast.makeText(context, Localization.t("toast_new_vault_init"), Toast.LENGTH_SHORT).show()
                                } else if (vaultStore.verifyVaultMarker(keyBytes)) {
                                    unlockVault(keyBytes, false)
                                    Toast.makeText(context, Localization.t("toast_vault_decrypted"), Toast.LENGTH_SHORT).show()
                                } else {
                                    showDecryptionErrorDialog = true
                                    Toast.makeText(context, Localization.t("toast_decryption_error"), Toast.LENGTH_LONG).show()
                                }
                            }
                        } catch (e: Throwable) {
                            Toast.makeText(context, "${Localization.t("toast_signing_failed")}: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }, Modifier.fillMaxWidth().height(56.dp).testTag("sign_message_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPurple), shape = RoundedCornerShape(28.dp)) {
                        Text(Localization.t("btn_sign_decrypt"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    if (computedSecretKey.isNotEmpty() && computedSecretKey != "ERRORGENERATE2FA") {
                        Text(liveOtpCode.chunked(3).joinToString(" "), fontSize = 28.sp, fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black, color = CyberPurple)
                        Text("Resets in ${remainingSeconds}s", fontSize = 11.sp, color = Slate300)
                    }
                }
            }
        }
    }

    if (showQrScanner) {
        CameraScannerOverlay(onDismiss = { showQrScanner = false }, onQrCodeScanned = { raw ->
            showQrScanner = false
            val parsed = parseOtpAuthUri(raw)
            if (parsed != null && derivedAesKey != null) {
                try {
                    val enc = AesEncryptionUtils.encrypt(parsed.secret, derivedAesKey!!)
                    val newAcc = ExternalAccount(UUID.randomUUID().toString(), parsed.label, parsed.issuer, enc, parsed.digits, parsed.period)
                    vaultStore.addAccount(newAcc)
                    plainSecrets[newAcc.id] = parsed.secret
                    externalAccounts.clear(); externalAccounts.addAll(vaultStore.getAccounts())
                    storedAccountCount = vaultStore.getStoredAccountCount()
                    Toast.makeText(context, "${parsed.issuer} saved!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Encryption error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    if (showManualAddDialog) {
        var inputLabel by remember { mutableStateOf("") }
        var inputIssuer by remember { mutableStateOf("Google") }
        var inputSecret by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showManualAddDialog = false },
            title = { Text(Localization.t("dialog_add_title")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(inputIssuer, { inputIssuer = it }, label = { Text(Localization.t("dialog_service_label")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(inputLabel, { inputLabel = it }, label = { Text(Localization.t("dialog_account_label")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(inputSecret, { inputSecret = it.uppercase().replace(" ", "") }, label = { Text(Localization.t("dialog_secret_label")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val sec = inputSecret.trim().uppercase()
                    if (inputLabel.isBlank() || inputIssuer.isBlank() || sec.isBlank() || derivedAesKey == null) return@Button
                    if (!sec.matches(Regex("^[A-Z2-7]+$"))) return@Button
                    try {
                        val enc = AesEncryptionUtils.encrypt(sec, derivedAesKey!!)
                        val newAcc = ExternalAccount(UUID.randomUUID().toString(), inputLabel, inputIssuer, enc)
                        vaultStore.addAccount(newAcc)
                        plainSecrets[newAcc.id] = sec
                        externalAccounts.clear(); externalAccounts.addAll(vaultStore.getAccounts())
                        storedAccountCount = vaultStore.getStoredAccountCount()
                        showManualAddDialog = false
                    } catch (_: Exception) {}
                }) { Text(Localization.t("btn_dialog_add")) }
            },
            dismissButton = { TextButton(onClick = { showManualAddDialog = false }) { Text(Localization.t("btn_dialog_cancel")) } }
        )
    }

    if (showYubiKeyTappingOverlay) {
        val sysActivity = context as? Activity
        val authenticatorCallback = remember {
            object : YubiKeyCallback {
                override fun onReady() {}
                override fun onScanning() {}
                override fun onSuccess(response: ByteArray) {
                    showYubiKeyTappingOverlay = false
                    try {
                        val sig = EthereumCryptoUtils.personalSign(signatureMessage, walletPrivateKey)
                        generatedSignature = sig
                        computedSecretKey = TotpUtil.generateSecretKeyFromSignature(sig)
                        otpauthUri = TotpUtil.getTOTPUri(loadedAddress, computedSecretKey)
                        val finalKey = CryptoKeyDerive.deriveFinalKey(sig, response)
                        if (!vaultStore.hasVaultMarker()) {
                            vaultStore.initializeVaultMarker(finalKey)
                            unlockVault(finalKey, true)
                        } else if (vaultStore.verifyVaultMarker(finalKey)) {
                            unlockVault(finalKey, true)
                        } else showDecryptionErrorDialog = true
                    } catch (e: Exception) {
                        Toast.makeText(context, "Hardware Auth Failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
                override fun onFailure(error: String) {
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                }
            }
        }
        val authenticator = remember { YubiKeyHmacAuthenticator(context, authenticatorCallback) }
        LaunchedEffect(Unit) {
            sysActivity?.let {
                authenticator.setChallenge(signatureMessage.toByteArray())
                authenticator.startNfcListening(it)
            }
        }
        DisposableEffect(Unit) { onDispose { sysActivity?.let { authenticator.stopNfcListening(it) } } }
        AlertDialog(
            onDismissRequest = { showYubiKeyTappingOverlay = false },
            title = { Text("Hardware YubiKey 2FA") },
            text = {
                Column {
                    Text("Tap YubiKey or simulate:", fontSize = 12.sp, color = Slate300)
                    Button(onClick = {
                        authenticatorCallback.onSuccess(
                            authenticator.emulateYubiKeyTap(walletPrivateKey.toByteArray(), signatureMessage.toByteArray())
                        )
                    }, modifier = Modifier.testTag("emulate_yubikey_tap")) {
                        Text("Simulate YubiKey Tap")
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showYubiKeyTappingOverlay = false }) { Text("Cancel") } }
        )
    }

    if (showDecryptionErrorDialog) {
        AlertDialog(
            onDismissRequest = { showDecryptionErrorDialog = false },
            title = { Text(Localization.t("conflict_title")) },
            text = { Text(Localization.t("conflict_desc"), fontSize = 12.sp) },
            confirmButton = {
                Column {
                    Button(onClick = { useYubiKeyForVault = !useYubiKeyForVault; showDecryptionErrorDialog = false }) {
                        Text(Localization.t("btn_conflict_toggle_yubi"))
                    }
                    Button(onClick = {
                        vaultStore.clearVault(); isVaultUnlocked = false; derivedAesKey = null
                        plainSecrets.clear(); externalAccounts.clear(); storedAccountCount = 0
                        showDecryptionErrorDialog = false
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                        Text(Localization.t("btn_conflict_reset"))
                    }
                }
            }
        )
    }
}
