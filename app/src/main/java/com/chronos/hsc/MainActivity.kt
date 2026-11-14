package com.chronos.hsc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chronos.hsc.ui.components.AppUpdateDialog
import com.chronos.hsc.ui.screens.MainScreen
import com.chronos.hsc.ui.theme.ChronosHscTheme
import com.chronos.hsc.ui.viewmodel.StudyViewModel
import com.chronos.hsc.updater.UpdateManager
import com.chronos.hsc.updater.UpdateViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as ChronosApplication).container

        setContent {
            ChronosHscTheme {
                val studyViewModel: StudyViewModel = viewModel(
                    factory = StudyViewModel.provideFactory(
                        repository = appContainer.studyRepository,
                        firebaseSyncManager = appContainer.firebaseSyncManager
                    )
                )
                val updateViewModel: UpdateViewModel = viewModel()
                val updateManager = updateViewModel.updateManager
                val availableUpdate by updateManager.availableUpdate.collectAsState()
                val isDialogVisible by updateManager.isDialogVisible.collectAsState()
                val downloadState by updateManager.downloadState.collectAsState()
                val coroutineScope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    updateManager.checkForUpdates()
                }

                MainScreen(
                    viewModel = studyViewModel,
                    isUpdateAvailable = availableUpdate != null,
                    onOpenUpdate = { updateManager.showUpdateDialog() }
                )

                if (isDialogVisible && availableUpdate != null) {
                    AppUpdateDialog(
                        updateInfo = availableUpdate!!,
                        downloadState = downloadState,
                        onStartUpdate = {
                            coroutineScope.launch {
                                updateManager.downloadAndInstall(availableUpdate!!)
                            }
                        },
                        onRetryInstall = { apkUri ->
                            updateManager.retryInstallAfterPermission(apkUri)
                        },
                        onDismiss = {
                            updateManager.dismissUpdateDialog()
                        }
                    )
                }
            }
        }
    }
}
