package dev.mathankumar.android.contactsync.sync

import android.content.Context
import androidx.core.content.edit

object SyncStateStore {
    private const val PREFS = "contact_sync_state"
    private const val KEY_LAST_SYNC = "last_sync"
    private const val KEY_LAST_RESULT = "last_result"
    private const val KEY_LAST_UPSERT_COUNT = "last_upsert_count"
    private const val KEY_LAST_DELETE_COUNT = "last_delete_count"

    fun setSuccess(
        context: Context,
        upsertCount: Int,
        deleteCount: Int
    ) {
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )
            .edit {
                putLong(
                    KEY_LAST_SYNC,
                    System.currentTimeMillis()
                )
                    .putString(
                        KEY_LAST_RESULT,
                        "SUCCESS"
                    )
                    .putInt(
                        KEY_LAST_UPSERT_COUNT,
                        upsertCount
                    )
                    .putInt(
                        KEY_LAST_DELETE_COUNT,
                        deleteCount
                    )

            }
    }


    fun setFailure(
        context: Context,
        message: String
    ) {
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )
            .edit {
                putLong(
                    KEY_LAST_SYNC,
                    System.currentTimeMillis()
                )
                    .putString(
                        KEY_LAST_RESULT,
                        "FAILED: $message"
                    )

            }
    }


    fun getLastSync(
        context: Context
    ): Long {
        return context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .getLong(
                KEY_LAST_SYNC,
                0L
            )
    }


    fun getLastResult(
        context: Context
    ): String {

        return context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .getString(
                KEY_LAST_RESULT,
                "Never synchronized"
            )
            ?: "Never synchronized"
    }


    fun getLastUpsertCount(
        context: Context
    ): Int {

        return context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .getInt(
                KEY_LAST_UPSERT_COUNT,
                0
            )
    }


    fun getLastDeleteCount(
        context: Context
    ): Int {

        return context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .getInt(
                KEY_LAST_DELETE_COUNT,
                0
            )
    }
}