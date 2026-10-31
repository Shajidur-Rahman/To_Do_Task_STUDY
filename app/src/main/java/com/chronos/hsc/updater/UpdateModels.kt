package com.chronos.hsc.updater

import android.net.Uri

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String
)

sealed interface UpdateDownloadState {
    data object Idle : UpdateDownloadState
    data class Downloading(val progressPercent: Int, val downloadedBytes: Long, val totalBytes: Long) : UpdateDownloadState
    data class ReadyToInstall(val apkUri: Uri, val targetFilePath: String) : UpdateDownloadState
    data class NeedsInstallPermission(val apkUri: Uri, val targetFilePath: String) : UpdateDownloadState
    data class Error(val message: String) : UpdateDownloadState
}
