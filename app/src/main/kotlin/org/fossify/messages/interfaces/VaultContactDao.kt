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

    @Query("SELECT vault_contacts.*, (SELECT body FROM vault_messages WHERE vault_messages.phone_number = vault_contacts.phone_number ORDER BY date DESC LIMIT 1) AS snippet, (SELECT date FROM vault_messages WHERE vault_messages.phone_number = vault_contacts.phone_number ORDER BY date DESC LIMIT 1) AS date, (SELECT COUNT(*) FROM vault_messages WHERE vault_messages.phone_number = vault_contacts.phone_number AND read = 0) AS unreadCount FROM vault_contacts ORDER BY date DESC")
    fun getAllWithSnippets(): List<org.fossify.messages.models.VaultContactWithSnippet>

    @Query("SELECT * FROM vault_contacts WHERE phone_number = :phoneNumber LIMIT 1")
    fun getByPhoneNumber(phoneNumber: String): VaultContact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(contact: VaultContact): Long

    @Query("DELETE FROM vault_contacts WHERE phone_number = :phoneNumber")
    fun deleteByPhoneNumber(phoneNumber: String)
}
