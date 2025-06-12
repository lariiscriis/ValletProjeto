package br.edu.fatecpg.valletprojeto.model

data class Endereco(
    val cep: String,
    val logradouro: String,
    val complemento: String?,
    val bairro: String,
    val localidade: String,
    val uf: String
)
