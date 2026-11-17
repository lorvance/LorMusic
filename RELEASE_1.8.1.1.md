# LorMusic 1.8.1.1

Android release · versionCode 75.

- Restored both small upper strokes in the monochrome adaptive icon.
- Fixed SoundCloud downloads retaining a ten-second HLS segment's MP3 duration and seek metadata. Downloaded audio is now assembled, measured, and tagged before it is published to the music library. AAC downloads are remuxed to M4A.
- Fixed artwork loading for gradients and dynamic player colors on downloaded SoundCloud tracks, including cached artwork paths. Dynamic colors can now work independently of the background gradient setting.
- Preserved the local-file identity and paths of saved SoundCloud downloads across restarts.

Previously downloaded files with incorrect internal audio headers should be downloaded again.

Validation: release build and JVM regression suite; independently verified a real three-segment VBR MP3 (old header: 10 seconds; repaired file: 30 seconds), including embedded artwork. Physical-device playback was not tested because no ADB device was connected.
