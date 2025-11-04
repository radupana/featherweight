package com.github.radupana.featherweight.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.KeyGenerator

// EncryptedSharedPreferences deprecated in April 2024 but still works. Google recommends
// DataStore + Tink but for a simple passphrase storage, ESP is sufficient. Will migrate
// when a non-deprecated alternative is stable or when ESP actually stops working.
@Suppress("DEPRECATION")
class DatabaseKeyManager(
    private val context: Context,
) {
    private val masterKey: MasterKey by lazy {
        MasterKey
            .Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun getDatabasePassphrase(): ByteArray {
        val existingKey = encryptedPrefs.getString(KEY_DATABASE_PASSPHRASE, null)
        return if (existingKey != null) {
            android.util.Base64.decode(existingKey, android.util.Base64.NO_WRAP)
        } else {
            generateAndStorePassphrase()
        }
    }

    private fun generateAndStorePassphrase(): ByteArray {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val keyGenSpec =
            KeyGenParameterSpec
                .Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .build()

        keyGenerator.init(keyGenSpec)
        val secretKey = keyGenerator.generateKey()

        val passphrase = secretKey.encoded
        val base64Passphrase = android.util.Base64.encodeToString(passphrase, android.util.Base64.NO_WRAP)
        encryptedPrefs.edit { putString(KEY_DATABASE_PASSPHRASE, base64Passphrase) }

        return passphrase
    }

    fun clearDatabaseKey() {
        encryptedPrefs.edit { remove(KEY_DATABASE_PASSPHRASE) }
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
        }
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "featherweight_db_key"
        private const val PREFS_NAME = "featherweight_encrypted_prefs"
        private const val KEY_DATABASE_PASSPHRASE = "database_passphrase"
    }
}
