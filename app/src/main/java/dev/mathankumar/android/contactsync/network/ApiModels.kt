package dev.mathankumar.android.contactsync.network

import androidx.annotation.Keep

@Keep
data class ContactSyncRequest(
    val deviceId: String,
    val syncTimestamp: Long,
    val upserts: List<ContactDto>,
    val deletedContactIds: List<Long>
)

@Keep
data class ContactDto(
    /**
     * ID on this Android device.
     *
     * The server should identify the contact using:
     *
     * deviceId + contactId
     */
    val contactId: Long,
    val lookupKey: String?,
    val displayName: String?,
    val phones: List<PhoneDto>,
    val emails: List<EmailDto>,
    val company: String?,
    val designation: String?,
    val lastUpdatedTimestamp: Long
)

@Keep
data class PhoneDto(
    val number: String,
    val normalizedNumber: String?,
    val type: String
)

@Keep
data class EmailDto(
    val email: String,
    val type: String
)

@Keep
data class ContactSyncResponse(
    val success: Boolean,
    val message: String? = null,
    val created: Int? = null,
    val updated: Int? = null,
    val deleted: Int? = null
)