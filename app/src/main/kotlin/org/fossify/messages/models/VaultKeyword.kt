package org.fossify.messages.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_keywords")
data class VaultKeyword(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "keyword") val keyword: String,
    @ColumnInfo(name = "interaction_policy") val interactionPolicy: Int = 0 // 0=Furtivo, 1=Discreto, 2=Autodestruição
)
