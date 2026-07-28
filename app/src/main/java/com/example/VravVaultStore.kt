package com.example

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject

data class ExternalAccount(
    val id: String,
    val label: String,
    val issuer: String,
    val encryptedSecret: String,
    val digits: Int = 6,
    val period: Int = 30
)

/**
 * Persistent encrypted vault. Survives phone reboot.
 * Uses a single prefs name so data is not orphaned if Keystore is briefly unavailable.
 */
class VravVaultStore(private val context: Context) {

    companion object {
        private const val TAG = "VravVaultStore"
        private const val PREFS_NAME = "vrav_vault_storage"
    }

    private val sharedPrefs: SharedPreferences by lazy { createEncryptedPrefs() }

    private fun createEncryptedPrefs(): SharedPreferences {
        // Keystore can be briefly unavailable right after boot until the user unlocks the device.
        var lastError: Exception? = null
        repeat(3) { attempt ->
            try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                return EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "EncryptedSharedPreferences attempt ${attempt + 1} failed: ${e.message}")
                try {
                    Thread.sleep(150L * (attempt + 1))
                } catch (_: InterruptedException) {
                }
            }
        }
        // Same PREFS_NAME — never create a second orphan file that "loses" accounts after reboot.
        Log.e(TAG, "Falling back to private SharedPreferences after Keystore failures", lastError)
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getAccounts(): List<ExternalAccount> {
        val rawJson = sharedPrefs.getString("external_accounts", null) ?: return emptyList()
        return try {
            val list = mutableListOf<ExternalAccount>()
            val array = JSONArray(rawJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ExternalAccount(
                        id = obj.getString("id"),
                        label = obj.getString("label"),
                        issuer = obj.getString("issuer"),
                        encryptedSecret = obj.getString("encryptedSecret"),
                        digits = obj.optInt("digits", 6),
                        period = obj.optInt("period", 30)
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "getAccounts parse error", e)
            emptyList()
        }
    }

    fun saveAccounts(accounts: List<ExternalAccount>) {
        try {
            val array = JSONArray()
            for (acc in accounts) {
                val obj = JSONObject()
                obj.put("id", acc.id)
                obj.put("label", acc.label)
                obj.put("issuer", acc.issuer)
                obj.put("encryptedSecret", acc.encryptedSecret)
                obj.put("digits", acc.digits)
                obj.put("period", acc.period)
                array.put(obj)
            }
            sharedPrefs.edit().putString("external_accounts", array.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "saveAccounts failed", e)
        }
    }

    fun deleteAccount(accountId: String) {
        val current = getAccounts().toMutableList()
        if (current.removeAll { it.id == accountId }) {
            saveAccounts(current)
        }
    }

    fun addAccount(account: ExternalAccount) {
        val current = getAccounts().toMutableList()
        current.removeAll { it.label == account.label && it.issuer == account.issuer }
        current.add(account)
        saveAccounts(current)
    }

    fun clearVault() {
        sharedPrefs.edit()
            .remove("external_accounts")
            .remove("vault_dec_marker")
            .remove("auth_mode")
            .apply()
    }

    fun exportEncryptedVault(): String {
        return try {
            val externalAccounts = sharedPrefs.getString("external_accounts", "[]") ?: "[]"
            val vaultDecMarker = sharedPrefs.getString("vault_dec_marker", "") ?: ""
            val authMode = sharedPrefs.getString("auth_mode", "wallet_only") ?: "wallet_only"
            val backupJson = JSONObject()
            backupJson.put("version", 2)
            backupJson.put("external_accounts", JSONArray(externalAccounts))
            backupJson.put("vault_dec_marker", vaultDecMarker)
            backupJson.put("auth_mode", authMode)
            backupJson.toString(4)
        } catch (e: Exception) {
            "{}"
        }
    }

    fun importEncryptedVault(backupJsonString: String): Boolean {
        return try {
            val backupJson = JSONObject(backupJsonString)
            val externalAccountsArray = backupJson.getJSONArray("external_accounts")
            val vaultDecMarker = backupJson.optString("vault_dec_marker", "")
            val authMode = backupJson.optString("auth_mode", "wallet_only")

            for (i in 0 until externalAccountsArray.length()) {
                val obj = externalAccountsArray.getJSONObject(i)
                if (!obj.has("id") || !obj.has("label") || !obj.has("issuer") || !obj.has("encryptedSecret")) {
                    return false
                }
            }

            val editor = sharedPrefs.edit()
            editor.putString("external_accounts", externalAccountsArray.toString())
            if (vaultDecMarker.isNotEmpty()) {
                editor.putString("vault_dec_marker", vaultDecMarker)
            }
            editor.putString("auth_mode", authMode)
            editor.apply()
            true
        } catch (e: Exception) {
            Log.e(TAG, "importEncryptedVault failed", e)
            false
        }
    }

    fun hasVaultMarker(): Boolean = sharedPrefs.contains("vault_dec_marker")

    fun initializeVaultMarker(keyBytes: ByteArray) {
        val cipher = AesEncryptionUtils.encrypt("VRAV_OK", keyBytes)
        sharedPrefs.edit().putString("vault_dec_marker", cipher).apply()
    }

    fun verifyVaultMarker(keyBytes: ByteArray): Boolean {
        val cipher = sharedPrefs.getString("vault_dec_marker", null) ?: return true
        return try {
            val decrypted = AesEncryptionUtils.decrypt(cipher, keyBytes)
            decrypted == "VRAV_OK"
        } catch (e: Exception) {
            false
        }
    }

    fun getLanguage(): String = sharedPrefs.getString("app_language", "EN") ?: "EN"

    fun saveLanguage(lang: String) {
        sharedPrefs.edit().putString("app_language", lang).apply()
    }

    /** Last used wallet address only — never store private key in plain text. */
    fun getLastAddress(): String = sharedPrefs.getString("last_wallet_address", "") ?: ""

    fun saveLastAddress(address: String) {
        sharedPrefs.edit().putString("last_wallet_address", address).apply()
    }

    fun getAuthMode(): String = sharedPrefs.getString("auth_mode", "wallet_only") ?: "wallet_only"

    fun saveAuthMode(mode: String) {
        sharedPrefs.edit().putString("auth_mode", mode).apply()
    }

    fun getStoredAccountCount(): Int = getAccounts().size
}
