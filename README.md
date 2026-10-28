# 🎵 LorMusic

> **Modern, expressive Android music player crafted with Kotlin, Jetpack Compose, Material 3 Expressive, and AndroidX Media3 (ExoPlayer).**

[![Android](https://img.shields.io/badge/Platform-Android_8.0+-3DDC84.svg?logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203%20Expressive-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Media3](https://img.shields.io/badge/Media3-ExoPlayer-red.svg)](https://developer.android.com/media/media3)
[![Releases](https://img.shields.io/github/v/release/Lorvance/LorMusic?logo=github&color=blue)](https://github.com/Lorvance/LorMusic/releases/latest)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 🌟 Features

### 🎧 3-in-1 Media Streaming & Local Library
- **📁 Local Media**:
  - High-performance MediaStore scanner with lossless artwork extraction and folder hierarchy navigation.
  - Built-in synthwave cover fallback generator for tracks without embedded art.
- **▶️ YouTube Music**:
  - Personalized feeds, trending charts, mood filters, and "My Wave" recommendation stream.
  - Secure browser-based Google authorization.
  - Offline song downloads with embedded metadata and high-resolution cover artwork.
  - Direct share sheet integration.
- **☁️ SoundCloud**:
  - Full SoundCloud catalog search, trending tracks by genre, Daily Drops, and Weekly Wave.
  - High-quality audio streaming via Media3 ExoPlayer HLS.
  - Personal profile support: view liked tracks, avatar, track counts, and followers.
  - In-app login via WebView or auth token.
  - Direct track downloading and link sharing.

### 🔀 Unified Playlists & Favorites
- Combine tracks from **Local Device**, **YouTube Music**, and **SoundCloud** into seamless custom playlists.
- Automatic gapless transitions between local audio files and remote streams.
- **Unified Favorites**: A single dedicated hub for favorite tracks across all platforms.

### 🎨 Material 3 Expressive & Liquid Glass Design
- **Material You Dynamic Theming**: Monet color palette dynamically generated from the user's wallpaper.
- **Apple Liquid Glass & Frosted Blur**: Hardware-accelerated real-time blur (`RenderEffect` / `Haze`) with customizable intensity.
- **Expressive Motion**: Organic shape morphing for playback controls, squiggly animated wave seekbar, and fluid swipe navigation.
- **Customizable Search Placement**: Quick toggle between top bar or bottom navigation bar search.

### 🎭 Dynamic App Icon Switcher & Splash Animations
- Choose between multiple launcher icons directly in settings (**Default**, **Minimalism**, **Classic**).
- Matching launch animations tailored to each icon style (Equalizer pulse, Retro flying ball, Bouncing sphere).

### 🏷️ Built-in Tag Editor & TagFix
- Edit song title, artist, album name, and custom cover art directly within the app.
- **TagFix**: Automatic filename cleanup, audio artifact removal, and metadata recovery.

### 🔄 In-App Auto-Updater
- Direct update checks and releases notifications from GitHub.
- Floating update card with native background blur and built-in APK installer.

---

## 🏗️ Architecture & Tech Stack

LorMusic follows modern Android architectural recommendations and unidirectional data flow (MVI / StateFlow):

- **UI Layer**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with [Material 3 Expressive](https://m3.material.io).
- **Audio Engine**: [AndroidX Media3 ExoPlayer](https://developer.android.com/media/media3) + HLS playback running inside a foreground `MediaSessionService` with lockscreen controls.
- **Networking**: OkHttp 4, native JSON parsing, YouTube Music API client, and SoundCloud streaming engine.
- **Storage & State**: DataStore Preferences and MediaStore content resolvers.
- **Image Loading**: [Coil 3](https://coil-kt.github.io/coil/) with crossfade transitions and custom decoders.
- **Internationalization**: Full localization for English (`en`), Russian (`ru`), and Ukrainian (`uk`).

---

## 🛠️ Building & Developing

### Requirements:
- Android Studio Ladybug (2024.2.1+) or newer
- JDK 17
- Android SDK 35 / compileSdk 35
- Min SDK 26 (Android 8.0 Oreo+)

### Build from Command Line:
```bash
# Clone the repository
git clone https://github.com/Lorvance/LorMusic.git
cd LorMusic

# Build Release APK
./gradlew assembleRelease

# Output APK path:
# app/build/outputs/apk/release/app-release.apk
```

---

## 🇷🇺 Русская версия (Описание на русском)

**LorMusic** — современный музыкальный плеер для Android с эстетикой Material 3 Expressive, поддержкой стриминговых сервисов, общими плейлистами и глубокой кастомизацией.

### Ключевые возможности:
- **3 источника медиа**:
  - **Локальная музыка**: быстрое сканирование, папочный вид, генератор обложек, редактирование тегов.
  - **YouTube Music**: персональные подборки, «Моя волна», чарты, скачивание песен с тегами и обложками, вход через Google.
  - **SoundCloud**: поиск треков, жанровые чарты, Daily Drops, профиль пользователя, скачивание треков и экспорт ссылок.
- **Общие плейлисты и избранное**: объединяйте локальные треки, YouTube Music и SoundCloud в единые плейлисты с бесшовным переключением.
- **Дизайн Material 3 Expressive и Liquid Glass**: адаптивные цвета Material You под обои рабочего стола, эффекты матового стекла и размытия (Blur), анимированный волнистый слайдер.
- **Смена иконок приложения**: Стандартная, Минимализм и Классика (Old) с уникальными анимациями запуска.
- **Встроенный TagFix**: интеллектуальное авто-исправление тегов и очистка названий файлов.
- **Автоапдейтер**: проверка и установка обновлений прямо из приложения с карточкой на фоне размытия.
- **Полная локализация**: русский, украинский и английский языки.

---

## 📄 License
This project is licensed under the [MIT License](LICENSE).
