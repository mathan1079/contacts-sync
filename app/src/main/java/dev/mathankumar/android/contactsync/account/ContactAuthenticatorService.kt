package dev.mathankumar.android.contactsync.account

import android.app.Service
import android.content.Intent
import android.os.IBinder

class ContactAuthenticatorService : Service() {

    private lateinit var authenticator:
            ContactAccountAuthenticator


    override fun onCreate() {
        super.onCreate()

        authenticator =
            ContactAccountAuthenticator(
                this
            )
    }


    override fun onBind(
        intent: Intent?
    ): IBinder {

        return authenticator
            .iBinder
    }
}