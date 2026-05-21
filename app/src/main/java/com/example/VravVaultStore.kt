package com.example

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.json.JSONArray
import org.json.JSONObject

data class ExternalAccount(
    val id: String,
    val label: String,
    val issuer: String,
    val encryptedSecret: String
)

class VravVaultStore(private val context: Context) {

    private val sharedPrefs by lazy {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                "vrav_vault_storage",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Safe fallback if KeyStore is unavailable (e.g., in some units or test conditions)
            context.getSharedPreferences("vrav_vault_storage_fallback", Context.MODE_PRIVATE)
        }
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
                        encryptedSecret = obj.getString("encryptedSecret")
                    )
                )
            }
            list
        } catch (e: Exception) {
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
                array.put(obj)
            }
            sharedPrefs.edit().putString("external_accounts", array.toString()).apply()
        } catch (e: Exception) {
            // Fail silently
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
        // Prevent duplicates
        current.removeAll { it.label == account.label && it.issuer == account.issuer }
        current.add(account)
        saveAccounts(current)
    }

    fun clearVault() {
        sharedPrefs.edit().clear().apply()
    }

    fun hasVaultMarker(): Boolean {
        return sharedPrefs.contains("vault_dec_marker")
    }

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
}
