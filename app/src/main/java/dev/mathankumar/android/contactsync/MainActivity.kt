package dev.mathankumar.android.contactsync

import android.Manifest
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import dev.mathankumar.android.contactsync.account.AccountHelper
import dev.mathankumar.android.contactsync.observer.ContactChangeObserver
import dev.mathankumar.android.contactsync.sync.SyncStateStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    companion object {

        const val EXTRA_FROM_ACCOUNT_AUTHENTICATOR =
            "from_account_authenticator"


        private const val CONTACT_PERMISSION_REQUEST =
            1001
    }


    private lateinit var statusText:
            TextView


    private var authenticatorResponse:
            AccountAuthenticatorResponse? = null


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        setContentView(
            R.layout.activity_main
        )


        statusText =
            findViewById(
                R.id.statusText
            )


        val createAccountButton =
            findViewById<Button>(
                R.id.createAccountButton
            )


        val openSettingsButton =
            findViewById<Button>(
                R.id.openSettingsButton
            )


        val syncNowButton =
            findViewById<Button>(
                R.id.syncNowButton
            )


        readAuthenticatorResponse()


        createAccountButton
            .setOnClickListener {

                startAccountSetup()
            }


        openSettingsButton
            .setOnClickListener {

                openSyncSettings()
            }


        syncNowButton
            .setOnClickListener {

                syncNow()
            }


        /*
         * If Android Settings called our authenticator,
         * immediately continue account setup.
         */
        if (
            intent.getBooleanExtra(
                EXTRA_FROM_ACCOUNT_AUTHENTICATOR,
                false
            )
        ) {

            startAccountSetup()
        }


        updateStatus()
        SyncDebug.dump(this)
    }


    override fun onResume() {
        super.onResume()

        /*
         * User may have just returned from Android
         * sync settings after toggling Contacts.
         */
        updateStatus()
    }


    @Suppress("DEPRECATION")
    private fun readAuthenticatorResponse() {

        authenticatorResponse =
            intent.getParcelableExtra(
                AccountManager
                    .KEY_ACCOUNT_AUTHENTICATOR_RESPONSE
            )


        authenticatorResponse
            ?.onRequestContinued()
    }


    private fun startAccountSetup() {

        /*
         * READ_CONTACTS must still be runtime-granted.
         *
         * Account synchronization toggle and Android
         * permission are two different things.
         */
        if (
            checkSelfPermission(
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_CONTACTS
                ),
                CONTACT_PERMISSION_REQUEST
            )

            return
        }


        finishAccountSetup()
    }


    private fun finishAccountSetup() {

        try {

            val account =
                AccountHelper.createAccount(
                    this
                )


            /*
             * Observer can now safely read Contacts.
             */
            ContactChangeObserver.register(
                applicationContext
            )


            Toast
                .makeText(
                    this,
                    "Account created. Enable Contacts sync in Android Settings.",
                    Toast.LENGTH_LONG
                )
                .show()


            sendAuthenticatorSuccess(
                account
            )


            updateStatus()
            SyncDebug.dump(this)


        } catch (
            throwable: Throwable
        ) {

            throwable.printStackTrace()


            authenticatorResponse
                ?.onError(
                    AccountManager.ERROR_CODE_REMOTE_EXCEPTION,
                    throwable.message
                        ?: "Unable to create account."
                )


            Toast
                .makeText(
                    this,
                    throwable.message
                        ?: "Unable to create account.",
                    Toast.LENGTH_LONG
                )
                .show()
        }
    }


    private fun sendAuthenticatorSuccess(
        account: Account
    ) {

        val response =
            authenticatorResponse
                ?: return


        val result =
            Bundle().apply {

                putString(
                    AccountManager.KEY_ACCOUNT_NAME,
                    account.name
                )

                putString(
                    AccountManager.KEY_ACCOUNT_TYPE,
                    account.type
                )
            }


        response.onResult(
            result
        )


        authenticatorResponse =
            null
    }


    private fun openSyncSettings() {

        /*
         * Official Android Sync Settings screen.
         */
        val intent =
            Intent(
                Settings.ACTION_SYNC_SETTINGS
            )


        try {

            startActivity(
                intent
            )

        } catch (
            throwable: Throwable
        ) {

            /*
             * Extremely unusual OEM fallback.
             */
            startActivity(
                Intent(
                    Settings.ACTION_SETTINGS
                )
            )
        }
    }


    private fun syncNow() {

        if (
            checkSelfPermission(
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            Toast
                .makeText(
                    this,
                    "Contacts permission is required.",
                    Toast.LENGTH_LONG
                )
                .show()

            return
        }


        if (
            AccountHelper.getAccount(
                this
            ) == null
        ) {

            Toast
                .makeText(
                    this,
                    "Create the Contact Sync account first.",
                    Toast.LENGTH_LONG
                )
                .show()

            return
        }


        /*
         * Very important:
         *
         * User has explicitly disabled Contacts Sync.
         *
         * Do NOT circumvent their setting.
         */
        if (
            !AccountHelper.isContactSyncEnabled(
                this
            )
        ) {

            Toast
                .makeText(
                    this,
                    "Contacts sync is OFF. Enable it from Android Settings first.",
                    Toast.LENGTH_LONG
                )
                .show()

            return
        }


        AccountHelper.requestSync(
            this
        )


        Toast
            .makeText(
                this,
                "Contact synchronization requested.",
                Toast.LENGTH_SHORT
            )
            .show()
    }


    private fun updateStatus() {

        val permissionGranted =
            checkSelfPermission(
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED


        val account =
            AccountHelper.getAccount(
                this
            )


        val accountExists =
            account != null


        val accountSyncEnabled =
            AccountHelper.isAccountSyncEnabled(
                this
            )


        val masterSyncEnabled =
            android.content.ContentResolver
                .getMasterSyncAutomatically()


        val effectiveSyncEnabled =
            AccountHelper.isContactSyncEnabled(
                this
            )


        val lastSync =
            SyncStateStore.getLastSync(
                this
            )


        val lastSyncText =

            if (
                lastSync == 0L
            ) {

                "Never"

            } else {

                SimpleDateFormat(
                    "dd MMM yyyy, hh:mm:ss a",
                    Locale.getDefault()
                ).format(
                    Date(
                        lastSync
                    )
                )
            }


        val lastResult =
            SyncStateStore.getLastResult(
                this
            )


        val lastUpserts =
            SyncStateStore.getLastUpsertCount(
                this
            )


        val lastDeletes =
            SyncStateStore.getLastDeleteCount(
                this
            )


        statusText.text =
            buildString {

                append(
                    "Contacts Permission: "
                )

                append(
                    if (permissionGranted)
                        "GRANTED"
                    else
                        "NOT GRANTED"
                )


                append(
                    "\n\n"
                )


                append(
                    "Account Created: "
                )

                append(
                    if (accountExists)
                        "YES"
                    else
                        "NO"
                )


                if (
                    account != null
                ) {

                    append(
                        "\nAccount: "
                    )

                    append(
                        account.name
                    )
                }


                append(
                    "\n\n"
                )


                append(
                    "Contacts Sync Toggle: "
                )

                append(
                    if (accountSyncEnabled)
                        "ON"
                    else
                        "OFF"
                )


                append(
                    "\nMaster Android Sync: "
                )

                append(
                    if (masterSyncEnabled)
                        "ON"
                    else
                        "OFF"
                )


                append(
                    "\nEffective Sync: "
                )

                append(
                    if (effectiveSyncEnabled)
                        "ENABLED"
                    else
                        "DISABLED"
                )


                append(
                    "\n\n"
                )


                append(
                    "Last Sync: "
                )

                append(
                    lastSyncText
                )


                append(
                    "\nLast Result: "
                )

                append(
                    lastResult
                )


                append(
                    "\nLast Upserts: "
                )

                append(
                    lastUpserts
                )


                append(
                    "\nLast Deletes: "
                )

                append(
                    lastDeletes
                )
            }
    }


    override fun onRequestPermissionsResult(

        requestCode: Int,

        permissions: Array<out String>,

        grantResults: IntArray

    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )


        if (
            requestCode !=
            CONTACT_PERMISSION_REQUEST
        ) {
            return
        }


        if (
            grantResults.isNotEmpty() &&
            grantResults[0] ==
            PackageManager.PERMISSION_GRANTED
        ) {

            finishAccountSetup()

        } else {

            authenticatorResponse
                ?.onError(
                    AccountManager.ERROR_CODE_BAD_ARGUMENTS,
                    "Contacts permission was not granted."
                )


            Toast
                .makeText(
                    this,
                    "Contacts permission is required for synchronization.",
                    Toast.LENGTH_LONG
                )
                .show()
        }


        updateStatus()
    }
}