package com.github.adriianh.melo.ui.login

import com.sun.jna.platform.win32.Crypt32Util
import java.io.File
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Decrypts the `encrypted_value` column that modern Chromium-based browsers (Chrome 80+, Brave,
 * Edge, Vivaldi...) use for sensitive cookies instead of the plaintext `value` column.
 *
 * Chromium's `os_crypt` protects the AES key differently per platform:
 *  - **Linux**: PBKDF2 over a password from the OS keyring (`secret-tool`), falling back to the
 *    well-known constant `"peanuts"` when no keyring backend is configured. AES-128-CBC, `v10`/`v11` prefix.
 *  - **macOS**: PBKDF2 over a password stored in the login Keychain under `"<Browser> Safe
 *    Storage"`. AES-128-CBC, `v10` prefix.
 *  - **Windows**: AES-256-GCM. The key itself is DPAPI-protected inside the browser's
 *    `Local State` file and is unwrapped via native Win32 `CryptUnprotectData` (via JNA Crypt32Util)
 *    with a PowerShell fallback. `v10` prefix.
 */
internal object CookieCrypto {

    private const val V10_PREFIX = "v10"
    private const val V11_PREFIX = "v11"
    private val POSIX_SALT = "saltysalt".toByteArray()
    private val POSIX_IV = ByteArray(16) { ' '.code.toByte() }

    private val windowsKeyCache = ConcurrentHashMap<String, ByteArray>()

    /**
     * @param encryptedValue the raw `encrypted_value` BLOB, prefix included.
     * @param macKeychainService the Keychain service name to query on macOS.
     * @param localStateFile optional path to the `Local State` file that holds the DPAPI key for this profile.
     */
    fun decryptChromiumCookie(
        encryptedValue: ByteArray,
        macKeychainService: String,
        localStateFile: File? = null,
    ): String? {
        if (encryptedValue.size < 3) return null
        val prefix = String(encryptedValue, 0, 3, Charsets.US_ASCII)
        if (prefix != V10_PREFIX && prefix != V11_PREFIX) return null
        val payload = encryptedValue.copyOfRange(3, encryptedValue.size)

        return when (HostOs.current) {
            HostOs.WINDOWS -> decryptWindows(payload, localStateFile)
            HostOs.MACOS -> decryptPosix(
                payload,
                macKeychainPassword(macKeychainService),
                iterations = 1003
            )

            HostOs.LINUX -> decryptPosix(payload, linuxSecretPassword(), iterations = 1)
        }
    }

    private fun decryptPosix(payload: ByteArray, password: CharArray, iterations: Int): String? =
        runCatching {
            val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                .generateSecret(PBEKeySpec(password, POSIX_SALT, iterations, 128))
                .encoded
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding").apply {
                init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(POSIX_IV))
            }
            String(cipher.doFinal(payload), Charsets.UTF_8)
        }.getOrNull()

    private fun linuxSecretPassword(): CharArray {
        val keyringPassword = runCommand("secret-tool", "lookup", "application", "chrome")
        return (keyringPassword ?: "peanuts").toCharArray()
    }

    private fun macKeychainPassword(serviceName: String): CharArray {
        val keychainPassword =
            runCommand("security", "find-generic-password", "-w", "-s", serviceName)
        return (keychainPassword ?: "").toCharArray()
    }

    private val log = Logger.getLogger("Melo.CookieCrypto")

    private fun decryptWindows(payload: ByteArray, localStateFile: File?): String? = runCatching {
        val nonceSize = 12
        val minTagSize = 16
        if (payload.size < nonceSize + minTagSize) return null
        val nonce = payload.copyOfRange(0, nonceSize)
        val ciphertextAndTag = payload.copyOfRange(nonceSize, payload.size)
        val key = getWindowsAesKey(localStateFile) ?: return null

        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        }
        String(cipher.doFinal(ciphertextAndTag), Charsets.UTF_8)
    }.getOrElse {
        log.fine("Cookie decryption failed: ${it.message}")
        null
    }

    private val LOCAL_STATE_CANDIDATES = listOf(
        "Google\\Chrome\\User Data\\Local State",
        "BraveSoftware\\Brave-Browser\\User Data\\Local State",
        "Microsoft\\Edge\\User Data\\Local State",
        "Vivaldi\\User Data\\Local State",
    )

    private fun getWindowsAesKey(localStateFile: File?): ByteArray? {
        if (localStateFile != null && localStateFile.exists()) {
            val path = runCatching { localStateFile.canonicalPath }.getOrDefault(localStateFile.absolutePath)
            val cached = windowsKeyCache[path]
            if (cached != null) return cached

            val loaded = loadWindowsAesKeyFromFile(localStateFile)
            if (loaded != null && loaded.isNotEmpty()) {
                windowsKeyCache[path] = loaded
                return loaded
            }
            return null
        }

        val localAppData = System.getenv("LOCALAPPDATA") ?: return null
        for (candidate in LOCAL_STATE_CANDIDATES) {
            val file = File(localAppData, candidate)
            if (file.exists()) {
                val key = getWindowsAesKey(file)
                if (key != null) return key
            }
        }
        return null
    }

    private fun loadWindowsAesKeyFromFile(localStateFile: File): ByteArray? = runCatching {
        val text = localStateFile.readText()
        val encryptedKeyBase64 = Regex(""""encrypted_key"\s*:\s*"([^"]+)"""")
            .find(text)
            ?.groupValues
            ?.get(1)
        if (encryptedKeyBase64 == null) {
            log.warning("No encrypted_key regex match in: ${localStateFile.absolutePath}")
            return null
        }

        val dpapiBlob = Base64.getDecoder().decode(encryptedKeyBase64)
        val dpapiPrefixSize = 5 // "DPAPI"
        if (dpapiBlob.size <= dpapiPrefixSize) {
            log.warning("DPAPI blob too small in: ${localStateFile.absolutePath}")
            return null
        }
        val key = unprotectWithDpapi(dpapiBlob.copyOfRange(dpapiPrefixSize, dpapiBlob.size))
        if (key != null) {
            log.info("Successfully loaded Windows AES key (${key.size} bytes) from: ${localStateFile.absolutePath}")
        }
        key
    }.getOrElse {
        log.warning("Failed to parse Local State from ${localStateFile.absolutePath}: ${it.message}")
        null
    }

    private fun unprotectWithDpapi(blob: ByteArray): ByteArray? {
        try {
            val jnaResult = Crypt32Util.cryptUnprotectData(blob)
            if (jnaResult != null && jnaResult.isNotEmpty()) {
                return jnaResult
            }
        } catch (e: Throwable) {
            log.warning("JNA Crypt32Util.cryptUnprotectData failed: ${e.message}, attempting PowerShell fallback")
        }

        return runCatching {
            val blobBase64 = Base64.getEncoder().encodeToString(blob)
            val script =
                $$"Add-Type -AssemblyName System.Security; [Convert]::ToBase64String([System.Security.Cryptography.ProtectedData]::Unprotect([Convert]::FromBase64String('$$blobBase64'), $null, [System.Security.Cryptography.DataProtectionScope]::CurrentUser))"
            val decodedBase64 = runCommand("powershell", "-ExecutionPolicy", "Bypass", "-NoProfile", "-NonInteractive", "-Command", script)
            decodedBase64?.let { Base64.getDecoder().decode(it) }
        }.getOrNull()
    }

    private fun runCommand(vararg command: String): String? = runCatching {
        val process = ProcessBuilder(*command).redirectErrorStream(false).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        val finished = process.waitFor(5, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            return null
        }
        output.takeIf { process.exitValue() == 0 && it.isNotBlank() }
    }.getOrNull()
}