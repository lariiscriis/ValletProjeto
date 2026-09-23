package br.edu.fatecpg.valletprojeto.model

import java.text.SimpleDateFormat
import java.util.*
data class VagaOcupada(
    val numeroVaga: String = "",
    val motoristaNome: String = "",
    val motoristaEmail: String = "",
    val motoristaTelefone: String = "",
    val motoristaFoto: String? = null,
    val carroModelo: String = "",
    val carroPlaca: String = "",
    val horaInicio: String = "",
    val horaFim: String = "",
    val localizacao: String = "",
    val preferencial: Boolean = false
) {
    val horaInicioDate: Date
        get() = try { SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH).parse(horaInicio) ?: Date() } catch (e: Exception) { Date() }

    val horaFimDate: Date
        get() = try { SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH).parse(horaFim) ?: Date() } catch (e: Exception) { Date() }
}
