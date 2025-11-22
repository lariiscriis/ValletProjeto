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

class CheckReservaWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val db = FirebaseFirestore.getInstance()

    override suspend fun doWork(): Result {
        val reservaId = inputData.getString("reservaId")
        val vagaId = inputData.getString("vagaId")

        if (reservaId == null || vagaId == null) {
            Log.e("CheckReservaWorker", "Dados de entrada incompletos.")
            return Result.failure()
        }

        try {
            val reservaRef = db.collection("reserva").document(reservaId)
            val vagaRef = db.collection("vaga").document(vagaId)

            val reservaDoc = reservaRef.get().await()

            if (!reservaDoc.exists()) {
                Log.e("CheckReservaWorker", "Reserva $reservaId não encontrada.")
                return Result.failure()
            }

            val fimReserva = reservaDoc.getTimestamp("fimReserva")
            val status = reservaDoc.getString("status")

            if (status == "ativa" && fimReserva != null && fimReserva.toDate().before(Date())) {

                val batch = db.batch()
                batch.update(reservaRef, "status", "finalizada")
                batch.update(vagaRef, "disponivel", true)
                batch.commit().await()

                Log.d("CheckReservaWorker", "Reserva $reservaId finalizada e vaga $vagaId liberada.")
                sendNotification(
                    title = "Reserva Finalizada",
                    message = "Sua reserva da vaga $vagaId foi encerrada."
                )

                return Result.success()
            }

            if (status != "ativa") {
                Log.d("CheckReservaWorker", "Reserva $reservaId já está $status.")
                return Result.success()
            }

            Log.d("CheckReservaWorker", "Reserva $reservaId ainda não expirou.")
            return Result.success()

        } catch (e: Exception) {
            Log.e("CheckReservaWorker", "Erro ao processar reserva: ${e.message}", e)
            return Result.retry()
        }
    }

    private fun sendNotification(title: String, message: String) {
        NotificationUtils.createNotificationChannel(applicationContext)

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(
            applicationContext,
            NotificationConstants.RESERVA_CHANNEL_ID
        )
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.valletlogo)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(Random().nextInt(), notification)
    }
}
