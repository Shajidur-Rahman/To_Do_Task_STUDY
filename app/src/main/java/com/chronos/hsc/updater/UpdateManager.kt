package com.chronos.hsc.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.chronos.hsc.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class UpdateManager(private val context: Context) {

    private val _availableUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val availableUpdate: StateFlow<AppUpdateInfo?> = _availableUpdate.asStateFlow()

    private val _downloadState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val downloadState: StateFlow<UpdateDownloadState> = _downloadState.asStateFlow()

    companion object {
        const val REMOTE_VERSION_URL =
            "https://raw.githubusercontent.com/Shajidur-Rahman/To_Do_Task_STUDY/main/version.json"
    }

    suspend fun checkForUpdates(): AppUpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val urlWithCacheBuster = "$REMOTE_VERSION_URL?t=${System.currentTimeMillis()}"
                val connection = (URL(urlWithCacheBuster).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    requestMethod = "GET"
                    useCaches = false
                    instanceFollowRedirects = true
                }

                if (connection.responseCode in 200..299) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(responseText)
                    val remoteVersionCode = json.optInt("versionCode", 0)
                    val remoteVersionName = json.optString("versionName", "")
                    val apkUrl = json.optString("apkUrl", "")
                    val releaseNotes = json.optString("releaseNotes", "")

                    if (remoteVersionCode > BuildConfig.VERSION_CODE && apkUrl.isNotBlank()) {
                        val info = AppUpdateInfo(
                            versionCode = remoteVersionCode,
                            versionName = remoteVersionName,
                            apkUrl = apkUrl,
                            releaseNotes = releaseNotes
                        )
                        _availableUpdate.value = info
                        return@withContext info
                    }
                }
            } catch (_: Exception) {
                // Network unavailable or offline
            }
            null
        }
    }

    fun dismissUpdate() {
        _availableUpdate.value = null
        _downloadState.value = UpdateDownloadState.Idle
    }

    suspend fun downloadAndInstall(updateInfo: AppUpdateInfo) {
        withContext(Dispatchers.IO) {
            try {
                val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: File(context.filesDir, "downloads").apply { mkdirs() }
                val targetApk = File(downloadDir, "update.apk")
                if (targetApk.exists()) {
                    targetApk.delete()
                }

                _downloadState.value = UpdateDownloadState.Downloading(
                    progressPercent = 0,
                    downloadedBytes = 0,
                    totalBytes = 0
                )

                // Follow redirects manually to handle GitHub Release 302 -> S3/objects.githubusercontent.com
                var currentUrl = updateInfo.apkUrl
                var connection: HttpURLConnection
                var redirectCount = 0

                while (true) {
                    connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15000
                        readTimeout = 15000
                        instanceFollowRedirects = false
                        setRequestProperty("User-Agent", "ChronosHSC-Updater")
                    }

                    val code = connection.responseCode
                    if (code == HttpURLConnection.HTTP_MOVED_PERM ||
                        code == HttpURLConnection.HTTP_MOVED_TEMP ||
                        code == HttpURLConnection.HTTP_SEE_OTHER ||
                        code == 307 || code == 308
                    ) {
                        currentUrl = connection.getHeaderField("Location") ?: break
                        redirectCount++
                        if (redirectCount > 8) break
                    } else {
                        break
                    }
                }

                val totalLength = connection.contentLength.toLong()
                var downloaded: Long = 0

                connection.inputStream.use { input ->
                    FileOutputStream(targetApk).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var read: Int
                        var lastReportTime = System.currentTimeMillis()

                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read

                            val now = System.currentTimeMillis()
                            if (now - lastReportTime > 150 || downloaded == totalLength) {
                                lastReportTime = now
                                val percent = if (totalLength > 0) {
                                    ((downloaded * 100) / totalLength).toInt().coerceIn(0, 100)
                                } else 0

                                _downloadState.value = UpdateDownloadState.Downloading(
                                    progressPercent = percent,
                                    downloadedBytes = downloaded,
                                    totalBytes = totalLength
                                )
                            }
                        }
                        output.flush()
                    }
                }

                if (targetApk.exists() && targetApk.length() > 0) {
                    val apkUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        targetApk
                    )

                    withContext(Dispatchers.Main) {
                        launchInstallFlow(targetApk, apkUri)
                    }
                } else {
                    _downloadState.value = UpdateDownloadState.Error("Downloaded APK is empty or failed to write.")
                }
            } catch (e: Exception) {
                _downloadState.value = UpdateDownloadState.Error("Update download failed: ${e.localizedMessage ?: "Network error"}")
            }
        }
    }

    fun launchInstallFlow(apkFile: File, apkUri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                _downloadState.value = UpdateDownloadState.NeedsInstallPermission(
                    apkUri = apkUri,
                    targetFilePath = apkFile.absolutePath
                )
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }
        }

        _downloadState.value = UpdateDownloadState.ReadyToInstall(
            apkUri = apkUri,
            targetFilePath = apkFile.absolutePath
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    }

    fun retryInstallAfterPermission(apkUri: Uri) {
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    }
}
