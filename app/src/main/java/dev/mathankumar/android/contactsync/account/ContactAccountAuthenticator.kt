package dev.mathankumar.android.contactsync.account

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import dev.mathankumar.android.contactsync.MainActivity
import dev.mathankumar.android.contactsync.constants.AppConstants

class ContactAccountAuthenticator(
    private val context: Context
) : AbstractAccountAuthenticator(
    context
) {

    override fun editProperties(
        response: AccountAuthenticatorResponse?,
        accountType: String?
    ): Bundle {

        return errorBundle(
            "Editing account properties is not supported."
        )
    }


    override fun addAccount(
        response: AccountAuthenticatorResponse?,
        accountType: String?,
        authTokenType: String?,
        requiredFeatures: Array<out String>?,
        options: Bundle?
    ): Bundle {

        /*
         * If Settings -> Add account -> Dotworld Contact Sync
         * is clicked, open our MainActivity.
         */
        val intent =
            Intent(
                context,
                MainActivity::class.java
            ).apply {
                putExtra(
                    AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
                    response
                )
                putExtra(
                    MainActivity.EXTRA_FROM_ACCOUNT_AUTHENTICATOR,
                    true
                )
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )
            }

        return Bundle().apply {

            putParcelable(
                AccountManager.KEY_INTENT,
                intent
            )
        }
    }


    override fun confirmCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        options: Bundle?
    ): Bundle {

        return Bundle().apply {

            putBoolean(
                AccountManager.KEY_BOOLEAN_RESULT,
                true
            )
        }
    }


    override fun getAuthToken(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        authTokenType: String?,
        options: Bundle?
    ): Bundle {

        /*
         * We're currently using our API auth directly.
         *
         * If you later want AccountManager to manage
         * OAuth tokens, implement it here.
         */

        if (
            account != null &&
            AppConstants.API_TOKEN.isNotBlank()
        ) {

            return Bundle().apply {

                putString(
                    AccountManager.KEY_ACCOUNT_NAME,
                    account.name
                )

                putString(
                    AccountManager.KEY_ACCOUNT_TYPE,
                    account.type
                )

                putString(
                    AccountManager.KEY_AUTHTOKEN,
                    AppConstants.API_TOKEN
                )
            }
        }


        return errorBundle(
            "No authentication token configured."
        )
    }


    override fun getAuthTokenLabel(
        authTokenType: String?
    ): String {

        return "Contact Sync API"
    }


    override fun updateCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        authTokenType: String?,
        options: Bundle?
    ): Bundle {

        return errorBundle(
            "Credential update is not currently supported."
        )
    }


    override fun hasFeatures(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        features: Array<out String>?
    ): Bundle {

        return Bundle().apply {

            putBoolean(
                AccountManager.KEY_BOOLEAN_RESULT,
                false
            )
        }
    }


    private fun errorBundle(
        message: String
    ): Bundle {

        return Bundle().apply {

            putInt(
                AccountManager.KEY_ERROR_CODE,
                AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION
            )

            putString(
                AccountManager.KEY_ERROR_MESSAGE,
                message
            )
        }
    }
}