package dev.mathankumar.android.contactsync

import android.accounts.AccountManager
import android.content.ContentResolver
import android.content.Context
import android.util.Log
import dev.mathankumar.android.contactsync.constants.AppConstants

object SyncDebug {

    private const val TAG =
        "SYNC_DEBUG"


    const val ACCOUNT_TYPE = AppConstants.ACCOUNT_TYPE

    const val AUTHORITY = AppConstants.CONTACT_AUTHORITY


    fun dump(
        context: Context
    ) {

        Log.e(
            TAG,
            "========================================"
        )

        Log.e(
            TAG,
            "CONTACT SYNC DEBUG"
        )


        /*
         * -----------------------------------------
         * ACCOUNT
         * -----------------------------------------
         */

        val accounts =
            AccountManager
                .get(context)
                .getAccountsByType(
                    ACCOUNT_TYPE
                )


        Log.e(
            TAG,
            "Accounts found = ${accounts.size}"
        )


        accounts.forEach { account ->

            Log.e(
                TAG,
                "ACCOUNT NAME = ${account.name}"
            )

            Log.e(
                TAG,
                "ACCOUNT TYPE = ${account.type}"
            )


            val syncable =
                ContentResolver.getIsSyncable(
                    account,
                    AUTHORITY
                )


            val automatic =
                ContentResolver
                    .getSyncAutomatically(
                        account,
                        AUTHORITY
                    )


            Log.e(
                TAG,
                "IS SYNCABLE = $syncable"
            )

            Log.e(
                TAG,
                "AUTO SYNC = $automatic"
            )
        }


        /*
         * -----------------------------------------
         * SYNC ADAPTER
         * -----------------------------------------
         */

        val allAdapters =
            ContentResolver
                .getSyncAdapterTypes()


        Log.e(
            TAG,
            "Installed sync adapters = ${allAdapters.size}"
        )


        val ours =
            allAdapters.filter {

                it.accountType ==
                        ACCOUNT_TYPE
            }


        Log.e(
            TAG,
            "Our sync adapters = ${ours.size}"
        )


        ours.forEach { adapter ->

            Log.e(
                TAG,
                "----------------------------------------"
            )

            Log.e(
                TAG,
                "AUTHORITY = ${adapter.authority}"
            )

            Log.e(
                TAG,
                "ACCOUNT TYPE = ${adapter.accountType}"
            )

            Log.e(
                TAG,
                "USER VISIBLE = ${adapter.isUserVisible}"
            )

            Log.e(
                TAG,
                "SUPPORTS UPLOAD = ${adapter.supportsUploading()}"
            )
        }


        val exactMatch =
            ours.any {

                it.authority ==
                        AUTHORITY &&
                        it.accountType ==
                        ACCOUNT_TYPE
            }


        Log.e(
            TAG,
            "EXACT ADAPTER MATCH = $exactMatch"
        )


        Log.e(
            TAG,
            "MASTER SYNC = ${
                ContentResolver
                    .getMasterSyncAutomatically()
            }"
        )


        Log.e(
            TAG,
            "========================================"
        )
    }
}