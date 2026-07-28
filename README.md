<div align="center">

# 🛡️ VRAV Stateless 2FA

### Offline Web3 authenticator — no servers, no cloud, no gas

**Encrypt TOTP secrets with your Ethereum wallet signature.**  
Works offline. Survives reboot. Compatible with Google, Binance, Госуслуги and any RFC 6238 service.

[![GitHub release](https://img.shields.io/github/v/release/zametkikostik/VRAV-Stateless-2FA?style=for-the-badge&color=7C3AED)](https://github.com/zametkikostik/VRAV-Stateless-2FA/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/zametkikostik/VRAV-Stateless-2FA/total?style=for-the-badge&color=14B8A6)](https://github.com/zametkikostik/VRAV-Stateless-2FA/releases)
[![Stars](https://img.shields.io/github/stars/zametkikostik/VRAV-Stateless-2FA?style=for-the-badge&color=F59E0B)](https://github.com/zametkikostik/VRAV-Stateless-2FA/stargazers)
[![License: MIT](https://img.shields.io/badge/License-MIT-22C55E?style=for-the-badge)](LICENSE)
[![Platform](https://img.shields.io/badge/Android%207%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/zametkikostik/VRAV-Stateless-2FA/releases/latest)

<br/>

### ⬇️ Download APK

[![Download Debug APK](https://img.shields.io/badge/Download-Debug%20APK-7C3AED?style=for-the-badge&logo=android&logoColor=white)](https://github.com/zametkikostik/VRAV-Stateless-2FA/releases/latest)
[![Download Release APK](https://img.shields.io/badge/Download-Release%20APK-14B8A6?style=for-the-badge&logo=android&logoColor=white)](https://github.com/zametkikostik/VRAV-Stateless-2FA/releases/latest)

**→ [Open latest release & grab the APK](https://github.com/zametkikostik/VRAV-Stateless-2FA/releases/latest)**

*After install: allow “Install unknown apps” for your browser/file manager if Android asks.*

<br/>

⭐ **If this project helps you — [leave a star](https://github.com/zametkikostik/VRAV-Stateless-2FA)** — it helps others find a truly offline 2FA wallet.

</div>

---

## Why VRAV?

Most authenticator apps store secrets in a way you cannot fully control.  
**VRAV** derives the vault key from **your** wallet signature (`personal_sign`). No account. No backend. No subscription.

| | Traditional apps | **VRAV Stateless 2FA** |
|---|---|---|
| Server / cloud | Often yes | **Never** |
| Unlock | PIN / biometrics only | **Wallet signature** (optional YubiKey) |
| Secrets at rest | Vendor-dependent | **AES-256** in `EncryptedSharedPreferences` |
| Network required | Sometimes | **100% offline** |
| Gas / blockchain tx | — | **Zero gas** (local sign only) |

---

## Features

- 🔐 **Wallet-gated vault** — AES-256 key from SHA-256 of a deterministic `personal_sign`
- 📱 **QR scanner** — CameraX + ML Kit, standard `otpauth://totp/` (Google, Binance, banks, Госуслуги…)
- ✍️ **Manual entry** — paste Base32 secrets when QR is not available
- 🔑 **Generate or import** Ethereum (Secp256k1) key — or open **MetaMask** if installed
- 💳 **Hardware option** — YubiKey HMAC challenge-response (NFC / emulate for tests)
- 💾 **Survives reboot** — encrypted accounts stay on device; session unlocks after the same key + Sign
- 📦 **Encrypted backup** — export / import vault JSON (still encrypted)
- 🌍 **EN · RU · BG** UI languages
- 🎨 **Material 3** dark cyberpunk UI with live TOTP countdown

---

## How it works

```text
  Private key (local)  ──►  personal_sign("Sign this message…")
                                    │
                                    ▼
                           SHA-256 → AES-256 key
                                    │
         QR / manual secret  ──►  encrypt  ──►  EncryptedSharedPreferences
                                    │
                              unlock vault
                                    │
                                    ▼
                         live 6-digit TOTP (RFC 6238)
```

1. **Load wallet** — generate new key or paste an existing hex private key  
2. **Sign** — one message unlocks (or creates) the vault  
3. **Add accounts** — scan QR or enter secret  
4. **Use codes** — same as Google Authenticator  
5. **Lock** — wipes the key from RAM; data stays encrypted on disk  

> After phone reboot the vault is still there. Enter the **same** private key and Sign again — codes return. Nothing is “reset”.

---

## Quick start

1. Download **VRAV-2FA-Debug.apk** (or Release) from [Releases](https://github.com/zametkikostik/VRAV-Stateless-2FA/releases/latest)
2. Install on Android 7.0+
3. Generate or import a wallet key → **Sign & Decrypt**
4. Scan a 2FA QR from any service
5. Optional: export encrypted backup to a safe place

### Build from source

```bash
git clone https://github.com/zametkikostik/VRAV-Stateless-2FA.git
cd VRAV-Stateless-2FA

# JDK 17+, Android SDK
gradle assembleDebug
# APK: app/build/outputs/apk/debug/
```

CI builds every push to `main` and publishes APKs under [Releases](https://github.com/zametkikostik/VRAV-Stateless-2FA/releases).

---

## Security notes

- Private key is **not** uploaded anywhere; keep offline backups yourself  
- AES session key lives **only in RAM** until Lock / process kill  
- Vault prefs are excluded from Android Auto Backup (Keystore keys do not restore cleanly)  
- Prefer a dedicated wallet for 2FA, not your main cold-storage key  
- Full MetaMask `personal_sign` round-trip needs WalletConnect (optional future); today you can open MetaMask and still sign offline with the same key in-app  

---

## Stack

Kotlin · Jetpack Compose · CameraX · ML Kit · Web3j · Security Crypto · Material 3

---

## Contributing & stars

Issues and PRs are welcome.

If you like the idea of **serverless, wallet-bound 2FA**:

### ⭐ [Star this repo](https://github.com/zametkikostik/VRAV-Stateless-2FA/stargazers)

It costs one click and makes the project visible to people who need offline security.

---

## 🇷🇺 Кратко по-русски

**VRAV Stateless 2FA** — офлайн-аутентификатор без серверов и без газа.

- Секреты 2FA шифруются **AES-256**; ключ получается из подписи Ethereum-кошелька  
- QR и ручной ввод (Google, Binance, Госуслуги и др.)  
- Данные **не пропадают** после выключения телефона — снова Sign тем же ключом  
- Языки: **EN / RU / BG**  

**Скачать APK:** [последний релиз](https://github.com/zametkikostik/VRAV-Stateless-2FA/releases/latest)  
**Поддержать проект:** поставь ⭐ на [странице репозитория](https://github.com/zametkikostik/VRAV-Stateless-2FA)

---

## License

[MIT](LICENSE) — use freely, keep users’ keys on their devices.

<div align="center">

**No cloud. No gas. Your wallet, your vault.**

[Download APK](https://github.com/zametkikostik/VRAV-Stateless-2FA/releases/latest) · [Star ⭐](https://github.com/zametkikostik/VRAV-Stateless-2FA) · [Issues](https://github.com/zametkikostik/VRAV-Stateless-2FA/issues)

</div>
