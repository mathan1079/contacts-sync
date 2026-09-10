package dev.mathankumar.android.contactsync.network

import dev.mathankumar.android.contactsync.constants.AppConstants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private val loggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

    private val authenticationInterceptor =
        okhttp3.Interceptor { chain ->
            val original =
                chain.request()

            val builder =
                original.newBuilder()
                    .header(
                        "Accept",
                        "application/json"
                    )
                    .header(
                        "Content-Type",
                        "application/json"
                    )

            if (AppConstants.API_TOKEN.isNotBlank()) {
                builder.header(
                    "Authorization",
                    "Bearer ${AppConstants.API_TOKEN}"
                )
            }

            chain.proceed(builder.build())
        }


    private val httpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(authenticationInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()


    private val retrofit =
        Retrofit
            .Builder()
            .baseUrl(AppConstants.API_BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()


    val api: ContactSyncApi =
        retrofit.create(
            ContactSyncApi::class.java
        )
}