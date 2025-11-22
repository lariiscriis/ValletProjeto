package br.edu.fatecpg.valletprojeto.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import br.edu.fatecpg.valletprojeto.model.Reserva
import br.edu.fatecpg.valletprojeto.model.Vaga
import br.edu.fatecpg.valletprojeto.model.Veiculo
import br.edu.fatecpg.valletprojeto.receiver.ReservaAvisoReceiver
import br.edu.fatecpg.valletprojeto.receiver.ReservaCriadaReceiver
import br.edu.fatecpg.valletprojeto.receiver.ReservaExpiredReceiver
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import java.util.concurrent.TimeUnit


sealed class ReservaUIState {
    object Initial : ReservaUIState()
    data class Idle(val vaga: Vaga, val veiculo: Veiculo) : ReservaUIState()
    object Loading : ReservaUIState()
    data class Active(val reserva: Reserva, val vaga: Vaga, val veiculo: Veiculo) : ReservaUIState()
    data class Finished(val message: String) : ReservaUIState()
    data class Error(val message: String) : ReservaUIState()
}

class ReservaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableLiveData<ReservaUIState>(ReservaUIState.Initial)
    val uiState: LiveData<ReservaUIState> = _uiState

    private val _tempoRestante = MutableLiveData<String>()
    val tempoRestante: LiveData<String> = _tempoRestante

    private var timer: CountDownTimer? = null
    private var idReservaAtiva: String? = null

    private var idVagaAtiva: String? = null

    fun carregarDadosIniciais(vagaId: String) {
        viewModelScope.launch {
            _uiState.value = ReservaUIState.Loading
            try {
                val vagaDoc = db.collection("vaga").document(vagaId).get().await()

                val vaga = vagaDoc.toObject(Vaga::class.java)?.apply {
                    id = vagaDoc.id
                }

                val veiculoAsync = async { buscarVeiculoPadrao() }
                val reservaAtivaAsync = async { buscarReservaAtiva(vagaId) }

                val veiculo = veiculoAsync.await()
                val reservaAtiva = reservaAtivaAsync.await()

                if (vaga != null && veiculo != null) {
                    if (reservaAtiva != null) {
                        _uiState.value = ReservaUIState.Active(reservaAtiva, vaga, veiculo)
                        iniciarTimer(reservaAtiva.fimReserva!!.toDate())
                    } else {
                        _uiState.value = ReservaUIState.Idle(vaga, veiculo)
                    }
                } else {
                    _uiState.value = ReservaUIState.Error("Vaga ou veículo padrão não encontrado.")
                }
            } catch (e: Exception) {
                _uiState.value = ReservaUIState.Error("Erro ao carregar dados: ${e.message}")
            }
        }
    }

    fun agendarNotificacoes(context: Context, reserva: Reserva) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val fimReservaMillis = reserva.fimReserva?.toDate()?.time ?: return
        Log.d("Notificacoes", "Agendando notificações para reserva: ${reserva.id}")

        // Notificação de aviso (10 minutos antes)
        val avisoIntent = Intent(context, ReservaAvisoReceiver::class.java).apply {
            putExtra("vagaId", reserva.vagaId)
            putExtra("estacionamentoId", reserva.estacionamentoId)
        }
        val avisoPendingIntent = PendingIntent.getBroadcast(
            context,
            reserva.id.hashCode(),
            avisoIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val avisoMillis = fimReservaMillis - TimeUnit.MINUTES.toMillis(10)
        if (avisoMillis > System.currentTimeMillis()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, avisoMillis, avisoPendingIntent)
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, avisoMillis, avisoPendingIntent)
            }
        }

        // Notificação de expiração
        val expiraIntent = Intent(context, ReservaExpiredReceiver::class.java).apply {
            putExtra("reservaId", reserva.id)
            putExtra("vagaId", reserva.vagaId)

        }
        val expiraPendingIntent = PendingIntent.getBroadcast(
            context,
            reserva.id.hashCode() + 1,
            expiraIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, fimReservaMillis, expiraPendingIntent)
            }
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, fimReservaMillis, expiraPendingIntent)
        }
        Log.d("Notificacoes", "Aviso agendado para: ${Date(avisoMillis)}")
        Log.d("Notificacoes", "Expiração agendada para: ${Date(fimReservaMillis)}")
        Log.d("Notificacoes", "Notificações agendadas para reserva ${reserva.id}")
    }

    fun criarReserva(vagaId: String, estacionamentoId: String, estacionamentoNome: String, horas: Int) {
        idVagaAtiva = vagaId
        viewModelScope.launch {
            _uiState.value = ReservaUIState.Loading
            val userId = auth.currentUser?.uid ?: return@launch
            try {
                val agora = Timestamp.now()
                val fimReserva = Timestamp(Date(agora.toDate().time + TimeUnit.HOURS.toMillis(horas.toLong())))

                val novaReserva = Reserva(
                    usuarioId = userId,
                    vagaId = vagaId,
                    estacionamentoId = estacionamentoId,
                    estacionamentoNome = estacionamentoNome,
                    status = "ativa",
                    inicioReserva = agora,
                    fimReserva = fimReserva
                )

                val docRef = db.collection("reserva").add(novaReserva).await()
                idReservaAtiva = docRef.id

                db.collection("vaga").document(vagaId).update("disponivel", false).await()
                val reservaCriada = novaReserva.apply { id = docRef.id }

                // 🔥 ADICIONE ESTAS LINHAS PARA NOTIFICAÇÃO IMEDIATA
                enviarNotificacaoReservaCriada(getApplication(), vagaId, estacionamentoNome)

                agendarNotificacoes(getApplication(), reservaCriada)
                carregarDadosIniciais(vagaId)
            } catch (e: Exception) {
                _uiState.value = ReservaUIState.Error("Erro ao criar reserva: ${e.message}")
            }
        }
    }

    // 🔥 ADICIONE ESTE MÉTODO NOVO
    private fun enviarNotificacaoReservaCriada(context: Context, vagaId: String, estacionamentoNome: String) {
        val intent = Intent(context, ReservaCriadaReceiver::class.java).apply {
            putExtra("vagaId", vagaId)
            putExtra("estacionamentoNome", estacionamentoNome)
        }
        context.sendBroadcast(intent)
        Log.d("Notificacoes", "Notificação de reserva criada enviada para vaga: $vagaId")
    }
    fun renovarReserva(reserva: Reserva) {
        viewModelScope.launch {
            val idReserva = reserva.id
            val idVaga = reserva.vagaId

            Log.d("Renovacao", "Iniciando renovação para reservaId: $idReserva, vagaId: $idVaga")

            _uiState.value = ReservaUIState.Loading
            try {
                val proximaReserva = db.collection("reserva")
                    .whereEqualTo("vagaId", idVaga)
                    .whereGreaterThan("inicioReserva", reserva.fimReserva!!)
                    .orderBy("inicioReserva")
                    .limit(1)
                    .get().await()

                if (!proximaReserva.isEmpty) {
                    Log.w("Renovacao", "Falha: Já existe uma reserva futura para a vaga $idVaga.")
                    _uiState.value = ReservaUIState.Error("Não é possível renovar. Vaga já reservada para o próximo horário.")
                    carregarDadosIniciais(idVaga)
                    return@launch
                }

                val novoFim = Timestamp(Date(reserva.fimReserva.toDate().time + TimeUnit.HOURS.toMillis(1)))

                Log.d("Renovacao", "Atualizando reserva $idReserva com novo fim: $novoFim")
                db.collection("reserva").document(idReserva).update("fimReserva", novoFim).await()

                Log.d("Renovacao", "Renovação bem-sucedida. Recarregando dados...")
                carregarDadosIniciais(idVaga)

            } catch (e: Exception) {
                Log.e("Renovacao", "ERRO CRÍTICO durante a renovação: ${e.message}", e)
                _uiState.value = ReservaUIState.Error("Erro ao renovar: ${e.message}")
            }
        }
    }

    fun cancelarReserva(vaga: Vaga) {
        viewModelScope.launch {
            _uiState.value = ReservaUIState.Loading
            val userId = auth.currentUser?.uid

            Log.d("Cancelamento", "Iniciando cancelamento para vagaId: ${vaga.id}")

            if (userId == null) {
                Log.e("Cancelamento", "Falha: Usuário não autenticado.")
                _uiState.value = ReservaUIState.Error("Usuário não autenticado.")
                return@launch
            }
            Log.d("Cancelamento", "Usuário autenticado: $userId")

            try {
                Log.d("Cancelamento", "Executando query com: usuarioId=${userId}, vagaId=${vaga.id}, status=ativa")

                val reservaAtivaSnapshot = db.collection("reserva")
                    .whereEqualTo("usuarioId", userId)
                    .whereEqualTo("vagaId", vaga.id)
                    .whereEqualTo("status", "ativa")
                    .limit(1)
                    .get()
                    .await()

                val reservaDoc = reservaAtivaSnapshot.documents.firstOrNull()

                if (reservaDoc == null) {
                    Log.e("Cancelamento", "FALHA: Nenhuma reserva ativa encontrada com os critérios da query.")
                    _uiState.value = ReservaUIState.Error("Nenhuma reserva ativa encontrada para cancelar.")
                    return@launch
                }

                Log.d("Cancelamento", "SUCESSO: Reserva encontrada, ID do documento: ${reservaDoc.id}. Iniciando batch...")

                val batch = db.batch()
                batch.update(reservaDoc.reference, "status", "cancelada")
                val vagaRef = db.collection("vaga").document(vaga.id)
                batch.update(vagaRef, "disponivel", true)

                Log.d("Cancelamento", "Executando batch.commit()...")
                batch.commit().await()

                timer?.cancel()
                Log.d("Cancelamento", "Batch commit bem-sucedido. Reserva cancelada.")
                _uiState.value = ReservaUIState.Finished("Reserva cancelada com sucesso.")

            } catch (e: Exception) {
                Log.e("Cancelamento", "ERRO CRÍTICO durante o cancelamento: ${e.message}", e)
                _uiState.value = ReservaUIState.Error("Erro ao cancelar: ${e.message}")
            }
        }
    }

    private suspend fun buscarVeiculoPadrao(): Veiculo? {
        val userId = auth.currentUser?.uid ?: return null
        return db.collection("veiculo")
            .whereEqualTo("usuarioId", userId)
            .whereEqualTo("padrao", true)
            .limit(1).get().await()
            .toObjects(Veiculo::class.java).firstOrNull()
    }

    private suspend fun buscarReservaAtiva(vagaId: String): Reserva? {
        val userId = auth.currentUser?.uid ?: return null
        val snapshot = db.collection("reserva")
            .whereEqualTo("usuarioId", userId)
            .whereEqualTo("vagaId", vagaId)
            .whereEqualTo("status", "ativa")
            .limit(1)
            .get()
            .await()

        if (snapshot.isEmpty) return null

        val doc = snapshot.documents.first()
        return doc.toObject(Reserva::class.java)?.apply {
            id = doc.id
        }
    }

    private fun iniciarTimer(dataFim: Date) {
        timer?.cancel()
        val tempoRestanteMs = dataFim.time - System.currentTimeMillis()
        if (tempoRestanteMs <= 0) {
            _tempoRestante.postValue("00:00:00")
            return
        }
        timer = object : CountDownTimer(tempoRestanteMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val horas = TimeUnit.MILLISECONDS.toHours(millisUntilFinished)
                val minutos = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60
                val segundos = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                _tempoRestante.postValue(String.format("%02d:%02d:%02d", horas, minutos, segundos))
            }
            override fun onFinish() {
                _tempoRestante.postValue("Expirada")
            }
        }.start()
    }

    override fun onCleared() {
        super.onCleared()
        timer?.cancel()
    }
}