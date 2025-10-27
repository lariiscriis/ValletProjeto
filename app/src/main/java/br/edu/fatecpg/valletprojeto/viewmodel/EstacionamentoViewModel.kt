package br.edu.fatecpg.valletprojeto.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.fatecpg.valletprojeto.dao.EstacionamentoDao
import br.edu.fatecpg.valletprojeto.model.Endereco
import br.edu.fatecpg.valletprojeto.model.Estacionamento
import br.edu.fatecpg.valletprojeto.repository.CepRepository
import kotlinx.coroutines.launch

class EstacionamentoViewModel : ViewModel() {

    private val cepRepository = CepRepository()
    fun cadastrar(
        est: Estacionamento,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        EstacionamentoDao.EstacionamentoDao.cadastrarEstacionamento(est, onSuccess, onFailure)
    }
    fun buscarCep(
        cep: String,
        onSuccess: (Endereco) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = cepRepository.buscarEndereco(cep)
            result
                .onSuccess { endereco -> onSuccess(endereco) }
                .onFailure { ex -> onError(ex.message ?: "Erro desconhecido") }
        }
    }
}
