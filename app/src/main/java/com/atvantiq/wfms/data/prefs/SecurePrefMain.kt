package com.atvantiq.wfms.data.prefs

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.ssas.jibli.data.prefs.SharedPrefPrint
import java.io.File
import java.security.KeyStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurePrefMain @Inject constructor(
    private val context: Context
) : SharedPrefPrint {

    private val sharedPreferences: SharedPreferences = createEncryptedPrefs()

    private fun createEncryptedPrefs(): SharedPreferences {
        return try {
            buildPrefs()
        } catch (firstError: Exception) {
            Log.w(TAG, "EncryptedSharedPreferences init failed. Resetting secure state.", firstError)
            resetCorruptedState()
            try {
                buildPrefs()
            } catch (secondError: Exception) {
                Log.e(TAG, "EncryptedSharedPreferences re-init failed after reset.", secondError)
                throw secondError
            }
        }
    }

    private fun buildPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PrefKeys.WFMS_SECURE_PREF,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun resetCorruptedState() {
        try {
            context.deleteSharedPreferences(PrefKeys.WFMS_SECURE_PREF)
            // Defensive cleanup for direct file paths on some devices/OS versions
            val spDir = File(context.applicationInfo.dataDir, "shared_prefs")
            File(spDir, "${PrefKeys.WFMS_SECURE_PREF}.xml").takeIf { it.exists() }?.delete()
            File(spDir, "__androidx_security_crypto_encrypted_prefs_keyset__.xml")
                .takeIf { it.exists() }?.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete encrypted shared preferences files", e)
        }
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val alias = MasterKey.DEFAULT_MASTER_KEY_ALIAS
            if (keyStore.containsAlias(alias)) {
                keyStore.deleteEntry(alias)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete master key alias", e)
        }
    }

    override fun put(key: String, value: Int) { sharedPreferences.edit().putInt(key, value).apply() }
    override fun get(key: String, defaultValue: Int): Int = sharedPreferences.getInt(key, defaultValue)
    override fun put(key: String, value: Float) { sharedPreferences.edit().putFloat(key, value).apply() }
    override fun get(key: String, defaultValue: Float): Float = sharedPreferences.getFloat(key, defaultValue)
    override fun put(key: String, value: Boolean) { sharedPreferences.edit().putBoolean(key, value).apply() }
    override fun get(key: String, defaultValue: Boolean): Boolean = sharedPreferences.getBoolean(key, defaultValue)
    override fun put(key: String, value: Long) { sharedPreferences.edit().putLong(key, value).apply() }
    override fun get(key: String, defaultValue: Long): Long = sharedPreferences.getLong(key, defaultValue)
    override fun put(key: String, value: String?) { sharedPreferences.edit().putString(key, value).apply() }
    override fun get(key: String, defaultValue: String?): String? = sharedPreferences.getString(key, defaultValue)
    override fun delete(key: String) { sharedPreferences.edit().remove(key).apply() }
    override fun deleteAll() { sharedPreferences.edit().clear().apply() }

    companion object {
        private const val TAG = "SecurePrefMain"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    }
}