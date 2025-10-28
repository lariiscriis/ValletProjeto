package br.edu.fatecpg.valletprojeto.model

data class Veiculo(
    val placa: String = "",
    val marca: String = "",
    val modelo: String = "",
    val apelido: String = "",
    val ano: String = "",
    val km: String="",
    val tipo: String="",
    var id: String = "",
    var usuarioEmail: String = "",
    var dataCadastro: String = ""
)
