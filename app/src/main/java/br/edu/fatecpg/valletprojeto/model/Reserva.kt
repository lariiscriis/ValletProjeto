package br.edu.fatecpg.valletprojeto.model

import com.google.firebase.Timestamp

data class Reserva(
    val usuarioId: String = "",
    val vagaId: String = "",
    val inicioReserva: Timestamp? = null,
    val fimReserva: Timestamp? = null,
    val estacionamentoId : String = "",
    val status: String = "ativa"
)
