package br.edu.fatecpg.valletprojeto.viewmodel

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.edu.fatecpg.valletprojeto.R
import br.edu.fatecpg.valletprojeto.ReservaActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import android.Manifest
import android.content.pm.PackageManager
import br.edu.fatecpg.valletprojeto.receiver.ReservaAvisoReceiver
import br.edu.fatecpg.valletprojeto.receiver.ReservaExpiredReceiver

sealed class ReservaState {
    object Loading : ReservaState()
    data class Success(val reservaId: String) : ReservaState()
    data class Error(val message: String) : ReservaState()
}

class ReservaViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _tempoRestante = MutableLiveData("00:00")
    val tempoRestante: LiveData<String> = _tempoRestante

    private val _reservaStatus = MutableLiveData<ReservaState>()
    val reservaStatus: LiveData<ReservaState> = _reservaStatus

    private var timer: CountDownTimer? = null
    private var currentReservaId: String? = null
    private var currentVagaId: String? = null
    private var currentEstacionamentoId: String? = null

    companion object {
        const val CHANNEL_ID = "reserva_channel"
        const val NOTIFICATION_ID = 1001
    }

    /**
     * 🔹 Cria uma nova reserva
     */
    fun iniciarReserva(vagaId: String, estacionamentoId: String, tempoMaxReservaHoras: Int, context: Context) {
        _reservaStatus.value = ReservaState.Loading

        val userId = auth.currentUser?.uid ?: run {
            _reservaStatus.value = ReservaState.Error("Usuário não autenticado")
            Log.e("ReservaVM", "Usuário não autenticado")
            return
        }

        if (vagaId.isBlank() || estacionamentoId.isBlank()) {
            _reservaStatus.value = ReservaState.Error("Dados da vaga/estacionamento inválidos")
            return
        }

        db.collection("reserva")
            .whereEqualTo("usuarioId", userId)
            .whereEqualTo("status", "ativa")
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    _reservaStatus.value = ReservaState.Error("Você já possui uma reserva ativa")
                    return@addOnSuccessListener
                }

                val agora = Timestamp.now()
                val fimMillis = agora.toDate().time + tempoMaxReservaHoras * 60L * 60L * 1000L
                val fimReserva = Timestamp(Date(fimMillis))

                val reservaMap = hashMapOf(
                    "usuarioId" to userId,
                    "vagaId" to vagaId,
                    "estacionamentoId" to estacionamentoId,
                    "inicioReserva" to agora,
                    "fimReserva" to fimReserva,
                    "status" to "ativa"
                )

                db.collection("reserva")
                    .add(reservaMap)
                    .addOnSuccessListener { docRef ->
                        currentReservaId = docRef.id
                        currentVagaId = vagaId
                        currentEstacionamentoId = estacionamentoId

                        db.collection("vaga").document(vagaId)
                            .update("disponivel", false)
                            .addOnSuccessListener {
                                _reservaStatus.value = ReservaState.Success(docRef.id)

                                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                                val inicioStr = sdf.format(agora.toDate())
                                val fimStr = sdf.format(fimReserva.toDate())

                                criarNotificacao(
                                    context,
                                    "Reserva Confirmada",
                                    "Vaga $vagaId reservada das $inicioStr às $fimStr",
                                    vagaId,
                                    estacionamentoId
                                )

                                iniciarTimer(fimReserva.toDate().time, vagaId, estacionamentoId, context)
                                agendarExpiracaoReserva(context, fimReserva.toDate().time)
                            }
                            .addOnFailureListener { eUpdate ->
                                db.collection("reserva").document(docRef.id).delete()
                                _reservaStatus.value = ReservaState.Error("Erro ao reservar vaga: ${eUpdate.message}")
                            }
                    }
                    .addOnFailureListener { e ->
                        _reservaStatus.value = ReservaState.Error("Erro ao criar reserva: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                _reservaStatus.value = ReservaState.Error("Erro ao verificar reservas: ${e.message}")
            }
    }

    /**
     * 🔹 Retoma o timer de uma reserva já existente (ativa)
     */
    fun retomarReservaAtiva(fimReserva: Date, vagaId: String, estacionamentoId: String, context: Context) {
        currentVagaId = vagaId
        currentEstacionamentoId = estacionamentoId
        iniciarTimer(fimReserva.time, vagaId, estacionamentoId, context)
        Log.d("ReservaVM", "Timer retomado para reserva ativa ($vagaId)")
    }

    /**
     * 🔹 Cancela uma reserva ativa
     */
    fun cancelarReserva(context: Context, vagaId: String, estacionamentoId: String) {
        val reservaId = currentReservaId ?: run {
            _reservaStatus.value = ReservaState.Error("Reserva não encontrada")
            return
        }

        db.collection("reserva").document(reservaId)
            .update("status", "cancelada")
            .addOnSuccessListener {
                db.collection("vaga").document(vagaId)
                    .update("disponivel", true)
                    .addOnSuccessListener {
                        timer?.cancel()
                        _tempoRestante.value = "00:00"
                        _reservaStatus.value = ReservaState.Success("cancelada")

                        criarNotificacao(context, "Reserva Cancelada", "Sua reserva foi cancelada.", vagaId, estacionamentoId)
                    }
                    .addOnFailureListener { e ->
                        _reservaStatus.value = ReservaState.Error("Reserva cancelada, mas falha ao liberar vaga: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                _reservaStatus.value = ReservaState.Error("Erro ao cancelar reserva: ${e.message}")
            }
    }

    /**
     * ⏱️ Inicia (ou reinicia) o contador regressivo
     */
    private fun iniciarTimer(fimTimestamp: Long, vagaId: String, estacionamentoId: String, context: Context) {
        timer?.cancel()

        val tempoRestanteMs = fimTimestamp - System.currentTimeMillis()
        if (tempoRestanteMs <= 0) {
            _tempoRestante.value = "00:00"
            return
        }

        timer = object : CountDownTimer(tempoRestanteMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutos = (millisUntilFinished / 1000) / 60
                val segundos = (millisUntilFinished / 1000) % 60
                _tempoRestante.value = String.format("%02d:%02d", minutos, segundos)

                if (minutos == 10L && segundos == 0L) {
                    criarNotificacao(context, "Aviso", "Faltam 10 minutos para sua reserva acabar!", vagaId, estacionamentoId)
                }
            }

            override fun onFinish() {
                _tempoRestante.value = "00:00"
                criarNotificacao(context, "Reserva Finalizada", "Sua reserva foi finalizada.", vagaId, estacionamentoId)

                currentReservaId?.let { reservaId ->
                    db.collection("reserva").document(reservaId)
                        .update("status", "finalizada")
                }

                db.collection("vaga").document(vagaId)
                    .update("disponivel", true)
            }
        }.start()
    }

    /**
     * 🔔 Agenda notificações de aviso e expiração
     */
    private fun agendarExpiracaoReserva(context: Context, fimReservaMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Aviso 10 min antes
        val avisoIntent = Intent(context, ReservaAvisoReceiver::class.java)
        val avisoPending = PendingIntent.getBroadcast(
            context, 1, avisoIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val avisoMillis = fimReservaMillis - 10 * 60 * 1000
        if (avisoMillis > System.currentTimeMillis()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, avisoMillis, avisoPending)
        }

        // Expiração
        val expiraIntent = Intent(context, ReservaExpiredReceiver::class.java)
        val expiraPending = PendingIntent.getBroadcast(
            context, 2, expiraIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fimReservaMillis, expiraPending)
    }

    /**
     * 🔔 Cria notificações personalizadas
     */
    fun criarNotificacao(
        context: Context,
        titulo: String,
        texto: String,
        vagaId: String = "",
        estacionamentoId: String = ""
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Reserva", NotificationManager.IMPORTANCE_HIGH)
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w("ReservaVM", "Sem permissão para enviar notificações.")
            return
        }

        val intent = Intent(context, ReservaActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("FROM_NOTIFICATION", true)
            if (vagaId.isNotEmpty()) putExtra("vagaId", vagaId)
            if (estacionamentoId.isNotEmpty()) putExtra("estacionamentoId", estacionamentoId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_parking)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            Log.e("ReservaVM", "Erro ao postar notificação: ${e.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        timer?.cancel()
    }
}
