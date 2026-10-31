package com.chronos.hsc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chronos.hsc.ui.screens.MainScreen
import com.chronos.hsc.ui.theme.ChronosHscTheme
import com.chronos.hsc.ui.viewmodel.StudyViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as ChronosApplication).container

        setContent {
            ChronosHscTheme {
                val studyViewModel: StudyViewModel = viewModel(
                    factory = StudyViewModel.provideFactory(appContainer.studyRepository)
                )
                MainScreen(viewModel = studyViewModel)
            }
        }
    }
}
