package com.ochoastack.habitus

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import javax.inject.Inject

@HiltAndroidApp
class HabitusApplication : Application(), Configuration.Provider {

    // Inyectamos el factory que Hilt genera automáticamente para los Workers
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // Configuramos WorkManager para usar el factory de Hilt
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
