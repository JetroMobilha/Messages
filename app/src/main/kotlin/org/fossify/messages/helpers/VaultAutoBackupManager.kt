package org.fossify.messages.helpers

import android.content.Context
import com.google.gson.Gson
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.extensions.vaultCallDB
import org.fossify.messages.extensions.vaultContactDB
import org.fossify.messages.extensions.vaultMessageDB
import org.fossify.messages.models.VaultExportPackage
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import android.util.Base64

object VaultAutoBackupManager {

    private const val BACKUP_FILE_NAME = "autobackup_vault.enc"
    // 10.0.2.2 is local loopback in Android Emulator mapping to Host localhost
    private const val API_BASE_URL = "http://10.0.2.2/messages-api/public/api/v1"

    fun performAutoBackup(context: Context, usernamePassKey: String, username: String, callback: (Boolean) -> Unit) {
        ensureBackgroundThread {
            try {
                val contacts = context.vaultContactDB.getAll()
                val messages = context.vaultMessageDB.getAll()
                val calls = context.vaultCallDB.getAll()

                val backupPackage = VaultExportPackage(
                    username = username,
                    sms = null,
                    mms = null,
                    vaultContacts = contacts,
                    vaultMessages = messages,
                    vaultCalls = calls
                )

                val gson = Gson()
                val jsonString = gson.toJson(backupPackage)
                val encryptedBytes = VaultEncryptionHelper.encrypt(jsonString.toByteArray(Charsets.UTF_8), usernamePassKey)

                // 1. Save locally
                val backupFile = File(context.filesDir, BACKUP_FILE_NAME)
                backupFile.writeBytes(encryptedBytes)

                // 2. Sync to Laravel API Server
                val base64Payload = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
                val parts = usernamePassKey.split(":")
                val pass = if (parts.size > 1) parts[1] else usernamePassKey
                
                syncToCloudApi(username, pass, base64Payload)

                callback(true)
            } catch (e: Exception) {
                callback(false)
            }
        }
    }

    fun restoreAutoBackup(context: Context, usernamePassKey: String, callback: (Boolean, String) -> Unit) {
        ensureBackgroundThread {
            try {
                val backupFile = File(context.filesDir, BACKUP_FILE_NAME)
                var encryptedBytes: ByteArray? = if (backupFile.exists()) backupFile.readBytes() else null

                // If local backup is missing or requested, try fetching from Cloud API
                val parts = usernamePassKey.split(":")
                val user = if (parts.isNotEmpty()) parts[0] else ""
                val pass = if (parts.size > 1) parts[1] else usernamePassKey

                if (encryptedBytes == null && user.isNotEmpty()) {
                    val cloudPayloadBase64 = fetchFromCloudApi(user, pass)
                    if (cloudPayloadBase64 != null) {
                        encryptedBytes = Base64.decode(cloudPayloadBase64, Base64.NO_WRAP)
                    }
                }

                if (encryptedBytes == null) {
                    callback(false, "Nenhum backup automático ou na nuvem encontrado")
                    return@ensureBackgroundThread
                }

                val decryptedBytes = VaultEncryptionHelper.decrypt(encryptedBytes, usernamePassKey)
                val jsonString = String(decryptedBytes, Charsets.UTF_8)

                val gson = Gson()
                val pkg = gson.fromJson(jsonString, VaultExportPackage::class.java)

                // Restore Vault Contacts
                pkg.vaultContacts?.forEach { contact ->
                    context.vaultContactDB.insert(contact)
                }

                // Restore Vault Messages
                pkg.vaultMessages?.forEach { msg ->
                    context.vaultMessageDB.insert(msg)
                }

                // Restore Vault Calls
                pkg.vaultCalls?.forEach { call ->
                    context.vaultCallDB.insert(call)
                }

                callback(true, "Restauração concluída com sucesso para o utilizador ${pkg.username}")
            } catch (e: Exception) {
                callback(false, "Falha ao restaurar: Senha incorreta ou ficheiro corrompido")
            }
        }
    }

    private fun syncToCloudApi(username: String, password: String, base64Payload: String) {
        try {
            val url = URL("$API_BASE_URL/backup/save")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val jsonBody = Gson().toJson(mapOf(
                "username" to username,
                "password" to password,
                "encrypted_payload" to base64Payload
            ))

            OutputStreamWriter(conn.outputStream).use { it.write(jsonBody) }
            conn.responseCode // Read response
        } catch (_: Exception) {
            // Ignore offline network errors, local backup is preserved
        }
    }

    private fun fetchFromCloudApi(username: String, password: String): String? {
        return try {
            val url = URL("$API_BASE_URL/backup/restore")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val jsonBody = Gson().toJson(mapOf(
                "username" to username,
                "password" to password
            ))

            OutputStreamWriter(conn.outputStream).use { it.write(jsonBody) }
            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val map = Gson().fromJson(responseText, Map::class.java)
                map["encrypted_payload"] as? String
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
