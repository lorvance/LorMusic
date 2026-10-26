# 🎵 LorMusic

> **Modern, expressive Android music player crafted with Kotlin, Jetpack Compose, Material 3 Expressive, and AndroidX Media3 (ExoPlayer).**

[![Android](https://img.shields.io/badge/Platform-Android_8.0+-3DDC84.svg?logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203%20Expressive-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Media3](https://img.shields.io/badge/Media3-ExoPlayer-red.svg)](https://developer.android.com/media/media3)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 🌟 Key Features

- **🎨 Material 3 Expressive Design & Material You**:
  - Full support for dynamic Monet theming (`Material You`) extracting colors from wallpaper.
  - Expressive animated shapes (organic spinning shapes for pause/play, animated morphing loaders).
  - Real-time frosted glass blur effects (`BlurMaskFilter` / Compose blur).
  - Modern typography and clean edge-to-edge UI.

- **🚀 Two Modes: Local Media & YouTube Music**:
  - **Local Media**: Fast MediaStore scanner, lossless album art extraction, full folder hierarchy, and built-in synthwave fallback generator.
  - **YouTube Music Mode**: Explore recent tracks, trending charts, "My Wave" recommendation stream, personalized feeds, and Google login integration.
  - Direct offline song downloads with complete metadata and embedded cover art.
  - Easy share sheet with direct YouTube Music links.

- **🖐️ Quick Access Gesture Navigation**:
  - Hold and swipe smoothly across the bottom navigation bar to preview and switch tabs in one fluid motion.
  - Configurable search bar placement (top/bottom per mode) in Settings.
  - Full gesture controls in Now Playing and Mini Player (swipe left/right to skip tracks).

- **🏷️ Built-in Tag Editor & TagFix**:
  - Edit song title, artist, album, and custom album art directly within the app.
  - **TagFix AI/Heuristics**: Automatically clean messy filenames, remove audio artifacts from titles, and fetch missing cover arts in bulk.

- **🎭 Dynamic App Icon Switcher**:
  - Choose between **Minimalism**, **Default (Material Vector)**, and **Classic (Old)** icons directly from the Settings screen with real-time previews.

- **⚡ Audio Engine & Player**:
  - Background playback using `MediaSessionService` with lock screen controls and system notification.
  - Seamless gapless playback, playback speed adjuster (0.75x - 2.0x), repeat modes, and smart sleep timer.

---

## 📸 Screenshots & Architecture

LorMusic follows modern Android architecture recommendations:
- **UI Layer**: Jetpack Compose + Material 3 Expressive with ViewModel and unidirectional data flow (MVI / StateFlow).
- **Service Layer**: AndroidX Media3 `MediaSessionService` connected via `MediaController`.
- **Data Layer**: MediaStore repository, YouTube Music repository, and DataStore preferences.

---

## 🛠️ Building & Developing

### Requirements:
- Android Studio Ladybug | 2024.2.1 or newer
- JDK 17
- Android SDK 36 (compileSdk) / Min SDK 26 (Android 8.0+)

### Build from Command Line:
```bash
# Debug APK
./gradlew assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📜 Russian / Описание на русском

**LorMusic** — современный музыкальный плеер для Android с упором на эстетику Material 3 Expressive, плавность и удобство.

### Основные возможности:
- Двойной режим: Локальная медиатека + YouTube Music с рекомендациями «Моя волна»
- Плавные скругленные анимации, органические морфинг-кнопки паузы/воспроизведения
- Навигация скольжением (Quick Access Gesture): зажмите навбар и ведите пальцем к нужному разделу
- Встроенный редактор тегов и авто-исправление **TagFix**
- Выбор стиля иконки приложения в настройках (Минимализм, Обычная, Old)
- Настройка расположения поисковой строки и размытия (Blur)
- Скачивание треков из YT Music с сохранением тегов и обложек
- Свайпы для переключения треков в мини-плеере и на экране «Сейчас играет»

---

## 📄 License
This project is licensed under the [MIT License](LICENSE).
