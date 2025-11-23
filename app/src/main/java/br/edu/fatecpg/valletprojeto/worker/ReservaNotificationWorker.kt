package br.edu.fatecpg.valletprojeto.worker

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.edu.fatecpg.valletprojeto.R
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ReservaNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val tipoNotificacao = inputData.getString("tipo")
        val vagaId = inputData.getString("vagaId")
        val estacionamentoId = inputData.getString("estacionamentoId")

        Log.d("ReservaNotificationWorker", "Executando worker: tipo=$tipoNotificacao, vagaId=$vagaId")

        return when (tipoNotificacao) {
            "aviso" -> enviarNotificacaoAviso(vagaId, estacionamentoId)
            "expirada" -> enviarNotificacaoExpirada(vagaId)
            else -> Result.failure()
        }
    }

    private suspend fun enviarNotificacaoAviso(vagaId: String?, estacionamentoId: String?): Result {
        if (vagaId == null) return Result.failure()

        NotificationUtils.createNotificationChannel(applicationContext)
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(applicationContext, NotificationConstants.RESERVA_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_parking)
            .setContentTitle("Sua reserva está acabando!")
            .setContentText("Faltam 10 minutos para o fim da sua reserva na vaga $vagaId.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(vagaId.hashCode() + 1, notification)
        Log.d("ReservaNotificationWorker", "Notificação de aviso enviada para vaga: $vagaId")

        return Result.success()
    }

    private suspend fun enviarNotificacaoExpirada(vagaId: String?): Result {
        if (vagaId == null) return Result.failure()

        // Verifica no Firebase se a reserva realmente expirou
        val db = FirebaseFirestore.getInstance()
        val reservas = db.collection("reserva")
            .whereEqualTo("vagaId", vagaId)
            .whereEqualTo("status", "ativa")
            .get()
            .await()

        if (!reservas.isEmpty) {
            val reserva = reservas.documents.first()
            val fimReserva = reserva.getTimestamp("fimReserva")?.toDate()

            if (fimReserva != null && fimReserva.time <= System.currentTimeMillis()) {
                // Reserva realmente expirou, envia notificação
                NotificationUtils.createNotificationChannel(applicationContext)
                val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                val notification = NotificationCompat.Builder(applicationContext, NotificationConstants.RESERVA_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_parking)
                    .setContentTitle("Reserva Expirada!")
                    .setContentText("Sua reserva na vaga $vagaId expirou.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()

                notificationManager.notify(vagaId.hashCode() + 2, notification)
                Log.d("ReservaNotificationWorker", "Notificação de expiração enviada para vaga: $vagaId")
            }
        }

        return Result.success()
    }
}