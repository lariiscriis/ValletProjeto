package br.edu.fatecpg.valletprojeto.model

data class Carro(
    val placa: String = "",
    val marca: String = "",
    val modelo: String = "",
    val ano: String = "",
    val km: String="",
    var id: String = "",
    var usuarioEmail: String = "",
    var dataCadastro: String = ""
)
