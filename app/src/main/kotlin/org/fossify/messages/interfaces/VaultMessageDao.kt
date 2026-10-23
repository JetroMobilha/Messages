package org.fossify.messages.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.fossify.messages.models.VaultMessage

@Dao
interface VaultMessageDao {
    @Query("SELECT * FROM vault_messages ORDER BY date DESC")
    fun getAll(): List<VaultMessage>

    @Query("SELECT * FROM vault_messages WHERE phone_number = :phoneNumber ORDER BY date ASC")
    fun getMessagesByPhoneNumber(phoneNumber: String): List<VaultMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(message: VaultMessage): Long

    @Query("UPDATE vault_messages SET type = :type WHERE id = :id")
    fun updateType(id: Long, type: Int)

    @Query("UPDATE vault_messages SET read = 1, read_timestamp = :readTimestamp WHERE id = :id")
    fun markAsRead(id: Long, readTimestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM vault_messages WHERE destroy_after_ms > 0 AND read_timestamp > 0 AND (:currentTime - read_timestamp) >= destroy_after_ms")
    fun deleteExpiredMessages(currentTime: Long = System.currentTimeMillis())

    @Query("DELETE FROM vault_messages WHERE phone_number = :phoneNumber")
    fun deleteByPhoneNumber(phoneNumber: String)
}
