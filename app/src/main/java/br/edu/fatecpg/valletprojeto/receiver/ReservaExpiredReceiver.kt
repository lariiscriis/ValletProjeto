package br.edu.fatecpg.valletprojeto.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import br.edu.fatecpg.valletprojeto.R
import br.edu.fatecpg.valletprojeto.viewmodel.ReservaViewModel
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class ReservaExpiredReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val db = FirebaseFirestore.getInstance()
        val agora = Date()

        db.collection("reserva")
            .whereEqualTo("status", "ativa")
            .get()
            .addOnSuccessListener { docs ->
                for (doc in docs) {
                    val fimReserva = doc.getTimestamp("fimReserva")?.toDate()
                    if (fimReserva != null && fimReserva.before(agora)) {
                        val vagaId = doc.getString("vagaId")

                        // Atualiza status e libera vaga em sequência
                        db.collection("reserva").document(doc.id)
                            .update("status", "finalizada")
                            .addOnSuccessListener {
                                if (!vagaId.isNullOrEmpty()) {
                                    db.collection("vaga").document(vagaId)
                                        .update("disponivel", true)
                                }

                                // Exibe notificação
                                val notificationManager =
                                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                val notification = NotificationCompat.Builder(context, ReservaViewModel.CHANNEL_ID)
                                    .setSmallIcon(R.drawable.ic_parking)
                                    .setContentTitle("Reserva finalizada")
                                    .setContentText("O tempo da sua reserva terminou. A vaga foi liberada.")
                                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                                    .setAutoCancel(true)
                                    .build()

                                notificationManager.notify(
                                    ReservaViewModel.NOTIFICATION_ID + 4,
                                    notification
                                )
                            }
                    }
                }
            }
    }
}
