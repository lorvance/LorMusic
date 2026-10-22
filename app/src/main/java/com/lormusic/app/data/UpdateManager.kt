package com.lormusic.app.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
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
        currentVersion: String = "1.6.0",
        includeBeta: Boolean = false
    ): UpdateState = withContext(Dispatchers.IO) {
        val baseRepos = listOf(
            "https://api.github.com/repos/Lorvance/LorMusic",
            "https://api.github.com/repos/Lorvance/LorMuisc"
        )

        var lastException: Exception? = null

        for (base in baseRepos) {
            try {
                val endpoint = if (includeBeta) "$base/releases" else "$base/releases/latest"
                val url = URL(endpoint)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    setRequestProperty("User-Agent", "LorMusic-Android")
                    connectTimeout = 8000
                    readTimeout = 8000
                }

                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    conn.disconnect()

                    val targetRelease = if (includeBeta) {
                        val array = org.json.JSONArray(body)
                        if (array.length() > 0) array.getJSONObject(0) else null
                    } else {
                        JSONObject(body)
                    }

                    if (targetRelease == null) continue

                    val rawTag = targetRelease.optString("tag_name", "").ifBlank { targetRelease.optString("name", "") }
                    val remoteVersion = rawTag.removePrefix("v").trim()
                    val changelog = targetRelease.optString("body", "").trim().ifBlank {
                        "Улучшения производительности и исправления ошибок."
                    }

                    var apkDownloadUrl = ""
                    val assets = targetRelease.optJSONArray("assets")
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
                        apkDownloadUrl = targetRelease.optString("html_url", "")
                    }

                    if (isNewerVersion(remoteVersion, currentVersion)) {
                        return@withContext UpdateState.Available(
                            AppUpdateInfo(
                                versionName = remoteVersion,
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

    private fun isNewerVersion(remote: String, current: String): Boolean {
        if (remote.isBlank()) return false
        val remoteParts = remote.split(".").mapNotNull { it.filter { char -> char.isDigit() }.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.filter { char -> char.isDigit() }.toIntOrNull() }

        val maxLength = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLength) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
