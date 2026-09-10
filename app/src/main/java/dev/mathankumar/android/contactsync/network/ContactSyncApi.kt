package dev.mathankumar.android.contactsync.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ContactSyncApi {

    @POST("api/v1/contact-sync")
    fun syncContacts(
        @Body request: ContactSyncRequest
    ): Call<ContactSyncResponse>
}