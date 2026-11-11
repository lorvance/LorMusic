package com.lormusic.app.model

/** All beta UI strings follow the selected in-app language, including System. */
private val betaTranslations = mapOf(
    "format" to listOf("Формат", "Format", "Формат", "Format"),
    "preview" to listOf("Предпросмотр", "Preview", "Попередній перегляд", "Podgląd"),
    "hide" to listOf("Скрыть", "Hide", "Сховати", "Ukryj"),
    "show" to listOf("Показать", "Show", "Показати", "Pokaż"),
    "expandingSearch" to listOf("Расширяемый поиск", "Expandable search", "Розгортаний пошук", "Rozwijane wyszukiwanie"),
    "expandingDesc" to listOf("Поле поиска плавно появляется внутри верхней панели вместо отдельной строки", "Search opens smoothly inside the header instead of a separate row", "Поле пошуку плавно з’являється в заголовку замість окремого рядка", "Pole wyszukiwania płynnie pojawia się w nagłówku zamiast osobnego wiersza"),
    "expressiveNav" to listOf("Навигация Material 3 Expressive", "Material 3 Expressive navigation", "Навігація Material 3 Expressive", "Nawigacja Material 3 Expressive"),
    "expressiveNavDesc" to listOf("Цвета Material You, активная подпись и плавные переходы без размытия", "Material You colors, animated active labels, and smooth transitions without blur", "Кольори Material You, анімовані активні підписи та плавні переходи без розмиття", "Kolory Material You, animowane aktywne etykiety i płynne przejścia bez rozmycia"),
    "backup" to listOf("Резервное копирование", "Backup & restore", "Резервне копіювання", "Kopia zapasowa"),
    "restoreText" to listOf("Восстановить из текста", "Restore from text", "Відновити з тексту", "Przywróć z tekstu"),
    "createBackup" to listOf("Создать резервную копию", "Create backup", "Створити резервну копію", "Utwórz kopię zapasową"),
    "restoreFile" to listOf("Восстановить из файла", "Restore from file", "Відновити з файлу", "Przywróć z pliku"),
    "paste" to listOf("Вставить из буфера", "Paste from clipboard", "Вставити з буфера", "Wklej ze schowka"),
    "validate" to listOf("Проверить копию", "Validate backup", "Перевірити копію", "Sprawdź kopię"),
    "invalidJson" to listOf("Некорректный JSON", "Invalid backup JSON", "Некоректний JSON копії", "Nieprawidłowy JSON kopii"),
    "tooLarge" to listOf("Резервная копия слишком большая", "Backup is too large", "Резервна копія завелика", "Kopia jest zbyt duża"),
    "retryExport" to listOf("Повторите экспорт", "Please export again", "Повторіть експорт", "Powtórz eksport"),
    "openFailed" to listOf("Не удалось открыть файл", "Could not open file", "Не вдалося відкрити файл", "Nie udało się otworzyć pliku"),
    "saved" to listOf("Резервная копия сохранена", "Backup saved", "Резервну копію збережено", "Zapisano kopię zapasową"),
    "saveError" to listOf("Ошибка сохранения", "Could not save backup", "Не вдалося зберегти копію", "Nie udało się zapisać kopii"),
    "fileUnavailable" to listOf("Файл недоступен", "File is unavailable", "Файл недоступний", "Plik jest niedostępny"),
    "readError" to listOf("Ошибка чтения", "Could not read backup", "Не вдалося прочитати копію", "Nie udało się odczytać kopii"),
    "exportError" to listOf("Ошибка экспорта", "Could not export backup", "Не вдалося експортувати копію", "Nie udało się wyeksportować kopii"),
    "close" to listOf("Закрыть", "Close", "Закрити", "Zamknij"),
    "back" to listOf("Назад", "Back", "Назад", "Wstecz"),
    "restoreQuestion" to listOf("Восстановить настройки?", "Restore settings?", "Відновити налаштування?", "Przywrócić ustawienia?"),
    "restoreWarning" to listOf("Настройки из копии заменят соответствующие текущие значения.", "Settings in this backup will replace the corresponding current values.", "Налаштування з копії замінять відповідні поточні значення.", "Ustawienia z kopii zastąpią odpowiadające im bieżące wartości."),
    "restored" to listOf("Настройки восстановлены", "Settings restored", "Налаштування відновлено", "Przywrócono ustawienia"),
    "restoreFailed" to listOf("Не удалось восстановить настройки", "Could not restore settings", "Не вдалося відновити налаштування", "Nie udało się przywrócić ustawień"),
    "restore" to listOf("Восстановить", "Restore", "Відновити", "Przywróć"),
    "cancel" to listOf("Отмена", "Cancel", "Скасувати", "Anuluj"),
    "audioFile" to listOf("Аудиофайл", "Audio file", "Аудіофайл", "Plik audio"),
    "externalFile" to listOf("Внешний аудиофайл", "External audio file", "Зовнішній аудіофайл", "Zewnętrzny plik audio"),
    "playFailed" to listOf("Не удалось воспроизвести файл. Проверьте доступ и формат аудио.", "Could not play this file. Check access and the audio format.", "Не вдалося відтворити файл. Перевірте доступ і формат аудіо.", "Nie udało się odtworzyć pliku. Sprawdź dostęp i format audio."),
    "mainPlayback" to listOf("Воспроизведение продолжено в основном плеере", "Playback continued in the main player", "Відтворення продовжено в основному плеєрі", "Odtwarzanie jest kontynuowane w głównym odtwarzaczu"),
    "noFile" to listOf("Приложение не передало доступный аудиофайл", "The app did not provide an accessible audio file", "Застосунок не передав доступний аудіофайл", "Aplikacja nie przekazała dostępnego pliku audio"),
    "noAccess" to listOf("Нет доступа к файлу", "No access to this file", "Немає доступу до файлу", "Brak dostępu do pliku"),
    "readDenied" to listOf("Файл недоступен или отсутствует разрешение на чтение", "File unavailable or read permission denied", "Файл недоступний або немає дозволу на читання", "Plik niedostępny lub brak uprawnienia do odczytu"),
    "closePlayer" to listOf("Закрыть проигрыватель", "Close player", "Закрити програвач", "Zamknij odtwarzacz"),
    "rewind" to listOf("Назад на 30 секунд", "Rewind 30 seconds", "Назад на 30 секунд", "Cofnij o 30 sekund"),
    "forward" to listOf("Вперёд на 30 секунд", "Forward 30 seconds", "Вперед на 30 секунд", "Przewiń o 30 sekund"),
    "authFailed" to listOf("Не удалось подтвердить вход. Google может блокировать встроенный браузер. Существующий аккаунт не изменён.", "Could not verify sign-in. Google may block embedded browsers. Your existing account was not changed.", "Не вдалося підтвердити вхід. Google може блокувати вбудований браузер. Наявний обліковий запис не змінено.", "Nie udało się potwierdzić logowania. Google może blokować przeglądarki wbudowane. Istniejące konto nie zostało zmienione."),
    "authBlocked" to listOf("Google ограничивает вход во встроенных браузерах. Для официального входа требуется OAuth-настройка LorMusic.", "Google restricts sign-in in embedded browsers. Official sign-in requires LorMusic OAuth configuration.", "Google обмежує вхід у вбудованих браузерах. Для офіційного входу потрібне налаштування OAuth LorMusic.", "Google ogranicza logowanie w przeglądarkach wbudowanych. Oficjalne logowanie wymaga konfiguracji OAuth LorMusic."),
)

fun AppStrings.betaText(key: String): String {
    val language = when (this) { StringsEn -> 1; StringsUk -> 2; StringsPl -> 3; else -> 0 }
    return requireNotNull(betaTranslations[key]) { "Missing UI translation: $key" }[language]
}
