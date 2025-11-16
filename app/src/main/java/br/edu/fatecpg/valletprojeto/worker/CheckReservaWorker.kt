package br.edu.fatecpg.valletprojeto.worker

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.edu.fatecpg.valletprojeto.R
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.*

class CheckReservaWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db = Firebase.firestore
        val agora = Date()

        try {
            val snapshot = db.collection("reserva")
                .whereEqualTo("status", "ativa")
                .whereLessThan("fimReserva", agora)
                .get()
                .await()

            if (snapshot.isEmpty) {
                return Result.success()
            }

            for (doc in snapshot.documents) {
                val vagaId = doc.getString("vagaId")

                db.collection("reserva").document(doc.id)
                    .update("status", "finalizada")
                    .await()

                if (!vagaId.isNullOrEmpty()) {
                    db.collection("vaga").document(vagaId)
                        .update("disponivel", true)
                        .await()
                }

                enviarNotificacao(vagaId ?: "desconhecida")
            }

            return Result.success()

        } catch (e: Exception) {
            return Result.failure()
        }
    }

    private fun enviarNotificacao(vagaId: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, NotificationConstants.RESERVA_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_parking)
            .setContentTitle("Reserva Finalizada")
            .setContentText("Sua reserva para a vaga $vagaId expirou e foi finalizada.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
