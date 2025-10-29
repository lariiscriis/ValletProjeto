package br.edu.fatecpg.valletprojeto.worker

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import br.edu.fatecpg.valletprojeto.R
import br.edu.fatecpg.valletprojeto.viewmodel.ReservaViewModel
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class CheckReservaWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    private val db = FirebaseFirestore.getInstance()

    override fun doWork(): Result {
        val agora = Date()

        db.collection("reserva")
            .whereEqualTo("status", "ativa")
            .get()
            .addOnSuccessListener { docs ->
                for (doc in docs) {
                    val fimReserva = doc.getTimestamp("fimReserva")?.toDate()
                    if (fimReserva != null && fimReserva.before(agora)) {
                        val vagaId = doc.getString("vagaId")

                        db.collection("reserva").document(doc.id)
                            .update("status", "finalizada")
                            .addOnSuccessListener {
                                if (!vagaId.isNullOrEmpty()) {
                                    db.collection("vaga").document(vagaId)
                                        .update("disponivel", true)
                                }

                                // Notificação de finalização
                                val notificationManager =
                                    applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                val notification = NotificationCompat.Builder(applicationContext, ReservaViewModel.CHANNEL_ID)
                                    .setSmallIcon(R.drawable.ic_parking)
                                    .setContentTitle("Reserva finalizada automaticamente")
                                    .setContentText("Sua reserva expirou e a vaga foi liberada.")
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                    .setAutoCancel(true)
                                    .build()

                                notificationManager.notify(
                                    ReservaViewModel.NOTIFICATION_ID + 7,
                                    notification
                                )
                            }
                    }
                }
            }

        // Retorna sucesso (mesmo que async)
        return Result.success()
    }
}
