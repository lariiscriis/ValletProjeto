package br.edu.fatecpg.valletprojeto.viewmodel
import br.edu.fatecpg.valletprojeto.dao.VagaDao
import androidx.lifecycle.ViewModel
import br.edu.fatecpg.valletprojeto.model.Vaga

class VagaViewModel : ViewModel() {
    private val VagaDao = VagaDao()
    fun cadastrarVaga(
        vaga: Vaga,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        VagaDao.cadastrarVaga(vaga, onSuccess, onFailure)
    }


}