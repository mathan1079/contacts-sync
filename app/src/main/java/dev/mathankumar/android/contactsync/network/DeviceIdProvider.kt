package dev.mathankumar.android.contactsync.network


import android.content.Context
import java.util.UUID
import androidx.core.content.edit

object DeviceIdProvider {

    private const val PREFS =
        "device_identity"

    private const val KEY_DEVICE_ID =
        "device_id"


    fun get(
        context: Context
    ): String {

        val preferences =
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )


        preferences.getString(
            KEY_DEVICE_ID,
            null
        )?.let {

            return it
        }


        val newId =
            UUID
                .randomUUID()
                .toString()


        preferences
            .edit {
                putString(
                    KEY_DEVICE_ID,
                    newId
                )
            }


        return newId
    }
}