package org.fossify.messages.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.fossify.messages.models.VaultContact

@Dao
interface VaultContactDao {
    @Query("SELECT * FROM vault_contacts")
    fun getAll(): List<VaultContact>

    @Query("SELECT * FROM vault_contacts WHERE phone_number = :phoneNumber LIMIT 1")
    fun getByPhoneNumber(phoneNumber: String): VaultContact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(contact: VaultContact): Long

    @Query("DELETE FROM vault_contacts WHERE phone_number = :phoneNumber")
    fun deleteByPhoneNumber(phoneNumber: String)
}
