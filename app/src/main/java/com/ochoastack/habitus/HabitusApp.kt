package com.ochoastack.habitus

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.ochoastack.habitus.utils.NotificationHelper

class HabitusApp : Application() { // Declaramos la aplicación de Habitus

    override fun onCreate() {
        super.onCreate()
        restaurarTema()
        // Creamos el canal de notificaciones al inicio de la aplicación.
        NotificationHelper.crearCanal(this)
        NotificationHelper.programarResumenSemanal(this)
    }

    // Restauramos el tema de la aplicación.
    private fun restaurarTema() {
        val prefs = getSharedPreferences("habitus_prefs", Context.MODE_PRIVATE)
        val temaGuardado = prefs.getString("app_theme", "light") ?: "light"

        val modo =
            when (temaGuardado) {
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                "system" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                else -> AppCompatDelegate.MODE_NIGHT_NO
            }
        AppCompatDelegate.setDefaultNightMode(modo)
    }
}
