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
import br.edu.fatecpg.valletprojeto.model.Reserva
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import android.Manifest
import android.content.pm.PackageManager

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

    // Guarde vagaId e estacionamentoId atuais para usar no timer e cancelamento
    private var currentVagaId: String? = null
    private var currentEstacionamentoId: String? = null

    companion object {
        const val CHANNEL_ID = "reserva_channel"
        const val NOTIFICATION_ID = 1001
    }
    fun continuarTimerComFim(fimReserva: Date, vagaId: String, estacionamentoId: String, context: Context) {
        currentVagaId = vagaId
        currentEstacionamentoId = estacionamentoId
        iniciarTimer(fimReserva.time, vagaId, estacionamentoId, context)
    }
    fun iniciarReserva(vagaId: String, estacionamentoId: String, tempoMaxReservaHoras: Int, context: Context) {
        _reservaStatus.value = ReservaState.Loading

        val userId = auth.currentUser?.uid ?: run {
            _reservaStatus.value = ReservaState.Error("Usuário não autenticado")
            return
        }

        val agora = Timestamp.now()
        val fimReserva = Timestamp(Date(agora.toDate().time + tempoMaxReservaHoras * 60 * 60 * 1000))

        val reserva = Reserva(
            usuarioId = userId,
            vagaId = vagaId,
            estacionamentoId = estacionamentoId,
            inicioReserva = agora,
            fimReserva = fimReserva
        )

        db.collection("reserva")
            .add(reserva)
            .addOnSuccessListener { docRef ->
                currentReservaId = docRef.id
                currentVagaId = vagaId
                currentEstacionamentoId = estacionamentoId

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
            }
            .addOnFailureListener {
                _reservaStatus.value = ReservaState.Error("Erro ao criar reserva")
            }
    }
    // No ViewModel
    fun atualizarReservaAtiva(reservaId: String, fimReserva: Date, vagaId: String, estacionamentoId: String, context: Context) {
        currentReservaId = reservaId
        currentVagaId = vagaId
        currentEstacionamentoId = estacionamentoId
        iniciarTimer(fimReserva.time, vagaId, estacionamentoId, context)
    }

    // Agora receber vagaId e estacionamentoId para repassar na notificação
    fun cancelarReserva(context: Context, vagaId: String, estacionamentoId: String) {
        val reservaId = currentReservaId
        if (reservaId == null) {
            _reservaStatus.value = ReservaState.Error("Reserva não encontrada")
            return
        }

        db.collection("reserva").document(reservaId)
            .update("status", "cancelled")
            .addOnSuccessListener {
                _reservaStatus.value = ReservaState.Success("Reserva cancelada")
                timer?.cancel()
                _tempoRestante.value = "00:00"
                criarNotificacao(context, "Reserva Cancelada", "Sua reserva foi cancelada.", vagaId, estacionamentoId)
            }
            .addOnFailureListener {
                _reservaStatus.value = ReservaState.Error("Erro ao cancelar reserva")
            }
    }

    // Adicionado estacionamentoId para usar na notificação
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
                    .update("disponivel", false)
            }
        }.start()
    }

    // Parâmetros vagaId e estacionamentoId agora opcionais com valores padrão
    fun criarNotificacao(
        context: Context,
        titulo: String,
        texto: String,
        vagaId: String = "",
        estacionamentoId: String = ""
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reserva",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            val permissionCheck = ContextCompat.checkSelfPermission(context, permission)
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                Log.w("ReservaViewModel", "Permissão POST_NOTIFICATIONS não concedida, notificação não enviada")
                return
            }
        }

        val intent = Intent(context, ReservaActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("FROM_NOTIFICATION", true)
            if (vagaId.isNotEmpty()) putExtra("vagaId", vagaId)
            if (estacionamentoId.isNotEmpty()) putExtra("estacionamentoId", estacionamentoId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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
            Log.e("ReservaViewModel", "Erro ao postar notificação: ${e.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        timer?.cancel()
    }
}
