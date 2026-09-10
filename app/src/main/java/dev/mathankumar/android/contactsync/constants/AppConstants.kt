package dev.mathankumar.android.contactsync.constants

import android.provider.ContactsContract

object AppConstants {

    /**
     * This MUST match:
     *
     * authenticator.xml
     * syncadapter.xml
     */
    const val ACCOUNT_TYPE =
        "dev.mathankumar.android.contactsync.account"

    const val ACCOUNT_NAME =
        "Mathan Contact Sync"


    /**
     * Android Contacts Provider.
     */
    const val CONTACT_AUTHORITY =
        ContactsContract.AUTHORITY


    /**
     * Around 1 hour.
     *
     * addPeriodicSync expects seconds.
     */
    const val SYNC_INTERVAL_SECONDS =
        60L * 60L


    /**
     * ====================================================
     * CHANGE THIS
     * ====================================================
     *
     * Retrofit requires trailing "/"
     *
     * Example:
     *
     * https://api.yourcompany.com/
     */
    const val API_BASE_URL =
        "https://your-server.com/"


    /**
     * Replace this with your authentication system.
     *
     * Prefer obtaining the token after login instead
     * of hardcoding production credentials here.
     */
    const val API_TOKEN = ""
}