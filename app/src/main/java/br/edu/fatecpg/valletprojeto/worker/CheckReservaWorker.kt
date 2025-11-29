package br.edu.fatecpg.valletprojeto.worker

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import br.edu.fatecpg.valletprojeto.R
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*
import java.util.concurrent.TimeUnit

class CheckReservaWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val db = FirebaseFirestore.getInstance()

    override suspend fun doWork(): Result {
        return try {
            val reservaId = inputData.getString("reservaId")
            val vagaId = inputData.getString("vagaId")
            val tipo = inputData.getString("tipo") ?: "verificacao"

            Log.d("CheckReservaWorker", "Iniciando trabalho - Tipo: $tipo, Reserva: $reservaId")

            if (reservaId == null || vagaId == null) {
                Log.e("CheckReservaWorker", "❌ Dados incompletos")
                return Result.failure()
            }

            val reservaDoc = db.collection("reserva").document(reservaId).get().await()

            if (!reservaDoc.exists()) {
                Log.e("CheckReservaWorker", "❌ Reserva não encontrada: $reservaId")
                return Result.success()
            }

            val status = reservaDoc.getString("status")
            val fimReserva = reservaDoc.getTimestamp("fimReserva")?.toDate()

            Log.d("CheckReservaWorker", "Status: $status, Fim: $fimReserva")

            when {
                status != "ativa" -> {
                    Log.d("CheckReservaWorker", "✅ Reserva já está $status - Nada a fazer")
                    return Result.success()
                }

                fimReserva != null && fimReserva.time <= System.currentTimeMillis() -> {
                    Log.d("CheckReservaWorker", "⏰ Reserva expirada - Finalizando...")
                    return finalizarReserva(reservaId, vagaId)
                }

                else -> {
                    val tempoRestante = fimReserva!!.time - System.currentTimeMillis()
                    Log.d("CheckReservaWorker", "⏳ Reserva ainda ativa - ${tempoRestante/1000}s restantes")

                    if (tempoRestante > TimeUnit.MINUTES.toMillis(1)) {
                        reagendarVerificacao(reservaId, vagaId, tempoRestante)
                    } else {
                        kotlinx.coroutines.delay(tempoRestante)
                        return finalizarReserva(reservaId, vagaId)
                    }
                    return Result.success()
                }
            }

        } catch (e: Exception) {
            Log.e("CheckReservaWorker", "❌ Erro crítico: ${e.message}", e)
            return Result.retry()
        }
    }

    private suspend fun finalizarReserva(reservaId: String, vagaId: String): Result {
        return try {
            val reservaRef = db.collection("reserva").document(reservaId)
            val vagaRef = db.collection("vaga").document(vagaId)

            db.runTransaction { transaction ->
                val reservaSnapshot = transaction.get(reservaRef)
                val currentStatus = reservaSnapshot.getString("status")

                if (currentStatus == "ativa") {
                    transaction.update(reservaRef, "status", "finalizada")
                    transaction.update(vagaRef, "disponivel", true)
                    Log.d("CheckReservaWorker", "✅ Transaction: Reserva finalizada e vaga liberada")
                } else {
                    Log.d("CheckReservaWorker", "ℹ️  Transaction: Reserva já estava $currentStatus")
                }
            }.await()

            val reservaVerificada = db.collection("reserva").document(reservaId).get().await()
            if (reservaVerificada.getString("status") == "finalizada") {
                enviarNotificacaoExpirada(vagaId)
                Log.d("CheckReservaWorker", "🎉 Reserva $reservaId finalizada com sucesso!")
                Result.success()
            } else {
                Log.e("CheckReservaWorker", "❌ Falha ao finalizar reserva - Status ainda ativo")
                Result.retry()
            }

        } catch (e: Exception) {
            Log.e("CheckReservaWorker", "❌ Erro ao finalizar reserva: ${e.message}", e)
            Result.retry()
        }
    }

    private fun reagendarVerificacao(reservaId: String, vagaId: String, delayMillis: Long) {
        val verificationData = workDataOf(
            "reservaId" to reservaId,
            "vagaId" to vagaId,
            "tipo" to "verificacao_expiracao"
        )

        val verificationRequest = OneTimeWorkRequestBuilder<CheckReservaWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(verificationData)
            .addTag("reserva_${reservaId}")
            .build()

        WorkManager.getInstance(context).enqueue(verificationRequest)
        Log.d("CheckReservaWorker", "🔄 Verificação reagendada para ${delayMillis/1000}s")
    }

    private suspend fun enviarNotificacaoExpirada(vagaId: String) {
        try {
            // 🔥 BUSCAR O NÚMERO DA VAGA NO FIRESTORE
            val vagaDoc = db.collection("vaga").document(vagaId).get().await()
            val numeroVaga = vagaDoc.getString("numero") ?: vagaId // Fallback para ID se não tiver número

            NotificationUtils.createNotificationChannel(context)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val notification = NotificationCompat.Builder(context, NotificationConstants.RESERVA_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_parking)
                .setContentTitle("Reserva Finalizada!")
                .setContentText("Sua reserva na vaga $numeroVaga foi encerrada.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("Sua reserva na vaga $numeroVaga foi finalizada automaticamente. Obrigado por usar nosso serviço!"))
                .build()

            notificationManager.notify("RESERVA_EXPIRADA_${vagaId}".hashCode(), notification)
            Log.d("CheckReservaWorker", "📲 Notificação de expiração enviada para vaga $numeroVaga")

        } catch (e: Exception) {
            Log.e("CheckReservaWorker", "❌ Erro ao enviar notificação: ${e.message}")
            enviarNotificacaoFallback(vagaId)
        }
    }

    private fun enviarNotificacaoFallback(vagaId: String) {
        try {
            NotificationUtils.createNotificationChannel(context)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val notification = NotificationCompat.Builder(context, NotificationConstants.RESERVA_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_parking)
                .setContentTitle("Reserva Finalizada!")
                .setContentText("Sua reserva foi encerrada.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("Sua reserva foi finalizada automaticamente. Obrigado por usar nosso serviço!"))
                .build()

            notificationManager.notify("RESERVA_EXPIRADA_${vagaId}".hashCode(), notification)
        } catch (e: Exception) {
            Log.e("CheckReservaWorker", "❌ Erro crítico no fallback: ${e.message}")
        }
    }
}
