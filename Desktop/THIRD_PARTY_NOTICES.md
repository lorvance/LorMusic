# LorMusic Desktop — third-party components

The desktop adaptation retains LorMusic's GNU GPL v3 license. Source repositories carried over from LorMusic originate at https://github.com/Lorvance/LorMusic.

| Component | Version | License / source |
| --- | --- | --- |
| Kotlin | 2.0.21 | Apache-2.0 — https://github.com/JetBrains/kotlin |
| Compose Multiplatform | 1.7.3 | Apache-2.0 — https://github.com/JetBrains/compose-multiplatform |
| Material icons | Compose 1.7.3 | Apache-2.0 — https://github.com/google/material-design-icons |
| Skiko | 0.8.18 | Apache-2.0 — https://github.com/JetBrains/skiko |
| kotlinx.coroutines | 1.9.0 | Apache-2.0 — https://github.com/Kotlin/kotlinx.coroutines |
| VLCJ | 4.8.3 | GPL-3.0 — https://github.com/caprica/vlcj |
| JNA | 5.14.0 | LGPL-2.1 / Apache-2.0 — https://github.com/java-native-access/jna |
| jaudiotagger | 3.0.1 | LGPL-2.1 — https://bitbucket.org/ijabz/jaudiotagger |
| Gson | 2.11.0 | Apache-2.0 — https://github.com/google/gson |
| JSON-java | 20240303 | Public domain — https://github.com/stleary/JSON-java |
| JCEF Maven | 152.0.6 | Apache-2.0; JCEF/Chromium retain upstream notices — https://github.com/jcefmaven/jcefmaven |
| Eclipse Temurin JRE | 21.0.13+9 | GPL-2.0 with Classpath Exception; see bundled legal/ — https://github.com/adoptium/temurin21-binaries |
| VLC (Windows bundle) | 3.0.21 | GPL-2.0-or-later / LGPL components, see vlc/COPYING.txt — https://download.videolan.org/pub/videolan/vlc/3.0.21/ |
| NSIS (installer tooling) | 3.10 | zlib/libpng license — https://nsis.sourceforge.io/License |

Linux uses the distribution-provided VLC. Windows includes the unmodified VideoLAN VLC distribution with its notices. Both platforms include an unmodified Temurin runtime or a jlink subset with its legal notices. Chromium is installed on demand by JCEF Maven, with upstream notices in its own distribution.

Network services (YouTube Music, SoundCloud, LRCLIB, lyrics services, translation services and metadata catalogs) remain the property of their respective owners. LorMusic does not claim ownership of service marks, music, artwork or lyrics.
