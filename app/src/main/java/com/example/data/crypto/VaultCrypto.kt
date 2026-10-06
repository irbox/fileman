package com.example.data.crypto

import android.util.Base64
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object VaultCrypto {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_DERIVATION_ALGO = "PBKDF2WithHmacSHA256"
    private const val ITERATION_COUNT = 10000
    private const val KEY_LENGTH = 256
    private const val GCM_TAG_LENGTH = 128
    private const val IV_LENGTH_BYTES = 12
    private const val SALT_LENGTH_BYTES = 16

    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        SecureRandom().nextBytes(salt)
        return salt
    }

    fun generateIv(): ByteArray {
        val iv = ByteArray(IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)
        return iv
    }

    fun deriveKey(passcode: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(passcode.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGO)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encryptFile(
        sourceFile: File,
        destinationFile: File,
        passcode: String
    ): Triple<String, String, Long> {
        val salt = generateSalt()
        val iv = generateIv()
        val key = deriveKey(passcode, salt)

        val cipher = Cipher.getInstance(ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)

        FileInputStream(sourceFile).use { input ->
            FileOutputStream(destinationFile).use { output ->
                CipherOutputStream(output, cipher).use { cipherOut ->
                    input.copyTo(cipherOut, bufferSize = 8192)
                }
            }
        }

        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        return Triple(saltBase64, ivBase64, destinationFile.length())
    }

    fun decryptFile(
        sourceFile: File,
        destinationFile: File,
        passcode: String,
        saltBase64: String,
        ivBase64: String
    ): Boolean {
        return try {
            val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val key = deriveKey(passcode, salt)

            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)

            FileInputStream(sourceFile).use { input ->
                CipherInputStream(input, cipher).use { cipherIn ->
                    FileOutputStream(destinationFile).use { output ->
                        cipherIn.copyTo(output, bufferSize = 8192)
                    }
                }
            }
            true
        } catch (e: Exception) {
            if (destinationFile.exists()) destinationFile.delete()
            false
        }
    }

    fun calculateChecksum(file: File, algorithm: String = "SHA-256"): String {
        return try {
            val digest = MessageDigest.getInstance(algorithm)
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
