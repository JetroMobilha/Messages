package org.fossify.messages.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_messages")
data class VaultMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "phone_number") val phoneNumber: String,
    @ColumnInfo(name = "body") val body: String,
    @ColumnInfo(name = "date") val date: Long,
    @ColumnInfo(name = "type") val type: Int, // e.g. Telephony.Sms.MESSAGE_TYPE_INBOX or MESSAGE_TYPE_SENT
    @ColumnInfo(name = "read") val read: Boolean = false,
    @ColumnInfo(name = "is_mms") val isMms: Boolean = false,
    @ColumnInfo(name = "read_timestamp") val readTimestamp: Long = 0,
    @ColumnInfo(name = "destroy_after_ms") val destroyAfterMs: Long = 0, // 0 = Never destroy
    @ColumnInfo(name = "subscription_id") val subscriptionId: Int = -1
)
