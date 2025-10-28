package br.edu.fatecpg.valletprojeto.viewmodel

import androidx.lifecycle.ViewModel
import br.edu.fatecpg.valletprojeto.dao.UsuarioDao
import br.edu.fatecpg.valletprojeto.dao.VeiculoDao
import br.edu.fatecpg.valletprojeto.model.Veiculo

class VeiculoViewModel(usuarioDao: UsuarioDao, veiculoDao: VeiculoDao) : ViewModel() {

    fun cadastrarVeiculo(
        veiculo: Veiculo,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        VeiculoDao.cadastrarVeiculo(veiculo, onSuccess, onFailure)
    }

    fun listarVeiculosDoUsuario(
        onSuccess: (List<Veiculo>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        VeiculoDao.listarVeiculosDoUsuario(onSuccess, onFailure)
    }

}
