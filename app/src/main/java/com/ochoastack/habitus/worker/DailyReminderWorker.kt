package com.ochoastack.habitus.worker

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.ochoastack.habitus.R
import com.ochoastack.habitus.utils.NotificationHelper

class DailyReminderWorker(
    private val ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        if (FirebaseAuth.getInstance().currentUser == null)
            return Result.success()

        val notificacion = androidx.core.app.NotificationCompat
            .Builder(ctx, NotificationHelper.CANAL_ID)
            .setSmallIcon(R.drawable.ic_leaf)
            .setContentTitle(ctx.getString(R.string.notif_reminder_title))
            .setContentText(ctx.getString(R.string.notif_reminder_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val manager = ctx.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager
        manager.notify(1004, notificacion)

        return Result.success()
    }
}
