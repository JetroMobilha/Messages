package org.fossify.messages.helpers

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object VaultEncryptionHelper {

    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val KEY_FACTORY_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATION_COUNT = 10000
    private const val KEY_LENGTH = 256
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 16

    fun encrypt(data: ByteArray, usernamePass: String): ByteArray {
        val salt = ByteArray(SALT_SIZE)
        SecureRandom().nextBytes(salt)

        val key = generateKey(usernamePass, salt)
        val iv = ByteArray(IV_SIZE)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))

        val encryptedData = cipher.doFinal(data)

        // Result structure: [Salt (16 bytes)] + [IV (16 bytes)] + [Encrypted Data]
        val result = ByteArray(salt.size + iv.size + encryptedData.size)
        System.arraycopy(salt, 0, result, 0, salt.size)
        System.arraycopy(iv, 0, result, salt.size, iv.size)
        System.arraycopy(encryptedData, 0, result, salt.size + iv.size, encryptedData.size)

        return result
    }

    fun decrypt(encryptedPayload: ByteArray, usernamePass: String): ByteArray {
        if (encryptedPayload.size < SALT_SIZE + IV_SIZE) {
            throw IllegalArgumentException("Payload inválido para decriptação")
        }

        val salt = ByteArray(SALT_SIZE)
        val iv = ByteArray(IV_SIZE)
        val encryptedData = ByteArray(encryptedPayload.size - SALT_SIZE - IV_SIZE)

        System.arraycopy(encryptedPayload, 0, salt, 0, SALT_SIZE)
        System.arraycopy(encryptedPayload, SALT_SIZE, iv, 0, IV_SIZE)
        System.arraycopy(encryptedPayload, SALT_SIZE + IV_SIZE, encryptedData, 0, encryptedData.size)

        val key = generateKey(usernamePass, salt)

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))

        return cipher.doFinal(encryptedData)
    }

    private fun generateKey(passphrase: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance(KEY_FACTORY_ALGORITHM)
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }
}
