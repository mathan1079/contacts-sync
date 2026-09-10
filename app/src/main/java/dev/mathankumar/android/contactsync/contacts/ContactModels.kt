package dev.mathankumar.android.contactsync.contacts

import androidx.annotation.Keep

@Keep
data class DeviceContact(
    val contactId: Long,
    val lookupKey: String?,
    val displayName: String?,
    val phones: List<ContactPhone>,
    val emails: List<ContactEmail>,
    val company: String?,
    val designation: String?,
    val lastUpdatedTimestamp: Long
)

@Keep
data class ContactPhone(
    val number: String,
    val normalizedNumber: String?,
    val type: String
)

@Keep
data class ContactEmail(
    val email: String,
    val type: String
)