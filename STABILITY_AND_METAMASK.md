# Stability + MetaMask (applied fixes)

## Already committed on `main`

1. **AndroidManifest** — `allowBackup=false`, `<queries>` for `io.metamask`, deep-link `vrav2fa://callback`
2. **backup_rules / data_extraction_rules** — vault prefs excluded from backup/transfer
3. **VravVaultStore** — `MasterKey`, Keystore retry, same prefs name (no orphan file), `last_wallet_address`, `auth_mode`
4. **MetaMaskHelper.kt** — detect + open MetaMask or Play Store
5. **CryptoKeyDerive.kt** — unified AES key from hex signature (+ optional Yubi)

## Wire into MainActivity.kt (required for UI)

### A) Replace local `deriveFinalKey` with:

```kotlin
fun deriveFinalKey(signature: String, yubiResponse: ByteArray?): ByteArray {
    return CryptoKeyDerive.deriveFinalKey(signature, yubiResponse)
}
```

### B) Inside `Stateless2FAScreen`, after `val vaultStore = remember { ... }`:

```kotlin
val plainSecrets = remember { mutableStateMapOf<String, String>() }
val storedAccountCount = remember { mutableIntStateOf(vaultStore.getStoredAccountCount()) }
val metaMaskInstalled = remember { MetaMaskHelper.isInstalled(context) }

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
```

### C) Replace timer decrypt loop with:

```kotlin
LaunchedEffect(computedSecretKey, externalAccounts.size, isVaultUnlocked, plainSecrets.size) {
    while (true) {
        try {
            val now = System.currentTimeMillis() / 1000
            currentTime = now
            if (computedSecretKey.isNotEmpty() && computedSecretKey != "ERRORGENERATE2FA") {
                val secretBytes = TotpUtil.decodeBase32(computedSecretKey)
                liveOtpCode = TotpUtil.generateTotp(secretBytes, now / 30)
            }
            if (isVaultUnlocked) {
                externalAccounts.forEach { acc ->
                    val plain = plainSecrets[acc.id]
                    if (!plain.isNullOrEmpty()) {
                        val secretBytes = TotpUtil.decodeBase32(plain)
                        externalAccountsOtp[acc.id] =
                            TotpUtil.generateTotp(secretBytes, now / acc.period, acc.digits)
                    }
                }
            }
        } catch (_: Throwable) {}
        delay(1000)
    }
}
```

### D) After successful unlock / init vault (both wallet-only and Yubi paths):

```kotlin
vaultStore.saveAuthMode(if (useYubiKeyForVault) "wallet_yubi" else "wallet_only")
vaultStore.saveLastAddress(loadedAddress)
refreshPlainSecrets(keyBytes /* or finalKey */, vaultStore.getAccounts())
storedAccountCount.intValue = vaultStore.getStoredAccountCount()
```

### E) On lock session:

```kotlin
isVaultUnlocked = false
derivedAesKey = null
plainSecrets.clear()
externalAccounts.clear()
storedAccountCount.intValue = vaultStore.getStoredAccountCount()
```

### F) Banner when locked but data exists (top of Column):

```kotlin
if (!isVaultUnlocked && storedAccountCount.intValue > 0) {
    Card(
        modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
        border = BorderStroke(1.dp, Color(0xFFFFB300))
    ) {
        Text(
            text = "Vault: ${storedAccountCount.intValue} account(s) saved on this device. " +
                "Enter the SAME private key and Sign to unlock. Data is NOT deleted after reboot.",
            modifier = Modifier.padding(16.dp),
            color = Color(0xFF6D4C00),
            fontSize = 13.sp
        )
    }
}
```

### G) MetaMask button (in Card 2, before Sign):

```kotlin
if (metaMaskInstalled) {
    OutlinedButton(
        onClick = {
            MetaMaskHelper.openMetaMask(context)
            Toast.makeText(
                context,
                "MetaMask opened. Offline 2FA still uses the key in the field above (same wallet as MetaMask).",
                Toast.LENGTH_LONG
            ).show()
        },
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("open_metamask_button"),
        border = BorderStroke(1.dp, Color(0xFFE2761B)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Text("Open MetaMask", fontWeight = FontWeight.Bold, color = Color(0xFFE2761B))
    }
}
```

### H) On Generate / Apply wallet success:

```kotlin
vaultStore.saveLastAddress(addr) // or address
```

## After reboot expected behavior

1. Accounts remain in EncryptedSharedPreferences
2. Session is locked (by design — AES key only in RAM)
3. User enters the **same** private key → Sign → codes return
4. Banner shows how many accounts are stored so it does not look like a reset

## MetaMask note

Opening MetaMask works when `io.metamask` is installed. Returning `personal_sign` into the app needs **WalletConnect** (Project ID + network). Until then, use the same wallet key offline in the app field.
