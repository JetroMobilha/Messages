package org.fossify.messages.models

import androidx.room.Embedded

data class VaultContactWithSnippet(
    @Embedded val contact: VaultContact,
    val snippet: String?,
    val date: Long?,
    val unreadCount: Int = 0
)
