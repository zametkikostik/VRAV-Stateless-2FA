package com.example

import org.web3j.utils.Numeric
import java.security.MessageDigest

/**
 * Unified AES key derivation so wallet-only and wallet+YubiKey paths
 * are consistent (always hash raw signature bytes, not UTF-8 of hex string).
 */
object CryptoKeyDerive {

    fun deriveFinalKey(signatureHex: String, yubiResponse: ByteArray? = null): ByteArray {
        return try {
            val clean = signatureHex.trim().removePrefix("0x").removePrefix("0X")
            val sigBytes = Numeric.hexStringToByteArray(clean)
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(sigBytes)
            if (yubiResponse != null && yubiResponse.isNotEmpty()) {
                digest.update(yubiResponse)
            }
            digest.digest()
        } catch (e: Exception) {
            // Fallback identical to legacy AesEncryptionUtils.deriveAesKey
            AesEncryptionUtils.deriveAesKey(signatureHex)
        }
    }
}
