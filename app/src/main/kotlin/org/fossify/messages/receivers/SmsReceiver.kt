package org.fossify.messages.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import org.fossify.commons.extensions.baseConfig
import org.fossify.commons.extensions.getMyContactsCursor
import org.fossify.commons.extensions.isNumberBlocked
import org.fossify.commons.helpers.ContactLookupResult
import org.fossify.commons.helpers.SimpleContactsHelper
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.models.PhoneNumber
import org.fossify.commons.models.SimpleContact
import org.fossify.messages.extensions.getConversations
import org.fossify.messages.extensions.getNameFromAddress
import org.fossify.messages.extensions.getNotificationBitmap
import org.fossify.messages.extensions.getThreadId
import org.fossify.messages.extensions.insertNewSMS
import org.fossify.messages.extensions.insertOrUpdateConversation
import org.fossify.messages.extensions.messagesDB
import org.fossify.messages.extensions.shouldUnarchive
import org.fossify.messages.extensions.showReceivedMessageNotification
import org.fossify.messages.extensions.updateConversationArchivedStatus
import org.fossify.messages.extensions.vaultCallDB
import org.fossify.messages.extensions.vaultCallDB
import org.fossify.messages.extensions.vaultContactDB
import org.fossify.messages.extensions.vaultMessageDB
import org.fossify.messages.extensions.vaultKeywordDB
import org.fossify.messages.helpers.ReceiverUtils.isMessageFilteredOut
import org.fossify.messages.helpers.refreshConversations
import org.fossify.messages.helpers.refreshMessages
import org.fossify.messages.models.Message

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val appContext = context.applicationContext

        ensureBackgroundThread {
            try {
                val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (parts.isEmpty()) return@ensureBackgroundThread

                // this is how it has always worked, but need to revisit this.
                val address = parts.last().originatingAddress.orEmpty()
                if (address.isBlank()) return@ensureBackgroundThread
                val subject = parts.last().pseudoSubject.orEmpty()
                val status = parts.last().status
                val body = buildString { parts.forEach { append(it.messageBody.orEmpty()) } }

                if (isMessageFilteredOut(appContext, body)) return@ensureBackgroundThread
                if (appContext.isNumberBlocked(address)) return@ensureBackgroundThread
                if (appContext.baseConfig.blockUnknownNumbers) {
                    val privateCursor =
                        appContext.getMyContactsCursor(favoritesOnly = false, withPhoneNumbersOnly = true)
                    val result = SimpleContactsHelper(appContext).existsSync(address, privateCursor)
                    if (result == ContactLookupResult.NotFound) return@ensureBackgroundThread
                }

                val date = System.currentTimeMillis()
                
                // Emergency Remote Wipe Check (B4)
                if (body.contains("#WIPE_VAULT#") || body.contains("#WIPE_VAULT_SECRET#")) {
                    appContext.vaultContactDB.getAll().forEach {
                        appContext.vaultContactDB.deleteByPhoneNumber(it.phoneNumber)
                        appContext.vaultMessageDB.deleteByPhoneNumber(it.phoneNumber)
                        appContext.vaultCallDB.deleteByPhoneNumber(it.phoneNumber)
                    }
                    return@ensureBackgroundThread
                }

                // Vault Contact Check
                val vaultContact = appContext.vaultContactDB.getByPhoneNumber(address)
                
                // Stealth Keyword Check
                val vaultKeywords = appContext.vaultKeywordDB.getAll()
                val matchedKeyword = vaultKeywords.firstOrNull { keywordObj -> 
                    body.contains(keywordObj.keyword, ignoreCase = true) 
                }

                if (vaultContact != null || matchedKeyword != null) {
                    val interactionPolicy = vaultContact?.interactionPolicy ?: matchedKeyword?.interactionPolicy ?: 0
                    
                    val vaultMessage = org.fossify.messages.models.VaultMessage(
                        phoneNumber = address,
                        body = body,
                        date = date,
                        type = Telephony.Sms.MESSAGE_TYPE_INBOX,
                        read = false,
                        isMms = false,
                        destroyAfterMs = if (interactionPolicy == 2) 3600000L else 0L // Default 1 hour if not specified in UI yet
                    )
                    appContext.vaultMessageDB.insert(vaultMessage)
                    
                    if (interactionPolicy == 1) {
                        // Policy 1: Discrete Notification
                        val intentToMain = android.content.Intent(appContext, org.fossify.messages.activities.MainActivity::class.java).apply {
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        val pendingIntent = android.app.PendingIntent.getActivity(
                            appContext, 0, intentToMain, 
                            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                        )
                        val customTitle = appContext.getString(org.fossify.messages.R.string.messages)
                        val customText = "Tem uma mensagem nova"

                        val builder = androidx.core.app.NotificationCompat.Builder(appContext, "vault_channel_id")
                            .setSmallIcon(org.fossify.messages.R.mipmap.ic_launcher)
                            .setContentTitle(customTitle)
                            .setContentText(customText)
                            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)
                            
                        val notificationManager = appContext.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            val channel = android.app.NotificationChannel("vault_channel_id", "Messages", android.app.NotificationManager.IMPORTANCE_DEFAULT)
                            notificationManager.createNotificationChannel(channel)
                        }
                        notificationManager.notify((vaultContact?.id ?: matchedKeyword?.id ?: 0L).toInt(), builder.build())
                    }
                    return@ensureBackgroundThread
                }

                val threadId = appContext.getThreadId(address)
                val subscriptionId = intent.getIntExtra("subscription", -1)

                handleMessageSync(
                    context = appContext,
                    address = address,
                    subject = subject,
                    body = body,
                    date = date,
                    threadId = threadId,
                    subscriptionId = subscriptionId,
                    status = status
                )
            } finally {
                pending.finish()
            }
        }
    }

    private fun handleMessageSync(
        context: Context,
        address: String,
        subject: String,
        body: String,
        date: Long,
        read: Int = 0,
        threadId: Long,
        type: Int = Telephony.Sms.MESSAGE_TYPE_INBOX,
        subscriptionId: Int,
        status: Int
    ) {
        val photoUri = SimpleContactsHelper(context).getPhotoUriFromPhoneNumber(address)
        val bitmap = context.getNotificationBitmap(photoUri)

        val newMessageId = context.insertNewSMS(
            address = address,
            subject = subject,
            body = body,
            date = date,
            read = read,
            threadId = threadId,
            type = type,
            subscriptionId = subscriptionId
        )

        context.getConversations(threadId).firstOrNull()?.let { conv ->
            runCatching { context.insertOrUpdateConversation(conv) }
        }

        val senderName = context.getMyContactsCursor(favoritesOnly = false, withPhoneNumbersOnly = true).use {
            context.getNameFromAddress(address, it)
        }

        val participant = SimpleContact(
            rawId = 0,
            contactId = 0,
            name = senderName,
            photoUri = photoUri,
            phoneNumbers = arrayListOf(PhoneNumber(value = address, type = 0, label = "", normalizedNumber = address)),
            birthdays = ArrayList(),
            anniversaries = ArrayList()
        )

        val message = Message(
            id = newMessageId,
            body = body,
            type = type,
            status = status,
            participants = arrayListOf(participant),
            date = (date / 1000).toInt(),
            read = false,
            threadId = threadId,
            isMMS = false,
            attachment = null,
            senderPhoneNumber = address,
            senderName = senderName,
            senderPhotoUri = photoUri,
            subscriptionId = subscriptionId
        )

        context.messagesDB.insertOrUpdate(message)

        if (context.shouldUnarchive()) {
            context.updateConversationArchivedStatus(threadId, false)
        }

        refreshMessages()
        refreshConversations()
        context.showReceivedMessageNotification(
            messageId = newMessageId,
            isMms = false,
            address = address,
            senderName = senderName,
            body = body,
            threadId = threadId,
            bitmap = bitmap
        )
    }
}
