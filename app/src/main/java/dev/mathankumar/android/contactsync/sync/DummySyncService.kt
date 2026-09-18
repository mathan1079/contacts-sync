package dev.mathankumar.android.contactsync.sync

import android.accounts.Account
import android.app.Service
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.Intent
import android.content.SyncResult
import android.os.Bundle
import android.os.IBinder

class DummySyncService : Service() {

    private lateinit var syncAdapter: AbstractThreadedSyncAdapter

    override fun onCreate() {
        super.onCreate()
        syncAdapter = object : AbstractThreadedSyncAdapter(applicationContext, true) {
            override fun onPerformSync(
                account: Account?,
                extras: Bundle?,
                authority: String?,
                provider: ContentProviderClient?,
                syncResult: SyncResult?
            ) {
                // Do nothing
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return syncAdapter.syncAdapterBinder
    }
}
