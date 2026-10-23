package org.fossify.messages.models

import com.google.gson.annotations.SerializedName

data class VaultExportPackage(
    @SerializedName("username") val username: String,
    @SerializedName("export_timestamp") val exportTimestamp: Long = System.currentTimeMillis(),
    @SerializedName("sms") val sms: List<SmsBackup>? = null,
    @SerializedName("mms") val mms: List<MmsBackup>? = null,
    @SerializedName("vault_contacts") val vaultContacts: List<VaultContact>? = null,
    @SerializedName("vault_messages") val vaultMessages: List<VaultMessage>? = null,
    @SerializedName("vault_calls") val vaultCalls: List<VaultCall>? = null
)
