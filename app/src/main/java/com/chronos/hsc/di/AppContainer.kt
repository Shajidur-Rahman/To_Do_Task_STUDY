package com.chronos.hsc.di

import android.content.Context
import com.chronos.hsc.data.local.StudyDao
import com.chronos.hsc.data.local.StudyDatabase
import com.chronos.hsc.data.repository.StudyRepository
import com.chronos.hsc.data.sync.FirebaseSyncManager

class AppContainer(context: Context) {
    private val appContext: Context = context.applicationContext

    val database: StudyDatabase by lazy {
        StudyDatabase.getInstance(appContext)
    }

    val studyDao: StudyDao by lazy {
        database.studyDao()
    }

    val firebaseSyncManager: FirebaseSyncManager by lazy {
        FirebaseSyncManager(appContext, studyDao)
    }

    val studyRepository: StudyRepository by lazy {
        StudyRepository(studyDao, appContext, firebaseSyncManager)
    }
}
