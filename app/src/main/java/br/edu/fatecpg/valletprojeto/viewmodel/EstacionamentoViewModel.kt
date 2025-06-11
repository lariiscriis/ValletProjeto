package br.edu.fatecpg.valletprojeto.viewmodel

import Estacionamento
import androidx.lifecycle.ViewModel
import br.edu.fatecpg.valletprojeto.dao.EstacionamentoDao

class EstacionamentoViewModel : ViewModel() {
    fun cadastrar(est: Estacionamento, onSuccess: ()->Unit, onFailure: (String)->Unit) {
        EstacionamentoDao.EstacionamentoDao.cadastrarEstacionamento(est, onSuccess, onFailure)
    }

    fun listar(onSuccess: (List<Estacionamento>)->Unit, onFailure: (String)->Unit) {
        EstacionamentoDao.EstacionamentoDao.listarPorAdmin(onSuccess, onFailure)
    }
}
