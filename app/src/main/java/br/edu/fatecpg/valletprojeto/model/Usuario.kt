package br.edu.fatecpg.valletprojeto.model

data class Usuario(
    val email: String = "",
    val nome: String = "",
    val senha: String = "",
    val tipo_user: String = "",
    val cnh: String? = null,
    val nome_empresa: String? = null,
    val cargo: String? = null
)
