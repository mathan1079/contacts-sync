package dev.mathankumar.android.contactsync.sync

import android.Manifest
import android.accounts.Account
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.SyncResult
import android.content.pm.PackageManager
import android.os.Bundle
import dev.mathankumar.android.contactsync.contacts.ContactHasher
import dev.mathankumar.android.contactsync.contacts.ContactReader
import dev.mathankumar.android.contactsync.contacts.DeviceContact
import dev.mathankumar.android.contactsync.database.ContactSnapshotDatabase
import dev.mathankumar.android.contactsync.network.ApiClient
import dev.mathankumar.android.contactsync.network.ContactDto
import dev.mathankumar.android.contactsync.network.ContactSyncRequest
import dev.mathankumar.android.contactsync.network.DeviceIdProvider
import dev.mathankumar.android.contactsync.network.EmailDto
import dev.mathankumar.android.contactsync.network.PhoneDto

class ContactSyncAdapter(
    context: Context,
    autoInitialize: Boolean
) : AbstractThreadedSyncAdapter(
    context,
    autoInitialize
) {

    override fun onPerformSync(
        account: Account?,
        extras: Bundle?,
        authority: String?,
        provider: ContentProviderClient?,
        syncResult: SyncResult?
    ) {

        /*
         * =============================================
         * CONTACT PERMISSION
         * =============================================
         */

        if (
            context.checkSelfPermission(
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            syncResult
                ?.stats
                ?.numAuthExceptions =
                (syncResult
                    ?.stats
                    ?.numAuthExceptions ?: 0) + 1


            SyncStateStore.setFailure(
                context,
                "READ_CONTACTS permission is not granted."
            )

            return
        }


        val database =
            ContactSnapshotDatabase(
                context
            )
        try {

            /*
             * =============================================
             * READ ALL CURRENT CONTACTS
             * =============================================
             */

            val currentContacts =
                ContactReader.readContacts(
                    context
                )

            /*
             * =============================================
             * CREATE CURRENT HASH MAP
             * =============================================
             */

            val currentHashes =
                mutableMapOf<Long, String>()


            currentContacts.forEach { contact ->

                currentHashes[
                    contact.contactId
                ] =
                    ContactHasher.calculate(
                        contact
                    )
            }


            /*
             * =============================================
             * LOAD PREVIOUS SUCCESSFUL SNAPSHOT
             * =============================================
             */

            val previousHashes =
                database.loadSnapshot()


            /*
             * =============================================
             * NEW + UPDATED CONTACTS
             * =============================================
             */

            val upserts =
                mutableListOf<DeviceContact>()


            currentContacts.forEach { contact ->

                val currentHash =
                    currentHashes[
                        contact.contactId
                    ]
                        ?: return@forEach


                val previousHash =
                    previousHashes[
                        contact.contactId
                    ]


                /*
                 * New:
                 *
                 * previousHash == null
                 *
                 * Updated:
                 *
                 * previousHash != currentHash
                 */

                if (
                    previousHash == null ||
                    previousHash != currentHash
                ) {

                    upserts.add(
                        contact
                    )
                }
            }


            /*
             * =============================================
             * DELETED CONTACTS
             * =============================================
             */

            val deletedContactIds =
                previousHashes
                    .keys
                    .filter { previousId ->

                        !currentHashes
                            .containsKey(
                                previousId
                            )
                    }


            /*
             * =============================================
             * NOTHING CHANGED
             * =============================================
             */

            if (
                upserts.isEmpty() &&
                deletedContactIds.isEmpty()
            ) {

                /*
                 * Nothing needs uploading.
                 *
                 * Snapshot already matches device.
                 */

                SyncStateStore.setSuccess(
                    context = context,
                    upsertCount = 0,
                    deleteCount = 0
                )

                /*
                 * Signal success to the system so "Last synced" time updates.
                 */
                syncResult?.stats?.numInserts = 0

                return
            }


            /*
             * =============================================
             * CREATE API REQUEST
             * =============================================
             */

            val request =
                ContactSyncRequest(

                    deviceId =
                        DeviceIdProvider.get(
                            context
                        ),

                    syncTimestamp =
                        System.currentTimeMillis(),

                    upserts =
                        upserts.map { contact ->

                            contact.toDto()
                        },

                    deletedContactIds =
                        deletedContactIds
                )


            /*
             * =============================================
             * CALL SERVER
             * =============================================
             *
             * SyncAdapter is already executing on a
             * background worker thread.
             *
             * Therefore synchronous Retrofit execute()
             * is appropriate here.
             */

            val response =
                ApiClient
                    .api
                    .syncContacts(
                        request
                    )
                    .execute()


            /*
             * =============================================
             * HTTP FAILURE
             * =============================================
             */

            if (!response.isSuccessful) {

                val message =
                    "HTTP ${response.code()} ${response.message()}"


                SyncStateStore.setFailure(
                    context,
                    message
                )


                syncResult
                    ?.stats
                    ?.numIoExceptions =
                    (syncResult
                        ?.stats
                        ?.numIoExceptions ?: 0) + 1


                /*
                 * IMPORTANT:
                 *
                 * Do NOT save the local snapshot.
                 *
                 * Next Android sync retries everything
                 * that failed.
                 */
                return
            }


            val body =
                response.body()


            /*
             * =============================================
             * API APPLICATION FAILURE
             * =============================================
             */

            if (
                body == null ||
                !body.success
            ) {

                val message =
                    body?.message
                        ?: "Server returned an invalid response."


                SyncStateStore.setFailure(
                    context,
                    message
                )


                syncResult
                    ?.stats
                    ?.numIoExceptions =
                    (syncResult
                        ?.stats
                        ?.numIoExceptions ?: 0) + 1


                return
            }


            /*
             * =============================================
             * SUCCESS
             * =============================================
             *
             * NOW save device state.
             *
             * Only do this AFTER the server accepted
             * the synchronization.
             */

            database.replaceSnapshot(
                currentHashes
            )


            SyncStateStore.setSuccess(

                context = context,

                upsertCount =
                    upserts.size,

                deleteCount =
                    deletedContactIds.size
            )


            syncResult
                ?.stats
                ?.numUpdates =
                upserts.size.toLong()


            syncResult
                ?.stats
                ?.numDeletes =
                deletedContactIds.size.toLong()


        } catch (
            throwable: Throwable
        ) {

            throwable.printStackTrace()


            SyncStateStore.setFailure(

                context,

                throwable.message
                    ?: throwable.javaClass.simpleName
            )


            syncResult
                ?.stats
                ?.numIoExceptions =
                (syncResult
                    ?.stats
                    ?.numIoExceptions ?: 0) + 1

        } finally {

            database.close()
        }
    }


    private fun DeviceContact.toDto():
            ContactDto {

        return ContactDto(

            contactId =
                contactId,

            lookupKey =
                lookupKey,

            displayName =
                displayName,

            phones =
                phones.map { phone ->

                    PhoneDto(
                        number =
                            phone.number,

                        normalizedNumber =
                            phone.normalizedNumber,

                        type =
                            phone.type
                    )
                },

            emails =
                emails.map { email ->
                    EmailDto(
                        email =
                            email.email,

                        type =
                            email.type
                    )
                },

            company =
                company,

            designation =
                designation,

            lastUpdatedTimestamp =
                lastUpdatedTimestamp
        )
    }
}