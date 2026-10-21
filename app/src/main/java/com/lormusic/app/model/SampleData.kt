package com.lormusic.app.model

object SampleData {
    val songs = listOf(
        Song(
            id = "demo_1",
            title = "Midnight Drive",
            artist = "Nova Waves",
            album = "Neon Hours",
            durationMs = 24_000L,
            uri = "",
            coverColorSeed = 1
        ),
        Song(
            id = "demo_2",
            title = "Glass City",
            artist = "Nova Waves",
            album = "Neon Hours",
            durationMs = 22_000L,
            uri = "",
            coverColorSeed = 2
        ),
        Song(
            id = "demo_3",
            title = "Afterglow",
            artist = "Kira Lane",
            album = "Slow Bloom",
            durationMs = 26_000L,
            uri = "",
            coverColorSeed = 3
        ),
        Song(
            id = "demo_4",
            title = "Static Heart",
            artist = "Rue",
            album = "Static Heart EP",
            durationMs = 20_000L,
            uri = "",
            coverColorSeed = 4
        ),
        Song(
            id = "demo_5",
            title = "Paper Moon",
            artist = "Kira Lane",
            album = "Slow Bloom",
            durationMs = 25_000L,
            uri = "",
            coverColorSeed = 5
        ),
        Song(
            id = "demo_6",
            title = "Cyber Horizon",
            artist = "Aether Pulse",
            album = "Future Echoes",
            durationMs = 23_000L,
            uri = "",
            coverColorSeed = 6
        ),
        Song(
            id = "demo_7",
            title = "Velvet Rain",
            artist = "Solstice",
            album = "Nocturne",
            durationMs = 24_000L,
            uri = "",
            coverColorSeed = 7
        )
    )

    val playlists = listOf(
        Playlist("p_evening", "Вечерний вайб", songs.take(4), coverColorSeed = 101),
        Playlist("p_workout", "Энергия и драйв", listOf(songs[0], songs[1], songs[3], songs[5]), coverColorSeed = 202),
        Playlist("p_chill", "Ночной лоу-фай", listOf(songs[2], songs[4], songs[6]), coverColorSeed = 303)
    )
}
