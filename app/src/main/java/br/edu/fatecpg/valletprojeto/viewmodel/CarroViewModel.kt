package br.edu.fatecpg.valletprojeto.viewmodel

import androidx.lifecycle.ViewModel
import br.edu.fatecpg.valletprojeto.dao.CarroDao
import br.edu.fatecpg.valletprojeto.model.Carro

class CarroViewModel : ViewModel() {

    fun cadastrarCarro(
        carro: Carro,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        CarroDao.cadastrarCarro(carro, onSuccess, onFailure)
    }
}
