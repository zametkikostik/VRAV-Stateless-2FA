# VRAV Stateless 2FA 🛡️

**VRAV Stateless 2FA** is a highly secure, offline-first, database-free, serverless hybrid 2FA authenticator designed to bridge modern Web3 ECDSA signatures with standard TOTP Google Authenticator algorithms. 

By combining standard QR scanning, cryptographic local storage, and Ethereum wallet controller signatures, VRAV Stateless 2FA keeps user credentials 100% private, local, and immune to server-side attacks or database leaks.

---

## 🚀 Key Features

- **Secured Offline Storage**: When scanning standard Google Authenticator/TOTP QR codes, the secret is immediately encrypted using an **AES-256** key.
- **Web3-Seeded Security Layer**: The AES-256 encryption key is derived directly from the SHA-256 hash of a deterministic Ethereum `personal_sign` message. Your wallet's private key controls access to the decryption key.
- **100% Serverless & Private**: All tokens, keys, and decrypted values live strictly in memory and encrypted local storage on your Android device (utilizing Android Jetpack's `EncryptedSharedPreferences`).
- **QR Code Scanner**: Built-in high-performance scanner using **CameraX** and **Google ML Kit Barcode Scanning** to seamlessly parse standard `otpauth://` URIs.
- **Smart Brand Recognition**: Automatic branding, styling, and indicator chips for popular services like **Google**, **Binance**, and **GoUslugi (Госуслуги)**.
- **Stunning Cyberpunk Design**: Deep slate visual themes, custom live countdown animations, and fluid Material 3 compositions.

---

## 🛠️ Architecture Journey

```
┌────────────────────────┐       ┌────────────────────────┐
│  Polygon Private Key   │ ───► │ Deterministic Signature │
│  (Secp256k1 Wallet)    │       │     (personal_sign)    │
└────────────────────────┘       └────────────────────────┘
                                              │
                                              ▼
┌────────────────────────┐       ┌────────────────────────┐
│   ML Kit QR Scanner    │       │  AES-256 Key Derivation│
│  (CameraX otpauth Uri) │       │       (SHA-256)        │
└────────────────────────┘       └────────────────────────┘
            │                                 │
            └───────────────┬─────────────────┘
                            ▼
           ┌──────────────────────────────────┐
           │ Android EncryptedSharedPreferences│
           │         (Encrypted Vault)        │
           └──────────────────────────────────┘
```

1. **Identity & Entry**: Upon launching, the user loads or generates a Secp256k1 private key.
2. **Deterministic Cryptographic Key**: The user signs a dedicated system message via the MetaMask-like layer. This signature is hashed to generate a unique 256-bit AES key.
3. **Vault Encryption**: External secrets are encrypted with the AES key and stored locally.
4. **Decryption and Playback**: When the app starts, signing the message unlocks the vault and displays live running 6-digit tokens. When locked, the memory is completely flushed.

---

## 🔧 Installation & Build Instructions

### Prerequisites
- Android Studio Ladybug or newer
- JDK 17
- Android SDK 24+ (Android 7.0 and up)

### Compilation from Source
Clone this repository and verify or build via Gradle in your workspace terminal:

```bash
# Verify unit and local cryptographic signature tests
gradle :app:testDebugUnitTest

# Assemble the debug APK
gradle assembleDebug
```

---

## 🧪 Security & Local Verification
We preserve robust unit testing standards. To verify the cryptographic personal signature and verification algorithm:
```bash
gradle :app:testDebugUnitTest
```
All standard tests are fully integrated and run locally inside the JVM sandbox without requiring an emulator.

---

## 📄 License & Privacy
- **No external server calls** — zero trackers, zero cloud connections.
- Licensed under the MIT License. Feel free to use and distribute safely.
