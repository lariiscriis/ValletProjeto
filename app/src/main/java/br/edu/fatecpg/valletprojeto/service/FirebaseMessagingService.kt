package br.edu.fatecpg.valletprojeto.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import br.edu.fatecpg.valletprojeto.DashboardBase
import br.edu.fatecpg.valletprojeto.R
import br.edu.fatecpg.valletprojeto.model.Vaga
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FirebaseMessagingService : FirebaseMessagingService() {
    private val db = FirebaseFirestore.getInstance()
    private val parkingService = ParkingService(db)
    private val auth = FirebaseAuth.getInstance()

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // 🔥 LOG DETALHADO DE TODOS OS DADOS RECEBIDOS
        Log.d("FCM_PARKING", "=== MENSAGEM FCM RECEBIDA ===")
        Log.d("FCM_PARKING", "Dados recebidos: ${remoteMessage.data}")
        Log.d("FCM_PARKING", "De: ${remoteMessage.from}")
        Log.d("FCM_PARKING", "ID da mensagem: ${remoteMessage.messageId}")

        if (remoteMessage.data.isNotEmpty()) {
            val data = remoteMessage.data
            val vagaId = data["vagaId"]
            val usuarioId = data["usuarioId"] ?: auth.currentUser?.uid
            val placa = data["placa"] ?: "Desconhecida"
            val tipo = data["tipo"] ?: "desconhecido"
            val notificationId = System.currentTimeMillis().toInt()

            Log.d("FCM_PARKING", "📊 Dados extraídos:")
            Log.d("FCM_PARKING", "   vagaId: $vagaId")
            Log.d("FCM_PARKING", "   usuarioId: $usuarioId")
            Log.d("FCM_PARKING", "   placa: $placa")
            Log.d("FCM_PARKING", "   tipo: $tipo")
            Log.d("FCM_PARKING", "   currentUser: ${auth.currentUser?.uid}")

            // 🔥 VERIFICAÇÃO MAIS ROBUSTA
            if (vagaId != null && usuarioId != null) {
                Log.d("FCM_PARKING", "✅ Processando evento de estacionamento para usuário")
                serviceScope.launch {
                    handleParkingEvent(vagaId, usuarioId, notificationId, placa, tipo)
                }
            } else if (vagaId != null && usuarioId == null) {
                Log.d("FCM_PARKING", "⚠️  Evento de estacionamento sem usuário ID")
                serviceScope.launch {
                    parkingService.handleNonUserParking(vagaId, placa)
                }
            } else {
                Log.e("FCM_PARKING", "❌ Dados insuficientes: vagaId=$vagaId, usuarioId=$usuarioId")

                // 🔥 ENVIA NOTIFICAÇÃO DE ERRO PARA DEBUG
                sendErrorNotification("Dados incompletos", "vagaId: $vagaId, usuarioId: $usuarioId")
            }
        } else {
            Log.e("FCM_PARKING", "❌ Mensagem FCM sem dados")
        }
    }

    private suspend fun handleParkingEvent(
        vagaId: String,
        usuarioId: String,
        notificationId: Int,
        placa: String,
        tipo: String
    ) {
        Log.d("FCM_PARKING", "🔄 Iniciando handleParkingEvent")
        Log.d("FCM_PARKING", "   vagaId: $vagaId")
        Log.d("FCM_PARKING", "   usuarioId: $usuarioId")
        Log.d("FCM_PARKING", "   placa: $placa")
        Log.d("FCM_PARKING", "   tipo: $tipo")

        val listener = object : ParkingService.ParkingActionListener {
            override fun askToCreateReservation(
                vaga: Vaga,
                usuarioId: String,
                onConfirm: () -> Unit,
                onCancel: () -> Unit
            ) {
                Log.d("FCM_PARKING", "📝 Solicitando criação de reserva para vaga: ${vaga.numero}")
                sendConfirmationNotification(vaga, usuarioId, notificationId, placa)
            }

            override fun notifyAdmin(vaga: Vaga, motivo: String, detalhes: Map<String, Any?>) {
                Log.d("FCM_PARKING", "👨‍💼 Notificando admin: $motivo")
                this@FirebaseMessagingService.notifyAdmin(vaga, motivo, detalhes)
            }
        }

        parkingService.handleUserParking(vagaId, usuarioId, listener)
    }

    // 🔥 ADICIONE ESTA FUNÇÃO PARA NOTIFICAÇÕES DE ERRO
    private fun sendErrorNotification(titulo: String, mensagem: String) {
        val channelId = "parking_errors"
        val channelName = "Erros do Sistema"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Erro no Sistema: $titulo")
            .setContentText(mensagem)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun notifyAdmin(vaga: Vaga, motivo: String, detalhes: Map<String, Any?>) {
        val uid = detalhes["usuarioId"] as? String ?: auth.currentUser?.uid ?: "desconhecido"
        val placa = detalhes["placaVeiculo"] as? String ?: ""

        val notificacao = hashMapOf(
            "vagaId" to vaga.id,
            "placa" to placa,
            "usuarioId" to uid,
            "motivo" to motivo,
            "timestamp" to Timestamp.now(),
            "tipo" to "sistema_alerta_estacionamento"
        )

        db.collection("notificacoes_admin").add(notificacao)
            .addOnSuccessListener { Log.d("FCM_PARKING", "Admin notificado com sucesso. Motivo: $motivo") }
            .addOnFailureListener { e -> Log.e("FCM_PARKING", "Erro ao notificar admin", e) }
    }

    private fun sendConfirmationNotification(vaga: Vaga, usuarioId: String, notificationId: Int, placa: String) {
        val channelId = "parking_alerts"
        val channelName = "Alertas de Estacionamento"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val confirmIntent = Intent(this, ConfirmacaoReceiver::class.java).apply {
            action = "ACTION_CONFIRM_RESERVATION"
            putExtra("confirmacao", true)
            putExtra("vagaId", vaga.id)
            putExtra("usuarioId", usuarioId)
            putExtra("placa", placa)
            putExtra("notification_id", notificationId)
        }
        val confirmPendingIntent = PendingIntent.getBroadcast(this, notificationId * 2, confirmIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val cancelIntent = Intent(this, ConfirmacaoReceiver::class.java).apply {
            action = "ACTION_CANCEL_RESERVATION"
            putExtra("confirmacao", false)
            putExtra("vagaId", vaga.id)
            putExtra("usuarioId", usuarioId)
            putExtra("placa", placa)
            putExtra("notification_id", notificationId)
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(this, notificationId * 2 + 1, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Estacionamento Detectado - Placa: $placa")
            .setContentText("Você estacionou na vaga ${vaga.numero}. Deseja criar uma reserva de 1 hora?")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(0, "Criar Reserva", confirmPendingIntent)
            .addAction(0, "Não", cancelPendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
        Log.d("FCM_PARKING", "📲 Notificação de confirmação enviada para o usuário")
    }

    override fun onNewToken(token: String) {
        Log.d("FCM_TOKEN", "Novo token: $token")
        // Atualize o token no Firebase se necessário
    }
}