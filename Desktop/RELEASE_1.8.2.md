# LorMusic Desktop 1.8.2

- New blue Material 3 desktop layout inspired by the supplied mockup: rounded navigation, expressive artwork, service cards and floating player.
- New LorMusic icon, including the two upper strokes, across the app and installers.
- Blue default accent; existing custom accent choices are preserved.
- Expanded player stays inside the existing window. Adaptive large artwork, blurred cover backdrop, independent artwork colors, thin seek/volume controls, optional wide lyrics/queue panes and a collapse button. Escape closes the expanded view.
- Queue reordering without interrupting the current track.
- GitHub updater: automatic daily checks, manual checks, platform-specific installer downloads, size and GitHub SHA-256 digest validation when available. Installation is opened explicitly after download.
- YouTube sign-in uses the default browser with a dedicated LorMusic profile. Chromium-based browsers use CDP; Firefox uses WebDriver BiDi. Other browsers open normally with explicit manual session import. Personal browser profiles are not read. SoundCloud keeps its own sign-in window.
- SoundCloud track metadata prefers the full duration instead of an artificial ten-second minimum.

## Android feature coverage

Local files and folders, tag/artwork/lyrics editing, tag repair and recognition, artist profiles, likes and separate favorites, playlist CRUD with delete confirmation, M3U import/export, queue, shuffle/repeat, speed, sleep timer, 10-band EQ, LRC with translation, YouTube Music search/recommendations/My Wave, SoundCloud search, MP3 downloads with tags/artwork, backups, five interface languages and rounded drawn flags are available on desktop. Android-specific UI effects and OS integrations are not presented as desktop features.

## Update publishing

Publish desktop release assets named `LorMusic-X.Y.Z-Linux-amd64.deb` and `LorMusic-X.Y.Z-Windows-x64-Setup.exe`, plus `LorMusic-X.Y.Z-Linux-x86_64.AppImage` in https://github.com/lorvance/LorMusic/releases. Android-only releases, drafts and prereleases are ignored. The updater does not publish releases itself.

## Interface revision

Removed DESKTOP branding, limited content and player widths on large displays, rebuilt the hero and service cards, tightened track spacing, enlarged track/navigation text and replaced the tall slider thumbs. The player no longer changes the OS window placement.

## Liquid Glass Update companion desktop changes (2026-11-19)

- All installer pages and welcome text support the app’s five languages: English, Russian, Ukrainian, Polish and German. Linux launch metadata is localized as well.
- Added a genuine Linux type-2 AppImage containing Java and VLC with automatic updates selecting the AppImage asset. A writable running AppImage can be replaced after the user explicitly chooses to install an update; otherwise the download folder is opened.
- Fixed SoundCloud public search/stream resolution and stale optional OAuth fallback. Real public streaming was verified with a 104-second track.

The AppImage is built on Ubuntu 26.10 and requires compatible host glibc/desktop libraries. No real Windows execution, personal Google sign-in or installed Firefox engine test was performed.
