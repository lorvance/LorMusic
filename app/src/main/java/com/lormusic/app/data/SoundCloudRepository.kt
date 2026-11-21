package com.lormusic.app.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.lormusic.app.model.Song
import com.lormusic.app.model.SongTagOverride
import com.lormusic.app.model.SoundCloudUserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

object SoundCloudRepository {

    private const val DEFAULT_CLIENT_ID = "3GKD969NJWD1eB6gvQJN0GnueYbcWszv"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    @Volatile
    private var activeClientId: String = DEFAULT_CLIENT_ID

    private val clientRefreshMutex = Mutex()
    private val transcodingsCache = ConcurrentHashMap<String, String>()
    private val trackPermalinkMap = ConcurrentHashMap<String, String>()

    var currentAuthToken: String? = null
    var currentProfile: SoundCloudUserProfile? = null

    fun cleanToken(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw.trim()
            .removePrefix("OAuth ")
            .removePrefix("oauth ")
            .removePrefix("Bearer ")
            .removePrefix("bearer ")
            .trim('"', '\'', ' ', '\\')
    }

    private fun getClientId(): String = activeClientId

    private fun openConnection(
        urlStr: String,
        includeAuth: Boolean = false,
        connectTimeout: Int = 10000,
        readTimeout: Int = 12000
    ): HttpURLConnection {
        return SoundCloudTransport.open(urlStr, if(includeAuth) cleanToken(currentAuthToken) else null, connectTimeout, readTimeout)
    }

    private suspend fun refreshClientIdIfNeeded(): String = clientRefreshMutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val conn = URL("https://soundcloud.com/discover").openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", USER_AGENT)
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                val html = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val pattern = Pattern.compile("https://[a-zA-Z0-9.-]+\\.sndcdn\\.com/assets/[a-zA-Z0-9.-]+\\.js")
                val matcher = pattern.matcher(html)
                val scripts = mutableListOf<String>()
                while (matcher.find()) {
                    scripts.add(matcher.group())
                }

                val idPattern = Pattern.compile("client_id[:=]\"?([a-zA-Z0-9]{32})\"?")
                for (scriptUrl in scripts.reversed()) {
                    try {
                        val sConn = URL(scriptUrl).openConnection() as HttpURLConnection
                        sConn.setRequestProperty("User-Agent", USER_AGENT)
                        sConn.connectTimeout = 6000
                        sConn.readTimeout = 6000
                        val js = sConn.inputStream.bufferedReader().use { it.readText() }
                        sConn.disconnect()

                        val idMatcher = idPattern.matcher(js)
                        if (idMatcher.find()) {
                            val found = idMatcher.group(1)
                            if (!found.isNullOrBlank()) {
                                activeClientId = found
                                return@withContext found
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
            } catch (_: Exception) {
            }
            activeClientId
        }
    }

    private fun parseSongFromJson(obj: JSONObject): Song? {
        val trackId = obj.optLong("id", -1L)
        if (trackId <= 0L) return null

        val title = obj.optString("title", "Без названия").trim()
        val userObj = obj.optJSONObject("user")
        val artist = userObj?.optString("username", "SoundCloud")?.trim() ?: "SoundCloud"
        val durationMs = obj.optLong("full_duration", 0L).takeIf { it > 0L } ?: obj.optLong("duration", 0L).coerceAtLeast(0L)
        val rawArt = obj.optString("artwork_url", "").ifBlank {
            userObj?.optString("avatar_url", "") ?: ""
        }
        val artworkUrl = if (rawArt.isNotBlank()) {
            rawArt.replace("-large", "-t500x500")
        } else null

        val mediaObj = obj.optJSONObject("media")
        val transcodings = mediaObj?.optJSONArray("transcodings")
        if (transcodings != null && transcodings.length() > 0) {
            var selectedUrl: String? = null
            for (i in 0 until transcodings.length()) {
                val tc = transcodings.optJSONObject(i) ?: continue
                val format = tc.optJSONObject("format")
                val protocol = format?.optString("protocol", "") ?: ""
                val tcUrl = tc.optString("url", "")
                if (protocol == "progressive" && tcUrl.isNotBlank()) {
                    selectedUrl = tcUrl
                    break
                }
            }
            if (selectedUrl == null) {
                selectedUrl = transcodings.optJSONObject(0)?.optString("url", "")
            }
            if (!selectedUrl.isNullOrBlank()) {
                transcodingsCache[trackId.toString()] = selectedUrl
            }
        }

        val permalink = obj.optString("permalink_url", "").trim()
        if (permalink.isNotBlank()) {
            trackPermalinkMap["sc_$trackId"] = permalink
            trackPermalinkMap[trackId.toString()] = permalink
        }

        return Song(
            id = "sc_$trackId",
            title = title,
            artist = artist,
            album = "SoundCloud",
            durationMs = durationMs,
            uri = "sc://$trackId",
            coverColorSeed = trackId.hashCode(),
            albumArtUri = artworkUrl,
            isLocal = false,
            folderName = "SoundCloud",
            dateAddedSec = System.currentTimeMillis() / 1000L,
            genre = obj.optString("genre", ""),
            year = "",
            isRemoteSc = true
        )
    }

    suspend fun searchSoundCloud(query: String, limit: Int = 25): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val songs = mutableListOf<Song>()
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")

        for (attempt in 0..1) {
            try {
                val cid = getClientId()
                val url = "https://api-v2.soundcloud.com/search/tracks?q=$encoded&client_id=$cid&limit=$limit&offset=0"
                val conn = openConnection(url)
                val code = conn.responseCode

                if (code == 401 && attempt == 0) {
                    conn.disconnect()
                    refreshClientIdIfNeeded()
                    continue
                }

                if (code in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    conn.disconnect()
                    val json = JSONObject(body)
                    val collection = json.optJSONArray("collection") ?: JSONArray()
                    for (i in 0 until collection.length()) {
                        val item = collection.optJSONObject(i) ?: continue
                        val track = item.optJSONObject("track") ?: item
                        parseSongFromJson(track)?.let {
                            if (it.durationMs < 600_000L) {
                                songs.add(it)
                            }
                        }
                    }
                    return@withContext songs
                } else {
                    conn.disconnect()
                }
            } catch (_: Exception) {
            }
        }
        songs
    }

    suspend fun fetchCharts(limit: Int = 30): List<Song> = withContext(Dispatchers.IO) {
        val queries = listOf("phonk", "trending", "remix", "hip hop", "bass", "electronic", "rock")
        val combined = mutableListOf<Song>()
        val seenIds = mutableSetOf<String>()
        for (q in queries) {
            val results = searchSoundCloud(q, limit = 15)
            for (song in results) {
                if (seenIds.add(song.id)) {
                    combined.add(song)
                    if (combined.size >= limit) return@withContext combined
                }
            }
        }
        combined
    }

    suspend fun fetchTrending(limit: Int = 30): List<Song> = withContext(Dispatchers.IO) {
        val queries = listOf("phonk", "dancehall", "trance", "reggae", "electronic")
        val combined = mutableListOf<Song>()
        val seenIds = mutableSetOf<String>()
        for (q in queries) {
            val results = searchSoundCloud(q, limit = 15)
            for (song in results) {
                if (seenIds.add(song.id)) {
                    combined.add(song)
                    if (combined.size >= limit) return@withContext combined
                }
            }
        }
        combined
    }

    suspend fun fetchUserLikes(limit: Int = 30): List<Song> = withContext(Dispatchers.IO) {
        val token = currentAuthToken ?: return@withContext emptyList()
        val songs = mutableListOf<Song>()
        try {
            val cid = getClientId()
            val url = "https://api-v2.soundcloud.com/me/likes/tracks?client_id=$cid&limit=$limit"
            val conn = openConnection(url, includeAuth = true)
            if (conn.responseCode in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = JSONObject(body)
                val collection = json.optJSONArray("collection") ?: JSONArray()
                for (i in 0 until collection.length()) {
                    val item = collection.optJSONObject(i) ?: continue
                    val track = item.optJSONObject("track") ?: item
                    parseSongFromJson(track)?.let {
                        if (it.durationMs < 600_000L) songs.add(it)
                    }
                }
                return@withContext songs
            }
            conn.disconnect()
        } catch (_: Exception) {
        }
        songs
    }

    suspend fun fetchUserStream(limit: Int = 30): List<Song> = withContext(Dispatchers.IO) {
        val token = currentAuthToken ?: return@withContext emptyList()
        val songs = mutableListOf<Song>()
        try {
            val cid = getClientId()
            val url = "https://api-v2.soundcloud.com/stream?client_id=$cid&limit=$limit"
            val conn = openConnection(url, includeAuth = true)
            if (conn.responseCode in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = JSONObject(body)
                val collection = json.optJSONArray("collection") ?: JSONArray()
                for (i in 0 until collection.length()) {
                    val item = collection.optJSONObject(i) ?: continue
                    val track = item.optJSONObject("track") ?: item
                    parseSongFromJson(track)?.let {
                        if (it.durationMs < 600_000L) songs.add(it)
                    }
                }
                return@withContext songs
            }
            conn.disconnect()
        } catch (_: Exception) {
        }
        songs
    }

    suspend fun fetchUserProfile(authToken: String): SoundCloudUserProfile? = withContext(Dispatchers.IO) {
        val clean = cleanToken(authToken)
        if (clean.isBlank() || clean == "guest") return@withContext null
        for (attempt in 0..1) {
            try {
                val cid = if (attempt == 0) getClientId() else refreshClientIdIfNeeded()
                val url = if (cid.isNotBlank()) {
                    "https://api-v2.soundcloud.com/me?client_id=$cid"
                } else {
                    "https://api-v2.soundcloud.com/me"
                }
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", USER_AGENT)
                conn.setRequestProperty("Authorization", "OAuth $clean")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000

                if (conn.responseCode in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    conn.disconnect()
                    val obj = JSONObject(body)
                    val username = obj.optString("username", "").ifBlank {
                        obj.optString("full_name", "").ifBlank { "SoundCloud User" }
                    }
                    val rawAvatar = obj.optString("avatar_url", "").ifBlank {
                        obj.optString("avatar_url_template", "")
                    }
                    val avatar = if (rawAvatar.isNotBlank()) rawAvatar.replace("-large", "-t500x500") else null
                    val permalink = obj.optString("permalink_url", "").takeIf { it.isNotBlank() }
                    val trackCount = obj.optInt("track_count", 0)
                    val likesCount = obj.optInt("public_favorites_count", obj.optInt("likes_count", 0))
                    val followersCount = obj.optInt("followers_count", 0)
                    val followingsCount = obj.optInt("followings_count", 0)

                    val profile = SoundCloudUserProfile(
                        username = username,
                        avatarUrl = avatar,
                        permalinkUrl = permalink,
                        trackCount = trackCount,
                        likesCount = likesCount,
                        followersCount = followersCount,
                        followingsCount = followingsCount
                    )
                    currentProfile = profile
                    currentAuthToken = clean
                    return@withContext profile
                }
                conn.disconnect()
            } catch (_: Exception) {
            }
        }
        null
    }

    fun resolveStreamUriForExoPlayer(uri: Uri): Uri = resolveStreamUri(uri, false)

    private fun resolveStreamUri(uri: Uri, forDownload: Boolean): Uri {
        val scheme = uri.scheme ?: return uri
        if (scheme != "sc") return uri

        val trackId = uri.schemeSpecificPart.removePrefix("//").substringBefore(".").substringBefore("?").trim()
        if (trackId.isBlank()) return uri

        val stream=SoundCloudTransport.resolve(trackId,::getClientId,{ kotlinx.coroutines.runBlocking { refreshClientIdIfNeeded() } },{cleanToken(currentAuthToken)},forDownload)
        if(stream!=null)return Uri.parse(stream)

        return uri
    }

    suspend fun downloadTrackToPhoneWithTags(
        context: Context, song: Song, onProgress: ((Float)->Unit)? = null
    ): Pair<Song,SongTagOverride>? = withContext(Dispatchers.IO) {
        val raw=File.createTempFile("sc-download-", ".tmp",context.cacheDir)
        val normalized=File.createTempFile("sc-audio-", ".tmp",context.cacheDir)
        var pending:Uri?=null
        try {
            val stream=resolveStreamUri(Uri.parse(song.uri),true).toString()
            require(stream.startsWith("http")){"No downloadable audio stream"}
            val hlsDuration=downloadAudio(stream,raw,onProgress)
            val (title,artist)=YouTubeMusicRepository.normalizeTrackTitleAndArtist(song.title,song.artist)
            val album=song.album.takeIf{it.isNotBlank()&&!it.equals("LorMusic",true)&&!it.equals("SoundCloud",true)} ?: "SoundCloud"
            val genre=song.genre.ifBlank{"SoundCloud"};val year=song.year
            val cover=song.albumArtUri?.let { art->runCatching {
                when {art.startsWith("http")->YouTubeMusicRepository.downloadBinaryBytes(art)
                    art.startsWith("//")->YouTubeMusicRepository.downloadBinaryBytes("https:$art")
                    art.startsWith("/")->File(art).readBytes()
                    else->context.contentResolver.openInputStream(Uri.parse(art))?.use{it.readBytes()}}
            }.getOrNull() }
            val prepared=DownloadedAudioPreparer.prepare(raw,normalized,Mp3DownloadMetadata.Tags(title,artist,album,genre,year,cover))
            require(prepared.durationMs>0){"Could not measure downloaded audio"}
            if(hlsDuration>0)require(kotlin.math.abs(prepared.durationMs-hlsDuration)<=maxOf(2500L,hlsDuration/20)){"Downloaded audio is incomplete"}
            val name=("$artist - $title").replace(Regex("""[\\/:*?"<>|]"""),"_").take(90)+prepared.extension
            var saved:String?=null;var path:String?=null;var numericId:Long?=null
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){
                val resolver=context.contentResolver
                val values=ContentValues().apply{
                    put(MediaStore.Audio.Media.DISPLAY_NAME,name);put(MediaStore.Audio.Media.TITLE,title);put(MediaStore.Audio.Media.ARTIST,artist);put(MediaStore.Audio.Media.ALBUM,album)
                    year.toIntOrNull()?.let{put(MediaStore.Audio.Media.YEAR,it)}
                    put(MediaStore.Audio.Media.MIME_TYPE,prepared.mime);put(MediaStore.Audio.Media.DURATION,prepared.durationMs)
                    put(MediaStore.Audio.Media.RELATIVE_PATH,Environment.DIRECTORY_MUSIC+"/LorMusic");put(MediaStore.Audio.Media.IS_MUSIC,1);put(MediaStore.Audio.Media.IS_PENDING,1)
                }
                val uri=resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,values) ?: error("Cannot create audio file")
                pending=uri
                requireNotNull(resolver.openOutputStream(uri,"w")).use{out->normalized.inputStream().use{it.copyTo(out)}}
                resolver.update(uri,ContentValues().apply{put(MediaStore.Audio.Media.IS_PENDING,0);put(MediaStore.Audio.Media.DURATION,prepared.durationMs)},null,null)
                saved=uri.toString();numericId=ContentUris.parseId(uri)
                path=runCatching{resolver.query(uri,arrayOf(MediaStore.Audio.Media.DATA),null,null,null)?.use{if(it.moveToFirst())it.getString(0)else null}}.getOrNull()
                pending=null
            }else{
                val dir=File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),"LorMusic").apply{mkdirs()}
                var file=File(dir,name);var index=2
                while(file.exists()){file=File(dir,name.removeSuffix(prepared.extension)+" (${index++})"+prepared.extension)}
                normalized.copyTo(file,overwrite=false);path=file.absolutePath;saved=Uri.fromFile(file).toString()
                MediaScannerConnection.scanFile(context,arrayOf(path),arrayOf(prepared.mime),null)
            }
            val id=numericId?.let{"local_$it"} ?: requireNotNull(saved)
            val cachedCover=cover?.takeIf{it.isNotEmpty()}?.let{bytes->runCatching{val file=File(File(context.filesDir,"tag_covers").apply{mkdirs()},"custom_${id.replace(Regex("[^a-zA-Z0-9_]"),"_")}.jpg");file.writeBytes(bytes);Uri.fromFile(file).toString()}.getOrNull()}
            val artwork=cachedCover ?: song.albumArtUri
            val local=Song(id,title,artist,album,prepared.durationMs,requireNotNull(saved),(artist+title).hashCode(),artwork,true,path,"LorMusic",System.currentTimeMillis()/1000,genre,year,"1",isRemoteSc=false)
            val override=SongTagOverride(id,title,artist,album,genre,year,"1",artwork)
            onProgress?.invoke(1f)
            Pair(local,override)
        }catch(cancel:kotlinx.coroutines.CancellationException){throw cancel}
        catch(_:Exception){null}
        finally{
            pending?.let{runCatching{context.contentResolver.delete(it,null,null)}}
            raw.delete();normalized.delete()
        }
    }

    private fun downloadAudio(url:String,file:File,progress:((Float)->Unit)?,depth:Int=0):Long{
        require(depth<=3){"Too many nested HLS playlists"}
        val conn=(URL(url).openConnection() as HttpURLConnection).apply{connectTimeout=12000;readTimeout=30000;setRequestProperty("User-Agent",USER_AGENT)}
        try {
            var duration=0L
            require(conn.responseCode in 200..299){"Audio server returned ${conn.responseCode}"}
            val type=conn.contentType.orEmpty()
            if(url.contains(".m3u8",true)||type.contains("mpegurl",true)){
                val playlist=HlsAudioPlaylist.parse(conn.url.toString(),conn.inputStream.bufferedReader().use{it.readText()})
                playlist.childPlaylist?.let{return downloadAudio(it,file,progress,depth+1)}
                duration=playlist.durationMs
                file.outputStream().buffered().use{out->playlist.parts.forEachIndexed{index,part->
                    val c=(URL(part.url).openConnection() as HttpURLConnection).apply{connectTimeout=12000;readTimeout=30000;setRequestProperty("User-Agent",USER_AGENT);part.range?.let{setRequestProperty("Range","bytes=${it.offset}-${it.offset+it.length-1}")}}
                    try{require(c.responseCode in 200..299);if(part.range!=null)require(c.responseCode==206){"Server ignored HLS byte range"};val copied=c.inputStream.use{it.copyTo(out)};part.range?.let{require(copied==it.length){"Incomplete HLS segment"}}}finally{c.disconnect()}
                    progress?.invoke(.9f*(index+1)/playlist.parts.size)
                }}
            }else{
                val length=conn.contentLengthLong;var downloaded=0L
                file.outputStream().buffered().use{out->conn.inputStream.use{input->val buffer=ByteArray(16384);while(true){val n=input.read(buffer);if(n<0)break;out.write(buffer,0,n);downloaded+=n;if(length>0)progress?.invoke((.9f*downloaded/length).coerceIn(0f,.9f))}}}
                if(length>0)require(downloaded==length){"Incomplete audio download"}
            }
            require(file.length()>4){"Empty audio download"}
            return duration
        }finally{conn.disconnect()}
    }

    fun getShareUrl(song: Song): String? {
        if (!song.isRemoteSc && !song.id.startsWith("sc_") && !song.uri.startsWith("sc://")) return null
        val cached = trackPermalinkMap[song.id] ?: trackPermalinkMap[song.id.removePrefix("sc_")]
        if (!cached.isNullOrBlank()) return cached
        val cleanArtist = song.artist.trim()
        val cleanTitle = song.title.trim()
        return try {
            val q = URLEncoder.encode("$cleanArtist $cleanTitle", "UTF-8")
            "https://soundcloud.com/search?q=$q"
        } catch (_: Exception) {
            "https://soundcloud.com"
        }
    }

    fun shareSongLink(context: Context, song: Song) {
        val shareUrl = getShareUrl(song) ?: "https://soundcloud.com"
        val text = "${song.artist} - ${song.title}\n$shareUrl"
        val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            putExtra(android.content.Intent.EXTRA_TEXT, text)
            putExtra(android.content.Intent.EXTRA_TITLE, "${song.artist} - ${song.title}")
            type = "text/plain"
        }
        val shareIntent = android.content.Intent.createChooser(sendIntent, null).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(shareIntent)
    }
}
