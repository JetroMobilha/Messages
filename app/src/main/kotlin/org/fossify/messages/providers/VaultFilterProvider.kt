package org.fossify.messages.providers

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
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
    ): Cursor? {
        // Expor todos os contactos ou procurar por phoneNumber se houver URI match
        // Note: For simplicity, returning a MatrixCursor or accessing SQLite directly is required for true ContentProvider Cursor.
        // A full ContentProvider to Room interface is complex, but we'll use raw queries for the Phone app
        val supportDb = db.openHelper.readableDatabase
        val query = "SELECT phone_number, block_calls FROM vault_contacts"
        return supportDb.query(androidx.sqlite.db.SimpleSQLiteQuery(query))
    }

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val phoneNumber = values?.getAsString("phone_number") ?: return null
        
        val contact = VaultContact(
            name = "Telefone: $phoneNumber",
            phoneNumber = phoneNumber,
            interactionPolicy = 0,
            groupName = "Bloqueado pelo Phone",
            blockCalls = true
        )
        val id = db.VaultContactDao().insert(contact)
        return Uri.withAppendedPath(uri, id.toString())
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
