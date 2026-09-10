package dev.mathankumar.android.contactsync.observer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import dev.mathankumar.android.contactsync.account.AccountHelper

object ContactChangeObserver {

    private const val DEBOUNCE_TIME_MS =
        15_000L


    private var registered =
        false


    private var applicationContext:
            Context? = null


    private val handler =
        Handler(
            Looper.getMainLooper()
        )


    private val syncRunnable =
        Runnable {

            val context =
                applicationContext
                    ?: return@Runnable


            /*
             * Do not override user's Android
             * Contacts sync preference.
             */
            if (
                AccountHelper.isContactSyncEnabled(
                    context
                )
            ) {

                AccountHelper.requestSync(
                    context
                )
            }
        }


    private val observer =
        object : ContentObserver(
            handler
        ) {

            override fun onChange(
                selfChange: Boolean
            ) {

                super.onChange(
                    selfChange
                )

                scheduleSync()
            }


            override fun onChange(
                selfChange: Boolean,
                uri: Uri?
            ) {

                super.onChange(
                    selfChange,
                    uri
                )

                scheduleSync()
            }
        }


    @Synchronized
    fun register(
        context: Context
    ) {

        if (registered) {
            return
        }


        if (
            context.checkSelfPermission(
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }


        applicationContext =
            context.applicationContext


        val resolver =
            context.contentResolver


        /*
         * Aggregated contacts.
         */
        resolver.registerContentObserver(

            ContactsContract
                .Contacts
                .CONTENT_URI,

            true,

            observer
        )


        /*
         * Phone / email / organization data.
         */
        resolver.registerContentObserver(

            ContactsContract
                .Data
                .CONTENT_URI,

            true,

            observer
        )


        /*
         * Raw contact changes and deletions.
         */
        resolver.registerContentObserver(

            ContactsContract
                .RawContacts
                .CONTENT_URI,

            true,

            observer
        )


        registered =
            true
    }


    private fun scheduleSync() {

        handler.removeCallbacks(
            syncRunnable
        )


        /*
         * If the Contacts app makes ten DB modifications
         * while creating one contact, don't initiate ten
         * server requests.
         *
         * Wait 15 seconds after the latest modification.
         */
        handler.postDelayed(
            syncRunnable,
            DEBOUNCE_TIME_MS
        )
    }
}