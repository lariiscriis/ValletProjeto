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

            // 🔥 VERIFICAR SE A RESERVA AINDA EXISTE E ESTÁ ATIVA
            val reservaDoc = db.collection("reserva").document(reservaId).get().await()

            if (!reservaDoc.exists()) {
                Log.e("CheckReservaWorker", "❌ Reserva não encontrada: $reservaId")
                return Result.failure()
            }

            val status = reservaDoc.getString("status")
            val fimReserva = reservaDoc.getTimestamp("fimReserva")?.toDate()

            Log.d("CheckReservaWorker", "Status: $status, Fim: $fimReserva")

            when {
                // 🔥 CASO 1: RESERVA JÁ ESTÁ FINALIZADA
                status != "ativa" -> {
                    Log.d("CheckReservaWorker", "✅ Reserva já está $status - Nada a fazer")
                    return Result.success()
                }

                // 🔥 CASO 2: RESERVA EXPIRADA - FINALIZAR
                fimReserva != null && fimReserva.time <= System.currentTimeMillis() -> {
                    Log.d("CheckReservaWorker", "⏰ Reserva expirada - Finalizando...")
                    return finalizarReserva(reservaId, vagaId)
                }

                // 🔥 CASO 3: RESERVA AINDA NÃO EXPIROU - REAGENDAR VERIFICAÇÃO
                else -> {
                    val tempoRestante = fimReserva!!.time - System.currentTimeMillis()
                    Log.d("CheckReservaWorker", "⏳ Reserva ainda ativa - ${tempoRestante/1000}s restantes")

                    // Reagendar verificação para o momento exato da expiração
                    if (tempoRestante > 0) {
                        reagendarVerificacao(reservaId, vagaId, tempoRestante)
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

            // 🔥 USAR TRANSACTION PARA GARANTIR CONSISTÊNCIA
            db.runTransaction { transaction ->
                // Verificar novamente o status dentro da transaction
                val reservaSnapshot = transaction.get(reservaRef)
                if (reservaSnapshot.getString("status") == "ativa") {
                    transaction.update(reservaRef, "status", "finalizada")
                    transaction.update(vagaRef, "disponivel", true)
                    Log.d("CheckReservaWorker", "✅ Transaction: Reserva finalizada e vaga liberada")
                } else {
                    Log.d("CheckReservaWorker", "ℹ️  Transaction: Reserva já estava ${reservaSnapshot.getString("status")}")
                }
            }.await()

            // 🔥 ENVIAR NOTIFICAÇÃO DE CONFIRMAÇÃO
            enviarNotificacaoExpirada(vagaId)
            Log.d("CheckReservaWorker", "🎉 Reserva $reservaId finalizada com sucesso!")

            Result.success()

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
            .build()

        WorkManager.getInstance(context).enqueue(verificationRequest)
        Log.d("CheckReservaWorker", "🔄 Verificação reagendada para ${delayMillis/1000}s")
    }

    private fun enviarNotificacaoExpirada(vagaId: String) {
        try {
            NotificationUtils.createNotificationChannel(context)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val notification = NotificationCompat.Builder(context, NotificationConstants.RESERVA_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_parking)
                .setContentTitle("Reserva Finalizada!")
                .setContentText("Sua reserva na vaga $vagaId foi encerrada.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("Sua reserva na vaga $vagaId foi finalizada automaticamente. Obrigado por usar nosso serviço!"))
                .build()

            notificationManager.notify("RESERVA_EXPIRADA_${vagaId}".hashCode(), notification)
            Log.d("CheckReservaWorker", "📲 Notificação de expiração enviada")

        } catch (e: Exception) {
            Log.e("CheckReservaWorker", "❌ Erro ao enviar notificação: ${e.message}")
        }
    }
}