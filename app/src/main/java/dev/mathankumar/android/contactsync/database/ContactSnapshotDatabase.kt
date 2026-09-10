package dev.mathankumar.android.contactsync.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ContactSnapshotDatabase(
    context: Context
) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    override fun onCreate(
        db: SQLiteDatabase
    ) {

        db.execSQL(
            """
            CREATE TABLE $TABLE_CONTACTS (
                contact_id INTEGER PRIMARY KEY,
                contact_hash TEXT NOT NULL
            )
            """.trimIndent()
        )
    }


    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {

        db.execSQL(
            "DROP TABLE IF EXISTS $TABLE_CONTACTS"
        )

        onCreate(db)
    }


    fun loadSnapshot(): Map<Long, String> {

        val result =
            mutableMapOf<Long, String>()


        readableDatabase.query(
            TABLE_CONTACTS,

            arrayOf(
                COLUMN_CONTACT_ID,
                COLUMN_HASH
            ),

            null,
            null,
            null,
            null,
            null
        ).use { cursor ->

            val idIndex =
                cursor.getColumnIndexOrThrow(
                    COLUMN_CONTACT_ID
                )

            val hashIndex =
                cursor.getColumnIndexOrThrow(
                    COLUMN_HASH
                )


            while (
                cursor.moveToNext()
            ) {

                result[
                    cursor.getLong(
                        idIndex
                    )
                ] =
                    cursor.getString(
                        hashIndex
                    )
            }
        }


        return result
    }


    /**
     * IMPORTANT:
     *
     * Call this ONLY after server API synchronization
     * succeeds.
     */
    fun replaceSnapshot(
        snapshot: Map<Long, String>
    ) {

        val db =
            writableDatabase


        db.beginTransaction()

        try {

            db.delete(
                TABLE_CONTACTS,
                null,
                null
            )


            snapshot.forEach { (contactId, hash) ->

                val values =
                    ContentValues().apply {

                        put(
                            COLUMN_CONTACT_ID,
                            contactId
                        )

                        put(
                            COLUMN_HASH,
                            hash
                        )
                    }


                db.insertOrThrow(
                    TABLE_CONTACTS,
                    null,
                    values
                )
            }


            db.setTransactionSuccessful()

        } finally {

            db.endTransaction()
        }
    }


    companion object {

        private const val DATABASE_NAME =
            "contact_sync.db"

        private const val DATABASE_VERSION =
            1


        private const val TABLE_CONTACTS =
            "contact_snapshot"


        private const val COLUMN_CONTACT_ID =
            "contact_id"

        private const val COLUMN_HASH =
            "contact_hash"
    }
}