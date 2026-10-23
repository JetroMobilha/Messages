package org.fossify.messages.helpers

import android.content.Context
import android.provider.CallLog
import android.provider.Telephony
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.extensions.vaultContactDB
import org.fossify.messages.extensions.vaultMessageDB

object VaultCleanupHelper {
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
                        if (vaultNumbers.any { it == number }) { // simple match for now
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

            // Scrub SMS
            try {
                context.contentResolver.query(
                    Telephony.Sms.CONTENT_URI,
                    arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS),
                    null, null, null
                )?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                        if (address != null && vaultNumbers.any { it == address }) {
                            val id = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms._ID))
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
