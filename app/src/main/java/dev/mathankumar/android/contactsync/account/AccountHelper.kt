package dev.mathankumar.android.contactsync.account

import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentResolver
import android.content.Context
import android.os.Bundle

object AccountHelper {

    const val ACCOUNT_TYPE =
        "dev.mathankumar.android.contactsync"

    const val ACCOUNT_NAME =
        "Mathan Contact Sync"

    const val AUTHORITY =
        "com.android.contacts"


    fun getAccount(
        context: Context
    ): Account? {

        return AccountManager
            .get(context)
            .getAccountsByType(
                ACCOUNT_TYPE
            )
            .firstOrNull()
    }


    fun createAccount(
        context: Context
    ): Account {

        /*
         * Existing account.
         *
         * IMPORTANT:
         * Do NOT reset setSyncAutomatically() here.
         */
        getAccount(context)?.let { existing ->

            ContentResolver.setIsSyncable(
                existing,
                AUTHORITY,
                1
            )

            ensurePeriodicSync(
                existing
            )

            return existing
        }


        val account =
            Account(
                ACCOUNT_NAME,
                ACCOUNT_TYPE
            )


        val accountManager =
            AccountManager.get(
                context
            )


        val added =
            accountManager.addAccountExplicitly(
                account,
                null,
                null
            )


        if (!added) {

            getAccount(context)?.let {
                return it
            }

            throw IllegalStateException(
                "Could not create Contact Sync account"
            )
        }


        /*
         * Tell Android this account supports
         * com.android.contacts synchronization.
         */
        ContentResolver.setIsSyncable(
            account,
            AUTHORITY,
            1
        )


        /*
         * Initially OFF.
         *
         * User can enable it from Android Settings.
         */
        ContentResolver.setSyncAutomatically(
            account,
            AUTHORITY,
            true
        )


        ensurePeriodicSync(
            account
        )


        return account
    }


    private fun ensurePeriodicSync(
        account: Account
    ) {

        ContentResolver.removePeriodicSync(
            account,
            AUTHORITY,
            Bundle.EMPTY
        )


        ContentResolver.addPeriodicSync(
            account,
            AUTHORITY,
            Bundle.EMPTY,
            60L * 60L
        )
    }


    fun isAccountSyncEnabled(
        context: Context
    ): Boolean {

        val account =
            getAccount(context)
                ?: return false


        return ContentResolver
            .getSyncAutomatically(
                account,
                AUTHORITY
            )
    }


    fun isContactSyncEnabled(
        context: Context
    ): Boolean {

        val account =
            getAccount(context)
                ?: return false


        val accountSync =
            ContentResolver
                .getSyncAutomatically(
                    account,
                    AUTHORITY
                )


        val masterSync =
            ContentResolver
                .getMasterSyncAutomatically()


        return accountSync &&
                masterSync
    }


    fun getSyncableState(
        context: Context
    ): Int {

        val account =
            getAccount(context)
                ?: return 0


        return ContentResolver.getIsSyncable(
            account,
            AUTHORITY
        )
    }


    fun requestSync(
        context: Context
    ) {

        val account =
            getAccount(context)
                ?: return


        if (
            !isContactSyncEnabled(
                context
            )
        ) {
            return
        }


        ContentResolver.requestSync(
            account,
            AUTHORITY,
            Bundle.EMPTY
        )
    }


    fun requestImmediateSync(
        context: Context
    ) {

        val account =
            getAccount(context)
                ?: return


        if (
            !isContactSyncEnabled(
                context
            )
        ) {
            return
        }


        val extras =
            Bundle().apply {

                putBoolean(
                    ContentResolver.SYNC_EXTRAS_MANUAL,
                    true
                )

                putBoolean(
                    ContentResolver.SYNC_EXTRAS_EXPEDITED,
                    true
                )
            }


        ContentResolver.requestSync(
            account,
            AUTHORITY,
            extras
        )
    }
}