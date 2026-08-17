package com.github.adriianh.melo.ui.login

import java.io.File
import java.util.Base64
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
 * The original implementation only read `value`, which is blank for these cookies on any
 * reasonably up-to-date Chromium browser -- meaning `SAPISID` would silently never be found
 * through the profile-import/automated-login paths on most machines.
 *
 * Chromium's `os_crypt` protects the AES key differently per platform:
 *  - **Linux**: PBKDF2 over a password from the OS keyring (`secret-tool`), falling back to the
 *    well-known constant `"peanuts"` when no keyring backend is configured -- this is Chromium's
 *    own documented fallback, not a workaround of ours. AES-128-CBC, `v10`/`v11` prefix.
 *  - **macOS**: PBKDF2 over a password stored in the login Keychain under `"<Browser> Safe
 *    Storage"`. AES-128-CBC, `v10` prefix.
 *  - **Windows**: AES-256-GCM. The key itself is DPAPI-protected inside the browser's
 *    `Local State` file and is unwrapped via `ProtectedData.Unprotect` (invoked through a small
 *    PowerShell call, since the JDK has no DPAPI binding). `v10` prefix.
 *
 * This is intentionally best-effort: any failure (missing keyring, locked/denied Keychain entry,
 * PowerShell unavailable, corrupted blob) yields `null` instead of throwing, so callers can fall
 * back to other auth methods (e.g. the in-app WebView login) instead of crashing.
 */
internal object CookieCrypto {

    private const val V10_PREFIX = "v10"
    private const val V11_PREFIX = "v11"
    private val POSIX_SALT = "saltysalt".toByteArray()
    private val POSIX_IV = ByteArray(16) { ' '.code.toByte() }

    /**
     * @param encryptedValue the raw `encrypted_value` BLOB, prefix included.
     * @param macKeychainService the Keychain service name to query on macOS,
     *   e.g. `"Chrome Safe Storage"`, `"Brave Safe Storage"`.
     */
    fun decryptChromiumCookie(encryptedValue: ByteArray, macKeychainService: String): String? {
        if (encryptedValue.size < 3) return null
        val prefix = String(encryptedValue, 0, 3, Charsets.US_ASCII)
        if (prefix != V10_PREFIX && prefix != V11_PREFIX) return null
        val payload = encryptedValue.copyOfRange(3, encryptedValue.size)

        return when (HostOs.current) {
            HostOs.WINDOWS -> decryptWindows(payload)
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

    private val windowsAesKey: ByteArray? by lazy { runCatching { loadWindowsAesKey() }.getOrNull() }

    private fun decryptWindows(payload: ByteArray): String? = runCatching {
        val nonceSize = 12
        val minTagSize = 16
        if (payload.size < nonceSize + minTagSize) return null
        val nonce = payload.copyOfRange(0, nonceSize)
        val ciphertextAndTag = payload.copyOfRange(nonceSize, payload.size)
        val key = windowsAesKey ?: return null

        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        }
        String(cipher.doFinal(ciphertextAndTag), Charsets.UTF_8)
    }.getOrNull()

    private val LOCAL_STATE_CANDIDATES = listOf(
        "Google\\Chrome\\User Data\\Local State",
        "BraveSoftware\\Brave-Browser\\User Data\\Local State",
        "Microsoft\\Edge\\User Data\\Local State",
        "Vivaldi\\User Data\\Local State",
    )

    private fun loadWindowsAesKey(): ByteArray? {
        val localAppData = System.getenv("LOCALAPPDATA") ?: return null
        val localState = LOCAL_STATE_CANDIDATES
            .map { File(localAppData, it) }
            .firstOrNull { it.exists() } ?: return null

        val encryptedKeyBase64 = Regex(""""encrypted_key"\s*:\s*"([^"]+)"""")
            .find(localState.readText())
            ?.groupValues
            ?.get(1)
            ?: return null

        val dpapiBlob = Base64.getDecoder().decode(encryptedKeyBase64)
        val dpapiPrefixSize = 5
        if (dpapiBlob.size <= dpapiPrefixSize) return null
        return unprotectWithDpapi(dpapiBlob.copyOfRange(dpapiPrefixSize, dpapiBlob.size))
    }

    private fun unprotectWithDpapi(blob: ByteArray): ByteArray? {
        val blobBase64 = Base64.getEncoder().encodeToString(blob)
        val script = """
            Add-Type -AssemblyName System.Security
            [Convert]::ToBase64String(
                [System.Security.Cryptography.ProtectedData]::Unprotect(
                    [Convert]::FromBase64String('$blobBase64'),
                    ${'$'}null,
                    [System.Security.Cryptography.DataProtectionScope]::CurrentUser
                )
            )
        """.trimIndent()
        val decodedBase64 =
            runCommand("powershell", "-NoProfile", "-NonInteractive", "-Command", script)
        return decodedBase64?.let { Base64.getDecoder().decode(it) }
    }

    private fun runCommand(vararg command: String): String? = runCatching {
        val process = ProcessBuilder(*command).redirectErrorStream(false).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        val finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            return null
        }
        output.takeIf { process.exitValue() == 0 && it.isNotBlank() }
    }.getOrNull()
}