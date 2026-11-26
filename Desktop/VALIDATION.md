# Desktop 1.8.2 validation

- Gradle createDistributable and exportRuntime: passed on Linux/JDK 21.
- Self-test: real WAV/MP3 tags, library JSON, playlists/M3U, LRC, session-cookie merging, VLC playback, seek, speed and queue passed.
- Updater fixtures: desktop assets only, Linux/Windows selection, multi-digit version ordering, current-version suppression, drafts/prereleases and untrusted URLs excluded.
- Chrome isolated-profile probe: launched an empty dedicated profile, discovered the random loopback endpoint and successfully requested Storage.getCookies. No user browser profile or personal account was read. Actual Google sign-in completion requires a user account and has not been verified.
- UI screenshots: 1500×940 home, 850×620 compact home and fullscreen-player view rendered in Xvfb with a synthetic audio fixture. Hero decoration and compact search wrapping corrected after review.
- Linux .deb version metadata checked; extracted package self-test runs separately.
- Windows NSIS installer is cross-built with Windows x64 JRE/VLC and Windows Skiko. It has not been executed on real Windows and is not publisher-signed.
- Future update install/download is not represented as an end-to-end production test; GitHub currently may have no newer desktop release. Publish assets as documented in RELEASE_1.8.2.md.

Protocol references: https://developer.chrome.com/blog/remote-debugging-port , https://chromedevtools.github.io/devtools-protocol/tot/Storage/ , https://docs.github.com/en/rest/releases/releases .

## Interface revision checks

Redesigned home/dock/expanded player reviewed at 1920×1080, 850×620, 1340×880 and 1640×1000 using synthetic local music and self-authored LRC text. Expanded player preserves the requested window dimensions. OS fullscreen placement calls removed. Desktop branding removed from app navigation, settings and Windows display metadata. No backend playback/account changes in this revision.

## 2026-11-19 companion update

- Gradle createDistributable/exportRuntime passed with the default-browser and AppImage updater changes.
- Updated self-test passed, including updater asset selection for .deb, .exe and AppImage, default browser command parsing, actual VLC playback/conversion/seek/rate/queue, metadata and persistence.
- Default-browser detection returned google-chrome-stable with automatic CDP bridge support on this machine. The earlier isolated Chrome Storage.getCookies protocol probe passed; personal Google account completion remains unverified. Firefox bridge code compiled, but Firefox is not installed on the build host.
- Public SoundCloud search, stale-session fallback and native playback passed: 104046 ms duration and advancing playback. No personal credentials used.
- The final type-2 AppImage launched in extract-and-run mode with an isolated data directory and passed the complete self-test using its bundled VLC executable/libraries. The first sandboxed run was blocked by audio system-call restrictions; the normal host run passed.
- Windows NSIS installer cross-build passed with all five localized welcome strings and language selection. Real Windows execution remains unverified.
- AppImage host ABI: Ubuntu 26.10 x86-64; compatibility with older glibc distributions is unverified.
