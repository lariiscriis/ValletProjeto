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
import java.util.*

class CheckReservaWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val db = FirebaseFirestore.getInstance()

    override suspend fun doWork(): Result {
        val reservaId = inputData.getString("reservaId")
        val vagaId = inputData.getString("vagaId")

        Log.d("CheckReservaWorker", "Verificando reserva: $reservaId, vaga: $vagaId")

        if (reservaId == null || vagaId == null) {
            Log.e("CheckReservaWorker", "Dados incompletos")
            return Result.failure()
        }

        return try {
            val reservaRef = db.collection("reserva").document(reservaId)
            val vagaRef = db.collection("vaga").document(vagaId)

            val reservaDoc = reservaRef.get().await()

            if (!reservaDoc.exists()) {
                Log.e("CheckReservaWorker", "Reserva não encontrada: $reservaId")
                return Result.failure()
            }

            val status = reservaDoc.getString("status")
            val fimReserva = reservaDoc.getTimestamp("fimReserva")?.toDate()

            Log.d("CheckReservaWorker", "Status: $status, Fim: $fimReserva")

            // 🔥 FINALIZA RESERVA SE ESTIVER EXPIRADA
            if (status == "ativa" && fimReserva != null && fimReserva.time <= System.currentTimeMillis()) {
                Log.d("CheckReservaWorker", "Reserva expirada, finalizando...")

                // Atualiza status da reserva e libera vaga
                val batch = db.batch()
                batch.update(reservaRef, "status", "finalizada")
                batch.update(vagaRef, "disponivel", true)
                batch.commit().await()

                Log.d("CheckReservaWorker", "Reserva finalizada e vaga liberada")

                // Envia notificação
                enviarNotificacaoExpirada(vagaId)

                return Result.success()
            }

            if (status != "ativa") {
                Log.d("CheckReservaWorker", "Reserva já está $status")
                return Result.success()
            }

            Log.d("CheckReservaWorker", "Reserva ainda não expirou")
            return Result.success()

        } catch (e: Exception) {
            Log.e("CheckReservaWorker", "Erro: ${e.message}", e)
            return Result.retry()
        }
    }

    private fun enviarNotificacaoExpirada(vagaId: String) {
        NotificationUtils.createNotificationChannel(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, NotificationConstants.RESERVA_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_parking)
            .setContentTitle("Reserva Finalizada!")
            .setContentText("Sua reserva na vaga $vagaId foi encerrada automaticamente.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Sua reserva na vaga $vagaId foi finalizada automaticamente. A vaga está disponível para novas reservas."))
            .build()

        notificationManager.notify(vagaId.hashCode() + 3, notification)
        Log.d("CheckReservaWorker", "Notificação de expiração enviada")
    }
}