package br.edu.fatecpg.valletprojeto.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import br.edu.fatecpg.valletprojeto.R
import br.edu.fatecpg.valletprojeto.ReservaActivity
import br.edu.fatecpg.valletprojeto.worker.NotificationConstants
import br.edu.fatecpg.valletprojeto.worker.NotificationUtils

class ReservaAvisoReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ReservaAvisoReceiver", "Receiver acionado")

        NotificationUtils.createNotificationChannel(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val vagaId = intent.getStringExtra("vagaId")
        val estacionamentoId = intent.getStringExtra("estacionamentoId")
        val numeroVaga = intent.getStringExtra("numeroVaga") ?: vagaId

        if (vagaId == null) {
            Log.e("ReservaAvisoReceiver", "vagaId é nulo")
            return
        }

        Log.d("ReservaAvisoReceiver", "Preparando notificação para vaga: $vagaId")

        val activityIntent = Intent(context, ReservaActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("vagaId", vagaId)
            putExtra("estacionamentoId", estacionamentoId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            vagaId.hashCode(),
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationConstants.RESERVA_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_parking)
            .setContentTitle("Sua reserva está acabando!")
            .setContentText("Faltam 10 minutos para o fim da sua reserva na vaga $numeroVaga.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(vagaId.hashCode() + 1, notification)
        Log.d("ReservaAvisoReceiver", "Notificação de aviso exibida para vaga: $numeroVaga")
    }
}
