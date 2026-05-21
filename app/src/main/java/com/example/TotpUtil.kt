package com.example

import org.web3j.utils.Numeric
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and

object TotpUtil {
    private const val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun decodeBase32(base32: String): ByteArray {
        val uppercase = base32.uppercase().replace("-", "").replace(" ", "")
        val out = ByteArray((uppercase.length * 5 / 8))
        var buffer = 0
        var bitsLeft = 0
        var count = 0
        for (char in uppercase) {
            val idx = BASE32_ALPHABET.indexOf(char)
            if (idx < 0) continue // Skip invalid chars or padding
            buffer = (buffer shl 5) or idx
            bitsLeft += 5
            if (bitsLeft >= 8) {
                out[count++] = (buffer ushr (bitsLeft - 8)).toByte()
                bitsLeft -= 8
            }
        }
        return out.copyOf(count)
    }

    fun encodeBase32(bytes: ByteArray): String {
        val sb = StringBuilder()
        var buffer = 0
        var bitsLeft = 0
        for (b in bytes) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                val idx = (buffer ushr (bitsLeft - 5)) and 0x1F
                sb.append(BASE32_ALPHABET[idx])
                bitsLeft -= 5
            }
        }
        if (bitsLeft > 0) {
            val idx = (buffer shl (5 - bitsLeft)) and 0x1F
            sb.append(BASE32_ALPHABET[idx])
        }
        return sb.toString()
    }

    // Hashes the signature using SHA-256, pulls the first 10 bytes (80 bits), and Base32-encodes it into 16 chars
    fun generateSecretKeyFromSignature(signatureHex: String): String {
        return try {
            val signatureBytes = Numeric.hexStringToByteArray(signatureHex)
            val md = MessageDigest.getInstance("SHA-256")
            val hash = md.digest(signatureBytes)
            val first10Bytes = hash.copyOf(10)
            encodeBase32(first10Bytes)
        } catch (e: Exception) {
            "ERRORGENERATE2FA"
        }
    }

    // Generates the otpauth URI standard URL for visual copy/setup
    fun getTOTPUri(address: String, secret: String): String {
        // Standard format: otpauth://totp/VRAV:0xAddress?secret=SECRET&issuer=VRAV
        val cleanAddress = if (address.length > 10) "${address.take(6)}...${address.takeLast(4)}" else address
        return "otpauth://totp/VRAV:$cleanAddress?secret=$secret&issuer=VRAV"
    }

    // Generates a 6-digit TOTP code for a given timestamp
    fun generateTotp(secretBytes: ByteArray, timeWindow: Long, digits: Int = 6): String {
        return try {
            val counterBytes = ByteArray(8)
            var temp = timeWindow
            for (i in 7 downTo 0) {
                counterBytes[i] = (temp and 0xFF).toByte()
                temp = temp ushr 8
            }

            val mac = Mac.getInstance("HmacSHA1")
            val keySpec = SecretKeySpec(secretBytes, "RAW")
            mac.init(keySpec)
            val hash = mac.doFinal(counterBytes)

            val offset = hash[hash.size - 1].toInt() and 0x0F
            val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                         ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                         ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                         (hash[offset + 3].toInt() and 0xFF)

            val otp = binary % Math.pow(10.0, digits.toDouble()).toInt()
            String.format("%0${digits}d", otp)
        } catch (e: Exception) {
            "000000"
        }
    }

    // Verifies if a user-supplied 6-digit code is valid for current, previous or next time window
    fun verifyTotp(secret: String, code: String, timeWindow: Long): Boolean {
        if (code.length != 6) return false
        val secretBytes = decodeBase32(secret)
        
        // Match with standard tolerance of +/- 1 time window (30 seconds each) to protect against tiny device clock drifts
        for (i in -1..1) {
            val generated = generateTotp(secretBytes, timeWindow + i)
            if (generated == code.trim()) {
                return true
            }
        }
        return false
    }
}
