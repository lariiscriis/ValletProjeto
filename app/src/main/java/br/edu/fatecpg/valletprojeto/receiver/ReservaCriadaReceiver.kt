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

class ReservaCriadaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationUtils.createNotificationChannel(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val vagaId = intent.getStringExtra("vagaId")
        val estacionamentoNome = intent.getStringExtra("estacionamentoNome")

        if (vagaId == null || estacionamentoNome == null) {
            Log.e("ReservaCriadaReceiver", "Dados incompletos: vagaId=$vagaId, estacionamentoNome=$estacionamentoNome")
            return
        }

        // Intent para abrir a Activity específica da reserva
        val activityIntent = Intent(context, ReservaActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("vagaId", vagaId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            vagaId.hashCode(),
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Usar ID único para cada notificação
        val notificationId = System.currentTimeMillis().toInt()

        val notification = NotificationCompat.Builder(context, NotificationConstants.RESERVA_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_parking)
            .setContentTitle("Reserva Confirmada! ✅")
            .setContentText("Vaga $vagaId no $estacionamentoNome reservada com sucesso!")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Aumente a prioridade
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Sua reserva na vaga $vagaId do estacionamento $estacionamentoNome foi iniciada com sucesso. Você receberá avisos antes do término."))
            .build()

        notificationManager.notify(notificationId, notification)
        Log.d("ReservaCriadaReceiver", "Notificação de reserva criada exibida para vaga: $vagaId")
    }
}