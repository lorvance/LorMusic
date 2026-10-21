package com.lormusic.app.data

import java.math.BigInteger
import java.text.Collator
import java.util.Locale

class AlphaNumericComparator(
    locale: Locale = Locale.getDefault()
) : Comparator<String?> {

    private val collator: Collator = Collator.getInstance(locale)

    override fun compare(s1: String?, s2: String?): Int {
        val str1 = s1?.trim().orEmpty()
        val str2 = s2?.trim().orEmpty()
        if (str1.isEmpty() && str2.isNotEmpty()) return -1
        if (str2.isEmpty() && str1.isNotEmpty()) return 1
        if (str1.isEmpty() && str2.isEmpty()) return 0

        var s1Index = 0
        var s2Index = 0
        while (s1Index < str1.length && s2Index < str2.length) {
            val s1Slice = slice(str1, s1Index)
            val s2Slice = slice(str2, s2Index)
            s1Index += s1Slice.length
            s2Index += s2Slice.length
            val result = if (s1Slice[0].isDigit() && s2Slice[0].isDigit()) {
                compareDigits(s1Slice, s2Slice)
            } else {
                collator.compare(s1Slice, s2Slice)
            }
            if (result != 0) return result
        }
        return Integer.signum(str1.length - str2.length)
    }

    private fun slice(s: String, startIndex: Int): String {
        var idx = startIndex
        val sb = StringBuilder()
        if (s[idx].isDigit()) {
            while (idx < s.length && s[idx].isDigit()) {
                sb.append(s[idx])
                idx++
            }
        } else {
            sb.append(s[idx])
        }
        return sb.toString()
    }

    private fun compareDigits(s1: String, s2: String): Int {
        return try {
            BigInteger(s1).compareTo(BigInteger(s2))
        } catch (_: Exception) {
            s1.compareTo(s2)
        }
    }
}
