package org.fossify.messages.providers

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import androidx.sqlite.db.SimpleSQLiteQuery
import org.fossify.messages.databases.MessagesDatabase
import org.fossify.messages.models.VaultContact

class VaultFilterProvider : ContentProvider() {

    private lateinit var db: MessagesDatabase

    override fun onCreate(): Boolean {
        context?.let {
            db = MessagesDatabase.getInstance(it)
            return true
        }
        return false
    }

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor {
        val supportDb = db.openHelper.readableDatabase
        val queryStr = buildString {
            append("SELECT * FROM vault_contacts")
            if (!selection.isNullOrEmpty()) {
                append(" WHERE $selection")
            }
            if (!sortOrder.isNullOrEmpty()) {
                append(" ORDER BY $sortOrder")
            }
        }
        return supportDb.query(SimpleSQLiteQuery(queryStr, selectionArgs))
    }

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (values == null) return null
        val phoneNumber = values.getAsString("phone_number") ?: return null
        val name = values.getAsString("name") ?: "Desconhecido (Phone)"
        val policy = values.getAsInteger("interaction_policy") ?: 0
        val group = values.getAsString("group_name") ?: "Telefone"
        val blockCalls = values.getAsBoolean("block_calls") ?: true
        
        val contact = VaultContact(
            name = name,
            phoneNumber = phoneNumber,
            interactionPolicy = policy,
            groupName = group,
            blockCalls = blockCalls
        )
        val id = db.VaultContactDao().insert(contact)
        return Uri.withAppendedPath(uri, id.toString())
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val supportDb = db.openHelper.writableDatabase
        val queryStr = buildString {
            append("DELETE FROM vault_contacts")
            if (!selection.isNullOrEmpty()) {
                append(" WHERE $selection")
            }
        }
        val stmt = supportDb.compileStatement(queryStr)
        selectionArgs?.forEachIndexed { index, arg ->
            stmt.bindString(index + 1, arg)
        }
        return stmt.executeUpdateDelete()
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        if (values == null || selection.isNullOrEmpty()) return 0
        val supportDb = db.openHelper.writableDatabase
        
        val setClauses = mutableListOf<String>()
        val bindArgs = mutableListOf<Any>()
        
        for (key in values.keySet()) {
            setClauses.add("$key = ?")
            bindArgs.add(values.get(key)!!)
        }
        
        val queryStr = "UPDATE vault_contacts SET ${setClauses.joinToString(", ")} WHERE $selection"
        val stmt = supportDb.compileStatement(queryStr)
        
        var bindIndex = 1
        for (arg in bindArgs) {
            when (arg) {
                is String -> stmt.bindString(bindIndex++, arg)
                is Int -> stmt.bindLong(bindIndex++, arg.toLong())
                is Long -> stmt.bindLong(bindIndex++, arg)
                is Boolean -> stmt.bindLong(bindIndex++, if (arg) 1L else 0L)
                else -> stmt.bindString(bindIndex++, arg.toString())
            }
        }
        selectionArgs?.forEach { arg ->
            stmt.bindString(bindIndex++, arg)
        }
        
        return stmt.executeUpdateDelete()
    }
}
