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
        "https://contacts-sync.mathankumar.dev/"


    /**
     * Replace this with your authentication system.
     *
     * Prefer obtaining the token after login instead
     * of hardcoding production credentials here.
     */
    const val API_TOKEN = "4165018f7ba850981e40031586c982a27186495e57b319626d283befa488e943e9459dbb2a369cd7a77f5da66ce07ded1f23c0c1ec75943c08ec11b8a12def0539d29bcae87e0942f21a34bae3070b8b8eb08fec49ea7940d652a46410ead98cb75a3ee843976ad92feb882ac256b52c2d649a3248b7f700a51da2438c98b642"
}