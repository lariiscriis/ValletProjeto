package br.edu.fatecpg.valletprojeto.worker

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.edu.fatecpg.valletprojeto.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*

class VerificarReservasExpiradasWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val db = FirebaseFirestore.getInstance()

    override suspend fun doWork(): Result {
        Log.d("VerificarReservasExpiradas", "Verificando todas as reservas expiradas...")

        return try {
            // Busca todas as reservas ativas que expiraram
            val agora = Timestamp.now()
            val reservasExpiradas = db.collection("reserva")
                .whereEqualTo("status", "ativa")
                .whereLessThan("fimReserva", agora)
                .get()
                .await()

            Log.d("VerificarReservasExpiradas", "Encontradas ${reservasExpiradas.size()} reservas expiradas")

            // Finaliza cada reserva expirada
            for (document in reservasExpiradas) {
                val reservaId = document.id
                val vagaId = document.getString("vagaId")

                if (vagaId != null) {
                    finalizarReservaExpirada(reservaId, vagaId)
                }
            }

            Log.d("VerificarReservasExpiradas", "Verificação concluída")
            Result.success()
        } catch (e: Exception) {
            Log.e("VerificarReservasExpiradas", "Erro: ${e.message}", e)
            Result.retry()
        }
    }

    private suspend fun finalizarReservaExpirada(reservaId: String, vagaId: String) {
        try {
            val reservaRef = db.collection("reserva").document(reservaId)
            val vagaRef = db.collection("vaga").document(vagaId)

            val batch = db.batch()
            batch.update(reservaRef, "status", "finalizada")
            batch.update(vagaRef, "disponivel", true)
            batch.commit().await()

            Log.d("VerificarReservasExpiradas", "Reserva $reservaId finalizada automaticamente")

            // Envia notificação
            enviarNotificacaoExpirada(vagaId)
        } catch (e: Exception) {
            Log.e("VerificarReservasExpiradas", "Erro ao finalizar reserva $reservaId: ${e.message}")
        }
    }

    private fun enviarNotificacaoExpirada(vagaId: String) {
        NotificationUtils.createNotificationChannel(applicationContext)
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(applicationContext, NotificationConstants.RESERVA_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_parking)
            .setContentTitle("Reserva Finalizada Automaticamente")
            .setContentText("Sua reserva na vaga $vagaId foi encerrada.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify("expirada_${vagaId}".hashCode(), notification)
    }
}