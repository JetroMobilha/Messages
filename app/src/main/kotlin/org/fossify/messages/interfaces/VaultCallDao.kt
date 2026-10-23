package org.fossify.messages.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.fossify.messages.models.VaultCall

@Dao
interface VaultCallDao {
    @Query("SELECT * FROM vault_calls ORDER BY date DESC")
    fun getAll(): List<VaultCall>

    @Query("SELECT * FROM vault_calls WHERE phone_number = :phoneNumber ORDER BY date DESC")
    fun getCallsByPhoneNumber(phoneNumber: String): List<VaultCall>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(call: VaultCall): Long

    @Query("DELETE FROM vault_calls WHERE phone_number = :phoneNumber")
    fun deleteByPhoneNumber(phoneNumber: String)
}
