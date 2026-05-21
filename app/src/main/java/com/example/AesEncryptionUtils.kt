package com.example

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object AesEncryptionUtils {
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val ALGORITHM = "AES"

    fun deriveAesKey(signatureHex: String): ByteArray {
        val cleanHex = signatureHex.trim().replace("0x", "")
        val signatureBytes = org.web3j.utils.Numeric.hexStringToByteArray(cleanHex)
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(signatureBytes)
    }

    fun encrypt(plainText: String, keyBytes: ByteArray): String {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val secretKey = SecretKeySpec(keyBytes, ALGORITHM)
            val iv = ByteArray(16)
            SecureRandom().nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            throw RuntimeException("Encryption failed: ${e.message}", e)
        }
    }

    fun decrypt(encryptedBase64: String, keyBytes: ByteArray): String {
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            val iv = ByteArray(16)
            System.arraycopy(combined, 0, iv, 0, iv.size)
            val encryptedSize = combined.size - iv.size
            val encryptedBytes = ByteArray(encryptedSize)
            System.arraycopy(combined, iv.size, encryptedBytes, 0, encryptedSize)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val secretKey = SecretKeySpec(keyBytes, ALGORITHM)
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            throw RuntimeException("Decryption failed. Invalid signature or corrupt vault data.", e)
        }
    }
}
