package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.crypto.VaultCrypto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LibreFiles", appName)
    }

    @Test
    fun `test vault crypto file encryption and decryption`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val originalFile = File(context.cacheDir, "secret_test.txt")
        originalFile.writeText("Confidential LibreFiles test payload!")

        val encryptedFile = File(context.cacheDir, "secret_test.enc")
        val decryptedFile = File(context.cacheDir, "secret_test_decrypted.txt")

        val passcode = "SuperMasterPassword123!"
        val (saltB64, ivB64, encSize) = VaultCrypto.encryptFile(
            sourceFile = originalFile,
            destinationFile = encryptedFile,
            passcode = passcode
        )

        assertTrue(encSize > 0)
        assertTrue(encryptedFile.exists())

        val decryptedSuccess = VaultCrypto.decryptFile(
            sourceFile = encryptedFile,
            destinationFile = decryptedFile,
            passcode = passcode,
            saltBase64 = saltB64,
            ivBase64 = ivB64
        )

        assertTrue(decryptedSuccess)
        assertEquals("Confidential LibreFiles test payload!", decryptedFile.readText())

        // Clean up
        originalFile.delete()
        encryptedFile.delete()
        decryptedFile.delete()
    }
}
