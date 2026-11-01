package com.chronos.hsc.updater

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    val updateManager = UpdateManager(application)
}
