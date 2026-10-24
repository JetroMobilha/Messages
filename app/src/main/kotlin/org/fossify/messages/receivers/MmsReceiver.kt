package org.fossify.messages.receivers

import android.content.Context
import android.net.Uri
import com.bumptech.glide.Glide
import com.klinker.android.send_message.MmsReceivedReceiver
import org.fossify.commons.extensions.baseConfig
import org.fossify.commons.extensions.getMyContactsCursor
import org.fossify.commons.extensions.isNumberBlocked
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.helpers.ContactLookupResult
import org.fossify.commons.helpers.SimpleContactsHelper
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.R
import org.fossify.messages.extensions.getConversations
import org.fossify.messages.extensions.getMMS
import org.fossify.messages.extensions.getLatestMMS
import org.fossify.messages.extensions.getNameFromAddress
import org.fossify.messages.extensions.insertOrUpdateConversation
import org.fossify.messages.extensions.shouldUnarchive
import org.fossify.messages.extensions.showReceivedMessageNotification
import org.fossify.messages.extensions.updateConversationArchivedStatus
import org.fossify.messages.extensions.vaultContactDB
import org.fossify.messages.extensions.vaultKeywordDB
import org.fossify.messages.extensions.vaultMessageDB
import org.fossify.messages.helpers.ReceiverUtils.isMessageFilteredOut
import org.fossify.messages.helpers.refreshConversations
import org.fossify.messages.helpers.refreshMessages
import org.fossify.messages.models.Message

class MmsReceiver : MmsReceivedReceiver() {

    override fun isAddressBlocked(context: Context, address: String): Boolean {
        if (context.isNumberBlocked(address)) return true
        if (context.baseConfig.blockUnknownNumbers) {
            val privateCursor = context.getMyContactsCursor(favoritesOnly = false, withPhoneNumbersOnly = true)
            val result = SimpleContactsHelper(context).existsSync(address, privateCursor)
            return result == ContactLookupResult.NotFound
        }

        return false
    }

    override fun isContentBlocked(context: Context, content: String): Boolean {
        return isMessageFilteredOut(context, content)
    }

    override fun onMessageReceived(context: Context, messageUri: Uri?) {
        val mms = if (messageUri != null) {
            context.getMMS(messageUri)
        } else {
            context.getLatestMMS()
        } ?: return

        val address = mms.getSender()?.phoneNumbers?.firstOrNull()?.normalizedNumber ?: mms.senderPhoneNumber
        
        ensureBackgroundThread {
            // Vault check
            val vaultContact = context.vaultContactDB.getByPhoneNumber(address)
            
            // Stealth Keyword Check
            val vaultKeywords = context.vaultKeywordDB.getAll()
            val mmsBody = mms.body.ifBlank { "MMS Attachment" }
            val matchedKeyword = vaultKeywords.firstOrNull { keywordObj -> 
                mmsBody.contains(keywordObj.keyword, ignoreCase = true) 
            }

            if (vaultContact != null || matchedKeyword != null) {
                val interactionPolicy = vaultContact?.interactionPolicy ?: matchedKeyword?.interactionPolicy ?: 0

                val vaultMessage = org.fossify.messages.models.VaultMessage(
                    phoneNumber = address,
                    body = mmsBody,
                    date = System.currentTimeMillis(),
                    type = android.provider.Telephony.Sms.MESSAGE_TYPE_INBOX,
                    read = false,
                    isMms = true,
                    destroyAfterMs = if (interactionPolicy == 2) 3600000L else 0L
                )
                context.vaultMessageDB.insert(vaultMessage)

                if (interactionPolicy == 1) {
                    val intentToMain = android.content.Intent(context, org.fossify.messages.activities.MainActivity::class.java).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    val pendingIntent = android.app.PendingIntent.getActivity(
                        context, 0, intentToMain, 
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    val builder = androidx.core.app.NotificationCompat.Builder(context, "vault_channel_id")
                        .setSmallIcon(org.fossify.messages.R.mipmap.ic_launcher)
                        .setContentTitle(context.getString(org.fossify.messages.R.string.messages))
                        .setContentText("Tem uma mensagem nova")
                        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        
                    val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        val channel = android.app.NotificationChannel("vault_channel_id", "Messages", android.app.NotificationManager.IMPORTANCE_DEFAULT)
                        notificationManager.createNotificationChannel(channel)
                    }
                    notificationManager.notify((vaultContact?.id ?: matchedKeyword?.id ?: 0L).toInt(), builder.build())
                }
                return@ensureBackgroundThread
            }

            val size = context.resources.getDimension(R.dimen.notification_large_icon_size).toInt()
            handleMmsMessage(context, mms, size, address)
        }
    }

    override fun onError(context: Context, error: String) {
        context.showErrorToast(context.getString(R.string.couldnt_download_mms))
    }

    private fun handleMmsMessage(
        context: Context,
        mms: Message,
        size: Int,
        address: String
    ) {
        val glideBitmap = try {
            Glide.with(context)
                .asBitmap()
                .load(mms.attachment!!.attachments.first().getUri())
                .centerCrop()
                .into(size, size)
                .get()
        } catch (e: Exception) {
            null
        }


        val senderName = context.getMyContactsCursor(favoritesOnly = false, withPhoneNumbersOnly = true).use {
            context.getNameFromAddress(address, it)
        }

        context.showReceivedMessageNotification(
            messageId = mms.id,
            isMms = true,
            address = address,
            senderName = senderName,
            body = mms.body,
            threadId = mms.threadId,
            bitmap = glideBitmap
        )

        val conversation = context.getConversations(mms.threadId).firstOrNull() ?: return
        runCatching { context.insertOrUpdateConversation(conversation) }
        if (context.shouldUnarchive()) {
            context.updateConversationArchivedStatus(mms.threadId, false)
        }
        refreshMessages()
        refreshConversations()
    }
}
