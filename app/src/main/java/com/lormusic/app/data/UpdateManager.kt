package com.lormusic.app.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.lormusic.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val versionName: String,
    val changelog: String,
    val downloadUrl: String
)

sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    data class Available(val info: AppUpdateInfo) : UpdateState
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : UpdateState
    data class ReadyToInstall(val apkFile: File) : UpdateState
    data class Error(val message: String) : UpdateState
    object UpToDate : UpdateState
}

class UpdateManager(private val context: Context) {

    suspend fun checkForUpdates(
        currentVersion: String = BuildConfig.VERSION_NAME,
        includeBeta: Boolean = false
    ): UpdateState = withContext(Dispatchers.IO) {
        val effectiveCurrentVersion = currentVersion.ifBlank { BuildConfig.VERSION_NAME }
        val baseRepos = listOf(
            "https://api.github.com/repos/Lorvance/LorMusic",
            "https://api.github.com/repos/Lorvance/LorMuisc"
        )

        var lastException: Exception? = null

        for (base in baseRepos) {
            try {
                val endpoint = "$base/releases"
                val url = URL(endpoint)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    setRequestProperty("User-Agent", "LorMusic-Android/$effectiveCurrentVersion")
                    connectTimeout = 8000
                    readTimeout = 8000
                }

                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    conn.disconnect()

                    val array = org.json.JSONArray(body)
                    if (array.length() == 0) continue

                    var bestRelease: JSONObject? = null
                    var bestVersion = ""

                    for (i in 0 until array.length()) {
                        val release = array.getJSONObject(i)
                        val isPrerelease = release.optBoolean("prerelease", false)
                        val isDraft = release.optBoolean("draft", false)
                        if (isDraft) continue
                        if (!includeBeta && isPrerelease) {
                            if (bestRelease != null) continue
                        }

                        val rawTag = release.optString("tag_name", "").ifBlank { release.optString("name", "") }
                        val remoteVersion = rawTag.removePrefix("v").removePrefix("V").trim()
                        if (remoteVersion.isNotBlank()) {
                            if (bestRelease == null || isNewerVersion(remoteVersion, bestVersion)) {
                                bestRelease = release
                                bestVersion = remoteVersion
                            }
                        }
                    }

                    if (bestRelease == null) continue

                    val changelog = bestRelease.optString("body", "").trim().ifBlank {
                        "Улучшения производительности и новые функции в версии $bestVersion."
                    }

                    var apkDownloadUrl = ""
                    val assets = bestRelease.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk", ignoreCase = true)) {
                                apkDownloadUrl = asset.optString("browser_download_url", "")
                                break
                            }
                        }
                    }

                    if (apkDownloadUrl.isBlank()) {
                        apkDownloadUrl = bestRelease.optString("html_url", "")
                    }

                    if (isNewerVersion(bestVersion, effectiveCurrentVersion)) {
                        return@withContext UpdateState.Available(
                            AppUpdateInfo(
                                versionName = bestVersion,
                                changelog = changelog,
                                downloadUrl = apkDownloadUrl
                            )
                        )
                    } else {
                        return@withContext UpdateState.UpToDate
                    }
                } else if (conn.responseCode == 404) {
                    conn.disconnect()
                    continue
                } else {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                lastException = e
            }
        }

        if (lastException != null) {
            UpdateState.Error(lastException.localizedMessage ?: "Ошибка проверки обновлений")
        } else {
            UpdateState.UpToDate
        }
    }

    suspend fun downloadUpdate(
        info: AppUpdateInfo,
        onProgress: (Float, Long, Long) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val updatesDir = File(context.getExternalFilesDir(null) ?: context.cacheDir, "updates")
            if (!updatesDir.exists()) {
                updatesDir.mkdirs()
            }
            val targetFile = File(updatesDir, "LorMusic-${info.versionName}.apk")
            if (targetFile.exists()) {
                targetFile.delete()
            }

            var currentUrl = info.downloadUrl
            var conn: HttpURLConnection
            var redirects = 0
            while (true) {
                val url = URL(currentUrl)
                conn = url.openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = false
                conn.setRequestProperty("User-Agent", "LorMusic-Android")
                conn.connectTimeout = 12000
                conn.readTimeout = 12000

                val status = conn.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                    status == HttpURLConnection.HTTP_MOVED_PERM ||
                    status == HttpURLConnection.HTTP_SEE_OTHER ||
                    status == 307 || status == 308
                ) {
                    val newUrl = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (newUrl != null && redirects < 5) {
                        currentUrl = newUrl
                        redirects++
                        continue
                    }
                }
                break
            }

            val totalBytes = conn.contentLengthLong.coerceAtLeast(1L)
            conn.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalDownloaded = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalDownloaded += bytesRead
                        val progress = (totalDownloaded.toFloat() / totalBytes).coerceIn(0f, 1f)
                        onProgress(progress, totalDownloaded, totalBytes)
                    }
                    output.flush()
                }
            }
            conn.disconnect()
            targetFile
        } catch (e: Exception) {
            null
        }
    }

    fun installApk(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    private data class ParsedVersion(
        val parts: List<Int>,
        val preReleaseNumber: Int?
    )

    private fun parseVersion(raw: String): ParsedVersion {
        val clean = raw.trim().removePrefix("v").removePrefix("V").trim()
        val sepIndex = clean.indexOfFirst { it == '-' || it == '_' || it == '|' }
        val basePart = (if (sepIndex >= 0) clean.substring(0, sepIndex) else clean).trim()
        val prePart = (if (sepIndex >= 0) clean.substring(sepIndex + 1) else "").trim()

        val parts = basePart.split(".").mapNotNull { segment ->
            segment.filter { it.isDigit() }.toIntOrNull()
        }

        val preNumber = if (prePart.isNotBlank()) {
            val digits = prePart.filter { it.isDigit() }
            digits.toIntOrNull() ?: 0
        } else {
            null
        }

        return ParsedVersion(parts, preNumber)
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        if (remote.isBlank()) return false
        val r = parseVersion(remote)
        val c = parseVersion(current)

        val maxLength = maxOf(r.parts.size, c.parts.size)
        for (i in 0 until maxLength) {
            val rPart = r.parts.getOrElse(i) { 0 }
            val cPart = c.parts.getOrElse(i) { 0 }
            if (rPart > cPart) return true
            if (rPart < cPart) return false
        }

        if (r.preReleaseNumber == null && c.preReleaseNumber != null) {
            return true
        }
        if (r.preReleaseNumber != null && c.preReleaseNumber == null) {
            return false
        }
        if (r.preReleaseNumber != null && c.preReleaseNumber != null) {
            return r.preReleaseNumber > c.preReleaseNumber
        }

        return false
    }
}
