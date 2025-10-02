// Android Secure Storage Implementation using Android Keystore and EncryptedSharedPreferences

// Add to build.gradle (Module: app)
dependencies {
    implementation "androidx.security:security-crypto:1.1.0-alpha06"
    implementation "androidx.security:security-identity-credential:1.0.0-alpha03"
}

// SecureStorageManager.kt
package com.startup.auth.android.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SecureStorageManager private constructor(private val context: Context) {

    companion object {
        private const val SECURE_PREFS_NAME = "startup_secure_prefs"
        private const val ACCESS_TOKEN_KEY = "access_token"
        private const val REFRESH_TOKEN_KEY = "refresh_token"
        private const val USER_ID_KEY = "user_id"
        private const val USER_EMAIL_KEY = "user_email"
        private const val USER_ROLES_KEY = "user_roles"
        private const val BIOMETRIC_KEY = "biometric_enabled"

        @Volatile
        private var INSTANCE: SecureStorageManager? = null

        fun getInstance(context: Context): SecureStorageManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecureStorageManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setRequestStrongBoxBacked(true) // Use StrongBox if available
            .build()
    }

    private val encryptedSharedPreferences: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // Token Management
    suspend fun storeAccessToken(token: String) = withContext(Dispatchers.IO) {
        encryptedSharedPreferences.edit()
            .putString(ACCESS_TOKEN_KEY, token)
            .apply()
    }

    suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        encryptedSharedPreferences.getString(ACCESS_TOKEN_KEY, null)
    }

    suspend fun storeRefreshToken(token: String) = withContext(Dispatchers.IO) {
        encryptedSharedPreferences.edit()
            .putString(REFRESH_TOKEN_KEY, token)
            .apply()
    }

    suspend fun getRefreshToken(): String? = withContext(Dispatchers.IO) {
        encryptedSharedPreferences.getString(REFRESH_TOKEN_KEY, null)
    }

    // User Data Management
    suspend fun storeUserData(userId: Long, email: String?, roles: List<String>) = withContext(Dispatchers.IO) {
        val rolesJson = roles.joinToString(",")
        encryptedSharedPreferences.edit()
            .putLong(USER_ID_KEY, userId)
            .putString(USER_EMAIL_KEY, email)
            .putString(USER_ROLES_KEY, rolesJson)
            .apply()
    }

    suspend fun getUserId(): Long = withContext(Dispatchers.IO) {
        encryptedSharedPreferences.getLong(USER_ID_KEY, -1L)
    }

    suspend fun getUserEmail(): String? = withContext(Dispatchers.IO) {
        encryptedSharedPreferences.getString(USER_EMAIL_KEY, null)
    }

    suspend fun getUserRoles(): List<String> = withContext(Dispatchers.IO) {
        val rolesString = encryptedSharedPreferences.getString(USER_ROLES_KEY, "")
        if (rolesString.isNullOrEmpty()) emptyList() else rolesString.split(",")
    }

    // Session Management
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        encryptedSharedPreferences.edit().clear().apply()
    }

    suspend fun isUserLoggedIn(): Boolean = withContext(Dispatchers.IO) {
        !getAccessToken().isNullOrEmpty() && !getRefreshToken().isNullOrEmpty()
    }
}