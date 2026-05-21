package com.example

import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Arrays

object EthereumCryptoUtils {

    // Helper to generate a new ECKeyPair
    fun generateWallet(): Pair<String, String> {
        return try {
            val ecKeyPair = Keys.createEcKeyPair()
            val privateKeyHex = Numeric.toHexStringNoPrefixZeroPadded(ecKeyPair.privateKey, 64)
            val address = Keys.toChecksumAddress(Keys.getAddress(ecKeyPair))
            Pair("0x$privateKeyHex", address)
        } catch (e: Throwable) {
            throw RuntimeException("EC key pair generation failed: ${e.message}", e)
        }
    }

    // Helper to load wallet from private key
    fun loadWallet(privateKeyHex: String): String {
        return try {
            val cleanKey = privateKeyHex.trim().replace("0x", "")
            val ecKeyPair = ECKeyPair.create(Numeric.toBigInt(cleanKey))
            Keys.toChecksumAddress(Keys.getAddress(ecKeyPair))
        } catch (e: Throwable) {
            throw IllegalArgumentException("Invalid Private key Hex: ${e.message}", e)
        }
    }

    // personal_sign equivalent
    fun personalSign(message: String, privateKeyHex: String): String {
        return try {
            val cleanKey = privateKeyHex.trim().replace("0x", "")
            val ecKeyPair = ECKeyPair.create(Numeric.toBigInt(cleanKey))
            
            // Ethers message prefix: "\u0019Ethereum Signed Message:\n" + message.length + message
            val prefix = "\u0019Ethereum Signed Message:\n" + message.length
            val prefixedMessage = prefix + message
            val messageBytes = prefixedMessage.toByteArray(StandardCharsets.UTF_8)
            
            val msgHash = org.web3j.crypto.Hash.sha3(messageBytes)
            val signatureData = Sign.signMessage(msgHash, ecKeyPair, false)
            
            // Form standard R, S, V signature hex (65 bytes)
            val rBytes = signatureData.r
            val sBytes = signatureData.s
            val vByte = signatureData.v[0]
            
            val signatureBytes = ByteArray(65)
            System.arraycopy(rBytes, 0, signatureBytes, 0, 32)
            System.arraycopy(sBytes, 0, signatureBytes, 32, 32)
            signatureBytes[64] = vByte
            
            Numeric.toHexString(signatureBytes)
        } catch (e: Throwable) {
            throw RuntimeException("Signing failed: ${e.message}", e)
        }
    }

    // Verify ethereum signature
    fun verifyPersonalSignature(message: String, signatureHex: String, expectedAddress: String): Boolean {
        return try {
            val prefix = "\u0019Ethereum Signed Message:\n" + message.length
            val prefixedMessage = prefix + message
            val messageBytes = prefixedMessage.toByteArray(StandardCharsets.UTF_8)
            val msgHash = org.web3j.crypto.Hash.sha3(messageBytes)

            val signatureBytes = Numeric.hexStringToByteArray(signatureHex)
            if (signatureBytes.size != 65) return false

            val r = Arrays.copyOfRange(signatureBytes, 0, 32)
            val s = Arrays.copyOfRange(signatureBytes, 32, 64)
            var v = signatureBytes[64]
            if (v < 27) v = (v + 27).toByte() // standard v is usually 27 or 28

            val sd = Sign.SignatureData(v, r, s)
            // Recover public key
            val publicKey = Sign.signedMessageToKey(msgHash, sd)
            val address = "0x" + Keys.getAddress(publicKey)
            address.equals(expectedAddress, ignoreCase = true)
        } catch (e: Throwable) {
            false
        }
    }
}
