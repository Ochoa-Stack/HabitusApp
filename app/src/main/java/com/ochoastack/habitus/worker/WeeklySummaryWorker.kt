package com.ochoastack.habitus.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ochoastack.habitus.R
import com.ochoastack.habitus.data.HabitRepository
import com.ochoastack.habitus.ui.WeeklySummaryActivity
import com.ochoastack.habitus.utils.NotificationHelper
import com.google.firebase.auth.FirebaseAuth

import androidx.hilt.work.HiltWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

// Configuramos el worker para recibir el repositorio de forma segura
@HiltWorker
class WeeklySummaryWorker @AssistedInject constructor(
    @Assisted private val ctx: Context,
    @Assisted params: WorkerParameters,
    private val habitRepository: HabitRepository
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        // No ejecutar si no hay sesión activa
        if (FirebaseAuth.getInstance().currentUser == null) return Result.success()

        val resultado = habitRepository.obtenerResumenSemanal()

        resultado.fold(
            onSuccess = { resumen ->
                mostrarNotificacion(resumen.porcentajeSemana, resumen.rachaMaxima)
            },
            onFailure = { /* Silencio, el resumen no es crítico */ }
        )

        return Result.success()
    }

    // Mostramos la notificación
    private fun mostrarNotificacion(porcentajeSemana: Int, rachaMaxima: Int) {
        val intent = Intent(ctx, WeeklySummaryActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val cuerpo = ctx.getString(
            R.string.notif_weekly_body,
            porcentajeSemana,
            rachaMaxima
        )

        val notificacion = NotificationCompat.Builder(ctx, NotificationHelper.CANAL_ID)
            .setSmallIcon(R.drawable.ic_leaf)
            .setContentTitle(ctx.getString(R.string.notif_weekly_title))
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NotificationHelper.NOTIF_ID_WEEKLY, notificacion)
    }
}
