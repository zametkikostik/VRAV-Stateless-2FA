package com.example

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Log
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

interface YubiKeyCallback {
    fun onReady()
    fun onScanning()
    fun onSuccess(response: ByteArray)
    fun onFailure(error: String)
}

class YubiKeyHmacAuthenticator(
    private val context: Context,
    private val callback: YubiKeyCallback
) : NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)
    private var activeChallenge: ByteArray = "VRAV_DEFAULT_CHALLENGE".toByteArray()

    fun setChallenge(challenge: ByteArray) {
        activeChallenge = challenge
    }

    fun startNfcListening(activity: Activity) {
        if (nfcAdapter == null) {
            callback.onFailure("NFC is not supported on this device.")
            return
        }
        if (!nfcAdapter!!.isEnabled) {
            callback.onFailure("NFC is disabled. Please enable it in Settings.")
            return
        }

        callback.onScanning()
        val options = Bundle()
        options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 1000)

        nfcAdapter!!.enableReaderMode(
            activity,
            this,
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            options
        )
    }

    fun stopNfcListening(activity: Activity) {
        nfcAdapter?.disableReaderMode(activity)
    }

    override fun onTagDiscovered(tag: Tag) {
        val isoDep = IsoDep.get(tag)
        if (isoDep == null) {
            callback.onFailure("Tag does not support ISO-DEP (Iso7816) communication.")
            return
        }

        try {
            isoDep.connect()
            isoDep.timeout = 3000

            // 1. SELECT APDU for YubiKey OTP application
            // AID: A0 00 00 03 00 01
            val selectOtpApdu = byteArrayOf(
                0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), 0x06.toByte(),
                0xA0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x03.toByte(), 0x00.toByte(), 0x01.toByte()
            )

            val selectResponse = isoDep.transceive(selectOtpApdu)
            val statusSelect = getStatusWord(selectResponse)

            if (statusSelect != 0x9000) {
                // Try OATH Applet selector: A0 00 00 05 27 21 01
                val selectOathApdu = byteArrayOf(
                    0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), 0x07.toByte(),
                    0xA0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x05.toByte(), 0x27.toByte(), 0x21.toByte(), 0x01.toByte()
                )
                val oathResponse = isoDep.transceive(selectOathApdu)
                val statusOath = getStatusWord(oathResponse)

                if (statusOath != 0x9000) {
                    callback.onFailure("Failed to select YubiKey Application. Status: ${String.format("%04X", statusSelect)}")
                    isoDep.close()
                    return
                }
            }

            // 2. Perform HMAC-SHA256 Challenge Response (Slot 2 Command APDU)
            // Class: 0x00, Instruction: 0x01, Parameter 1: 0x38 (Slot 2 HMAC SHA256), Parameter 2: 0x00
            // Challenge size can vary; but we must prepare challenge command APDU
            val challengeBytes = if (activeChallenge.size > 64) {
                // If larger, hash it to fit standard SHA-256 HMAC slot length
                val digest = MessageDigest.getInstance("SHA-256")
                digest.digest(activeChallenge)
            } else {
                activeChallenge
            }

            val header = byteArrayOf(
                0x00.toByte(), // CLA
                0x01.toByte(), // INS (HMAC Challenge Response)
                0x38.toByte(), // P1 (Slot 2 - HMAC-SHA256)
                0x00.toByte(), // P2
                challengeBytes.size.toByte() // Lc (length of command payload)
            )

            val commandApdu = header + challengeBytes + byteArrayOf(0x00.toByte()) // append Le response length
            val responseApdu = isoDep.transceive(commandApdu)
            val statusWord = getStatusWord(responseApdu)

            if (statusWord == 0x9000) {
                val dataResponse = responseApdu.copyOfRange(0, responseApdu.size - 2)
                callback.onSuccess(dataResponse)
            } else {
                callback.onFailure("Challenge rejected by YubiKey hardware interface. Status: ${String.format("%04X", statusWord)}")
            }

            isoDep.close()
        } catch (e: Exception) {
            callback.onFailure("NFC Transmission Error: ${e.message}")
        }
    }

    private fun getStatusWord(response: ByteArray): Int {
        if (response.size < 2) return 0
        val b1 = response[response.size - 2].toInt() and 0xFF
        val b2 = response[response.size - 1].toInt() and 0xFF
        return (b1 shl 8) or b2
    }

    /**
     * Executes a highly reliable, deterministic software HMAC-SHA256 simulation
     * using the cryptographic signature/seed, representing a Virtual YubiKey.
     * This is critical to ensure proper verification in unit tests, emulators,
     * or local continuous integration where physical hardware cannot be tapped.
     */
    fun emulateYubiKeyTap(softwareSeed: ByteArray, challenge: ByteArray): ByteArray {
        return try {
            val keySpec = SecretKeySpec(softwareSeed, "HmacSHA256")
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(keySpec)
            mac.doFinal(challenge)
        } catch (e: Exception) {
            MessageDigest.getInstance("SHA-256").digest(softwareSeed + challenge)
        }
    }
}
