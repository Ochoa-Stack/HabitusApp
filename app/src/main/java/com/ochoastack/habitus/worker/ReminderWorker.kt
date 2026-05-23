package com.ochoastack.habitus.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.ochoastack.habitus.MainActivity
import com.ochoastack.habitus.R
import com.ochoastack.habitus.utils.NotificationHelper

// Declaramos el worker de recordatorio
class ReminderWorker(
    private val ctx: Context,
    params: WorkerParameters,
) : Worker(ctx, params) {
    override fun doWork(): Result {
        mostrarNotificacion()
        return Result.success()
    }

    // Mostramos la notificación
    private fun mostrarNotificacion() {
        // Al tocar la notificación, el usuario va directamente a la lista de hábitos
        val intent =
            Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        // Creamos el PendingIntent para la notificación
        val pendingIntent =
            PendingIntent.getActivity(
                ctx,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE,
            )
        // Creamos la notificación
        val notificacion =
            NotificationCompat.Builder(ctx, NotificationHelper.CANAL_ID)
                .setSmallIcon(R.drawable.ic_leaf)
                .setContentTitle(ctx.getString(R.string.notif_titulo))
                .setContentText(ctx.getString(R.string.notif_cuerpo))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1001, notificacion)
    }
}
