package br.edu.fatecpg.valletprojeto.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import br.edu.fatecpg.valletprojeto.R
import br.edu.fatecpg.valletprojeto.viewmodel.ReservaViewModel

class ReservaExpiredReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, ReservaViewModel.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_parking)
            .setContentTitle("Reserva expirada")
            .setContentText("O tempo da sua reserva acabou. A vaga foi liberada.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(ReservaViewModel.NOTIFICATION_ID + 4, notification)
    }
}
