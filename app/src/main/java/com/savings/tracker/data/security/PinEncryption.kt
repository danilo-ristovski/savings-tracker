package com.savings.tracker.data.security

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object PinEncryption {

    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val KEY_ALGORITHM = "AES"

    // 16-byte AES-128 key and IV
    private val KEY_BYTES = byteArrayOf(
        0x53, 0x61, 0x76, 0x65, 0x54, 0x72, 0x61, 0x63,
        0x6B, 0x65, 0x72, 0x31, 0x36, 0x4B, 0x65, 0x79
    ) // "SaveTracker16Key"

    private val IV_BYTES = byteArrayOf(
        0x53, 0x61, 0x76, 0x69, 0x6E, 0x67, 0x73, 0x54,
        0x72, 0x61, 0x63, 0x6B, 0x49, 0x56, 0x31, 0x36
    ) // "SavingsTrackIV16"

    private val secretKey: SecretKeySpec by lazy {
        SecretKeySpec(KEY_BYTES, KEY_ALGORITHM)
    }

    private val ivSpec: IvParameterSpec by lazy {
        IvParameterSpec(IV_BYTES)
    }

    fun encrypt(pin: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        val encrypted = cipher.doFinal(pin.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    fun decrypt(encrypted: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        val decoded = Base64.decode(encrypted, Base64.NO_WRAP)
        val decrypted = cipher.doFinal(decoded)
        return String(decrypted, Charsets.UTF_8)
    }

    fun verify(inputPin: String, encryptedPin: String): Boolean {
        return try {
            val decryptedPin = decrypt(encryptedPin)
            inputPin == decryptedPin
        } catch (e: Exception) {
            false
        }
    }
}
