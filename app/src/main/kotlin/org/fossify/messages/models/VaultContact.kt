package org.fossify.messages.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_contacts")
data class VaultContact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "phone_number") val phoneNumber: String,
    @ColumnInfo(name = "added_date") val addedDate: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "interaction_policy") val interactionPolicy: Int = 0 // 0=Furtivo, 1=Discreto, 2=Autodestruição
)
