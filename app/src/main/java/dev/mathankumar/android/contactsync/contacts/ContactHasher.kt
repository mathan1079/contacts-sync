package dev.mathankumar.android.contactsync.contacts

import java.security.MessageDigest
import java.util.Locale

object ContactHasher {

    fun calculate(
        contact: DeviceContact
    ): String {

        val canonical =
            buildString {

                append(
                    normalize(
                        contact.displayName
                    )
                )

                append("|")


                /*
                 * Phone order in ContactsProvider
                 * isn't guaranteed.
                 *
                 * Sort before hashing.
                 */
                contact.phones
                    .map { phone ->

                        buildString {

                            append(
                                normalize(
                                    phone.normalizedNumber
                                        ?: phone.number
                                )
                            )

                            append(":")

                            append(
                                normalize(
                                    phone.type
                                )
                            )
                        }
                    }
                    .sorted()
                    .forEach {

                        append(it)

                        append(";")
                    }


                append("|")


                contact.emails
                    .map { email ->

                        buildString {

                            append(
                                normalize(
                                    email.email
                                )
                            )

                            append(":")

                            append(
                                normalize(
                                    email.type
                                )
                            )
                        }
                    }
                    .sorted()
                    .forEach {

                        append(it)

                        append(";")
                    }


                append("|")

                append(
                    normalize(
                        contact.company
                    )
                )


                append("|")


                append(
                    normalize(
                        contact.designation
                    )
                )
            }


        val digest =
            MessageDigest
                .getInstance("SHA-256")
                .digest(
                    canonical
                        .toByteArray(
                            Charsets.UTF_8
                        )
                )


        return digest.joinToString(
            separator = ""
        ) {
            "%02x".format(it)
        }
    }


    private fun normalize(
        value: String?
    ): String {

        return value
            ?.trim()
            ?.lowercase(
                Locale.ROOT
            )
            ?: ""
    }
}