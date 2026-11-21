package com.lormusic.app.data

import java.security.MessageDigest

internal object BetaTesterAccess {
    private val passphraseDigest =
        "af6115a1a241da76cfb9177c960f9d8598c058ad721570b8825c22e409b99884".chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    fun matches(password: String): Boolean = MessageDigest.isEqual(
        passphraseDigest,
        MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8)),
    )
}
