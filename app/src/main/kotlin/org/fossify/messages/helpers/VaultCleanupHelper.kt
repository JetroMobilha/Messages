package org.fossify.messages.helpers

import android.content.Context
import android.provider.CallLog
import android.provider.Telephony
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.extensions.conversationsDB
import org.fossify.messages.extensions.messagesDB
import org.fossify.messages.extensions.vaultContactDB
import org.fossify.messages.extensions.vaultMessageDB
import org.fossify.messages.workers.VaultCleanupWorker
import java.util.concurrent.TimeUnit

object VaultCleanupHelper {
    private const val SWEEPER_WORK_NAME = "VaultSweeperWork"

    fun isSamePhoneNumber(num1: String, num2: String): Boolean {
        if (num1 == num2) return true
        val clean1 = num1.replace(Regex("[^0-9]"), "")
        val clean2 = num2.replace(Regex("[^0-9]"), "")
        if (clean1.isEmpty() || clean2.isEmpty()) return false
        if (clean1 == clean2) return true
        val minLen = minOf(clean1.length, clean2.length)
        if (minLen >= 7) {
            return clean1.takeLast(7) == clean2.takeLast(7)
        }
        return false
    }

    fun scheduleSweeper(context: Context, intervalMinutes: Int) {
        val workRequest = PeriodicWorkRequest.Builder(
            VaultCleanupWorker::class.java,
            intervalMinutes.toLong(), TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SWEEPER_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun cancelSweeper(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(SWEEPER_WORK_NAME)
    }

    fun runCleanup(context: Context) {
        ensureBackgroundThread {
            // Delete expired Vault messages (Self-destruct policy)
            try {
                context.vaultMessageDB.deleteExpiredMessages()
            } catch (_: Exception) {}

            val vaultContacts = context.vaultContactDB.getAll()
            if (vaultContacts.isEmpty()) return@ensureBackgroundThread

            val vaultNumbers = vaultContacts.map { it.phoneNumber }

            // Scrub Call Logs
            try {
                context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    arrayOf(CallLog.Calls._ID, CallLog.Calls.NUMBER),
                    null, null, null
                )?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val number = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER))
                        if (number != null && vaultNumbers.any { isSamePhoneNumber(it, number) }) {
                            val id = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls._ID))
                            context.contentResolver.delete(
                                CallLog.Calls.CONTENT_URI,
                                "${CallLog.Calls._ID} = ?",
                                arrayOf(id.toString())
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore permissions or other issues
            }

            // Scrub SMS and Local DB Conversations & Import them into Vault
            try {
                val allConvs = (try { context.conversationsDB.getNonArchived() } catch (_: Exception) { emptyList() }) +
                               (try { context.conversationsDB.getAllArchived() } catch (_: Exception) { emptyList() })

                vaultNumbers.forEach { phone ->
                    val matchingConvs = allConvs.filter { isSamePhoneNumber(phone, it.phoneNumber) }
                    matchingConvs.forEach { conv ->
                        try {
                            val publicMessages = context.messagesDB.getThreadMessages(conv.threadId)
                            publicMessages.forEach { pubMsg ->
                                val vaultMsg = org.fossify.messages.models.VaultMessage(
                                    phoneNumber = phone,
                                    body = pubMsg.body,
                                    date = pubMsg.date.toLong() * 1000L,
                                    type = pubMsg.type,
                                    read = pubMsg.read
                                )
                                context.vaultMessageDB.insert(vaultMsg)
                            }
                        } catch (_: Exception) {}
                        context.messagesDB.deleteThreadMessages(conv.threadId)
                        context.conversationsDB.deleteThreadId(conv.threadId)
                    }
                }

                context.contentResolver.query(
                    Telephony.Sms.CONTENT_URI,
                    arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE, Telephony.Sms.READ),
                    null, null, null
                )?.use { cursor ->
                    val idIdx = cursor.getColumnIndex(Telephony.Sms._ID)
                    val addrIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                    val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
                    val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)
                    val typeIdx = cursor.getColumnIndex(Telephony.Sms.TYPE)
                    val readIdx = cursor.getColumnIndex(Telephony.Sms.READ)

                    while (cursor.moveToNext()) {
                        val address = if (addrIdx >= 0) cursor.getString(addrIdx) else null
                        if (address != null && vaultNumbers.any { isSamePhoneNumber(it, address) }) {
                            val id = cursor.getLong(idIdx)
                            val body = if (bodyIdx >= 0) cursor.getString(bodyIdx) ?: "" else ""
                            val date = if (dateIdx >= 0) cursor.getLong(dateIdx) else System.currentTimeMillis()
                            val type = if (typeIdx >= 0) cursor.getInt(typeIdx) else Telephony.Sms.MESSAGE_TYPE_INBOX
                            val read = if (readIdx >= 0) cursor.getInt(readIdx) == 1 else true

                            val vaultMsg = org.fossify.messages.models.VaultMessage(
                                phoneNumber = address,
                                body = body,
                                date = date,
                                type = type,
                                read = read
                            )
                            context.vaultMessageDB.insert(vaultMsg)

                            context.contentResolver.delete(
                                Telephony.Sms.CONTENT_URI,
                                "${Telephony.Sms._ID} = ?",
                                arrayOf(id.toString())
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore exceptions
            }
        }
    }
}
