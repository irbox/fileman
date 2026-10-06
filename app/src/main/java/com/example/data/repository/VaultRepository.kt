package com.example.data.repository

import android.content.Context
import android.util.Base64
import com.example.data.crypto.VaultCrypto
import com.example.data.db.AppDatabase
import com.example.data.db.AuditLogEntity
import com.example.data.db.VaultItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.UUID

class VaultRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val vaultDao = database.vaultDao()
    private val auditLogDao = database.auditLogDao()

    val vaultItems: Flow<List<VaultItemEntity>> = vaultDao.getAllVaultItems()

    private val vaultDir: File by lazy {
        File(context.filesDir, "libre_vault").apply { if (!exists()) mkdirs() }
    }

    private val authConfigFile: File by lazy {
        File(context.filesDir, "vault_auth.bin")
    }

    private val bioSecretFile: File by lazy {
        File(context.filesDir, "vault_bio_key.bin")
    }

    fun isVaultConfigured(): Boolean {
        return authConfigFile.exists()
    }

    fun isBiometricUnlockEnabled(): Boolean {
        return bioSecretFile.exists() && isVaultConfigured()
    }

    fun enableBiometricUnlock(passcode: String): Boolean {
        return try {
            if (!verifyPasscode(passcode)) return false
            val deviceSecret = context.packageName.toByteArray(Charsets.UTF_8)
            val encBytes = passcode.toByteArray(Charsets.UTF_8).mapIndexed { i, b ->
                (b.toInt() xor deviceSecret[i % deviceSecret.size].toInt()).toByte()
            }.toByteArray()
            val b64 = Base64.encodeToString(encBytes, Base64.NO_WRAP)
            bioSecretFile.writeText(b64)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun disableBiometricUnlock() {
        if (bioSecretFile.exists()) {
            bioSecretFile.delete()
        }
    }

    fun getPasscodeFromBiometrics(): String? {
        if (!bioSecretFile.exists()) return null
        return try {
            val b64 = bioSecretFile.readText()
            val encBytes = Base64.decode(b64, Base64.NO_WRAP)
            val deviceSecret = context.packageName.toByteArray(Charsets.UTF_8)
            val decBytes = encBytes.mapIndexed { i, b ->
                (b.toInt() xor deviceSecret[i % deviceSecret.size].toInt()).toByte()
            }.toByteArray()
            val passcode = String(decBytes, Charsets.UTF_8)
            if (verifyPasscode(passcode)) passcode else null
        } catch (e: Exception) {
            null
        }
    }

    fun setupVaultPasscode(passcode: String): Boolean {
        return try {
            val salt = VaultCrypto.generateSalt()
            val derived = VaultCrypto.deriveKey(passcode, salt).encoded
            val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
            val hashB64 = Base64.encodeToString(derived, Base64.NO_WRAP)
            authConfigFile.writeText("$saltB64:$hashB64")
            enableBiometricUnlock(passcode)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun verifyPasscode(passcode: String): Boolean {
        if (!authConfigFile.exists()) return false
        return try {
            val parts = authConfigFile.readText().split(":")
            if (parts.size != 2) return false
            val salt = Base64.decode(parts[0], Base64.NO_WRAP)
            val storedHash = parts[1]

            val derived = VaultCrypto.deriveKey(passcode, salt).encoded
            val currentHash = Base64.encodeToString(derived, Base64.NO_WRAP)
            storedHash == currentHash
        } catch (e: Exception) {
            false
        }
    }

    suspend fun encryptFileToVault(
        sourceFile: File,
        passcode: String,
        deleteOriginal: Boolean = true
    ): Boolean = withContext(Dispatchers.IO) {
        if (!sourceFile.exists()) return@withContext false

        val itemId = UUID.randomUUID().toString()
        val encFileName = "$itemId.enc"
        val encFile = File(vaultDir, encFileName)

        try {
            val (saltBase64, ivBase64, encSize) = VaultCrypto.encryptFile(
                sourceFile = sourceFile,
                destinationFile = encFile,
                passcode = passcode
            )

            val vaultItem = VaultItemEntity(
                id = itemId,
                originalName = sourceFile.name,
                encryptedFileName = encFileName,
                originalPath = sourceFile.path,
                mimeType = "application/octet-stream",
                sizeBytes = sourceFile.length(),
                ivBase64 = ivBase64,
                saltBase64 = saltBase64,
                dateEncrypted = System.currentTimeMillis()
            )

            vaultDao.insertVaultItem(vaultItem)

            if (deleteOriginal) {
                sourceFile.delete()
            }

            auditLogDao.insertLog(
                AuditLogEntity(
                    action = "VAULT_ENCRYPT",
                    target = sourceFile.name,
                    details = "Encrypted with AES-256-GCM ($encSize bytes)"
                )
            )
            true
        } catch (e: Exception) {
            if (encFile.exists()) encFile.delete()
            false
        }
    }

    suspend fun decryptFileFromVault(
        itemId: String,
        targetDirectory: File,
        passcode: String
    ): Boolean = withContext(Dispatchers.IO) {
        val item = vaultDao.getVaultItemById(itemId) ?: return@withContext false
        val encFile = File(vaultDir, item.encryptedFileName)
        if (!encFile.exists()) return@withContext false

        val targetFile = File(targetDirectory, item.originalName)

        val success = VaultCrypto.decryptFile(
            sourceFile = encFile,
            destinationFile = targetFile,
            passcode = passcode,
            saltBase64 = item.saltBase64,
            ivBase64 = item.ivBase64
        )

        if (success) {
            encFile.delete()
            vaultDao.deleteVaultItem(itemId)
            auditLogDao.insertLog(
                AuditLogEntity(
                    action = "VAULT_DECRYPT",
                    target = targetFile.name,
                    details = "Decrypted item back to ${targetDirectory.path}"
                )
            )
        }
        success
    }

    suspend fun deleteVaultItemPermanently(itemId: String): Boolean = withContext(Dispatchers.IO) {
        val item = vaultDao.getVaultItemById(itemId) ?: return@withContext false
        val encFile = File(vaultDir, item.encryptedFileName)
        if (encFile.exists()) encFile.delete()
        vaultDao.deleteVaultItem(itemId)
        auditLogDao.insertLog(
            AuditLogEntity(
                action = "VAULT_DELETE",
                target = item.originalName,
                details = "Permanently erased encrypted record"
            )
        )
        true
    }
}
