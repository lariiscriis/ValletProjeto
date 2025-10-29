package br.edu.fatecpg.valletprojeto.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import br.edu.fatecpg.valletprojeto.R
import br.edu.fatecpg.valletprojeto.viewmodel.ReservaViewModel

class ReservaAvisoReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, ReservaViewModel.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_parking)
            .setContentTitle("Aviso de reserva")
            .setContentText("Faltam 10 minutos para o fim da sua reserva!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(ReservaViewModel.NOTIFICATION_ID + 3, notification)
    }
}
