package com.lormusic.app.model

enum class AppLanguage(val code: String, val displayName: String) {
    SYSTEM("system", "По умолчанию (Система)"),
    RU("ru", "Русский"),
    EN("en", "English"),
    UK("uk", "Українська");

    fun getLabel(strings: AppStrings): String {
        return when (this) {
            SYSTEM -> strings.systemLanguage
            RU -> "Русский"
            EN -> "English"
            UK -> "Українська"
        }
    }

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: SYSTEM
        }
    }
}
