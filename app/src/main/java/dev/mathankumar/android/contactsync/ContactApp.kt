package dev.mathankumar.android.contactsync

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import dagger.hilt.android.HiltAndroidApp
import dev.mathankumar.android.contactsync.observer.ContactChangeObserver

@HiltAndroidApp
class ContactApp : Application() {
    companion object {
        lateinit var instance: ContactApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        if (
            checkSelfPermission(
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            ContactChangeObserver.register(this)
        }
    }
}