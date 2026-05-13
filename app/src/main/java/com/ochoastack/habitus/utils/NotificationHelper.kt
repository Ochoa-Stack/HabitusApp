package com.ochoastack.habitus.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ochoastack.habitus.worker.ReminderWorker
import com.ochoastack.habitus.worker.WeeklySummaryWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationHelper {    // Declaramos el objeto de helper de notificaciones
    // Constantes para el canal de notificaciones
    const val CANAL_ID         = "habitus_recordatorios"
    const val WORK_TAG         = "habitus_reminder"
    const val WORK_TAG_WEEKLY  = "habitus_weekly_summary"
    const val NOTIF_ID_WEEKLY  = 1003

    // Creamos el canal de notificaciones requerido en Android 8.0 (API 26) en adelante
    fun crearCanal(context: Context) {
        val canal = NotificationChannel(
            CANAL_ID,
            context.getString(com.ochoastack.habitus.R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(
                com.ochoastack.habitus.R.string.notif_channel_description
            )
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(canal)
    }
    /* Programa el recordatorio diario en la hora indicada. Calcula el retardo
    inicialhasta la próxima ocurrencia de esa hora y luego repite cada 24 horas.
    Si ya había un recordatorio activo, lo reemplaza */
    fun programarRecordatorio(context: Context, horaStr: String) {
        val (hora, minuto) = when (horaStr) {
            "morning"   -> 8  to 0
            "afternoon" -> 14 to 0
            else        -> 20 to 0
        }
        // Calculamos el retardo inicial hasta la próxima hora
        val ahora    = Calendar.getInstance()
        val objetivo = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hora)
            set(Calendar.MINUTE, minuto)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // Si la hora ya pasó hoy, programamos para mañana
            if (!after(ahora)) add(Calendar.DAY_OF_YEAR, 1)
        }
        // Calculamos la diferencia en milisegundos
        val retrasoMs = objetivo.timeInMillis - ahora.timeInMillis
        // Programamos el recordatorio
        val solicitud = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(retrasoMs, TimeUnit.MILLISECONDS)
            .addTag(WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            solicitud
        )
    }
    // Cancelamos el recordatorio programado
    fun cancelarRecordatorio(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG)
    }

    // Programa el resumen semanal para cada domingo a las 8pm.
    fun programarResumenSemanal(context: Context) {
        val ahora    = Calendar.getInstance()
        val objetivo = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE,      0)
            set(Calendar.SECOND,      0)
            set(Calendar.MILLISECOND, 0)
            // Avanzar hasta el próximo domingo a las 8pm que no haya pasado aún
            while (get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY ||
                   timeInMillis <= ahora.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val retrasoMs = objetivo.timeInMillis - ahora.timeInMillis

        val solicitud = PeriodicWorkRequestBuilder<WeeklySummaryWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(retrasoMs, TimeUnit.MILLISECONDS)
            .addTag(WORK_TAG_WEEKLY)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_TAG_WEEKLY,
            ExistingPeriodicWorkPolicy.KEEP,
            solicitud
        )
    }

    fun cancelarResumenSemanal(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG_WEEKLY)
    }
}
