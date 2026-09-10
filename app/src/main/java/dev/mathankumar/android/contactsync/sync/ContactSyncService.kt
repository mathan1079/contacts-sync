package dev.mathankumar.android.contactsync.sync

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class ContactSyncService : Service() {

    companion object {

        private const val TAG =
            "CONTACT_SYNC_SERVICE"

        private val LOCK =
            Any()

        @Volatile
        private var syncAdapter:
                ContactSyncAdapter? = null
    }


    override fun onCreate() {

        super.onCreate()


        Log.e(
            TAG,
            "ContactSyncService onCreate()"
        )


        synchronized(LOCK) {

            if (syncAdapter == null) {

                syncAdapter =
                    ContactSyncAdapter(
                        applicationContext,
                        true
                    )
            }
        }
    }


    override fun onBind(
        intent: Intent?
    ): IBinder? {

        Log.e(
            TAG,
            "ContactSyncService onBind(): ${intent?.action}"
        )


        return syncAdapter
            ?.syncAdapterBinder
    }
}