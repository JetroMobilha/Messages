package org.fossify.messages.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.fossify.messages.models.VaultKeyword

@Dao
interface VaultKeywordDao {
    @Query("SELECT * FROM vault_keywords ORDER BY keyword ASC")
    fun getAll(): List<VaultKeyword>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(keyword: VaultKeyword): Long

    @Query("DELETE FROM vault_keywords WHERE id = :id")
    fun delete(id: Long)
}
