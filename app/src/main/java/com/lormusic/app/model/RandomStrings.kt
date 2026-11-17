package com.lormusic.app.model

/** A stable silly dictionary: labels do not change mid-gesture or affect music metadata. */
object RandomRussian {
    private val words =
        listOf(
            "Пельмешки",
            "Химия",
            "Сухарики",
            "Космолёт",
            "Кабачок",
            "Бубен",
            "Карамель",
            "Жабоскок",
            "Шуршалки",
            "Крендель",
            "Пылесос",
            "Капибара",
            "Смех",
            "Компот",
            "Тапочки",
            "Мармелад",
            "Барабулька",
            "Укроп",
            "Лунтик",
            "Чебурек",
            "Турбожук",
            "Шаурма",
            "Гравицапа",
            "Шлёп",
            "Огурцы",
            "Бублик",
            "Парашют",
            "Хрум",
            "Бульбулятор",
            "Кукумбер",
            "Кефир",
            "Кукуруза",
            "Шерстяной движ",
            "Баклажан",
            "Трактор",
            "Облачный борщ",
            "Сосисочная",
            "Пуховик",
            "Мегажмых",
            "Плюшки",
            "Пирожок",
            "Пердолёт",
            "Лаборатория",
            "Макарошки",
            "Пузырь",
            "Банановая тяга",
            "Горошек",
            "Крякалка",
        )
    private val fixed =
        mapOf(
            "navSettings" to "Сухарики",
            "navLibrary" to "Химия",
            "navPlaylists" to "Кладовка",
            "searchTitle" to "Смех",
            "ytHome" to "Берлога",
            "ytLibraries" to "Сундук",
            "ytMyWave" to "Мой компот",
            "scTopCharts" to "Хит-пельмени",
            "playAll" to "Жахнуть всё",
            "close" to "Свернуть лавочку",
            "cancel" to "Передумал",
            "delete" to "В утиль",
            "confirm" to "Погнали",
            "settingsCategoryAppearance" to "Прикид",
            "settingsCategoryNavigation" to "Компас огурца",
        )

    fun text(key: String, original: String): String {
        fixed[key]?.let {
            return it
        }
        if (original.isBlank()) return original
        // Destructive confirmations and file/permission diagnostics remain readable.
        if (
            key.contains("delete", true) ||
                key.contains("warning", true) ||
                key.contains("permission", true) ||
                key.contains("error", true) ||
                key.contains("invalid", true) ||
                key.contains("failed", true) ||
                key.contains("blocked", true) ||
                key.contains("denied", true) ||
                key.contains("access", true)
        )
            return "Бульк! $original"
        val word = words[Math.floorMod(key.hashCode(), words.size)]
        val slots =
            Regex("""%(?:\d+\$)?[-#+ 0,(<]*\d*(?:\.\d+)?[tT]?[A-Za-z%]""")
                .findAll(original)
                .map { it.value }
                .toList()
        return if (slots.isEmpty()) word else word + " " + slots.joinToString(" · ")
    }
}

val StringsRandom: AppStrings =
    AppStrings(
        navLibrary = RandomRussian.text("navLibrary", StringsRu.navLibrary),
        navYtMusic = RandomRussian.text("navYtMusic", StringsRu.navYtMusic),
        navPlaylists = RandomRussian.text("navPlaylists", StringsRu.navPlaylists),
        navSettings = RandomRussian.text("navSettings", StringsRu.navSettings),
        nowPlaying = RandomRussian.text("nowPlaying", StringsRu.nowPlaying),
        nowPlayingTitle = RandomRussian.text("nowPlayingTitle", StringsRu.nowPlayingTitle),
        inQueue = RandomRussian.text("inQueue", StringsRu.inQueue),
        lyrics = RandomRussian.text("lyrics", StringsRu.lyrics),
        download = RandomRussian.text("download", StringsRu.download),
        share = RandomRussian.text("share", StringsRu.share),
        delete = RandomRussian.text("delete", StringsRu.delete),
        favorites = RandomRussian.text("favorites", StringsRu.favorites),
        inFavorites = RandomRussian.text("inFavorites", StringsRu.inFavorites),
        addToPlaylist = RandomRussian.text("addToPlaylist", StringsRu.addToPlaylist),
        playNext = RandomRussian.text("playNext", StringsRu.playNext),
        editTags = RandomRussian.text("editTags", StringsRu.editTags),
        recentsTitle = RandomRussian.text("recentsTitle", StringsRu.recentsTitle),
        recentsSubtitle = RandomRussian.text("recentsSubtitle", StringsRu.recentsSubtitle),
        playAll = RandomRussian.text("playAll", StringsRu.playAll),
        radioMixesTitle = RandomRussian.text("radioMixesTitle", StringsRu.radioMixesTitle),
        radioMixesSubtitle = RandomRussian.text("radioMixesSubtitle", StringsRu.radioMixesSubtitle),
        recommendationsTitle =
            RandomRussian.text("recommendationsTitle", StringsRu.recommendationsTitle),
        recommendationsSubtitle =
            RandomRussian.text("recommendationsSubtitle", StringsRu.recommendationsSubtitle),
        myWaveTitle = RandomRussian.text("myWaveTitle", StringsRu.myWaveTitle),
        searchTitle = RandomRussian.text("searchTitle", StringsRu.searchTitle),
        signInGoogle = RandomRussian.text("signInGoogle", StringsRu.signInGoogle),
        googleRequiredTitle =
            RandomRussian.text("googleRequiredTitle", StringsRu.googleRequiredTitle),
        googleRequiredDesc = RandomRussian.text("googleRequiredDesc", StringsRu.googleRequiredDesc),
        backToLocal = RandomRussian.text("backToLocal", StringsRu.backToLocal),
        syncWithGoogle = RandomRussian.text("syncWithGoogle", StringsRu.syncWithGoogle),
        switchAccount = RandomRussian.text("switchAccount", StringsRu.switchAccount),
        signOut = RandomRussian.text("signOut", StringsRu.signOut),
        close = RandomRussian.text("close", StringsRu.close),
        done = RandomRussian.text("done", StringsRu.done),
        back = RandomRussian.text("back", StringsRu.back),
        create = RandomRussian.text("create", StringsRu.create),
        clear = RandomRussian.text("clear", StringsRu.clear),
        more = RandomRussian.text("more", StringsRu.more),
        actions = RandomRussian.text("actions", StringsRu.actions),
        systemLanguage = RandomRussian.text("systemLanguage", StringsRu.systemLanguage),
        understood = RandomRussian.text("understood", StringsRu.understood),
        great = RandomRussian.text("great", StringsRu.great),
        collapse = RandomRussian.text("collapse", StringsRu.collapse),
        songActions = RandomRussian.text("songActions", StringsRu.songActions),
        shareSongLink = RandomRussian.text("shareSongLink", StringsRu.shareSongLink),
        downloadTrack = RandomRussian.text("downloadTrack", StringsRu.downloadTrack),
        shuffle = RandomRussian.text("shuffle", StringsRu.shuffle),
        previousTrack = RandomRussian.text("previousTrack", StringsRu.previousTrack),
        nextTrack = RandomRussian.text("nextTrack", StringsRu.nextTrack),
        repeatMode = RandomRussian.text("repeatMode", StringsRu.repeatMode),
        lyricsTab = RandomRussian.text("lyricsTab", StringsRu.lyricsTab),
        timerChip = RandomRussian.text("timerChip", StringsRu.timerChip),
        queueTitle = RandomRussian.text("queueTitle", StringsRu.queueTitle),
        removeFromQueue = RandomRussian.text("removeFromQueue", StringsRu.removeFromQueue),
        sleepTimerDialogTitle =
            RandomRussian.text("sleepTimerDialogTitle", StringsRu.sleepTimerDialogTitle),
        sleepTimerOff = RandomRussian.text("sleepTimerOff", StringsRu.sleepTimerOff),
        minutesFormat = RandomRussian.text("minutesFormat", StringsRu.minutesFormat),
        loadingLyrics = RandomRussian.text("loadingLyrics", StringsRu.loadingLyrics),
        tapToOpenLyrics = RandomRussian.text("tapToOpenLyrics", StringsRu.tapToOpenLyrics),
        searchingLyrics = RandomRussian.text("searchingLyrics", StringsRu.searchingLyrics),
        lyricsNotFound = RandomRussian.text("lyricsNotFound", StringsRu.lyricsNotFound),
        lyricsHint = RandomRussian.text("lyricsHint", StringsRu.lyricsHint),
        retrySearch = RandomRussian.text("retrySearch", StringsRu.retrySearch),
        refreshLyrics = RandomRussian.text("refreshLyrics", StringsRu.refreshLyrics),
        syncedLyrics = RandomRussian.text("syncedLyrics", StringsRu.syncedLyrics),
        plainLyrics = RandomRussian.text("plainLyrics", StringsRu.plainLyrics),
        libraryTitle = RandomRussian.text("libraryTitle", StringsRu.libraryTitle),
        hiddenFolderName = RandomRussian.text("hiddenFolderName", StringsRu.hiddenFolderName),
        noTracksInHidden = RandomRussian.text("noTracksInHidden", StringsRu.noTracksInHidden),
        noTracksInFavorites =
            RandomRussian.text("noTracksInFavorites", StringsRu.noTracksInFavorites),
        noTracksLocal = RandomRussian.text("noTracksLocal", StringsRu.noTracksLocal),
        emptyTrackList = RandomRussian.text("emptyTrackList", StringsRu.emptyTrackList),
        deviceMusicTitle = RandomRussian.text("deviceMusicTitle", StringsRu.deviceMusicTitle),
        deviceMusicPermissionDesc =
            RandomRussian.text("deviceMusicPermissionDesc", StringsRu.deviceMusicPermissionDesc),
        grantPermission = RandomRussian.text("grantPermission", StringsRu.grantPermission),
        searchPlaceholder = RandomRussian.text("searchPlaceholder", StringsRu.searchPlaceholder),
        sortOptionTitleAsc = RandomRussian.text("sortOptionTitleAsc", StringsRu.sortOptionTitleAsc),
        sortOptionTitleDesc =
            RandomRussian.text("sortOptionTitleDesc", StringsRu.sortOptionTitleDesc),
        sortOptionDateDesc = RandomRussian.text("sortOptionDateDesc", StringsRu.sortOptionDateDesc),
        sortOptionArtist = RandomRussian.text("sortOptionArtist", StringsRu.sortOptionArtist),
        sortOptionDuration = RandomRussian.text("sortOptionDuration", StringsRu.sortOptionDuration),
        sorting = RandomRussian.text("sorting", StringsRu.sorting),
        rescanMemory = RandomRussian.text("rescanMemory", StringsRu.rescanMemory),
        mixMode = RandomRussian.text("mixMode", StringsRu.mixMode),
        newPlaylist = RandomRussian.text("newPlaylist", StringsRu.newPlaylist),
        newPlaylistDialogTitle =
            RandomRussian.text("newPlaylistDialogTitle", StringsRu.newPlaylistDialogTitle),
        playlistNameLabel = RandomRussian.text("playlistNameLabel", StringsRu.playlistNameLabel),
        autoPlaylistBadge = RandomRussian.text("autoPlaylistBadge", StringsRu.autoPlaylistBadge),
        playPlaylist = RandomRussian.text("playPlaylist", StringsRu.playPlaylist),
        deletePlaylist = RandomRussian.text("deletePlaylist", StringsRu.deletePlaylist),
        playAction = RandomRussian.text("playAction", StringsRu.playAction),
        emptyFavoritesHint = RandomRussian.text("emptyFavoritesHint", StringsRu.emptyFavoritesHint),
        emptyPlaylistHint = RandomRussian.text("emptyPlaylistHint", StringsRu.emptyPlaylistHint),
        addTracksButton = RandomRussian.text("addTracksButton", StringsRu.addTracksButton),
        removeFromPlaylist = RandomRussian.text("removeFromPlaylist", StringsRu.removeFromPlaylist),
        allTracksAlreadyInPlaylist =
            RandomRussian.text("allTracksAlreadyInPlaylist", StringsRu.allTracksAlreadyInPlaylist),
        deleteSongDialogTitle =
            RandomRussian.text("deleteSongDialogTitle", StringsRu.deleteSongDialogTitle),
        fileDetailsTitle = RandomRussian.text("fileDetailsTitle", StringsRu.fileDetailsTitle),
        playNow = RandomRussian.text("playNow", StringsRu.playNow),
        playNextInQueue = RandomRussian.text("playNextInQueue", StringsRu.playNextInQueue),
        editTagsAndCover = RandomRussian.text("editTagsAndCover", StringsRu.editTagsAndCover),
        editTagsSubtitle = RandomRussian.text("editTagsSubtitle", StringsRu.editTagsSubtitle),
        downloadToPhone = RandomRussian.text("downloadToPhone", StringsRu.downloadToPhone),
        downloadSubtitle = RandomRussian.text("downloadSubtitle", StringsRu.downloadSubtitle),
        shareSong = RandomRussian.text("shareSong", StringsRu.shareSong),
        removeFromFavorites =
            RandomRussian.text("removeFromFavorites", StringsRu.removeFromFavorites),
        addToFavorites = RandomRussian.text("addToFavorites", StringsRu.addToFavorites),
        addToPlaylistAction =
            RandomRussian.text("addToPlaylistAction", StringsRu.addToPlaylistAction),
        unhideFromStream = RandomRussian.text("unhideFromStream", StringsRu.unhideFromStream),
        hideFromStream = RandomRussian.text("hideFromStream", StringsRu.hideFromStream),
        unhideSubtitle = RandomRussian.text("unhideSubtitle", StringsRu.unhideSubtitle),
        hideSubtitle = RandomRussian.text("hideSubtitle", StringsRu.hideSubtitle),
        trackDetailsAction = RandomRussian.text("trackDetailsAction", StringsRu.trackDetailsAction),
        deleteFromDevice = RandomRussian.text("deleteFromDevice", StringsRu.deleteFromDevice),
        deleteFromDeviceSubtitle =
            RandomRussian.text("deleteFromDeviceSubtitle", StringsRu.deleteFromDeviceSubtitle),
        selectPlaylist = RandomRussian.text("selectPlaylist", StringsRu.selectPlaylist),
        newPlaylistPlaceholder =
            RandomRussian.text("newPlaylistPlaceholder", StringsRu.newPlaylistPlaceholder),
        tagEditorTitle = RandomRussian.text("tagEditorTitle", StringsRu.tagEditorTitle),
        coverFromGallery = RandomRussian.text("coverFromGallery", StringsRu.coverFromGallery),
        autoSearchTags = RandomRussian.text("autoSearchTags", StringsRu.autoSearchTags),
        recognizeViaShazam = RandomRussian.text("recognizeViaShazam", StringsRu.recognizeViaShazam),
        trackTitleField = RandomRussian.text("trackTitleField", StringsRu.trackTitleField),
        artistField = RandomRussian.text("artistField", StringsRu.artistField),
        albumField = RandomRussian.text("albumField", StringsRu.albumField),
        genreField = RandomRussian.text("genreField", StringsRu.genreField),
        yearField = RandomRussian.text("yearField", StringsRu.yearField),
        lyricsField = RandomRussian.text("lyricsField", StringsRu.lyricsField),
        settingsTitle = RandomRussian.text("settingsTitle", StringsRu.settingsTitle),
        appearanceSection = RandomRussian.text("appearanceSection", StringsRu.appearanceSection),
        themeModeTitle = RandomRussian.text("themeModeTitle", StringsRu.themeModeTitle),
        themeSystem = RandomRussian.text("themeSystem", StringsRu.themeSystem),
        themeDark = RandomRussian.text("themeDark", StringsRu.themeDark),
        themeLight = RandomRussian.text("themeLight", StringsRu.themeLight),
        dynamicColorsTitle = RandomRussian.text("dynamicColorsTitle", StringsRu.dynamicColorsTitle),
        squigglySliderTitle =
            RandomRussian.text("squigglySliderTitle", StringsRu.squigglySliderTitle),
        blurEffectTitle = RandomRussian.text("blurEffectTitle", StringsRu.blurEffectTitle),
        blurEffectDesc = RandomRussian.text("blurEffectDesc", StringsRu.blurEffectDesc),
        liquidGlassTitle = RandomRussian.text("liquidGlassTitle", StringsRu.liquidGlassTitle),
        liquidGlassDescSupported =
            RandomRussian.text("liquidGlassDescSupported", StringsRu.liquidGlassDescSupported),
        liquidGlassUnavailableTitle =
            RandomRussian.text(
                "liquidGlassUnavailableTitle",
                StringsRu.liquidGlassUnavailableTitle,
            ),
        albumArtGradientTitle =
            RandomRussian.text("albumArtGradientTitle", StringsRu.albumArtGradientTitle),
        dynamicCoverGradientTitle =
            RandomRussian.text("dynamicCoverGradientTitle", StringsRu.dynamicCoverGradientTitle),
        dynamicCoverGradientDesc =
            RandomRussian.text("dynamicCoverGradientDesc", StringsRu.dynamicCoverGradientDesc),
        betaUpdatesTitle = RandomRussian.text("betaUpdatesTitle", StringsRu.betaUpdatesTitle),
        betaUpdatesDesc = RandomRussian.text("betaUpdatesDesc", StringsRu.betaUpdatesDesc),
        autoTagFixTitle = RandomRussian.text("autoTagFixTitle", StringsRu.autoTagFixTitle),
        autoTagFixDesc = RandomRussian.text("autoTagFixDesc", StringsRu.autoTagFixDesc),
        languageTitle = RandomRussian.text("languageTitle", StringsRu.languageTitle),
        appIconTitle = RandomRussian.text("appIconTitle", StringsRu.appIconTitle),
        playbackSection = RandomRussian.text("playbackSection", StringsRu.playbackSection),
        playbackSpeedTitle = RandomRussian.text("playbackSpeedTitle", StringsRu.playbackSpeedTitle),
        sleepTimerTitle = RandomRussian.text("sleepTimerTitle", StringsRu.sleepTimerTitle),
        swipeGestureTitle = RandomRussian.text("swipeGestureTitle", StringsRu.swipeGestureTitle),
        playerSwipeTitle = RandomRussian.text("playerSwipeTitle", StringsRu.playerSwipeTitle),
        playerSwipeDesc = RandomRussian.text("playerSwipeDesc", StringsRu.playerSwipeDesc),
        searchBarPlacementTitle =
            RandomRussian.text("searchBarPlacementTitle", StringsRu.searchBarPlacementTitle),
        searchPlacementTitle =
            RandomRussian.text("searchPlacementTitle", StringsRu.searchPlacementTitle),
        searchPlacementBottom =
            RandomRussian.text("searchPlacementBottom", StringsRu.searchPlacementBottom),
        searchPlacementTop = RandomRussian.text("searchPlacementTop", StringsRu.searchPlacementTop),
        librarySection = RandomRussian.text("librarySection", StringsRu.librarySection),
        hideDemoTracksTitle =
            RandomRussian.text("hideDemoTracksTitle", StringsRu.hideDemoTracksTitle),
        rescanLibraryTitle = RandomRussian.text("rescanLibraryTitle", StringsRu.rescanLibraryTitle),
        tagFixSectionTitle = RandomRussian.text("tagFixSectionTitle", StringsRu.tagFixSectionTitle),
        checkForUpdatesTitle =
            RandomRussian.text("checkForUpdatesTitle", StringsRu.checkForUpdatesTitle),
        aboutAppSection = RandomRussian.text("aboutAppSection", StringsRu.aboutAppSection),
        appVersion = RandomRussian.text("appVersion", StringsRu.appVersion),
        cancel = RandomRussian.text("cancel", StringsRu.cancel),
        apply = RandomRussian.text("apply", StringsRu.apply),
        save = RandomRussian.text("save", StringsRu.save),
        classicLibraryDesignTitle =
            RandomRussian.text("classicLibraryDesignTitle", StringsRu.classicLibraryDesignTitle),
        classicLibraryDesignDesc =
            RandomRussian.text("classicLibraryDesignDesc", StringsRu.classicLibraryDesignDesc),
        shuffleAll = RandomRussian.text("shuffleAll", StringsRu.shuffleAll),
        rescan = RandomRussian.text("rescan", StringsRu.rescan),
        artistProfile = RandomRussian.text("artistProfile", StringsRu.artistProfile),
        tracksCount = RandomRussian.text("tracksCount", StringsRu.tracksCount),
        searchOnYouTube = RandomRussian.text("searchOnYouTube", StringsRu.searchOnYouTube),
        statsTracks = RandomRussian.text("statsTracks", StringsRu.statsTracks),
        statsFavorites = RandomRussian.text("statsFavorites", StringsRu.statsFavorites),
        statsPlaylists = RandomRussian.text("statsPlaylists", StringsRu.statsPlaylists),
        appIconSectionTitle =
            RandomRussian.text("appIconSectionTitle", StringsRu.appIconSectionTitle),
        appIconSectionDesc = RandomRussian.text("appIconSectionDesc", StringsRu.appIconSectionDesc),
        iconStyleDefault = RandomRussian.text("iconStyleDefault", StringsRu.iconStyleDefault),
        iconStyleMinimal = RandomRussian.text("iconStyleMinimal", StringsRu.iconStyleMinimal),
        iconStyleClassic = RandomRussian.text("iconStyleClassic", StringsRu.iconStyleClassic),
        appearanceAndPlayerSection =
            RandomRussian.text("appearanceAndPlayerSection", StringsRu.appearanceAndPlayerSection),
        materialYouTitle = RandomRussian.text("materialYouTitle", StringsRu.materialYouTitle),
        materialYouDesc = RandomRussian.text("materialYouDesc", StringsRu.materialYouDesc),
        pureMaterialYouTitle =
            RandomRussian.text("pureMaterialYouTitle", StringsRu.pureMaterialYouTitle),
        pureMaterialYouDesc =
            RandomRussian.text("pureMaterialYouDesc", StringsRu.pureMaterialYouDesc),
        showNavBarLabelsTitle =
            RandomRussian.text("showNavBarLabelsTitle", StringsRu.showNavBarLabelsTitle),
        showNavBarLabelsDesc =
            RandomRussian.text("showNavBarLabelsDesc", StringsRu.showNavBarLabelsDesc),
        squigglySliderDesc = RandomRussian.text("squigglySliderDesc", StringsRu.squigglySliderDesc),
        updateAvailable = RandomRussian.text("updateAvailable", StringsRu.updateAvailable),
        changelogTitle = RandomRussian.text("changelogTitle", StringsRu.changelogTitle),
        downloadingUpdate = RandomRussian.text("downloadingUpdate", StringsRu.downloadingUpdate),
        installUpdate = RandomRussian.text("installUpdate", StringsRu.installUpdate),
        downloadUpdate = RandomRussian.text("downloadUpdate", StringsRu.downloadUpdate),
        remindLater = RandomRussian.text("remindLater", StringsRu.remindLater),
        latestVersionInstalled =
            RandomRussian.text("latestVersionInstalled", StringsRu.latestVersionInstalled),
        moodForYourTaste = RandomRussian.text("moodForYourTaste", StringsRu.moodForYourTaste),
        moodEnergetic = RandomRussian.text("moodEnergetic", StringsRu.moodEnergetic),
        moodCalm = RandomRussian.text("moodCalm", StringsRu.moodCalm),
        moodNightVibe = RandomRussian.text("moodNightVibe", StringsRu.moodNightVibe),
        moodInstrumental = RandomRussian.text("moodInstrumental", StringsRu.moodInstrumental),
        refreshStream = RandomRussian.text("refreshStream", StringsRu.refreshStream),
        ytSearchHeader = RandomRussian.text("ytSearchHeader", StringsRu.ytSearchHeader),
        ytSearchPlaceholder =
            RandomRussian.text("ytSearchPlaceholder", StringsRu.ytSearchPlaceholder),
        tagFixTitle = RandomRussian.text("tagFixTitle", StringsRu.tagFixTitle),
        tagFixConfirmTitle = RandomRussian.text("tagFixConfirmTitle", StringsRu.tagFixConfirmTitle),
        tagFixSubtitle = RandomRussian.text("tagFixSubtitle", StringsRu.tagFixSubtitle),
        tagFixConfirmSubtitle =
            RandomRussian.text("tagFixConfirmSubtitle", StringsRu.tagFixConfirmSubtitle),
        tagFixProgressFormat =
            RandomRussian.text("tagFixProgressFormat", StringsRu.tagFixProgressFormat),
        tagFixShazamHint = RandomRussian.text("tagFixShazamHint", StringsRu.tagFixShazamHint),
        tagFixShazamMatchTitle =
            RandomRussian.text("tagFixShazamMatchTitle", StringsRu.tagFixShazamMatchTitle),
        tagFixWasFormat = RandomRussian.text("tagFixWasFormat", StringsRu.tagFixWasFormat),
        tagFixConfirmCount = RandomRussian.text("tagFixConfirmCount", StringsRu.tagFixConfirmCount),
        tagFixProblematicTab =
            RandomRussian.text("tagFixProblematicTab", StringsRu.tagFixProblematicTab),
        tagFixAllTab = RandomRussian.text("tagFixAllTab", StringsRu.tagFixAllTab),
        reset = RandomRussian.text("reset", StringsRu.reset),
        tagFixRandomNameHint =
            RandomRussian.text("tagFixRandomNameHint", StringsRu.tagFixRandomNameHint),
        tagFixNeedsCleaningHint =
            RandomRussian.text("tagFixNeedsCleaningHint", StringsRu.tagFixNeedsCleaningHint),
        tagFixFixSelected = RandomRussian.text("tagFixFixSelected", StringsRu.tagFixFixSelected),
        allTracksCount = RandomRussian.text("allTracksCount", StringsRu.allTracksCount),
        foundCount = RandomRussian.text("foundCount", StringsRu.foundCount),
        nothingFoundQuery = RandomRussian.text("nothingFoundQuery", StringsRu.nothingFoundQuery),
        ytHome = RandomRussian.text("ytHome", StringsRu.ytHome),
        ytLibraries = RandomRussian.text("ytLibraries", StringsRu.ytLibraries),
        ytMyWave = RandomRussian.text("ytMyWave", StringsRu.ytMyWave),
        searchInLibrary = RandomRussian.text("searchInLibrary", StringsRu.searchInLibrary),
        searchInYtMusic = RandomRussian.text("searchInYtMusic", StringsRu.searchInYtMusic),
        googleSignInTitle = RandomRussian.text("googleSignInTitle", StringsRu.googleSignInTitle),
        googleSignInSubtitle =
            RandomRussian.text("googleSignInSubtitle", StringsRu.googleSignInSubtitle),
        googleConnectedDesc =
            RandomRussian.text("googleConnectedDesc", StringsRu.googleConnectedDesc),
        syncingWithGoogle = RandomRussian.text("syncingWithGoogle", StringsRu.syncingWithGoogle),
        loadingYtFeed = RandomRussian.text("loadingYtFeed", StringsRu.loadingYtFeed),
        ytLibrariesTitle = RandomRussian.text("ytLibrariesTitle", StringsRu.ytLibrariesTitle),
        ytLibrariesSubtitle =
            RandomRussian.text("ytLibrariesSubtitle", StringsRu.ytLibrariesSubtitle),
        myPlaylistsCount = RandomRussian.text("myPlaylistsCount", StringsRu.myPlaylistsCount),
        tracksCountFormat = RandomRussian.text("tracksCountFormat", StringsRu.tracksCountFormat),
        favoritesAndSynced = RandomRussian.text("favoritesAndSynced", StringsRu.favoritesAndSynced),
        newLibraryPlaylist = RandomRussian.text("newLibraryPlaylist", StringsRu.newLibraryPlaylist),
        nowInMyWaveFormat = RandomRussian.text("nowInMyWaveFormat", StringsRu.nowInMyWaveFormat),
        switchLayout = RandomRussian.text("switchLayout", StringsRu.switchLayout),
        autoTagFixPromptTitle =
            RandomRussian.text("autoTagFixPromptTitle", StringsRu.autoTagFixPromptTitle),
        autoTagFixPromptMessage =
            RandomRussian.text("autoTagFixPromptMessage", StringsRu.autoTagFixPromptMessage),
        fixNow = RandomRussian.text("fixNow", StringsRu.fixNow),
        remindLaterBtn = RandomRussian.text("remindLaterBtn", StringsRu.remindLaterBtn),
        dynamicPlayerColorsTitle =
            RandomRussian.text("dynamicPlayerColorsTitle", StringsRu.dynamicPlayerColorsTitle),
        dynamicPlayerColorsDesc =
            RandomRussian.text("dynamicPlayerColorsDesc", StringsRu.dynamicPlayerColorsDesc),
        downloadingSong = RandomRussian.text("downloadingSong", StringsRu.downloadingSong),
        extra =
            ExtraStrings(
                backupTitle = RandomRussian.text("backupTitle", StringsRu.extra.backupTitle),
                backupDesc = RandomRussian.text("backupDesc", StringsRu.extra.backupDesc),
                createBackup = RandomRussian.text("createBackup", StringsRu.extra.createBackup),
                restoreBackup = RandomRussian.text("restoreBackup", StringsRu.extra.restoreBackup),
                backupCopiedToast =
                    RandomRussian.text("backupCopiedToast", StringsRu.extra.backupCopiedToast),
                backupRestoredToast =
                    RandomRussian.text("backupRestoredToast", StringsRu.extra.backupRestoredToast),
                backupInvalidToast =
                    RandomRussian.text("backupInvalidToast", StringsRu.extra.backupInvalidToast),
                pasteBackupHint =
                    RandomRussian.text("pasteBackupHint", StringsRu.extra.pasteBackupHint),
                pasteFromClipboard =
                    RandomRussian.text("pasteFromClipboard", StringsRu.extra.pasteFromClipboard),
                restore = RandomRussian.text("restore", StringsRu.extra.restore),
                shareBackup = RandomRussian.text("shareBackup", StringsRu.extra.shareBackup),
                backupWarningTitle =
                    RandomRussian.text("backupWarningTitle", StringsRu.extra.backupWarningTitle),
                backupWarningMessage =
                    RandomRussian.text(
                        "backupWarningMessage",
                        StringsRu.extra.backupWarningMessage,
                    ),
                noThanks = RandomRussian.text("noThanks", StringsRu.extra.noThanks),
                easterEgg1Title =
                    RandomRussian.text("easterEgg1Title", StringsRu.extra.easterEgg1Title),
                easterEgg1Text =
                    RandomRussian.text("easterEgg1Text", StringsRu.extra.easterEgg1Text),
                easterEgg1Button =
                    RandomRussian.text("easterEgg1Button", StringsRu.extra.easterEgg1Button),
                easterEgg2Title =
                    RandomRussian.text("easterEgg2Title", StringsRu.extra.easterEgg2Title),
                easterEgg2Text =
                    RandomRussian.text("easterEgg2Text", StringsRu.extra.easterEgg2Text),
                easterEgg2Button =
                    RandomRussian.text("easterEgg2Button", StringsRu.extra.easterEgg2Button),
                deletePlaylistConfirmTitle =
                    RandomRussian.text(
                        "deletePlaylistConfirmTitle",
                        StringsRu.extra.deletePlaylistConfirmTitle,
                    ),
                deletePlaylistConfirmMessage =
                    RandomRussian.text(
                        "deletePlaylistConfirmMessage",
                        StringsRu.extra.deletePlaylistConfirmMessage,
                    ),
                selectAll = RandomRussian.text("selectAll", StringsRu.extra.selectAll),
                deselectAll = RandomRussian.text("deselectAll", StringsRu.extra.deselectAll),
                confirm = RandomRussian.text("confirm", StringsRu.extra.confirm),
                selectedCount = RandomRussian.text("selectedCount", StringsRu.extra.selectedCount),
                backgroundPlaybackTitle =
                    RandomRussian.text(
                        "backgroundPlaybackTitle",
                        StringsRu.extra.backgroundPlaybackTitle,
                    ),
                backgroundPlaybackDesc =
                    RandomRussian.text(
                        "backgroundPlaybackDesc",
                        StringsRu.extra.backgroundPlaybackDesc,
                    ),
                mostPlayedTrack =
                    RandomRussian.text("mostPlayedTrack", StringsRu.extra.mostPlayedTrack),
                playsCountFormat =
                    RandomRussian.text("playsCountFormat", StringsRu.extra.playsCountFormat),
                totalListeningTime =
                    RandomRussian.text("totalListeningTime", StringsRu.extra.totalListeningTime),
                topAlbumOnRepeat =
                    RandomRussian.text("topAlbumOnRepeat", StringsRu.extra.topAlbumOnRepeat),
                hoursMinutesFormat =
                    RandomRussian.text("hoursMinutesFormat", StringsRu.extra.hoursMinutesFormat),
                noStatsYet = RandomRussian.text("noStatsYet", StringsRu.extra.noStatsYet),
                artistTopTracks =
                    RandomRussian.text("artistTopTracks", StringsRu.extra.artistTopTracks),
                artistAlbums = RandomRussian.text("artistAlbums", StringsRu.extra.artistAlbums),
                artistSinglesAndEPs =
                    RandomRussian.text("artistSinglesAndEPs", StringsRu.extra.artistSinglesAndEPs),
                artistPlayAll = RandomRussian.text("artistPlayAll", StringsRu.extra.artistPlayAll),
                artistShuffle = RandomRussian.text("artistShuffle", StringsRu.extra.artistShuffle),
                nativeGoogleSignIn =
                    RandomRussian.text("nativeGoogleSignIn", StringsRu.extra.nativeGoogleSignIn),
                webGoogleSignIn =
                    RandomRussian.text("webGoogleSignIn", StringsRu.extra.webGoogleSignIn),
                selectGoogleAccount =
                    RandomRussian.text("selectGoogleAccount", StringsRu.extra.selectGoogleAccount),
                translateLyrics =
                    RandomRussian.text("translateLyrics", StringsRu.extra.translateLyrics),
                translatingLyrics =
                    RandomRussian.text("translatingLyrics", StringsRu.extra.translatingLyrics),
                classicRemovedTitle =
                    RandomRussian.text("classicRemovedTitle", StringsRu.extra.classicRemovedTitle),
                classicRemovedMessage =
                    RandomRussian.text(
                        "classicRemovedMessage",
                        StringsRu.extra.classicRemovedMessage,
                    ),
                classicRemovedConfirm =
                    RandomRussian.text(
                        "classicRemovedConfirm",
                        StringsRu.extra.classicRemovedConfirm,
                    ),
                tracksInAlbum = RandomRussian.text("tracksInAlbum", StringsRu.extra.tracksInAlbum),
                navSoundCloud = RandomRussian.text("navSoundCloud", StringsRu.extra.navSoundCloud),
                soundCloudTitle =
                    RandomRussian.text("soundCloudTitle", StringsRu.extra.soundCloudTitle),
                soundCloudPlaceholder =
                    RandomRussian.text(
                        "soundCloudPlaceholder",
                        StringsRu.extra.soundCloudPlaceholder,
                    ),
                signInSoundCloud =
                    RandomRussian.text("signInSoundCloud", StringsRu.extra.signInSoundCloud),
                unifiedPlaylistsTitle =
                    RandomRussian.text(
                        "unifiedPlaylistsTitle",
                        StringsRu.extra.unifiedPlaylistsTitle,
                    ),
                unifiedPlaylist =
                    RandomRussian.text("unifiedPlaylist", StringsRu.extra.unifiedPlaylist),
                createUnifiedPlaylist =
                    RandomRussian.text(
                        "createUnifiedPlaylist",
                        StringsRu.extra.createUnifiedPlaylist,
                    ),
                unifiedPlaylistInfoTitle =
                    RandomRussian.text(
                        "unifiedPlaylistInfoTitle",
                        StringsRu.extra.unifiedPlaylistInfoTitle,
                    ),
                unifiedPlaylistInfoDesc =
                    RandomRussian.text(
                        "unifiedPlaylistInfoDesc",
                        StringsRu.extra.unifiedPlaylistInfoDesc,
                    ),
                unifiedFavorites =
                    RandomRussian.text("unifiedFavorites", StringsRu.extra.unifiedFavorites),
                addToUnifiedPlaylist =
                    RandomRussian.text(
                        "addToUnifiedPlaylist",
                        StringsRu.extra.addToUnifiedPlaylist,
                    ),
                scTrendingByGenre =
                    RandomRussian.text("scTrendingByGenre", StringsRu.extra.scTrendingByGenre),
                scYourLikes = RandomRussian.text("scYourLikes", StringsRu.extra.scYourLikes),
                scMadeForYou = RandomRussian.text("scMadeForYou", StringsRu.extra.scMadeForYou),
                scDailyDropsTitle =
                    RandomRussian.text("scDailyDropsTitle", StringsRu.extra.scDailyDropsTitle),
                scDailyDropsDesc =
                    RandomRussian.text("scDailyDropsDesc", StringsRu.extra.scDailyDropsDesc),
                scWeeklyWaveTitle =
                    RandomRussian.text("scWeeklyWaveTitle", StringsRu.extra.scWeeklyWaveTitle),
                scWeeklyWaveDesc =
                    RandomRussian.text("scWeeklyWaveDesc", StringsRu.extra.scWeeklyWaveDesc),
                scMoreOfWhatYouLike =
                    RandomRussian.text("scMoreOfWhatYouLike", StringsRu.extra.scMoreOfWhatYouLike),
                scTrendingTracks =
                    RandomRussian.text("scTrendingTracks", StringsRu.extra.scTrendingTracks),
                scTopCharts = RandomRussian.text("scTopCharts", StringsRu.extra.scTopCharts),
                scNoTracksAvailable =
                    RandomRussian.text("scNoTracksAvailable", StringsRu.extra.scNoTracksAvailable),
                scSavedTracks = RandomRussian.text("scSavedTracks", StringsRu.extra.scSavedTracks),
                scNoSavedTracks =
                    RandomRussian.text("scNoSavedTracks", StringsRu.extra.scNoSavedTracks),
                scSignInSubtitle =
                    RandomRussian.text("scSignInSubtitle", StringsRu.extra.scSignInSubtitle),
                scSignOutConfirmTitle =
                    RandomRussian.text(
                        "scSignOutConfirmTitle",
                        StringsRu.extra.scSignOutConfirmTitle,
                    ),
                scSignOutConfirmDesc =
                    RandomRussian.text(
                        "scSignOutConfirmDesc",
                        StringsRu.extra.scSignOutConfirmDesc,
                    ),
                scProfileTitle =
                    RandomRussian.text("scProfileTitle", StringsRu.extra.scProfileTitle),
                scLikedTracksCount =
                    RandomRussian.text("scLikedTracksCount", StringsRu.extra.scLikedTracksCount),
                scUploadedTracksCount =
                    RandomRussian.text(
                        "scUploadedTracksCount",
                        StringsRu.extra.scUploadedTracksCount,
                    ),
                scFollowersCount =
                    RandomRussian.text("scFollowersCount", StringsRu.extra.scFollowersCount),
                scAddToUnifiedFavorites =
                    RandomRussian.text(
                        "scAddToUnifiedFavorites",
                        StringsRu.extra.scAddToUnifiedFavorites,
                    ),
                scRemoveFromUnifiedFavorites =
                    RandomRussian.text(
                        "scRemoveFromUnifiedFavorites",
                        StringsRu.extra.scRemoveFromUnifiedFavorites,
                    ),
                scInUnifiedFavorites =
                    RandomRussian.text(
                        "scInUnifiedFavorites",
                        StringsRu.extra.scInUnifiedFavorites,
                    ),
                scForceTokenLogin =
                    RandomRussian.text("scForceTokenLogin", StringsRu.extra.scForceTokenLogin),
                scWebBrowserLogin =
                    RandomRussian.text("scWebBrowserLogin", StringsRu.extra.scWebBrowserLogin),
                scGenreAll = RandomRussian.text("scGenreAll", StringsRu.extra.scGenreAll),
                scGenrePhonk = RandomRussian.text("scGenrePhonk", StringsRu.extra.scGenrePhonk),
                scGenreElectronic =
                    RandomRussian.text("scGenreElectronic", StringsRu.extra.scGenreElectronic),
                scGenreHipHop = RandomRussian.text("scGenreHipHop", StringsRu.extra.scGenreHipHop),
                scGenreRemix = RandomRussian.text("scGenreRemix", StringsRu.extra.scGenreRemix),
                scGenreRock = RandomRussian.text("scGenreRock", StringsRu.extra.scGenreRock),
                scGenreTrance = RandomRussian.text("scGenreTrance", StringsRu.extra.scGenreTrance),
                scGenreDancehall =
                    RandomRussian.text("scGenreDancehall", StringsRu.extra.scGenreDancehall),
                pressBackAgainToExit =
                    RandomRussian.text(
                        "pressBackAgainToExit",
                        StringsRu.extra.pressBackAgainToExit,
                    ),
                betaSeparateHeaderBubblesTitle =
                    RandomRussian.text(
                        "betaSeparateHeaderBubblesTitle",
                        StringsRu.extra.betaSeparateHeaderBubblesTitle,
                    ),
                betaSeparateHeaderBubblesDesc =
                    RandomRussian.text(
                        "betaSeparateHeaderBubblesDesc",
                        StringsRu.extra.betaSeparateHeaderBubblesDesc,
                    ),
                betaAutoHideNavBarTitle =
                    RandomRussian.text(
                        "betaAutoHideNavBarTitle",
                        StringsRu.extra.betaAutoHideNavBarTitle,
                    ),
                betaAutoHideNavBarDesc =
                    RandomRussian.text(
                        "betaAutoHideNavBarDesc",
                        StringsRu.extra.betaAutoHideNavBarDesc,
                    ),
                betaDynamicPlaybackContextTitle =
                    RandomRussian.text(
                        "betaDynamicPlaybackContextTitle",
                        StringsRu.extra.betaDynamicPlaybackContextTitle,
                    ),
                betaDynamicPlaybackContextDesc =
                    RandomRussian.text(
                        "betaDynamicPlaybackContextDesc",
                        StringsRu.extra.betaDynamicPlaybackContextDesc,
                    ),
                fromAlbum = RandomRussian.text("fromAlbum", StringsRu.extra.fromAlbum),
                fromMyWave = RandomRussian.text("fromMyWave", StringsRu.extra.fromMyWave),
                fromRadio = RandomRussian.text("fromRadio", StringsRu.extra.fromRadio),
                singleTrack = RandomRussian.text("singleTrack", StringsRu.extra.singleTrack),
                fromPlaylist = RandomRussian.text("fromPlaylist", StringsRu.extra.fromPlaylist),
                fromFavorites = RandomRussian.text("fromFavorites", StringsRu.extra.fromFavorites),
                copyLyrics = RandomRussian.text("copyLyrics", StringsRu.extra.copyLyrics),
                lyricsCopied = RandomRussian.text("lyricsCopied", StringsRu.extra.lyricsCopied),
                trackFromYtMusic =
                    RandomRussian.text("trackFromYtMusic", StringsRu.extra.trackFromYtMusic),
                trackFromSoundCloud =
                    RandomRussian.text("trackFromSoundCloud", StringsRu.extra.trackFromSoundCloud),
                trackFromLocal =
                    RandomRussian.text("trackFromLocal", StringsRu.extra.trackFromLocal),
                radioTracksTitle =
                    RandomRussian.text("radioTracksTitle", StringsRu.extra.radioTracksTitle),
                equalizerTitle =
                    RandomRussian.text("equalizerTitle", StringsRu.extra.equalizerTitle),
                betaCategorizedSettingsTitle =
                    RandomRussian.text(
                        "betaCategorizedSettingsTitle",
                        StringsRu.extra.betaCategorizedSettingsTitle,
                    ),
                betaCategorizedSettingsDesc =
                    RandomRussian.text(
                        "betaCategorizedSettingsDesc",
                        StringsRu.extra.betaCategorizedSettingsDesc,
                    ),
                settingsCategoryAppearance =
                    RandomRussian.text(
                        "settingsCategoryAppearance",
                        StringsRu.extra.settingsCategoryAppearance,
                    ),
                settingsCategoryNavigation =
                    RandomRussian.text(
                        "settingsCategoryNavigation",
                        StringsRu.extra.settingsCategoryNavigation,
                    ),
                settingsCategoryPlayback =
                    RandomRussian.text(
                        "settingsCategoryPlayback",
                        StringsRu.extra.settingsCategoryPlayback,
                    ),
                settingsCategoryBeta =
                    RandomRussian.text(
                        "settingsCategoryBeta",
                        StringsRu.extra.settingsCategoryBeta,
                    ),
                settingsCategorySystem =
                    RandomRussian.text(
                        "settingsCategorySystem",
                        StringsRu.extra.settingsCategorySystem,
                    ),
                audioOutputSpeaker =
                    RandomRussian.text("audioOutputSpeaker", StringsRu.extra.audioOutputSpeaker),
                audioOutputHeadphones =
                    RandomRussian.text(
                        "audioOutputHeadphones",
                        StringsRu.extra.audioOutputHeadphones,
                    ),
                audioOutputBluetooth =
                    RandomRussian.text(
                        "audioOutputBluetooth",
                        StringsRu.extra.audioOutputBluetooth,
                    ),
                equalizerStatusEnabled =
                    RandomRussian.text(
                        "equalizerStatusEnabled",
                        StringsRu.extra.equalizerStatusEnabled,
                    ),
                equalizerStatusDisabled =
                    RandomRussian.text(
                        "equalizerStatusDisabled",
                        StringsRu.extra.equalizerStatusDisabled,
                    ),
                audioOutputPickerTitle =
                    RandomRussian.text(
                        "audioOutputPickerTitle",
                        StringsRu.extra.audioOutputPickerTitle,
                    ),
                audioOutputPickerDesc =
                    RandomRussian.text(
                        "audioOutputPickerDesc",
                        StringsRu.extra.audioOutputPickerDesc,
                    ),
                connectBluetoothDevice =
                    RandomRussian.text(
                        "connectBluetoothDevice",
                        StringsRu.extra.connectBluetoothDevice,
                    ),
                betaHideDividersTitle =
                    RandomRussian.text(
                        "betaHideDividersTitle",
                        StringsRu.extra.betaHideDividersTitle,
                    ),
                betaHideDividersDesc =
                    RandomRussian.text(
                        "betaHideDividersDesc",
                        StringsRu.extra.betaHideDividersDesc,
                    ),
                settingsCategoryAppearanceDesc =
                    RandomRussian.text(
                        "settingsCategoryAppearanceDesc",
                        StringsRu.extra.settingsCategoryAppearanceDesc,
                    ),
                settingsCategoryNavigationDesc =
                    RandomRussian.text(
                        "settingsCategoryNavigationDesc",
                        StringsRu.extra.settingsCategoryNavigationDesc,
                    ),
                settingsCategoryPlaybackDesc =
                    RandomRussian.text(
                        "settingsCategoryPlaybackDesc",
                        StringsRu.extra.settingsCategoryPlaybackDesc,
                    ),
                settingsCategoryBetaDesc =
                    RandomRussian.text(
                        "settingsCategoryBetaDesc",
                        StringsRu.extra.settingsCategoryBetaDesc,
                    ),
                settingsCategorySystemDesc =
                    RandomRussian.text(
                        "settingsCategorySystemDesc",
                        StringsRu.extra.settingsCategorySystemDesc,
                    ),
                welcomeTitle = RandomRussian.text("welcomeTitle", StringsRu.extra.welcomeTitle),
                welcomeFeature1Title =
                    RandomRussian.text(
                        "welcomeFeature1Title",
                        StringsRu.extra.welcomeFeature1Title,
                    ),
                welcomeFeature1Desc =
                    RandomRussian.text("welcomeFeature1Desc", StringsRu.extra.welcomeFeature1Desc),
                welcomeFeature2Title =
                    RandomRussian.text(
                        "welcomeFeature2Title",
                        StringsRu.extra.welcomeFeature2Title,
                    ),
                welcomeFeature2Desc =
                    RandomRussian.text("welcomeFeature2Desc", StringsRu.extra.welcomeFeature2Desc),
                welcomeFeature3Title =
                    RandomRussian.text(
                        "welcomeFeature3Title",
                        StringsRu.extra.welcomeFeature3Title,
                    ),
                welcomeFeature3Desc =
                    RandomRussian.text("welcomeFeature3Desc", StringsRu.extra.welcomeFeature3Desc),
                welcomeFeature4Title =
                    RandomRussian.text(
                        "welcomeFeature4Title",
                        StringsRu.extra.welcomeFeature4Title,
                    ),
                welcomeFeature4Desc =
                    RandomRussian.text("welcomeFeature4Desc", StringsRu.extra.welcomeFeature4Desc),
                nextButton = RandomRussian.text("nextButton", StringsRu.extra.nextButton),
                permissionNotificationsTitle =
                    RandomRussian.text(
                        "permissionNotificationsTitle",
                        StringsRu.extra.permissionNotificationsTitle,
                    ),
                permissionNotificationsDesc =
                    RandomRussian.text(
                        "permissionNotificationsDesc",
                        StringsRu.extra.permissionNotificationsDesc,
                    ),
                permissionStorageTitle =
                    RandomRussian.text(
                        "permissionStorageTitle",
                        StringsRu.extra.permissionStorageTitle,
                    ),
                permissionStorageDesc =
                    RandomRussian.text(
                        "permissionStorageDesc",
                        StringsRu.extra.permissionStorageDesc,
                    ),
                allowButton = RandomRussian.text("allowButton", StringsRu.extra.allowButton),
                allowedStatus = RandomRussian.text("allowedStatus", StringsRu.extra.allowedStatus),
                setupChoiceTitle =
                    RandomRussian.text("setupChoiceTitle", StringsRu.extra.setupChoiceTitle),
                setupChoiceDesc =
                    RandomRussian.text("setupChoiceDesc", StringsRu.extra.setupChoiceDesc),
                setupButton = RandomRussian.text("setupButton", StringsRu.extra.setupButton),
                skipButton = RandomRussian.text("skipButton", StringsRu.extra.skipButton),
                allSetButton = RandomRussian.text("allSetButton", StringsRu.extra.allSetButton),
                welcomeFinishToast =
                    RandomRussian.text("welcomeFinishToast", StringsRu.extra.welcomeFinishToast),
                testOnboardingTitle =
                    RandomRussian.text("testOnboardingTitle", StringsRu.extra.testOnboardingTitle),
                testOnboardingDesc =
                    RandomRussian.text("testOnboardingDesc", StringsRu.extra.testOnboardingDesc),
            ),
    )
