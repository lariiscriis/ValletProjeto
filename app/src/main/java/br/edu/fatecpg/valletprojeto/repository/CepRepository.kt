package br.edu.fatecpg.valletprojeto.repository

import br.edu.fatecpg.valletprojeto.model.Endereco
import br.edu.fatecpg.valletprojeto.service.RetrofitClient

class CepRepository {
    private val api = RetrofitClient.viaCepService

    suspend fun buscarEndereco(cep: String): Result<Endereco> {
        return try {
            val response = api.buscarEndereco(cep)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erro ao buscar CEP: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
