package dev.mathankumar.android.contactsync.contacts


import android.content.Context
import android.provider.ContactsContract

object ContactReader {

    fun readContacts(
        context: Context
    ): List<DeviceContact> {

        val resolver =
            context.contentResolver


        val baseContacts =
            mutableMapOf<Long, BaseContact>()


        /*
         * =============================================
         * CONTACTS
         * =============================================
         */
        val contactProjection =
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.LOOKUP_KEY,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP
            )


        resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            contactProjection,
            null,
            null,
            null
        )?.use { cursor ->

            val idIndex =
                cursor.getColumnIndexOrThrow(
                    ContactsContract.Contacts._ID
                )

            val lookupIndex =
                cursor.getColumnIndexOrThrow(
                    ContactsContract.Contacts.LOOKUP_KEY
                )

            val nameIndex =
                cursor.getColumnIndexOrThrow(
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
                )

            val updatedIndex =
                cursor.getColumnIndexOrThrow(
                    ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP
                )


            while (cursor.moveToNext()) {

                val contactId =
                    cursor.getLong(idIndex)

                val lookupKey =
                    cursor.getString(lookupIndex)

                val displayName =
                    cursor.getString(nameIndex)

                val lastUpdated =
                    cursor.getLong(updatedIndex)


                baseContacts[contactId] =
                    BaseContact(
                        contactId = contactId,
                        lookupKey = lookupKey,
                        displayName = displayName,
                        lastUpdatedTimestamp = lastUpdated
                    )
            }
        }


        /*
         * =============================================
         * PHONE NUMBERS
         * =============================================
         */

        val phoneMap =
            mutableMapOf<Long, MutableList<ContactPhone>>()


        val phoneProjection =
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.LABEL
            )


        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            phoneProjection,
            null,
            null,
            null
        )?.use { cursor ->

            val contactIdIndex =
                cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                )

            val numberIndex =
                cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )

            val normalizedIndex =
                cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER
                )

            val typeIndex =
                cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Phone.TYPE
                )

            val labelIndex =
                cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Phone.LABEL
                )


            while (cursor.moveToNext()) {

                val contactId =
                    cursor.getLong(contactIdIndex)

                /*
                 * Contact may have disappeared between queries.
                 */
                if (!baseContacts.containsKey(contactId)) {
                    continue
                }


                val number =
                    cursor.getString(numberIndex)
                        ?: continue


                val normalized =
                    cursor.getString(
                        normalizedIndex
                    )


                val type =
                    cursor.getInt(typeIndex)


                val customLabel =
                    cursor.getString(labelIndex)


                val displayType =
                    ContactsContract.CommonDataKinds.Phone
                        .getTypeLabel(
                            context.resources,
                            type,
                            customLabel
                        )
                        .toString()


                phoneMap
                    .getOrPut(contactId) {
                        mutableListOf()
                    }
                    .add(
                        ContactPhone(
                            number = number,
                            normalizedNumber = normalized,
                            type = displayType
                        )
                    )
            }
        }


        /*
         * =============================================
         * EMAIL ADDRESSES
         * =============================================
         */

        val emailMap =
            mutableMapOf<Long, MutableList<ContactEmail>>()


        val emailProjection =
            arrayOf(
                ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.TYPE,
                ContactsContract.CommonDataKinds.Email.LABEL
            )


        resolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            emailProjection,
            null,
            null,
            null
        )?.use { cursor ->

            val contactIdIndex =
                cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Email.CONTACT_ID
                )

            val emailIndex =
                cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Email.ADDRESS
                )

            val typeIndex =
                cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Email.TYPE
                )

            val labelIndex =
                cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Email.LABEL
                )


            while (cursor.moveToNext()) {

                val contactId =
                    cursor.getLong(
                        contactIdIndex
                    )


                if (!baseContacts.containsKey(contactId)) {
                    continue
                }


                val email =
                    cursor.getString(emailIndex)
                        ?: continue


                val type =
                    cursor.getInt(typeIndex)


                val customLabel =
                    cursor.getString(labelIndex)


                val displayType =
                    ContactsContract.CommonDataKinds.Email
                        .getTypeLabel(
                            context.resources,
                            type,
                            customLabel
                        )
                        .toString()


                emailMap
                    .getOrPut(contactId) {
                        mutableListOf()
                    }
                    .add(
                        ContactEmail(
                            email = email,
                            type = displayType
                        )
                    )
            }
        }


        /*
         * =============================================
         * ORGANIZATION / DESIGNATION
         * =============================================
         */

        val organizationMap =
            mutableMapOf<Long, OrganizationInfo>()


        val organizationProjection =
            arrayOf(
                ContactsContract.Data.CONTACT_ID,

                ContactsContract.CommonDataKinds.Organization.COMPANY,

                ContactsContract.CommonDataKinds.Organization.TITLE
            )


        val selection =
            "${ContactsContract.Data.MIMETYPE} = ?"


        val selectionArgs =
            arrayOf(
                ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE
            )


        resolver.query(
            ContactsContract.Data.CONTENT_URI,
            organizationProjection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->

            val contactIdIndex =
                cursor.getColumnIndexOrThrow(
                    ContactsContract.Data.CONTACT_ID
                )


            val companyIndex =
                cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Organization.COMPANY
                )


            val titleIndex =
                cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Organization.TITLE
                )


            while (cursor.moveToNext()) {

                val contactId =
                    cursor.getLong(
                        contactIdIndex
                    )


                if (!baseContacts.containsKey(contactId)) {
                    continue
                }


                val company =
                    cursor.getString(
                        companyIndex
                    )


                val designation =
                    cursor.getString(
                        titleIndex
                    )


                /*
                 * For a basic contact sync application,
                 * keep the first organization row.
                 */
                if (
                    !organizationMap.containsKey(
                        contactId
                    )
                ) {

                    organizationMap[contactId] =
                        OrganizationInfo(
                            company = company,
                            designation = designation
                        )
                }
            }
        }


        /*
         * =============================================
         * COMBINE EVERYTHING
         * =============================================
         */

        return baseContacts
            .values
            .map { base ->

                val organization =
                    organizationMap[
                        base.contactId
                    ]


                DeviceContact(

                    contactId =
                        base.contactId,

                    lookupKey =
                        base.lookupKey,

                    displayName =
                        base.displayName,

                    phones =
                        phoneMap[
                            base.contactId
                        ]
                            ?.distinct()
                            ?: emptyList(),

                    emails =
                        emailMap[
                            base.contactId
                        ]
                            ?.distinct()
                            ?: emptyList(),

                    company =
                        organization?.company,

                    designation =
                        organization?.designation,

                    lastUpdatedTimestamp =
                        base.lastUpdatedTimestamp
                )
            }
            .sortedBy {
                it.contactId
            }
    }


    private data class BaseContact(

        val contactId: Long,

        val lookupKey: String?,

        val displayName: String?,

        val lastUpdatedTimestamp: Long
    )


    private data class OrganizationInfo(

        val company: String?,

        val designation: String?
    )
}